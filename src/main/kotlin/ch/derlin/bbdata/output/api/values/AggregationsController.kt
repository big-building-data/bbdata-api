package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.common.cassandra.*
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.joda.time.YearMonth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

/**
 * date: 11.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@RestController
@Tag(name = "Values Aggregated", description = "Get aggregated object values")
class AggregationsController(private val aggregationsRepository: AggregationsRepository,
                             private val cassandraObjectStreamer: CassandraObjectStreamer) {

    val quarters = 15
    val hours = 60

    @Protected
    @GetMapping("/values/quarters", produces = ["application/json", "text/plain"])
    @ApiResponse(content = [
        Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = Aggregation::class))),
        Content(mediaType = "text/plain", schema = Schema(example = Aggregation.csvHeadersString))
    ])
    fun getQuarterAggregationsStream(
            @UserId userId: Int,
            @CType contentType: String,
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
    @ApiResponse(content = [
        Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = Aggregation::class))),
        Content(mediaType = "text/plain", schema = Schema(example = Aggregation.csvHeadersString))
    ])
    fun getHoursAggregationsStream(
            @UserId userId: Int,
            @CType contentType: String,
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