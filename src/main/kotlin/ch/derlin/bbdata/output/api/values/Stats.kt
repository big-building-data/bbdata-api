package ch.derlin.bbdata.output.api.values

import com.datastax.driver.core.DataType
import org.joda.time.DateTime
import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import org.springframework.stereotype.Repository

/**
 * date: 25.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Table("objects_stats")
data class ObjectStats(
        @PrimaryKey("object_id")
        val objectId: Int = -1,

        @Column("avg_sample_period")
        val avgSamplePeriod: Float = .0f,

        @Column("last_ts")
        val lastTimestamp: DateTime? = null
){
    fun computeUpdatedPeriod() = avgSamplePeriod
}

@Table("objects_stats_counter")
data class ObjectStatsCounter(
        @PrimaryKey("object_id")
        val objectId: Int = -1,

        @Column("n_reads")
        @CassandraType(type = DataType.Name.COUNTER)
        val nReads: Long = 0,

        @Column("n_values")
        @CassandraType(type = DataType.Name.COUNTER)
        val nValues: Long = 0
)

@Repository
interface ObjectStatsRepository : CassandraRepository<ObjectStats, Int> {

    @Query("UPDATE objects_stats SET last_ts = :lastTimestamp, avg_sample_period = :avgSamplePeriod WHERE object_id = :objectId")
    fun update(objectId: Int, avgSamplePeriod: Float, lastTimestamp: DateTime)
}

@Repository
interface ObjectStatsCounterRepository : CassandraRepository<ObjectStatsCounter, Int> {

    @Query("UPDATE objects_stats_counter SET n_values = n_values + 1 WHERE object_id = :objectId")
    fun updateCounter(objectId: Int)
}


