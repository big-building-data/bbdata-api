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
 * date: 10.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@RestController
@Tag(name = "Values", description = "Get raw object values")
class RawValuesController(private val rawValueRepository: RawValueRepository,
                          private val cassandraObjectStreamer: CassandraObjectStreamer) {


    @Protected
    @GetMapping("/values", produces = ["application/json", "text/plain"])
    @ApiResponse(content = [
        Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = RawValue::class))),
        Content(mediaType = "text/plain", schema = Schema(example = RawValue.csvHeadersString))
    ])
    fun getValuesStream(
            @UserId userId: Int,
            @CType contentType: String,
            @RequestParam(name = "ids", required = true) ids: List<Long>,
            @RequestParam(name = "from", required = true) from: DateTime,
            @RequestParam(name = "to", required = false) optionalTo: DateTime?,
            response: HttpServletResponse) {

        val to = optionalTo ?: DateTime.now()
        val months = CassandraUtils.monthsBetween(YearMonth(from), YearMonth(to))
        cassandraObjectStreamer.stream(contentType, response, userId, ids, RawValue.csvHeaders, {
            rawValueRepository.findByTimestampBetween(it, months, from, to)
        })

    }

    @Protected
    @GetMapping("/values/latest", produces = ["application/json", "text/plain"])
    @ApiResponse(content = [
        Content(mediaType = "application/json", schema = Schema(implementation = RawValue::class)),
        Content(mediaType = "text/plain", schema = Schema(example = RawValue.csvHeadersString))])
    fun getLatestValue(
            @UserId userId: Int,
            @CType contentType: String,
            @RequestParam(name = "ids", required = true) ids: List<Long>,
            @RequestParam(name = "before", required = false) optionalBefore: DateTime?,
            response: HttpServletResponse) {
        val before = optionalBefore ?: DateTime.now()
        val monthBefore = YearMonth(before)
        val searchMonths = CassandraUtils.monthsBetween(
                monthBefore.minusMonths(CassandraUtils.MAX_SEARCH_DEPTH), monthBefore)
        cassandraObjectStreamer.stream(contentType, response, userId, ids, RawValue.csvHeaders, { objectId ->
            val res = searchMonths.asSequence()
                    .map { rawValueRepository.getLatest(objectId, it, before) }
                    .dropWhile { it == null }
                    .firstOrNull()
            if (res == null) listOf() else listOf(res)
        })
    }

}