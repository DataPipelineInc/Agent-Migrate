package com.datapipeline.agent.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class OffsetValue(
    val last: Long,
    val size: Long,
    @JsonProperty("nine.offset") val nine_offset: Long,
    val dp_done_timestamp: Long,
    val miningScn: Long,
    val totalb: Long,
    val totalc: Long,
    val idx: Long,
    val transactionScn: Long,
    val snapshot: Boolean
) {
    constructor() : this(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false)
}
