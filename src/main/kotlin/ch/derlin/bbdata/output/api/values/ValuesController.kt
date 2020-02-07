package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.common.cassandra.*
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.exceptions.WrongParamsException
import ch.derlin.bbdata.output.api.objects.ObjectRepository
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse


/**
 * date: 10.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@RestController
@Tag(name = "Objects Values", description = "Submit and query objects values")
class ValuesController(
        private val objectsRepository: ObjectRepository,
        private val rawValueRepository: RawValueRepository,
        private val aggregationsRepository: AggregationsRepository,
        private val objectStatsCounterRepository: ObjectStatsCounterRepository,
        private val cassandraObjectStreamer: CassandraObjectStreamer) {


    @Protected
    @GetMapping("/objects/{objectId}/values", produces = ["application/json", "text/plain"])
    @ApiResponse(content = [
        Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = RawValue::class))),
        Content(mediaType = "text/plain", schema = Schema(example = RawValue.csvHeadersString))
    ])
    fun getRawValuesStream(
            @UserId userId: Int,
            @CType contentType: String,
            @PathVariable(name = "objectId") objectId: Long,
            @RequestParam(name = "from", required = true) from: DateTime,
            @RequestParam(name = "to", required = false) optionalTo: DateTime?,
            response: HttpServletResponse) {
        // check params
        checkObject(userId, objectId)
        val to = optionalTo ?: DateTime.now()
        val months = CassandraUtils.monthsBetween(YearMonth(from), YearMonth(to))
        // stream the values
        cassandraObjectStreamer.stream(contentType, response, RawValue.csvHeaders,
                rawValueRepository.findByTimestampBetween(objectId.toInt(), months, from, to))
        // update stats
        objectStatsCounterRepository.updateReadCounter(objectId.toInt())

    }

    @Protected
    @GetMapping("/objects/{objectId}/values/latest", produces = ["application/json", "text/plain"])
    @ApiResponse(content = [
        Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = RawValue::class))),
        Content(mediaType = "text/plain", schema = Schema(example = RawValue.csvHeadersString))])
    fun getLatestValue(
            @UserId userId: Int,
            @CType contentType: String,
            @PathVariable(name = "objectId") objectId: Long,
            @RequestParam(name = "before", required = false) optionalBefore: DateTime?,
            response: HttpServletResponse) {
        // check/prepare params
        val before = optionalBefore ?: DateTime.now()
        val monthBefore = YearMonth(before)
        checkObject(userId, objectId)
        // do the search, one month at a time, stop when one is found
        val searchMonths = CassandraUtils.monthsBetween(
                monthBefore.minusMonths(CassandraUtils.MAX_SEARCH_DEPTH), monthBefore)
        val valuesIter = searchMonths.asSequence()
                .map { rawValueRepository.getLatest(objectId.toInt(), it, before) }
                .dropWhile { it == null }
                .firstOrNull()
        // stream the results
        cassandraObjectStreamer.stream(
                contentType, response, RawValue.csvHeaders,
                if (valuesIter == null) listOf() else listOf(valuesIter))
        // update stats
        objectStatsCounterRepository.updateReadCounter(objectId.toInt())
    }

    @Protected
    @GetMapping("/objects/{objectId}/values/aggregated", produces = ["application/json", "text/plain"])
    @ApiResponse(content = [
        Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = Aggregation::class))),
        Content(mediaType = "text/plain", schema = Schema(example = Aggregation.csvHeadersString))
    ])
    fun getQuarterAggregationsStream(
            @UserId userId: Int,
            @CType contentType: String,
            @PathVariable(name = "objectId") objectId: Long,
            @RequestParam(name = "from", required = true) from: DateTime,
            @RequestParam(name = "to", required = false) to: DateTime?,
            @RequestParam(name = "granularity", defaultValue = "hours") granularity: AggregationGranularity? = null,
            response: HttpServletResponse) {
        // check/prepare params
        checkObject(userId, objectId)
        if (granularity == null) throw WrongParamsException("granularity should be one of ${AggregationGranularity.aceptableValues}")
        val minutes = granularity.minutes
        val to = to ?: DateTime.now()
        val months = CassandraUtils.monthsBetween(YearMonth(from), YearMonth(to))
        // stream the results
        cassandraObjectStreamer.stream(
                contentType, response, Aggregation.csvHeaders,
                aggregationsRepository.findByTimestampBetween(minutes, objectId.toInt(), months, from, to))
        // update stats
        objectStatsCounterRepository.updateReadCounter(objectId.toInt())
    }

    private fun checkObject(userId: Int, objectId: Long) {
        objectsRepository.findById(objectId, userId, writable = false).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
    }


}