package com.datapipeline.agent.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vertx.core.json.JsonObject

data class SecurityConfig (
    val authType: SecurityAuthType? = null,
    val kerberos: KerberosConfig? = null,
    val ldap: LdapConfig? = null,
    val scramConfig: ScramConfig? = null,
    @JsonIgnore val sslConfig: JsonObject? = null,
    @JsonIgnore val sshConfig: JsonObject? = null,
) {
    constructor(): this(SecurityAuthType.SIMPLE)
}

data class KerberosConfig (
    val confFileId: Int? = null,
    val principal: String? = null,
    val keytabFileId: Int? = null,
    val ticketCachePath: String? = null
)

data class LdapConfig (
    val user: String? = null,
    val password: String? = null
)

data class ScramConfig (
    val userName: String? = null,
    val password: String? = null
)