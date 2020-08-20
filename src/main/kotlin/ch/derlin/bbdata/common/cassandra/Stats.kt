package ch.derlin.bbdata.common.cassandra

import com.datastax.driver.core.DataType
import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime
import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table

/**
 * date: 27.01.20
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
)

@Table("objects_stats_counter")
data class ObjectStatsCounter(
        // !! if only one letter before the first uppercase, e.g. "nReads" jackson will convert it to "nreads"
        // see https://stackoverflow.com/q/30205006 for an explanation
        @PrimaryKey("object_id")
        val objectId: Int = -1,

        @Column("n_reads")
        @get:JsonProperty("nReads")
        @CassandraType(type = DataType.Name.COUNTER)
        val nReads: Long = 0,

        @Column("n_values")
        @get:JsonProperty("nValues")
        @CassandraType(type = DataType.Name.COUNTER)
        val nValues: Long = 0
)