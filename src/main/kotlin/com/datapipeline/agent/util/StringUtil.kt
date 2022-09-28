package com.datapipeline.agent.util

import com.datapipeline.agent.entity.OracleNodeConfig

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
    return OracleNodeConfig(
        hostAndPort.first,
        hostAndPort.second.toInt(),
        conn.second,
        login.first,
        login.second
    )
}