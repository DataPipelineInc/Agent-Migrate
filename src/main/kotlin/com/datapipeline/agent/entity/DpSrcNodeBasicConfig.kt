package com.datapipeline.agent.entity

import com.fasterxml.jackson.annotation.JsonIgnore

data class DpSrcNodeBasicConfig(
    val `@class`: String? = null,
    val mode: String? = null,
    @JsonIgnore val resourceGroup: String? = null,
    @JsonIgnore val resourceGroupLabel: String? = null,
    @JsonIgnore val rollbackConfig: Map<String, Any>? = null,
    @JsonIgnore val endPointConfig: Map<String, Any>? = null,
    @JsonIgnore val srcFileNotExistPolicy: Map<String, Any>? = null,
) {
    constructor() : this(null, null)
}
