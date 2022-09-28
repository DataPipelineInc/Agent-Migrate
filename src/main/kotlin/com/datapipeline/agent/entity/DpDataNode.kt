package com.datapipeline.agent.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.vertx.core.json.JsonObject

data class DpDataNode(
    val id: Int,
    val name: String,
    val type: String,
    val state: DpDataNodeState,
    val modes: Set<DpTransmissionMode>?,
    var basicConfig: DpDataNodeBasicConfig? = null,
    @JsonIgnore val unsupportedModes: Set<DpTransmissionMode>? = null,
    @JsonIgnore val createdAt: String? = null,
    @JsonIgnore val createdBy: JsonObject? = null,
    @JsonIgnore val updatedAt: String? = null,
    @JsonIgnore val updatedBy: JsonObject? = null,
    @JsonIgnore val editable: Boolean? = null,
    @JsonIgnore val deletable: Boolean = false,
    @JsonIgnore val description: String? = null,
    @JsonIgnore val favorite: Boolean = false,
    @JsonIgnore val participants: JsonObject? = null,
    @JsonIgnore val connectState: String? = null,
    @JsonIgnore val versionInfo: JsonObject? = null,
    @JsonIgnore val limitConfig: JsonObject? = null,
    @JsonIgnore val policyConfig: JsonObject? = null,
    @JsonIgnore val refType: String? = null,
    @JsonIgnore val summary: JsonObject? = null,
    @JsonIgnore val agentStates: List<JsonNode>? = null,
    @JsonIgnore val casePolicy: String? = null,
) {
    constructor() : this(0, "", "ORACLE", DpDataNodeState.SUSPEND, null, null)
}
