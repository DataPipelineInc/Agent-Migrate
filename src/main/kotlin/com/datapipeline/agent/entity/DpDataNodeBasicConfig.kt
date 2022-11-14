package com.datapipeline.agent.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vertx.core.json.JsonObject

data class DpDataNodeBasicConfig(
    val `@class`: String,
    val host: String,
    val port: Int,
    val database: String,
    val schema: String?,
    val username: String,
    val oracleAgentConfig: DpOracleAgentConfig? = null,
    var oragentConfig: OragentConfig,
    val dpToken: String? = null,
    var params: ArrayList<HashMap<Any, Any>>? = null,
    @JsonIgnore val password: String? = null,
    @JsonIgnore val ibmCdcNodeConfig: JsonObject? = null,
    @JsonIgnore val sessionTimeZone: String? = null,
    @JsonIgnore val securityConfig: JsonObject? = null,
    @JsonIgnore val customParams: List<JsonObject>? = null
) {
    constructor() : this("", "", 0, "", null, "",  DpOracleAgentConfig(), OragentConfig.DEFAULT)
}