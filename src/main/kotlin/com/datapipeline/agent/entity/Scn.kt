package com.datapipeline.agent.entity

import com.fasterxml.jackson.annotation.JsonIgnore

data class Scn(
    val scn: Long = 0,
    val scn_sub: Short = 0,
    @JsonIgnore val len: Int = 0
) {
    constructor() : this(0L, 0, 0)
}