package entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.vertx.core.json.JsonObject

data class DpDataLink(
    val id: Int,
    val name: String,
    val state: DpDataLinkState,
    val srcNodes: List<DpSrcNode>?,
    val sinkNodes: List<JsonNode>?,
    @JsonIgnore val createdAt: String? = null,
    @JsonIgnore val createdBy: JsonObject? = null,
    @JsonIgnore val updatedAt: String? = null,
    @JsonIgnore val updatedBy: JsonObject? = null,
    @JsonIgnore val projectId: Int? = null,
    @JsonIgnore val editable: Boolean? = null,
    @JsonIgnore val deletable: Boolean = false,
    @JsonIgnore val mappingRules: List<JsonObject>? = null,
    @JsonIgnore val relationTasks: List<String>? = null,
    @JsonIgnore val summary: JsonObject? = null,
    @JsonIgnore val limitConfig: JsonObject? = null,
    @JsonIgnore val policyConfig: JsonObject? = null,
    @JsonIgnore val participants: JsonObject? = null,
    @JsonIgnore val description: String? = null,
    @JsonIgnore val favorite: Boolean = false,
)