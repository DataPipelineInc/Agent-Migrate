package com.datapipeline.agent.entity

import com.fasterxml.jackson.annotation.JsonIgnore

data class DpSrcNodeBasicConfig(
    val `@class`: String? = null,
    val mode: String? = null,
    @JsonIgnore val resourceGroup: String? = null,
    @JsonIgnore val rollbackConfig: Map<String, Any>? = null
) {
    constructor() : this(null, null)
}
