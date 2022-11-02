package com.datapipeline.agent

import com.datapipeline.agent.entity.*
import com.fasterxml.jackson.databind.node.POJONode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

internal class DataMigrationTest {
    private val nodeInfoText = """
        {
            "data": {
                "id": 3,
                "name": "Oracle Source With Param",
                "type": "ORACLE",
                "state": "ACTIVE",
                "modes": [
                    "JDBC_WRITE",
                    "JDBC_READ",
                    "LOG_MINER"
                ],
                "basicConfig": {
                    "@class": "com.datapipeline.internal.bean.node.OracleNodeConfig",
                    "params": [
                        {
                            "key": "PARAM1",
                            "value": "true"
                        }
                    ],
                    "sessionTimeZone": "+08:00",
                    "host": "192.168.0.14",
                    "port": 1521,
                    "database": "orcl",
                    "username": "DP_TEST",
                    "password": "cT08rnSXe23SPcUZUhHcnw==",
                    "oracleAgentConfig": {
                        "agentSourceHost": "192.168.0.14",
                        "agentSourcePort": "8303",
                        "agentSinkHost": "192.168.0.31",
                        "agentSinkPort": "18081"
                    },
                    "oragentConfig": {
                        "mode": "RAW"
                    },
                    "ibmCdcNodeConfig": {
                        "kafkaHosts": []
                    }
                }
            }
        }
    """.trimIndent()

    private val nodeInfoNoParamTest = """
        {
            "data": {
                "id": 4,
                "name": "Oracle Source No Param",
                "type": "ORACLE",
                "state": "SUSPEND",
                "modes": [
                    "JDBC_WRITE",
                    "JDBC_READ",
                    "LOG_MINER"
                ],
                "basicConfig": {
                    "@class": "com.datapipeline.internal.bean.node.OracleNodeConfig",
                    "sessionTimeZone": "+08:00",
                    "host": "192.168.0.14",
                    "port": 1521,
                    "database": "orcl",
                    "username": "PY_AUTO",
                    "password": "cT08rnSXe23SPcUZUhHcnw==",
                    "oracleAgentConfig": {
                        "agentSourceHost": "192.168.0.14",
                        "agentSourcePort": "8303",
                        "agentSinkHost": "192.168.0.31",
                        "agentSinkPort": "18081"
                    },
                    "oragentConfig": {
                        "mode": "RAW"
                    },
                    "ibmCdcNodeConfig": {
                        "kafkaHosts": []
                    }
                }
            }
        }
    """.trimIndent()

    @Test
    fun test() {
        val dataMigration = DataMigration()
        val mapper = dataMigration.mapper
        val jsonFactory = dataMigration.jsonFactory
        val confResult = com.uchuhimo.konf.Config {
            addSpec(SrcSpec)
            addSpec(AsmSpec)
        }.from.json.file(RESULT_CONF_PATH, true)
            .from.env()

        val apiResult = mapper.readValue(nodeInfoText, ApiResult::class.java)
        val nodeConfig = mapper.readValue(jsonFactory.pojoNode(apiResult.data).toString(), DpDataNode::class.java)
        val basicConfig = nodeConfig.basicConfig
        val expectedBasicConfig = DpDataNodeBasicConfig(
            "com.datapipeline.internal.bean.node.OracleNodeConfig",
            "192.168.0.14",
            1521,
            "orcl",
            null,
            "DP_TEST",
            DpOracleAgentConfig("192.168.0.14", "8303", "192.168.0.31", "18081"),
            OragentConfig(mode = Mode.RAW),
            null,
            arrayListOf(hashMapOf("key" to "PARAM1", "value" to "true"))
        )
        assertEquals(basicConfig, expectedBasicConfig)

        val apiResultNew = mapper.readValue(nodeInfoNoParamTest, ApiResult::class.java)
        val nodeConfigNoParam = mapper.readValue(jsonFactory.pojoNode(apiResultNew.data).toString(), DpDataNode::class.java)
        val basicConfigNoParam = nodeConfigNoParam.basicConfig
        val expectedBasicConfigNoParam = DpDataNodeBasicConfig(
            "com.datapipeline.internal.bean.node.OracleNodeConfig",
            "192.168.0.14",
            1521,
            "orcl",
            null,
            "PY_AUTO",
            DpOracleAgentConfig("192.168.0.14", "8303", "192.168.0.31", "18081"),
            OragentConfig(mode = Mode.RAW),
            null,
            null
        )
        assertEquals(basicConfigNoParam, expectedBasicConfigNoParam)

        val updateNodeJson = dataMigration.getUpdateNodeJson(basicConfig!!, confResult)
        val basicConfigNode = updateNodeJson["basicConfig"]
        assertIs<POJONode>(basicConfigNode)
        val newBasicConfig = basicConfigNode.pojo
        assertIs<DpDataNodeBasicConfig>(newBasicConfig)
        assertEquals(newBasicConfig.params, arrayListOf(hashMapOf("key" to "PARAM1", "value" to "true"), hashMapOf("key" to "LEGACY_FORMAT", "value" to "true")))

        val updateNodeJsonNoParam = dataMigration.getUpdateNodeJson(basicConfigNoParam!!, confResult)
        val basicConfigNoParamNode = updateNodeJsonNoParam["basicConfig"]
        assertIs<POJONode>(basicConfigNoParamNode)
        val newBasicConfigNoParam = basicConfigNoParamNode.pojo
        assertIs<DpDataNodeBasicConfig>(newBasicConfigNoParam)
        assertEquals(newBasicConfigNoParam.params, arrayListOf(hashMapOf("key" to "LEGACY_FORMAT", "value" to "true")))
    }
}