package com.datapipeline.agent.entity

data class Trans(
    val trans_id: Long,
    val scn_bgn0: Long,
    val scn_bgn: Scn,
    val scn_max: Scn,
    val scn: Scn,
    val scn_time_bgn: Long,
    val scn_time: ScnTime,
    val scn_time_max: Long,
    val thread_id: Int,
    val rba: Long,
    val uba: Long,
    val dba: Long,
    val slt: Int,
    val cur_fno: Int,
    val max_fno: Int,
    val changed: Long,
    val cached_len: Long,
    val total_len: Long,
    val flag: Int,
    val latencyMakeStartTime: Long,
    val _cmted: Boolean = false
) {
    constructor() : this(0L, 0L, Scn(), Scn(), Scn(), 0L, ScnTime(), 0L, 0, 0L, 0L, 0L, 0, 0, 0, 0L, 0L, 0L, 0, 0L)
}
