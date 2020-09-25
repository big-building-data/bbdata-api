package ch.derlin.bbdata.common.stats

import ch.derlin.bbdata.Profiles
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * date: 25.05.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Profile(Profiles.SQL_STATS)
@Repository
interface SqlStatsRepository : JpaRepository<SqlStats, Long> {

    @Transactional
    @Modifying
    @Query("""
        INSERT INTO stats (object_id, last_ts, n_writes)
        VALUES (:objectId, :ts, 1) ON DUPLICATE KEY UPDATE
        avg_sample_period = CASE WHEN VALUES(last_ts) > last_ts 
            THEN ( (avg_sample_period * (n_writes-1)) + ABS((UNIX_TIMESTAMP(last_ts) * 1000) - (UNIX_TIMESTAMP(VALUES(last_ts)) * 1000)) ) / n_writes 
            ELSE avg_sample_period 
        END,
        n_writes = n_writes + 1,
        last_ts = GREATEST(last_ts, VALUES(last_ts))
    """, nativeQuery = true)
    fun updateWriteStats(objectId: Long, ts: DateTime)

    @Transactional
    @Modifying
    @Query("INSERT INTO stats (object_id, n_reads) VALUES (:objectId, 1) ON DUPLICATE KEY UPDATE n_reads = n_reads + 1", nativeQuery = true)
    fun updateReadCounter(objectId: Long)

}