package com.datapipeline.agent.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vertx.core.json.JsonObject

data class KafkaConfig (
    val `@class`: String,
    var params: ArrayList<HashMap<Any, Any>>? = null,
    val host: String? = null,
    val zkAddr: String? = null,
    val schemaRegistryHost: String? = null,
    val securityConfig: SecurityConfig? = null,
    @JsonIgnore val sessionTimeZone: String? = null,
    @JsonIgnore val customParams: List<JsonObject>? = null,
) {
    constructor(): this("com.datapipeline.internal.bean.node.KafkaNodeConfig", securityConfig = SecurityConfig())
}