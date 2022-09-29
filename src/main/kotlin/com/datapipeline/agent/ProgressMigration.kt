package com.datapipeline.agent

import com.datapipeline.agent.entity.Savepoint
import com.datapipeline.agent.entity.Trans
import io.vertx.core.http.HttpMethod.GET
import io.vertx.core.http.HttpMethod.POST
import io.vertx.core.json.JsonArray
import com.datapipeline.agent.util.sendRequest
import kotlin.math.min

class ProgressMigration : AbstractMigration(), Migrate {

    override fun migrate() {
        try {
            val conf_result = com.uchuhimo.konf.Config {
                addSpec(SrcSpec)
                addSpec(AsmSpec)
            }.from.json.file(RESULT_CONF_PATH, true)
                .from.env()
            val srcId = conf_result[SrcSpec.id]
            // 停止旧 agent
            val stopResp = sendRequest(Config.OLD_AGENT, POST, "/fzsstop", timeoutMs = 60000L)
            val stopBody = stopResp.bodyAsString()
            if (!stopBody.contains("停止成功")) {
                throw Exception("Failed to stop old agent, Response Content: [$stopBody]")
            }
            // 读取旧 agent 的 cfg.loginfo
            val oldSavepointResp = sendRequest(
                Config.NEW_AGENT,
                GET,
                "/export/old/savepoint/${srcId}?path=${old_conf[OldConfSpec.path]}"
            )
            // 读取旧 agent 的 translist
            val oldTransResp = sendRequest(
                Config.NEW_AGENT,
                GET,
                "/export/old/translist/${srcId}?path=${old_conf[OldConfSpec.path]}"
            )
            val savepoints = arrayListOf<Savepoint>()
            val transArray = oldTransResp.bodyAsJsonArray()
            if (!transArray.isEmpty) {
                // translist 存在数据（包含未提交事务）
                sendRequest(Config.NEW_AGENT, POST, "/export/translist", transArray)
                // 寻找每个 Thread 所对应的最小 scn
                val minScnList = mutableMapOf<Int, Long>()
                for (i in 0 until transArray.size()) {
                    val trans = mapper.readValue(transArray.getJsonObject(i).toString(), Trans::class.java)
                    val threadId = trans.thread_id
                    val minScn = minScnList[threadId]
                    minScnList[threadId] = if (minScn != null) min(minScn, trans.scn_bgn.scn) else trans.scn_bgn.scn
                }
                val minScnJson = jsonFactory.objectNode().also {
                    minScnList.forEach { (t, u) -> it.put(t.toString(), u) }
                }
                // 调用新agent 接口，在数据库查询对应的 Sequence
                sendRequest(Config.NEW_AGENT, POST, "/tools/seqByScn", minScnJson).bodyAsJsonObject().onEach {
                    savepoints.add(Savepoint(it.key.toInt(), it.value.toString().toLong(), 0, 0, 0))
                }
            } else {
                // translist 无数据，则直接使用旧 agent 的 savepoint 信息
                val array =
                    oldSavepointResp.bodyAsJsonArray().takeUnless { it.isEmpty } ?: throw Exception("Empty Savepoint")
                array.forEachIndexed { i, _ ->
                    savepoints.add(mapper.readValue(array.getJsonObject(i).toString(), Savepoint::class.java))
                }
            }
            LOGGER.info { "Got Savepoints : $savepoints" }
            // 修改新 agent 的 cfg.loginfo
            sendRequest(Config.NEW_AGENT, POST, "/export/savepoint/full", JsonArray(savepoints))
            onComplete("进度迁移执行完成", null)
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun onError(e: Throwable, args: Map<String, Any>?) {
        LOGGER.error("Exception:${e.message}", e)
        sendRequest(Config.NEW_AGENT, POST, "/export/savepoint/full", JsonArray())
        sendRequest(Config.NEW_AGENT, POST, "/export/translist", JsonArray())
        sendRequest(Config.OLD_AGENT, POST, "/fzsstart")
        throw e
    }

    override fun onComplete(msg: String, args: Map<String, String>?) {
        LOGGER.info { msg }
        val dataMigration = DataMigration()
        dataMigration.migrate()
        return
    }

}

fun main(args: Array<String>) {
    try {
        val progressMigration = ProgressMigration()
        progressMigration.migrate()
    } catch (e: Throwable) {
        throw e
    } finally {
        VERTX.close()
    }
}