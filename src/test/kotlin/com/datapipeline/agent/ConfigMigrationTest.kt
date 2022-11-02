package com.datapipeline.agent

import com.datapipeline.agent.entity.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ConfigMigrationTest {
    private val testExportConfigWithAsm = ExportConfig(
        "1",
        "DP_TEST as sysdba/123456@dn-oracle-source:1521/ORCL",
        "agentasm as sysdba/agentasm@//10.10.10.20:1521/+ASM",
        "raw",
        "1",
        "+ASM",
        "/u01/app/oracle/product/12.2.0/db_1",
        "1 , 2 , 3",
        "/dev/asm-diskc , /dev/asm-diskd , /dev/asm-diske",
        "/dev/sdb1 , /dev/sdb2 , /dev/sdb3")

    private val testExportConfig = ExportConfig(
        "1",
        "DP_TEST as sysdba/123456@dn-oracle-source:1521/ORCL",
        "/as sysdba",
        "raw",
        asm_oracle_sid = "+ASM",
        asm_oracle_home = "/u01/app/oracle/product/12.2.0/db_1"
    )

    @Test
    fun test() {
        val configMigration = ConfigMigration()
        val oracleNodeConfig = configMigration.getOracleNodeConfig(testExportConfig)
        val oracleNodeConfigWithAsm = configMigration.getOracleNodeConfig(testExportConfigWithAsm)
        val oragentConfig = OragentConfig(1, Mode.RAW, "", null, "", "/as sysdba", "", "/u01/app/oracle/product/12.2.0/db_1", "+ASM", emptyList(), false)
        val expectedOracleNodeConfig = OracleNodeConfig("dn-oracle-source", 1521, "ORCL", "DP_TEST as sysdba", "123456", oragentConfig = oragentConfig)
        val asmDisks = arrayListOf(AsmDiskInfo("/dev/asm-diskc", "/dev/sdb1"), AsmDiskInfo("/dev/asm-diskd", "/dev/sdb2"), AsmDiskInfo("/dev/asm-diske", "/dev/sdb3"))
        val oragentConfigWithAsm = OragentConfig(1, Mode.RAW, "//10.10.10.20", 1521, "//10.10.10.20:1521/+ASM", "agentasm as sysdba", "agentasm", "/u01/app/oracle/product/12.2.0/db_1", "+ASM", asmDisks, true)
        val expectedOracleNodeConfigWithAsm = OracleNodeConfig("dn-oracle-source", 1521, "ORCL", "DP_TEST as sysdba", "123456", oragentConfig = oragentConfigWithAsm)
        assertEquals(expectedOracleNodeConfig, oracleNodeConfig, "oracle node config not equals.")
        assertEquals(expectedOracleNodeConfigWithAsm, oracleNodeConfigWithAsm, "oracle node config with asm not equals.")
    }
}