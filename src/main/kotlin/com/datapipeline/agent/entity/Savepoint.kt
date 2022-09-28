package com.datapipeline.agent.entity

data class Savepoint(
    val RAC: Int,
    val SEQ: Long,
    val BLK: Long,
    val BKO: Int,
    val LBK: Long? = null
) {
    constructor() : this(0, 0L, 0L, 0)

    override fun toString(): String {
        return "Savepoint: RAC-[$RAC], SEQ-[$SEQ], BLK-[$BLK], BKO-[$BKO], LBK-[$LBK]"
    }
}
