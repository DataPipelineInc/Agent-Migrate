package entity

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vertx.core.json.JsonObject

data class DpDataNodeBasicConfig(
    val `@class`: String,
    val host: String,
    val port: Int,
    val database: String,
    val schema: String?,
    val username: String,
    val password: String,
    val oracleAgentConfig: DpOracleAgentConfig,
    var oragentConfig: OragentConfig,
    val dpToken: String? = null,
    @JsonIgnore val ibmCdcNodeConfig: JsonObject? = null,
    @JsonIgnore val sessionTimeZone: String? = null,
    @JsonIgnore val securityConfig: JsonObject? = null,
    @JsonIgnore val params: List<JsonObject>? = null,
    @JsonIgnore val customParams: List<JsonObject>? = null
) {
    constructor() : this("", "", 0, "", null, "", "", DpOracleAgentConfig(), OragentConfig.DEFAULT)
}