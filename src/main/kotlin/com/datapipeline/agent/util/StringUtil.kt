package com.datapipeline.agent.util

import com.datapipeline.agent.LOGGER
import com.datapipeline.agent.entity.OracleNodeConfig
import java.lang.NumberFormatException

fun String.cutHalf(delim: String): Pair<String, String> {
    return if (this.indexOf(delim) < 0)
        this to ""
    else
        substringBefore(delim, "") to substringAfter(delim, "")
}

fun getOracleConf(src_login: String): OracleNodeConfig {
    val cut = src_login.cutHalf("@")
    val login = cut.first.cutHalf("/")
    val conn = cut.second.cutHalf("/")
    val hostAndPort = conn.first.cutHalf(":")
    var port: Int? = null
    try {
        port = hostAndPort.second.toInt()
    } catch (_: NumberFormatException) {
        LOGGER.error { "Port: [${hostAndPort.second}] is not a valid number" }
    }
    return OracleNodeConfig(
        hostAndPort.first,
        port,
        conn.second,
        login.first,
        login.second
    )
}