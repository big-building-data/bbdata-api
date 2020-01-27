package ch.derlin.bbdata.common.cassandra

import ch.derlin.bbdata.common.dates.JodaUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import org.joda.time.DateTime
import org.springframework.data.cassandra.core.cql.Ordering
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.*
import java.io.Serializable
import java.util.*

/**
 * date: 27.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@PrimaryKeyClass
data class AggregationPK(
        @PrimaryKeyColumn(name = "minutes", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        @JsonIgnore
        val minutes: Int? = null,

        @PrimaryKeyColumn(name = "object_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
        @JsonIgnore
        val objectId: Int? = null,

        @PrimaryKeyColumn(name = "date", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
        @JsonIgnore
        val month: String? = null,

        @PrimaryKeyColumn(name = "timestamp", ordinal = 3, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
        @JsonProperty("timestamp")
        val timestamp: DateTime? = null
) : Serializable

@Table("aggregations")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Aggregation(
        @PrimaryKey
        @field:JsonUnwrapped
        private val key: AggregationPK,

        @Column val last: Float? = null,

        @Column("last_ts") private val lastTimestamp: Long? = null,

        @Column val min: Float? = null,

        @Column val max: Float? = null,

        @Column val sum: Float? = null,

        @Column private val mean: Float? = null,

        @Column val count: Int? = null,

        @Column private val std: Float? = null,

        @Column val comment: String? = null


) : StreamableCsv {

    fun getStd(): Float? = if (std == null || std.isNaN()) null else std

    // round mean... Here, ensure your are using a locale with "." and not "," !!
    fun getMean(): Float? = mean?.let { "%.5f".format(Locale.US, it).toFloat() }

    fun getLastTimestamp(): String? = lastTimestamp?.let { JodaUtils.format(it) }

    override fun csvValues(vararg args: Any): List<Any?> = listOf(
            key.objectId, key.timestamp?.let { JodaUtils.format(it) },
            last, getLastTimestamp(),
            min, max, sum, getMean(), getStd(), count, comment)

    companion object {
        const val csvHeadersString = "object_id,timestamp," +
                "last,last_timestamp," +
                "min,max,sum,mean,std,count,comment"

        val csvHeaders: List<String> = csvHeadersString.split(",")
    }

}