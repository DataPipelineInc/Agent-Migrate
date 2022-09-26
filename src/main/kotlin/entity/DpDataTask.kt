package entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.vertx.core.json.JsonObject

data class DpDataTask(
    val id: Int,
    val name: String,
    val state: DpDataTaskState?,
    val srcNodes: List<JsonNode>?,
    val sinkNodes: List<JsonNode>?,
    val basicConfig: JsonObject? = null,
    @JsonIgnore val createdAt: String? = null,
    @JsonIgnore val createdBy: JsonObject? = null,
    @JsonIgnore val updatedAt: String? = null,
    @JsonIgnore val updatedBy: JsonObject? = null,
    @JsonIgnore val editable: Boolean? = null,
    @JsonIgnore val deletable: Boolean = false,
    @JsonIgnore val projectId: Int? = null,
    @JsonIgnore val description: String? = null,
    @JsonIgnore val favorite: Boolean = false,
    @JsonIgnore val participants: JsonObject? = null,
    @JsonIgnore val monitor: Boolean = false,
    @JsonIgnore val summary: JsonObject? = null,
    @JsonIgnore val connectorInfo: JsonObject? = null,
    @JsonIgnore val limitConfig: JsonObject? = null,
    @JsonIgnore val policyConfig: JsonObject? = null,
    @JsonIgnore val dataLink: JsonObject? = null,
) {
    constructor() : this(0, "", DpDataTaskState.ACTIVE, emptyList(), emptyList())
}
