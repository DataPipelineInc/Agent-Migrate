package com.datapipeline.agent

import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.yaml

const val CONF_PATH = "conf/config.yml"
const val RESULT_CONF_PATH = "result/config.json"
const val RESULT_DATA_PATH = "result/data.json"

object OldConfSpec : ConfigSpec("old") {
    val host by required<String>()
    val web_port by optional(8303)
    val path by required<String>()
    val token by required<String>()
}

object NewConfSpec : ConfigSpec("new") {
    val host by required<String>()
    val web_port by optional(8888)
}

object DpConfSpec : ConfigSpec("dp") {
    val host by required<String>()
    val web_port by required<Int>()
    val token by required<String>()
}

object SrcSpec : ConfigSpec() {
    val id by required<Int>()
    val login by required<String>()
}

object AsmSpec : ConfigSpec() {
    val mode by optional("RAW")
    val connectionString by required<String>()
    val asmUser by required<String>()
    val asmPassword by required<String>()
    val oracleHome by required<String>()
    val sid by required<String>()
}

object DataSpec : ConfigSpec() {
    val topicNames by required<List<String>>()
    val nineOffsets by required<Map<String, Long>>()
    val endOffsets by required<Map<String, Long>>()
    val topicConfigs by required<Map<String, Map<String, String>>>()
    val topicOffsets by required<Map<String, Map<String, Long>>>()
}

val old_conf = com.uchuhimo.konf.Config {
    addSpec(OldConfSpec)
}.enable(Feature.OPTIONAL_SOURCE_BY_DEFAULT)
    .from.yaml.file(CONF_PATH)
    .from.env()

val new_conf = com.uchuhimo.konf.Config {
    addSpec(NewConfSpec)
}.enable(Feature.OPTIONAL_SOURCE_BY_DEFAULT)
    .from.yaml.file(CONF_PATH)
    .from.env()

val dp_conf = com.uchuhimo.konf.Config {
    addSpec(DpConfSpec)
}.enable(Feature.OPTIONAL_SOURCE_BY_DEFAULT)
    .from.yaml.file(CONF_PATH)
    .from.env()

val conf_result = com.uchuhimo.konf.Config {
    addSpec(SrcSpec)
    addSpec(AsmSpec)
}.from.json.file(RESULT_CONF_PATH, true)
    .from.env()

val data_result = com.uchuhimo.konf.Config {
    addSpec(DataSpec)
}.from.json.file(RESULT_DATA_PATH, true)
    .from.env()

enum class Config(val host: String, val port: Int, val token: String?) {
    OLD_AGENT(old_conf[OldConfSpec.host], old_conf[OldConfSpec.web_port], old_conf[OldConfSpec.token]),
    NEW_AGENT(new_conf[NewConfSpec.host], new_conf[NewConfSpec.web_port], null),
    DP(dp_conf[DpConfSpec.host], dp_conf[DpConfSpec.web_port], dp_conf[DpConfSpec.token])
}