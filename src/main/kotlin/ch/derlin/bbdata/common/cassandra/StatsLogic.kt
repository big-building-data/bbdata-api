package ch.derlin.bbdata.common.cassandra

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.stats.SqlStats
import ch.derlin.bbdata.common.stats.SqlStatsRepository
import ch.derlin.bbdata.input.NewValue
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * date: 25.05.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

interface StatsLogic {
    fun updateStats(v: NewValue)
    fun incrementReadCounter(objectId: Long)
}

@Component
@Profile(Profiles.CASSANDRA_STATS)
class CassandraStatsLogic(private val objectStatsRepository: ObjectStatsRepository,
                          private val objectStatsCounterRepository: ObjectStatsCounterRepository) : StatsLogic {

    override fun updateStats(v: NewValue) {
        // get old stats
        val objectId = v.objectId!!.toInt()
        val objectStats = objectStatsRepository.findById(objectId).orElse(ObjectStats())
        val objectStatsCounter = objectStatsCounterRepository.findById(objectId).orElse(ObjectStatsCounter())

        // compute new stats
        val deltaMs = Math.abs(v.timestamp!!.millis - (objectStats.lastTimestamp ?: v.timestamp).millis)
        val nRecords = objectStatsCounter.nValues
        val newSamplePeriod = (objectStats.avgSamplePeriod * (nRecords - 1) + deltaMs) / nRecords

        // save new stats
        objectStatsRepository.update(v.objectId.toInt(), newSamplePeriod, v.timestamp)
        objectStatsCounterRepository.updateWriteCounter(v.objectId.toInt())
    }

    override fun incrementReadCounter(objectId: Long) =
            objectStatsCounterRepository.updateReadCounter(objectId.toInt())
}

@Component
@Profile(Profiles.SQL_STATS)
class SqlStatsLogic(private val statsRepository: SqlStatsRepository) : StatsLogic {
    override fun updateStats(v: NewValue) {
        val stats = statsRepository.findById(v.objectId!!).orElse(SqlStats(objectId = v.objectId))
        stats.updateWithNewValue(v)
        statsRepository.save(stats)
    }

    override fun incrementReadCounter(objectId: Long) =
            statsRepository.incrementReadCounter(objectId)
}