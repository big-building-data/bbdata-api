package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.output.Constants
import ch.derlin.bbdata.output.api.objects.ObjectRepository
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joda.time.DateTime
import org.joda.time.YearMonth
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.*
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import java.time.format.DateTimeFormatter

/**
 * date: 10.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

//@Configuration
//class CassandraConfig : AbstractCassandraConfiguration() {
//    override fun getKeyspaceName(): String = "bbdata2"
//    override fun getContactPoints(): String = "10.10.10.51,10.10.10.52,10.10.10.53,10.10.10.54"
//    override fun getMetricsEnabled(): Boolean = false
//}

@PrimaryKeyClass
data class RawValuePK(
        @PrimaryKeyColumn(name = "object_id", type = PrimaryKeyType.PARTITIONED)
        val objectId: Int? = null,

        @PrimaryKeyColumn(name = "month", type = PrimaryKeyType.PARTITIONED)
        val month: String? = null,

        @PrimaryKeyColumn(name = "timestamp", type = PrimaryKeyType.CLUSTERED)
        val timestamp: DateTime = DateTime.now()
) : Serializable

@Table("raw_values")
data class RawValue(
        @PrimaryKeyColumn(name = "object_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        @JsonIgnore
        val objectId: Int? = null,

        @PrimaryKeyColumn(name = "month", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
        @JsonIgnore
        val month: String? = null,

        @PrimaryKeyColumn(name = "timestamp", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
        val timestamp: DateTime,

        @Column
        val value: String = "",

        @Column
        val comment: String? = null
)

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
    @Query("SELECT value, timestamp FROM raw_values WHERE object_id = :objectId AND month IN :months")
    fun findByKey(objectId: Int, months: List<String>): List<RawValue>

    @Query("SELECT value, timestamp FROM raw_values WHERE object_id = :objectId AND month IN :months " +
            "AND timestamp >= :dfrom AND timestamp <= :dto")
    fun findByTimestampBetween(objectId: Int, months: List<String>, dfrom: DateTime, dto: DateTime): Iterable<RawValue>
}

@RestController
class RawValuesController(private val rawValueRepository: RawValueRepository,
                          private val objectsRepository: ObjectRepository) {
    @GetMapping("/values")
    fun getValues(
            @RequestHeader(value = Constants.HEADER_USER) userId: Int,
            @RequestParam(name = "ids", required = true) ids: List<Long>,
            @RequestParam(name = "from", required = true)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: DateTime,
            @RequestParam(name = "to", required = false) to: DateTime?
    ): List<Map<String, Any>> {
        val to = to ?: DateTime.now()
        val months = CassandraUtils.monthsBetween(YearMonth(from), YearMonth(to))
        return ids.map { objectId ->
            val o = objectsRepository.findById(objectId, userId, writable = false).orElse(null)
            if (o != null) {
                mapOf(
                        "objectId" to objectId,
                        "unit" to o.unit,
                        "values" to rawValueRepository.findByTimestampBetween(objectId.toInt(), months, from, to)
                )

            } else {
                mapOf("objectId" to objectId, "error" to "Not found.")
            }
        }
    }
}