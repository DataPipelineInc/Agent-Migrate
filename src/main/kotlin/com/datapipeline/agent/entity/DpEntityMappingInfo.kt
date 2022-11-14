package com.datapipeline.agent.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode

data class DpEntityMappingInfo(
    val id: Int,
    val mappingId: Int,
    val srcEntity: DpDataNodeEntity,
    val sinkEntity: DpDataNodeEntity,
    val delete: Boolean = false,
    @JsonIgnore val config: JsonNode? = null,
    @JsonIgnore val group: String? = null,
    @JsonIgnore val fieldMappings: JsonNode? = null,
    @JsonIgnore val srcSchemaChanges: Set<JsonNode>? = null,
    @JsonIgnore val sinkSchemaChanges: Set<JsonNode>? = null,
    @JsonIgnore val states: Set<JsonNode>? = null,
    @JsonIgnore val taskRef: Boolean = true,
    @JsonIgnore val diffs: List<JsonNode>? = null
) {
    constructor() : this(0, 0, DpDataNodeEntity(), DpDataNodeEntity())
}
