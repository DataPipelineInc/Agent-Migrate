package com.datapipeline.agent

import com.datapipeline.agent.entity.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.vertx.core.http.HttpMethod.GET
import io.vertx.core.http.HttpMethod.POST
import io.vertx.core.http.HttpMethod.PUT
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonArray
import com.datapipeline.agent.util.cutHalf
import com.datapipeline.agent.util.getOracleConf
import com.datapipeline.agent.util.sendRequest
import com.datapipeline.agent.util.swapAndWrite
import kotlin.math.min

class ConfigMigration : AbstractMigration(), Migrate {

    override fun migrate() {
        try {
            // 获取旧 agent 同步对象
            val mapResp = sendRequest(Config.OLD_AGENT, GET, "/getusermap")
            val mapArray = mapResp.bodyAsJsonArray()
            val type = mapArray.getJsonObject(0)?.getString("type") ?: throw Exception("Failed to get old map type.")
            val list = arrayListOf<String>()
            when (type) {
                // 表级别，直接获取列表
                "map_table" -> {
                    for (i in 1 until mapArray.size()) {
                        list.add(mapArray.getJsonObject(i).getString("table"))
                    }
                }
                // 用户级别或全库级别，查找关联的运行中任务并获取列表
                "map_user", "map_db" -> {
                    getTaskIds(new_conf[NewConfSpec.src_id]).forEach {
                        val entitiesResp = sendRequest(Config.DP, GET, "/v3/entity/mappings/task/$it")
                        val apiResult = mapper.readValue(entitiesResp.bodyAsString(), ApiResult::class.java)
                        val apiData =
                            mapper.readValue(jsonFactory.pojoNode(apiResult.data).toString(), ApiData::class.java)
                        apiData.items.forEach { e ->
                            val entity = mapper.convertValue(e, DpEntityMappingInfo::class.java)
                            val srcEntity = entity.srcEntity
                            list.add("${srcEntity.schema}.${srcEntity.name}")
                        }
                    }
                }

                else -> throw Exception("Unknown old map type : $type")
            }
            if (list.isEmpty()) {
                onComplete("未检索到符合条件的表", mapOf("CONTINUE" to "false"))
            } else {
                // 修改新 agent 的 map.yml
                sendRequest(Config.NEW_AGENT, POST, "/export/tables", JsonArray(list), 60000L, listOf(200, 201))
                // 获取旧 agent 的数据库配置信息并持久化
                val oldConfResp = sendRequest(Config.NEW_AGENT, GET, "/export/config/legacy?path=${old_conf[OldConfSpec.path]}")
                val oldConf = oldConfResp.bodyAsJson(ExportConfig::class.java)
                val oracleNodeConfig = getOracleNodeConfig(oldConf)
                val jsonObj = jsonFactory.objectNode().also {
                    it.set<JsonNode>("old_conf", jsonFactory.pojoNode(oldConf))
                    it.set<JsonNode>("node_config", jsonFactory.pojoNode(oracleNodeConfig))
                }
                swapAndWrite(RESULT_CONF_PATH, jsonObj.toPrettyString())
                // 修改新 agent 的 oracle.yml
                sendRequest(Config.NEW_AGENT, POST, "/export/config", JsonNodeFactory.instance.pojoNode(oracleNodeConfig))
                // 修改新 agent 的 param.yml
                val useString = new_conf[NewConfSpec.migrate_topic_format].equals("string", true)
                sendRequest(Config.NEW_AGENT, PUT, "/param/legacy_format", JsonNodeFactory.instance.textNode(useString.toString()))
                onComplete("配置迁移执行完成", mapOf("CONTINUE" to "true"))
            }
        } catch (e: Throwable) {
            onError(e)
        }
    }

    fun getOracleNodeConfig(oldConf: ExportConfig): OracleNodeConfig {
        val oracleNodeConfig = getOracleConf(oldConf.src_login)
        val asmCut = oldConf.asm_login.cutHalf("@")
        val asmConn = asmCut.second
        val validAsm = asmConn.isNotEmpty()
        val asmLogin = if (validAsm) asmCut.first.cutHalf("/") else asmCut.first to ""
        val list = arrayListOf<AsmDiskInfo>()
        if (validAsm) {
            if (oldConf.asm_disk.isNullOrEmpty().not() && oldConf.asm_dev.isNullOrEmpty().not()) {
                val asmDisks = oldConf.asm_disk!!.split(" , ")
                val asmDevs = oldConf.asm_dev!!.split(" , ")
                min(asmDisks.size, asmDevs.size).takeIf { it > 0 }?.apply {
                    for (i in 0 until this) {
                        list.add(AsmDiskInfo(asmDisks[i], asmDevs[i]))
                    }
                }
            }
        }
        val exadata = oldConf.param_exadata == "1"
        OragentConfig(
            new_conf[NewConfSpec.src_id],
            Mode.valueOf(oldConf.asm_mode.uppercase()),
            new_conf[NewConfSpec.host],
            new_conf[NewConfSpec.web_port],
            asmConn,
            asmLogin.first,
            asmLogin.second,
            oldConf.asm_oracle_home ?: "",
            oldConf.asm_oracle_sid ?: "",
            list,
            exadata
        ).also { oracleNodeConfig.oragentConfig = it }
        return oracleNodeConfig
    }

    override fun onError(e: Throwable, args: Map<String, Any>?) {
        LOGGER.error("Exception:${e.message}", e)
        sendRequest(Config.NEW_AGENT, POST, "/export/config", JsonNodeFactory.instance.pojoNode(OracleNodeConfig.DEFAULT))
        sendRequest(
            Config.NEW_AGENT,
            POST,
            "/export/tables",
            JsonArray(listOf("SCHEMA.TABLE")),
            60000L,
            listOf(200, 201)
        )
        when (e) {
            is DecodeException -> throw Exception("接口返回信息解析失败")
        }
        throw e
    }

    override fun onComplete(msg: String, args: Map<String, String>?) {
        LOGGER.info { msg }
        args?.get("CONTINUE").takeIf { it.toBoolean() } ?: return
        val progressMigration = ProgressMigration()
        progressMigration.migrate()
        return
    }

}

fun main(args: Array<String>) {
    try {
        val configMigration = ConfigMigration()
        configMigration.migrate()
    } catch (e: Throwable) {
        throw e
    } finally {
        VERTX.close()
    }
}