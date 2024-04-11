package com.datapipeline.agent.entity

data class Savepoint(
    val thr: Int,
    val seq: Long,
    val cbk: Long,
    val cbo: Int,
    val lwn_block_num: Long = 0L,
    val name: String? = null,
    val resetlog_no: Long? = null,
    val first_change: Long? = null
) {
    constructor() : this(0, 0L, 0L, 0)

    override fun toString(): String {
        return "Savepoint: thr-[$thr], seq-[$seq], cbk-[$cbk], cbo-[$cbo], lwn_block_num-[$lwn_block_num]"
    }
}
