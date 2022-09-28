package com.datapipeline.agent

import com.datapipeline.agent.util.getOracleConf
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

class DataSource(private val src_login: String) {
    private lateinit var dataSource: HikariDataSource

    fun get(): HikariDataSource {
        if (this::dataSource.isInitialized.not()) {
            this.dataSource = src_login.let {
                val config = HikariConfig()
                val oracleConf = getOracleConf(it)
                config.username = oracleConf.username
                config.password = oracleConf.password
                config.maximumPoolSize = 1
                config.connectionTimeout = 3000
                config.jdbcUrl = "jdbc:oracle:thin:@${oracleConf.host}:${oracleConf.port}:${oracleConf.database}"
                LOGGER.info { "Init database connection: ${config.jdbcUrl}, ${config.username}/${config.password}" }
                HikariDataSource(config)
            }
        }
        return this.dataSource
    }

    fun close() {
        if (this::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}