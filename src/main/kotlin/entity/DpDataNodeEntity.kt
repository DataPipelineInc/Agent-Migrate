package entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.vertx.core.json.JsonObject

data class DpDataNodeEntity(
    val id: Int,
    val nodeId: Int,
    val name: String,
    val schema: String,
    val database: String,
    val type: String,
    @JsonIgnore val basicConfig: JsonNode? = null,
    @JsonIgnore val status: String? = null,
    @JsonIgnore val fields: List<JsonNode>? = null,
    @JsonIgnore val indices: List<JsonNode>? = null,
    @JsonIgnore val hasPk: Boolean = false,
    @JsonIgnore val deleted: Boolean = false,
    @JsonIgnore val comment: String? = null,
    @JsonIgnore val createdAt: String? = null,
    @JsonIgnore val createdBy: JsonObject? = null,
    @JsonIgnore val updatedAt: String? = null,
    @JsonIgnore val updatedBy: JsonObject? = null,
    @JsonIgnore val editable: Boolean? = null,
    @JsonIgnore val deletable: Boolean = false,
    @JsonIgnore val projectId: Int? = null,
) {
    constructor() : this(0, 0, "", "", "", "")
}
