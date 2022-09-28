package com.datapipeline.agent.util

import com.datapipeline.agent.LOGGER
import com.datapipeline.agent.entity.Savepoint
import com.vladsch.kotlin.jdbc.Row
import com.vladsch.kotlin.jdbc.session
import com.vladsch.kotlin.jdbc.sqlQuery
import com.vladsch.kotlin.jdbc.using
import com.zaxxer.hikari.HikariDataSource

const val GET_RESETLOGS_CHANGE_SQL = "select to_char(resetlogs_change#) from v${'$'}database"
const val GET_DB_ROLE_SQL = "select database_role from v${'$'}database"

val GET_OLDEST_VALID_ARCH_LOG_SQL = """
    select sequence#
    from v${'$'}archived_log
    where name is not null
    order by recid asc fetch first 1 row only
""".trimIndent()

fun getSeqByScn(dataSource: HikariDataSource, scnMap: Map<Int, Long>): List<Savepoint> {
    val rlc = querySingle(dataSource, GET_RESETLOGS_CHANGE_SQL) { it.long(1) }
        ?: throw Exception("Missed resetlogs_change#")
    val adg = querySingle(dataSource, GET_DB_ROLE_SQL) { it.string(1) != "PRIMARY" }
        ?: throw Exception("Missed database_role")
    val sortedMap = scnMap.toSortedMap(Comparator.naturalOrder())
    val list = ArrayList<Savepoint>(sortedMap.size)

    sortedMap.iterator().forEach {
        val sql = """
            select l.sequence#
            from v${'$'}${if (adg) "standby_log" else "log"} l,
                 v${'$'}logfile f
            where l.group# = f.group#
              and f.member is not null
              and l.thread# = ${it.key}
              ${if (adg) " and l.status = 'ACTIVE' " else ""} 
              and first_change# <= ${it.value}
              and next_change# > ${it.value}
              and (f.status is null or f.status not in ('INVALID', 'INCOMPLETE', 'STALE'))
            union
            select sequence#
            from v${'$'}archived_log
            where name is not null
              and thread# = ${it.key}
              and resetlogs_change# = $rlc
              and first_change# <= ${it.value}
              and next_change# > ${it.value}
              and (creator = 'ARCH' or creator = 'FGRD' or creator = 'RMAN' or creator = 'LGWR')
              and standby_dest = 'NO'
            order by 1
        """.trimIndent()
        // 查找 Sequence ，未找到则寻找最早归档日志的 Sequence
        val seq =
            querySingle(dataSource, sql) { r -> r.long(1) }
                ?: querySingle(dataSource, GET_OLDEST_VALID_ARCH_LOG_SQL) { r -> r.long(1) }
                ?: throw Exception("Missed sequence# for thread#[${it.key}]")
        list.add(Savepoint(it.key, seq, 0, 0, 0))
    }

    return list
}

fun <A> querySingle(dataSource: HikariDataSource, sql: String, extractor: (Row) -> A?): A? =
    using(session(dataSource)) { session ->
        LOGGER.info { sql }
        session.first(sqlQuery(sql), extractor)
    }
