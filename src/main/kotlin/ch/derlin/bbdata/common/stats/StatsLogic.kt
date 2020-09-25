package ch.derlin.bbdata.common.stats

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.cassandra.ObjectStats
import ch.derlin.bbdata.common.cassandra.ObjectStatsCounter
import ch.derlin.bbdata.common.cassandra.ObjectStatsCounterRepository
import ch.derlin.bbdata.common.cassandra.ObjectStatsRepository
import ch.derlin.bbdata.input.NewValue
import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import kotlin.math.abs

/**
 * date: 25.05.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

data class Stats(
        val objectId: Long,
        // without the annotations, it will be serialized as "nreads" and "nwrites" (all lowercase)
        @get:JsonProperty("nReads") val nReads: Long = 0,
        @get:JsonProperty("nWrites") val nWrites: Long = 0,
        val lastTs: DateTime? = null,
        val avgSamplePeriod: Double = .0
)

interface StatsLogic {
    fun incrementReadCounter(objectId: Long)
    fun getStats(objectId: Long): Stats
    fun updateStats(v: NewValue)

    @Async
    fun updateAllStatsAsync(vs: List<NewValue>) = updateAllStats(vs)

    fun updateAllStats(vs: List<NewValue>) {
        vs.forEach { updateStats(it) }
    }

}

@Component
@Profile("${Profiles.CASSANDRA} && ${Profiles.CASSANDRA_STATS}")
class CassandraStatsLogic(private val objectStatsRepository: ObjectStatsRepository,
                          private val objectStatsCounterRepository: ObjectStatsCounterRepository) : StatsLogic {

    override fun updateStats(v: NewValue) {
        // get old stats
        val objectId = v.objectId!!.toInt()
        val objectStats = objectStatsRepository.findById(objectId).orElse(ObjectStats())
        val objectStatsCounter = objectStatsCounterRepository.findById(objectId).orElse(ObjectStatsCounter())

        // compute new stats
        val deltaMs = abs(v.timestamp!!.millis - (objectStats.lastTimestamp ?: v.timestamp).millis)
        val nRecords = objectStatsCounter.nValues
        val newSamplePeriod = if (nRecords > 0) (objectStats.avgSamplePeriod * (nRecords - 1) + deltaMs) / nRecords else .0f

        // save new stats
        objectStatsRepository.update(v.objectId.toInt(), newSamplePeriod, v.timestamp)
        objectStatsCounterRepository.updateWriteCounter(v.objectId.toInt())
    }

    override fun incrementReadCounter(objectId: Long) {

        objectStatsCounterRepository.updateReadCounter(objectId.toInt())
    }

    override fun getStats(objectId: Long): Stats =
            objectStatsRepository.findById(objectId.toInt()).let { osp ->
                val os = if (osp.isPresent) osp.get() else null
                objectStatsCounterRepository.findById(objectId.toInt()).let { ocp ->
                    val oc = if (ocp.isPresent) ocp.get() else null
                    Stats(
                            objectId = objectId,
                            nReads = oc?.nReads ?: 0,
                            nWrites = oc?.nValues ?: 0,
                            lastTs = os?.lastTimestamp,
                            avgSamplePeriod = os?.avgSamplePeriod?.toDouble() ?: .0
                    )
                }
            }
}

@Component
@Profile(Profiles.SQL_STATS)
class SqlStatsLogic(private val statsRepository: SqlStatsRepository) : StatsLogic {

    override fun updateStats(v: NewValue) {
        val stats = statsRepository.findById(v.objectId!!).orElse(SqlStats(objectId = v.objectId))
        stats.updateWithNewValue(v)
        statsRepository.save(stats)
    }

    override fun updateAllStats(vs: List<NewValue>) {
        // get all unique objects whose objectIds are in vs
        val stats = vs.map { it.objectId!! }.toSet().map{ objectId ->
            objectId to statsRepository.findById(objectId).orElse(SqlStats(objectId = objectId))
        }.toMap()

        // update each: this will also work when multiple values target the same object
        vs.forEach {
            stats[it.objectId]!!.updateWithNewValue(it)
        }

        // use the bulk save option of MySQL repositories to speed up the process
        statsRepository.saveAll(stats.values)
    }

    override fun incrementReadCounter(objectId: Long) {
        val stats = statsRepository.findById(objectId).orElse(SqlStats(objectId = objectId))
        stats.nReads += 1
        statsRepository.save(stats)
    }

    override fun getStats(objectId: Long): Stats {
        var stats: Stats? = null
        statsRepository.findById(objectId).ifPresent {
            stats = Stats(
                    objectId = objectId,
                    nReads = it.nReads,
                    nWrites = it.nWrites,
                    lastTs = it.lastTs,
                    avgSamplePeriod = it.avgSamplePeriod
            )
        }
        return stats ?: Stats(objectId = objectId)
    }
}