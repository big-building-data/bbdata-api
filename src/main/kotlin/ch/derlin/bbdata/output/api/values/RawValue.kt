package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.output.dates.JodaUtils
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.joda.time.YearMonth
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
import javax.servlet.http.HttpServletResponse

/**
 * date: 10.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@PrimaryKeyClass
data class RawValuePK(
        @PrimaryKeyColumn(name = "object_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        @JsonIgnore
        val objectId: Int? = null,

        @PrimaryKeyColumn(name = "month", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
        @JsonIgnore
        val month: String? = null,

        @PrimaryKeyColumn(name = "timestamp", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
        @JsonProperty("timestamp")
        val timestamp: DateTime? = null
) : Serializable

@Table("raw_values")
data class RawValue(
        @PrimaryKey
        @field:JsonUnwrapped
        private val key: RawValuePK,

        @Column
        val value: String = "",

        @Column
        val comment: String? = null
) : StreamableCsv {
    override fun csvValues(vararg args: Any): List<Any?> =
            listOf(key.objectId, key.timestamp?.let { JodaUtils.format(it) }, value, comment)

    override fun csvHeaders(): List<String> = listOf("object_id", "timestamp", "value", "comment")

}

object CassandraUtils {
    val YM_FORMAT = org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM")

    fun xMonthsFrom(d1: YearMonth, x: Int): List<String> {
        val ym = YearMonth(d1)
        return monthsBetween(ym.minusMonths(x), ym)
    }

    fun monthsBetween(d1: YearMonth, d2: YearMonth? = null): List<String> {
        val months = mutableListOf<String>()
        var d2 = d2 ?: YearMonth.now()
        while (d1.compareTo(d2) <= 0) {
            months.add(YM_FORMAT.print(d2))
            d2 = d2.minusMonths(1)
        }
        return months
    }
}


@Repository
interface RawValueRepository : CassandraRepository<RawValue, RawValuePK> {
    @Query("SELECT * FROM raw_values WHERE object_id = :objectId AND month IN :months " +
            "AND timestamp >= :dfrom AND timestamp <= :dto")
    fun findByTimestampBetween(objectId: Int, months: List<String>, dfrom: DateTime, dto: DateTime): Iterable<RawValue>
}

@RestController
@Tag(name = "Values", description = "Get raw object values")
class RawValuesController(private val rawValueRepository: RawValueRepository,
                          private val cassandraObjectStreamer: CassandraObjectStreamer) {


    @Protected
    @GetMapping("/values", produces = arrayOf("application/json", "text/plain"))
    fun getValuesStream(
            @UserId userId: Int,
            @RequestHeader(value = "Content-Type") contentType: String,
            @RequestParam(name = "ids", required = true) ids: List<Long>,
            @RequestParam(name = "from", required = true) from: DateTime,
            @RequestParam(name = "to", required = false) to: DateTime?,
            response: HttpServletResponse) {

        val to = to ?: DateTime.now()
        val months = CassandraUtils.monthsBetween(YearMonth(from), YearMonth(to))
        cassandraObjectStreamer.stream(contentType, response, userId, ids, {
            rawValueRepository.findByTimestampBetween(it, months, from, to)
        })

    }


}