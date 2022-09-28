package com.datapipeline.agent

import com.datapipeline.agent.entity.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.vertx.core.http.HttpMethod.GET
import io.vertx.core.http.HttpMethod.POST
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonArray
import com.datapipeline.agent.util.cutHalf
import com.datapipeline.agent.util.getOracleConf
import com.datapipeline.agent.util.sendRequest
import com.datapipeline.agent.util.swapAndWrite

class ConfigMigration : AbstractMigration(), Migrate {

    override fun migrate() {
        try {
            // 获取旧 agent 的数据库配置信息
            val oldConfResp = sendRequest(Config.NEW_AGENT, GET, "/config/old?path=${old_conf[OldConfSpec.path]}")
            val oldConf = oldConfResp.bodyAsJson(ExportConfig::class.java)
            // 持久化
            val srcId = oldConf.src_id.toInt()
            val src = jsonFactory.objectNode().put("id", srcId).put("login", oldConf.src_login)
            val jsonObj = jsonFactory.objectNode().also { it.set<JsonNode>("src", src) }
            swapAndWrite(RESULT_CONF_PATH, jsonObj.toPrettyString())
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
                    getTaskIds(srcId).forEach {
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
                // 修改新 agent 的 oracle.yml
                val oracleNodeConfig = getOracleConf(oldConf.src_login)
                val asmCut = oldConf.asm_login.cutHalf("@")
                val asmLogin = asmCut.first.cutHalf("/")
                val mode = oldConf.asm_mode.uppercase()
                val oragentConfig = OragentConfig(
                    srcId, Mode.valueOf(mode),
                    connectionString = asmCut.second,
                    asmUser = asmLogin.first,
                    asmPassword = asmLogin.second,
                    oracleHome = oldConf.asm_oracle_home,
                    sid = oldConf.asm_oracle_sid
                )
                oracleNodeConfig.oragentConfig = oragentConfig
                val asm = jsonFactory.objectNode().put("mode", mode)
                    .put("connectionString", oragentConfig.connectionString)
                    .put("asmUser", oragentConfig.asmUser)
                    .put("asmPassword", oragentConfig.asmPassword)
                    .put("oracleHome", oragentConfig.oracleHome)
                    .put("sid", oragentConfig.sid)
                jsonObj.set<JsonNode>("asm", asm)
                swapAndWrite(RESULT_CONF_PATH, jsonObj.toPrettyString())
                sendRequest(Config.NEW_AGENT, POST, "/config/", JsonNodeFactory.instance.pojoNode(oracleNodeConfig))
                onComplete("配置迁移执行完成", mapOf("CONTINUE" to "true"))
            }
        } catch (e: Throwable) {
            onError(e)
        }
    }

    override fun onError(e: Throwable, args: Map<String, Any>?) {
        LOGGER.error("Exception:${e.message}", e)
        sendRequest(Config.NEW_AGENT, POST, "/config/", JsonNodeFactory.instance.pojoNode(OracleNodeConfig.DEFAULT))
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