package com.datapipeline.agent.entity

data class ExportConfig(
    val src_id: String,
    val src_login: String,
    val asm_login: String,
    val asm_oracle_sid: String?,
    val asm_oracle_home: String?,
    val asm_mode: String
) {
    constructor() : this("0", "", "", null, null, "")
}
