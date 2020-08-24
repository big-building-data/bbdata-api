package ch.derlin.bbdata.common.cassandra

import ch.derlin.bbdata.Profiles
import org.joda.time.DateTime
import org.joda.time.YearMonth
import org.springframework.context.annotation.Profile
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import org.springframework.stereotype.Repository

/**
 * date: 27.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface RawValueRepository : CassandraRepository<RawValue, RawValuePK> {
    @Query("SELECT * FROM raw_values WHERE object_id = :objectId AND month IN :months " +
            "AND timestamp >= :dfrom AND timestamp <= :dto")
    fun findByTimestampBetween(objectId: Int, months: List<String>, dfrom: DateTime, dto: DateTime): Iterable<RawValue>

    @Query("SELECT * FROM raw_values WHERE object_id = :objectId AND month = :month " +
            "AND timestamp <= :before ORDER BY timestamp DESC limit 1")
    fun getLatest(objectId: Int, month: String, before: DateTime): RawValue?
}


@Repository
interface AggregationsRepository : CassandraRepository<Aggregation, AggregationPK> {
    @Query("SELECT * FROM aggregations WHERE minutes = :minutes AND object_id = :objectId AND date IN :months " +
            "AND timestamp >= :dfrom AND timestamp <= :dto")
    fun findByTimestampBetween(
            minutes: Int, objectId: Int, months: List<String>, dfrom: DateTime, dto: DateTime): Iterable<Aggregation>
}

@Profile(Profiles.CASSANDRA_STATS)
@Repository
interface ObjectStatsRepository : CassandraRepository<ObjectStats, Int> {

    @Query("UPDATE objects_stats SET last_ts = :lastTimestamp, avg_sample_period = :avgSamplePeriod WHERE object_id = :objectId")
    fun update(objectId: Int, avgSamplePeriod: Float, lastTimestamp: DateTime)
}

@Profile(Profiles.CASSANDRA_STATS)
@Repository
interface ObjectStatsCounterRepository : CassandraRepository<ObjectStatsCounter, Int> {

    @Query("UPDATE objects_stats_counter SET n_values = n_values + 1 WHERE object_id = :objectId")
    fun updateWriteCounter(objectId: Int)

    @Query("UPDATE objects_stats_counter SET n_reads = n_reads + 1 WHERE object_id = :objectId")
    fun updateReadCounter(objectId: Int)
}


fun RawValueRepository.findLatestValue(objectId: Long, from: DateTime, to: DateTime): RawValue? {
    // do the search, one month at a time, stop when one is found
    val searchMonths = CassandraUtils.monthsBetween(YearMonth(from), YearMonth(to))
    return searchMonths.asSequence()
            .map { this.getLatest(objectId.toInt(), it, to) }
            .dropWhile { it == null }
            .firstOrNull()
}