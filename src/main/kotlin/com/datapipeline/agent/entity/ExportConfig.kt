package com.datapipeline.agent.entity

data class ExportConfig(
    val src_id: String,
    val src_login: String,
    val asm_login: String,
    val asm_mode: String,
    val param_exadata: String = "0",
    val asm_oracle_sid: String? = null,
    val asm_oracle_home: String? = null,
    val asm_device_id: String? = null,
    val asm_disk: String? = null,
    val asm_dev: String? = null
) {
    constructor() : this("0", "", "", "")
}
