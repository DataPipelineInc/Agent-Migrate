package com.datapipeline.agent.entity

data class OragentConfig(
    val uniqueId: Int? = null,
    val mode: Mode? = null,
    val host: String? = null,
    val port: Int? = null,
    val connectionString: String? = null,
    val asmUser: String? = null,
    val asmPassword: String? = null,
    val oracleHome: String? = null,
    val sid: String? = null,
    val asmDisks: List<AsmDiskInfo>? = null,
    val bigEndian: Boolean = false,
) {
    companion object {
        val DEFAULT = OragentConfig(
            1,
            Mode.DB,
            connectionString = "//10.10.10.20:1521/+ASM",
            asmUser = "asmUser as sysdba",
            asmPassword = "asmPassword",
            oracleHome = "",
            sid = ""
        )
    }
}

data class AsmDiskInfo(
    var asmDisk: String? = null,
    var asmDevice: String? = null
)

enum class Mode {
    RAW,
    RAW_ASM,
    DB
}
