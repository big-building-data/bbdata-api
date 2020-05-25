package ch.derlin.bbdata.common.stats

import ch.derlin.bbdata.Profiles
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
    @Query("UPDATE SqlStats s set s.nReads = s.nReads + 1 WHERE s.objectId = :objectId")
    fun incrementReadCounter(objectId: Long)
}