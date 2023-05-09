package com.datapipeline.agent.entity

data class OracleNodeConfig(
    var host: String? = null,
    var port: Int? = null,
    var database: String? = null,
    var username: String? = null,
    var password: String? = null,
    val dpToken: String? = null,
    var oragentConfig: OragentConfig? = null
) {
    companion object {
        val DEFAULT = OracleNodeConfig("192.168.0.14", 1521, "orcl", "DP_TEST", "123456", null, OragentConfig.DEFAULT)
    }

    fun irregularFormat(): Boolean {
        return host.isNullOrBlank() || port == null || database.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()
    }
}
