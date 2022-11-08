package com.datapipeline.agent

import com.datapipeline.agent.entity.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.vertx.core.http.HttpMethod.GET
import io.vertx.core.http.HttpMethod.POST
import io.vertx.core.http.HttpMethod.PUT
import io.vertx.core.json.JsonArray
import org.apache.kafka.clients.admin.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.ConfigResource
import com.datapipeline.agent.util.sendRequest
import com.datapipeline.agent.util.swapAndWrite
import java.time.Duration
import java.util.Properties
import java.util.function.Function
import java.util.stream.Collectors

class DataMigration : AbstractMigration(), Migrate {

    private val srcId = new_conf[NewConfSpec.src_id]
    private val useString = new_conf[NewConfSpec.migrate_topic_format].equals("string", true)
    private val legacyFormatParam = HashMap<Any, Any>().also {
        it["key"] = "LEGACY_FORMAT"
        it["value"] = useString.toString()
    }

    override fun migrate() {
        val errorArgs = mutableMapOf<String, Any>()
        try {
            val confResult = com.uchuhimo.konf.Config {
                addSpec(SrcSpec)
                addSpec(AsmSpec)
            }.from.json.file(RESULT_CONF_PATH, true)
                .from.env()
            val taskIds = getTaskIds(srcId)

            if (!useString) {
                // 初始化 Kafka 配置
                val properties = Properties()
                properties["bootstrap.servers"] = dp_conf[DpConfSpec.kafka_bootstrap_servers]
                properties["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
                properties["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
                properties["auto.offset.reset"] = "earliest"
                // 新建消费者消费，查找 nine.offset
                val consumer = KafkaConsumer<String, String>(properties)
                val offsetTopic = TopicPartition("offset_connect_source_dp", 1)
                consumer.assign(listOf(offsetTopic))
                consumer.seekToBeginning(listOf(offsetTopic))

                val nineOffsetMap = hashMapOf<String, Long>()
                val nineOffsetStart = System.currentTimeMillis()
                while (true) {
                    val poll = consumer.poll(Duration.ZERO)
                    val records = poll.records(offsetTopic)
                    records.forEach {
                        // Key formats like: ["dp-oracle-connector-dptask_<taskId>_1",{"table":"<taskId>.<database>.<schema>.<table>"}]
                        val key = JsonArray(it.key())
                        val table = key.getJsonObject(1).getString("table")
                        val tableInfo = table.split(".", limit = 4)
                        val taskId = tableInfo[0].toInt()
                        // Value format: JSON
                        val offsetValue = mapper.readValue(it.value(), OffsetValue::class.java)
                        if (taskIds.contains(taskId)) {
                            nineOffsetMap[table] = offsetValue.nine_offset
                        }
                    }

                    if (records.isNotEmpty()) {
                        break
                    } else {
                        if (System.currentTimeMillis() - nineOffsetStart > 10000L) {
                            throw Exception("消费 offset_connect_source_dp 超时")
                        }
                    }
                }

                val topicList = arrayListOf<TopicPartition>()
                val topicNameList = arrayListOf<JsonNode>()
                val offsetTopicMap = hashMapOf<TopicPartition, String>()
                nineOffsetMap.mapTo(topicList) { offset ->
                    val tableInfo = offset.key.split(".", limit = 4)
                    val topicName = "dp_agent_${srcId}_${tableInfo[2]}_${tableInfo[3]}"
                    topicNameList.add(TextNode(topicName))
                    TopicPartition(topicName, 0).also { offsetTopicMap[it] = offset.key }
                }
                // 持久化 agent topic 名称及 nine.offset
                val nineOffsets = jsonFactory.objectNode().also {
                    nineOffsetMap.forEach { offset -> it.put(offset.key, offset.value) }
                }
                val jsonObj = jsonFactory.objectNode().also {
                    it.set<JsonNode>("topicNames", ArrayNode(jsonFactory, topicNameList))
                    it.set<JsonNode>("nineOffsets", nineOffsets)
                }
                swapAndWrite(RESULT_DATA_PATH, jsonObj.toPrettyString())
                // 存储 nine.offset != endOffset 的 topic
                val remainingTopics = arrayListOf<TopicPartition>().also { it.addAll(topicList) }
                val offsetEqualStart = System.currentTimeMillis()
                consumer.assign(topicList)
                // 等待所有的 topic 都满足 nine.offset == endOffset
                val endOffsetMap = hashMapOf<String, JsonNode>()
                while (remainingTopics.isNotEmpty()) {
                    val endOffsets = consumer.endOffsets(topicList)
                    endOffsets.forEach { (t, endOffset) ->
                        endOffsetMap[t.topic()] = LongNode(endOffset)
                        LOGGER.info { "Topic end offset: ${t.topic()} - $endOffset" }
                        val nineOffset = offsetTopicMap[t]?.let {
                            nineOffsetMap[it]
                        } ?: throw Exception("Failed to find nine offset of [${t.topic()}]")
                        LOGGER.info { "Topic nine offset: ${t.topic()} - $nineOffset" }
                        if (endOffset == nineOffset) {
                            remainingTopics.remove(t)
                        }
                    }
                    Thread.sleep(1000L)
                    if (System.currentTimeMillis() - offsetEqualStart > 10000L) {
                        jsonObj.set<JsonNode>("endOffsets", ObjectNode(jsonFactory, endOffsetMap))
                        swapAndWrite(RESULT_DATA_PATH, jsonObj.toPrettyString())
                        val topicNames =
                            remainingTopics.stream().map(TopicPartition::topic).collect(Collectors.toList())
                        throw Exception("等待 [nine.offset == topic.endOffset] 超时, Topics: [${topicNames.joinToString()}]")
                    }
                }
                jsonObj.set<JsonNode>("endOffsets", ObjectNode(jsonFactory, endOffsetMap))
                swapAndWrite(RESULT_DATA_PATH, jsonObj.toPrettyString())
                // 获取已有的动态 topic 配置并持久化
                val adminClient = AdminClient.create(properties)
                val existedConfigMap = hashMapOf<String, MutableMap<String, JsonNode>>()
                val topics =
                    topicNameList.map { ConfigResource(ConfigResource.Type.TOPIC, (it as TextNode).textValue()) }
                val describeConfigs = adminClient.describeConfigs(topics)
                describeConfigs.all().get().forEach { (t, u) ->
                    existedConfigMap[t.name()] =
                        u.entries().filter { it.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG }.stream()
                            .collect(Collectors.toMap(ConfigEntry::name) { entry -> TextNode(entry.value()) })
                }

                val objectNode = jsonFactory.objectNode()
                existedConfigMap.forEach { (t, u) ->
                    objectNode.set<JsonNode>(t, ObjectNode(jsonFactory, u))
                }
                jsonObj.set<JsonNode>("topicConfigs", objectNode)
                swapAndWrite(RESULT_DATA_PATH, jsonObj.toPrettyString())

                // 缩短数据的保留时间为1s，topic中的数据会很快被标记为删除状态，直至log cleaner将其删除
                adminClient.alterConfigs(alterRetentionMs(topics, existedConfigMap, 1000L))

                /*topicNameList.map { (it as TextNode).textValue() }.forEach {
                val retention = ConfigResource(ConfigResource.Type.TOPIC, it)
                val retentionEntry = ConfigEntry("retention.ms", "1000")
                val alterReq = AlterConfigOp(retentionEntry, AlterConfigOp.OpType.APPEND)
                //val configs = hashMapOf<ConfigResource, Collection<AlterConfigOp>>(retention to listOf(alterReq))
                //AlterConfigsRequest.Builder(mapOf(retention to AlterConfigsRequest.Config(listOf(retentionEntry))), false)
                val result = AdminClient.create(properties).incrementalAlterConfigs(mapOf(retention to listOf(alterReq)))
                }*/

                // 存储数据未全部清除的 topic
                val remainingTopicsNew = arrayListOf<TopicPartition>().also { it.addAll(topicList) }
                val dataDeleteStart = System.currentTimeMillis()
                consumer.assign(topicList)
                while (remainingTopicsNew.isNotEmpty()) {
                    val beginningOffsets = consumer.beginningOffsets(topicList)
                    val endOffsets = consumer.endOffsets(topicList)
                    val startOffsetMap = hashMapOf<String, Long>()
                    val endOffsetsMap = hashMapOf<String, Long>()
                    LOGGER.info { "==== Loop ====" }
                    beginningOffsets.forEach { (t, begin) ->
                        startOffsetMap[t.topic()] = begin
                        LOGGER.info { "Topic start offset: ${t.topic()} - $begin" }
                    }
                    endOffsets.forEach { (t, endOffset) ->
                        val startOffset =
                            startOffsetMap[t.topic()]
                                ?: throw Exception("Start offset of topic: [${t.topic()}] not found")
                        LOGGER.info { "Topic end offset: ${t.topic()} - $endOffset" }
                        if (endOffset == startOffset) {
                            remainingTopicsNew.remove(t)
                        } else {
                            endOffsetsMap[t.topic()] = endOffset
                        }
                    }
                    LOGGER.info { "==== Loop End ====" }
                    Thread.sleep(5000L)
                    if (System.currentTimeMillis() - dataDeleteStart > 360000L) {
                        adminClient.alterConfigs(alterRetentionMs(topics, existedConfigMap))
                        val topicOffsets = jsonFactory.objectNode()
                        val topicNames =
                            remainingTopicsNew.stream().map(TopicPartition::topic).collect(Collectors.toList())
                        topicNames.forEach {
                            topicOffsets.set<ArrayNode>(
                                it,
                                ArrayNode(
                                    jsonFactory,
                                    listOf(
                                        TextNode(startOffsetMap[it].toString()),
                                        TextNode(endOffsetsMap[it].toString())
                                    )
                                )
                            )
                        }
                        jsonObj.set<JsonNode>("topicOffsets", topicOffsets)
                        swapAndWrite(RESULT_DATA_PATH, jsonObj.toPrettyString())
                        throw Exception("等待数据删除超时, Topics: [${topicNames.joinToString()}]")
                    }
                }

                // 恢复 topic 的配置
                adminClient.alterConfigs(alterRetentionMs(topics, existedConfigMap))
            }

            // 暂停对应任务
            taskIds.forEach {
                sendRequest(Config.DP, POST, "/v3/data-tasks/${it}/suspend?mode=NORMAL")
            }
            // 等待任务暂停
            val notSuspendedTaskIds = arrayListOf<Int>().also { it.addAll(taskIds) }
            val taskSuspendStart = System.currentTimeMillis()
            while (notSuspendedTaskIds.isNotEmpty()) {
                taskIds.forEach {
                    val taskBody = sendRequest(Config.DP, GET, "/v3/data-tasks/${it}")
                    val apiResult = mapper.readValue(taskBody.bodyAsString(), ApiResult::class.java)
                    val taskInfo = mapper.readValue(jsonFactory.pojoNode(apiResult.data).toString(), DpDataTask::class.java)
                    if (taskInfo.state == DpDataTaskState.SUSPEND) {
                        notSuspendedTaskIds.remove(it)
                    }
                }
                if (System.currentTimeMillis() - taskSuspendStart > 60000L) {
                    taskIds.forEach {
                        sendRequest(Config.DP, POST, "/v3/data-tasks/$it/restart")
                    }
                    throw Exception("等待任务暂停超时, 任务ID列表: [${notSuspendedTaskIds.joinToString()}]")
                }
            }
            // 修改节点配置
            val dataNodeResp = sendRequest(Config.DP, GET, "/v3/data-nodes/${srcId}")
            val apiResult = mapper.readValue(dataNodeResp.bodyAsString(), ApiResult::class.java)
            val nodeConfig = mapper.readValue(jsonFactory.pojoNode(apiResult.data).toString(), DpDataNode::class.java)
            val basicConfig = nodeConfig.basicConfig!!
            sendRequest(Config.DP, PUT, "/v3/data-nodes/${srcId}", getUpdateNodeJson(basicConfig, confResult))
            // 节点可用性校验
            val initResp = sendRequest(Config.DP, POST, "/v3/data-nodes/${srcId}/init?modes=ORAGENT")
            val initBody = mapper.readValue(initResp.bodyAsString(), ApiResult::class.java)
            val supportInfo =
                mapper.readValue(jsonFactory.pojoNode(initBody.data).toString(), DpDataNodeSupportInfo::class.java)
            if (supportInfo.exceptionMessages.isNotEmpty()) {
                throw Exception("校验节点可用性失败: 异常信息: ${supportInfo.exceptionMessages}")
            }
            // 查看关联链路并修改其读取模式
            val linkConfig = hashMapOf<Int, DpSrcNode>()
            val linksResp = sendRequest(Config.DP, GET, "/v3/data-nodes/${srcId}/relation-links")
            val linksBody = mapper.readValue(linksResp.bodyAsString(), ApiResult::class.java)
            val apiData = mapper.readValue(jsonFactory.pojoNode(linksBody.data).toString(), ApiData::class.java)
            apiData.items.forEach { e ->
                val link = mapper.convertValue(e, DpDataLink::class.java)
                val srcNodes = link.srcNodes ?: throw Exception("No src node for link: ${link.id}")
                srcNodes.filter { it.node.id == srcId }.forEach {
                    it.basicConfig =
                        DpSrcNodeBasicConfig("com.datapipeline.internal.bean.DataNodeRelationSrcBasicConfig", "ORAGENT")
                    it.node.basicConfig = nodeConfig.basicConfig
                    it.removed = false
                    linkConfig[link.id] = it
                    val reqBody = jsonFactory.objectNode()
                        .set<JsonNode>("srcNodes", ArrayNode(jsonFactory, listOf(jsonFactory.pojoNode(it))))
                    sendRequest(Config.DP, PUT, "/v3/data-links/${link.id}", reqBody)
                }
            }
            errorArgs["linkConfig"] = linkConfig
            // 启动新 agent ，重启对应任务
            sendRequest(Config.NEW_AGENT, POST, "/export/start")
            taskIds.forEach {
                sendRequest(Config.DP, POST, "/v3/data-tasks/$it/restart")
            }
            onComplete("数据迁移执行完成", null)
        } catch (e: Throwable) {
            onError(e, errorArgs)
        }
    }

    override fun onError(e: Throwable, args: Map<String, Any>?) {
        LOGGER.error("Exception:${e.message}", e)
        if (!args.isNullOrEmpty()) {
            args["linkConfig"]?.let {
                val config = it as Map<Int, DpSrcNode>
                config.forEach { (linkId, config) ->
                    config.basicConfig =
                        DpSrcNodeBasicConfig(
                            "com.datapipeline.internal.bean.DataNodeRelationSrcBasicConfig",
                            "ORACLE_AGENT"
                        )
                    val reqBody = jsonFactory.objectNode()
                        .set<JsonNode>("srcNodes", ArrayNode(jsonFactory, listOf(jsonFactory.pojoNode(config))))
                    sendRequest(Config.DP, PUT, "/v3/data-links/$linkId", reqBody)
                }
            }
        }
        sendRequest(Config.OLD_AGENT, POST, "/fzsstart")
        throw e
    }

    override fun onComplete(msg: String, args: Map<String, String>?) {
        LOGGER.info { msg }
        return
    }

    fun getUpdateNodeJson(basicConfig: DpDataNodeBasicConfig, confResult: com.uchuhimo.konf.Config): ObjectNode {
        basicConfig.oragentConfig = OragentConfig(
            srcId,
            Mode.valueOf(confResult[AsmSpec.mode]),
            new_conf[NewConfSpec.host],
            new_conf[NewConfSpec.web_port],
            confResult[AsmSpec.connectionString],
            confResult[AsmSpec.asmUser],
            confResult[AsmSpec.asmPassword],
            confResult[AsmSpec.oracleHome],
            confResult[AsmSpec.sid],
            confResult[AsmSpec.asmDisks],
            confResult[AsmSpec.bigEndian]
        )
        val params = basicConfig.params
        if (params == null) {
            basicConfig.params = arrayListOf(legacyFormatParam)
        } else {
            params.add(legacyFormatParam)
        }
        return jsonFactory.objectNode().also {
            it.put("type", "ORACLE")
            it.set<JsonNode>("basicConfig", jsonFactory.pojoNode(basicConfig))
        }
    }

}

fun alterRetentionMs(
    topics: List<ConfigResource>,
    configMap: Map<String, MutableMap<String, JsonNode>>,
    retentionMs: Long = -1L
): MutableMap<ConfigResource, org.apache.kafka.clients.admin.Config> =
    topics.stream().collect(Collectors.toMap(Function.identity()) {
        val list = arrayListOf<ConfigEntry>()
        val configs = configMap[it.name()] ?: throw Exception("Topic [${it.name()} has no configs]")
        configs["retention.ms"] = TextNode(retentionMs.toString())
        configs.mapTo(list) { config ->
            ConfigEntry(config.key, (config.value as TextNode).textValue())
        }
        Config(list)
    })

fun main(args: Array<String>) {
    try {
        val dataMigration = DataMigration()
        dataMigration.migrate()
    } catch (e: Throwable) {
        throw e
    } finally {
        VERTX.close()
    }
}