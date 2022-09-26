package entity

data class DpOracleAgentConfig(
    val agentSourceHost: String,
    val agentSourcePort: String,
    val agentSinkHost: String,
    val agentSinkPort: String,
    val kafkaBrokers: String? = null,
    val kafkaTopicPrefix: String? = null,
) {
    constructor() : this("", "", "", "")
}
