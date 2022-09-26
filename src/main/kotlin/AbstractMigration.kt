import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import entity.*
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import mu.KotlinLogging
import util.sendRequest

val LOGGER = KotlinLogging.logger {}
val VERTX: Vertx = Vertx.vertx()

abstract class AbstractMigration {
    val mapper = jacksonObjectMapper()
    val jsonFactory: JsonNodeFactory = JsonNodeFactory.instance

    abstract fun migrate()

    /**
     * 查找节点关联的运行中任务
     */
    fun getTaskIds(srcId: Int): List<Int> {
        val taskIds = mutableListOf<Int>()
        val dpTasksResp = sendRequest(Config.DP, HttpMethod.GET, "/v3/data-nodes/${srcId}/relation-tasks")
        val apiResult = mapper.readValue(dpTasksResp.bodyAsString(), ApiResult::class.java)
        val apiData = mapper.readValue(jsonFactory.pojoNode(apiResult.data).toString(), ApiData::class.java)
        apiData.items.forEach { e ->
            val task = mapper.convertValue(e, DpDataTask::class.java)
            if (task.state == DpDataTaskState.ACTIVE) {
                task.srcNodes?.forEach {
                    val srcNode = mapper.convertValue(it, DpSrcNode::class.java)
                    if (!srcNode.removed && srcNode.node.id == srcId && srcNode.node.state == DpDataNodeState.ACTIVE) {
                        taskIds.add(task.id)
                    }
                } ?: throw Exception("No srcNodes for node [${srcId}]")
            }
        }
        return taskIds
    }
}