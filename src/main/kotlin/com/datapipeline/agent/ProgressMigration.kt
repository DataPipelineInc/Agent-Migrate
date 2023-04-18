package com.datapipeline.agent

import com.datapipeline.agent.entity.Savepoint
import io.vertx.core.http.HttpMethod.GET
import io.vertx.core.http.HttpMethod.POST
import io.vertx.core.json.JsonArray
import com.datapipeline.agent.util.sendRequest
import kotlin.math.min

class ProgressMigration : AbstractMigration(), Migrate {

    override fun migrate() {
        try {
            check(srcId)
            // 停止旧 agent
            val stopResp = sendRequest(Config.OLD_AGENT, POST, "/fzsstop")
            // 读取旧 agent 的 cfg.loginfo
            val oldSavepointResp = sendRequest(
                Config.NEW_AGENT,
                GET,
                "/migrate/legacy/1/savepoint?path=${old_conf[OldConfSpec.path]}"
            )
            // 读取旧 agent 的 translist
            val oldTransResp = sendRequest(
                Config.NEW_AGENT,
                GET,
                "/migrate/legacy/1/translist/scn?path=${old_conf[OldConfSpec.path]}"
            )
            val savepoints = arrayListOf<Savepoint>()
            val scnArray = oldTransResp.bodyAsJsonArray()
            if (!scnArray.isEmpty) {
                // translist 存在数据（包含未提交事务）
                // 寻找最小 scn
                val minScn = scnArray.minOf { it.toString().toLong() }
                // 调用新agent 接口，在数据库查询对应的 Sequence
                val savepointMap = sortedMapOf<Int, Long>()
                val redoLogInfoList =
                    sendRequest(Config.NEW_AGENT, GET, "/tools/logs?startScn=$minScn").bodyAsJsonArray()
                        .takeIf { it.isEmpty.not() }
                        ?: sendRequest(
                            Config.NEW_AGENT,
                            GET,
                            "/tools/logs?startScn=$minScn&reset=EARLIEST"
                        ).bodyAsJsonArray()
                redoLogInfoList.forEach {
                    val savepoint = mapper.readValue(it.toString(), Savepoint::class.java)
                    savepointMap.merge(savepoint.thr, savepoint.seq) { oldVal, newVal -> min(oldVal, newVal) }
                }
                savepointMap.forEach {
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
            sendRequest(Config.NEW_AGENT, POST, "/export/savepoint", JsonArray(savepoints))
            onComplete("进度迁移执行完成", null)
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun onError(e: Throwable, args: Map<String, Any>?) {
        LOGGER.error("Exception:${e.message}", e)
        sendRequest(Config.NEW_AGENT, POST, "/export/savepoint", JsonArray())
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