package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.output.dates.JodaUtils
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.joda.time.YearMonth
import org.springframework.data.cassandra.core.cql.Ordering
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.*
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import java.util.*
import javax.servlet.http.HttpServletResponse

/**
 * date: 11.12.19
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
        val csvHeaders: List<String> = listOf(
                "object_id", "timestamp",
                "last", "last_timestamp",
                "min", "max", "sum", "mean", "std", "count", "comment"
        )
    }

}

@Repository
interface AggregationsRepository : CassandraRepository<Aggregation, AggregationPK> {
    @Query("SELECT * FROM aggregations WHERE minutes = :minutes AND object_id = :objectId AND date IN :months " +
            "AND timestamp >= :dfrom AND timestamp <= :dto")
    fun findByTimestampBetween(
            minutes: Int, objectId: Int, months: List<String>, dfrom: DateTime, dto: DateTime): Iterable<Aggregation>
}

@RestController
@Tag(name = "Values Aggregated", description = "Get aggregated object values")
class AggregationsController(private val aggregationsRepository: AggregationsRepository,
                             private val cassandraObjectStreamer: CassandraObjectStreamer) {

    val quarters = 15
    val hours = 60

    @Protected
    @GetMapping("/values/quarters", produces = arrayOf("application/json", "text/plain"))
    fun getQuarterAggregationsStream(
            @UserId userId: Int,
            @RequestHeader(value = "Content-Type") contentType: String,
            @RequestParam(name = "ids", required = true) ids: List<Long>,
            @RequestParam(name = "from", required = true) from: DateTime,
            @RequestParam(name = "to", required = false) to: DateTime?,
            response: HttpServletResponse) {

        val to = to ?: DateTime.now()
        val months = CassandraUtils.monthsBetween(YearMonth(from), YearMonth(to))
        cassandraObjectStreamer.stream(contentType, response, userId, ids, Aggregation.csvHeaders, {
            aggregationsRepository.findByTimestampBetween(quarters, it, months, from, to)
        })

    }

    @Protected
    @GetMapping("/values/hours", produces = arrayOf("application/json", "text/plain"))
    fun getHoursAggregationsStream(
            @UserId userId: Int,
            @RequestHeader(value = "Content-Type") contentType: String,
            @RequestParam(name = "ids", required = true) ids: List<Long>,
            @RequestParam(name = "from", required = true) from: DateTime,
            @RequestParam(name = "to", required = false) to: DateTime?,
            response: HttpServletResponse) {

        val to = to ?: DateTime.now()
        val months = CassandraUtils.monthsBetween(YearMonth(from), YearMonth(to))
        cassandraObjectStreamer.stream(contentType, response, userId, ids, Aggregation.csvHeaders, {
            aggregationsRepository.findByTimestampBetween(hours, it, months, from, to)
        })

    }


}