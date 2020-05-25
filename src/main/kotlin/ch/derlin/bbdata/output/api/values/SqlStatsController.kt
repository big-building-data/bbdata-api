package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.stats.SqlStatsRepository
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * date: 25.05.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Profile(Profiles.SQL_STATS)
@RestController
@Tag(name = "Objects Statistics", description = "Get various statistics about object values")
class SqlStatsController(
        private val sqlStatsRepository: SqlStatsRepository) {

    data class SqlStatsS(
            val objectId: Int,
            val avgSamplePeriod: Float, val
            lastTimestamp: DateTime)

    data class SqlStatsC(
            val objectId: Int,
            @get:JsonProperty("nReads") val nReads: Long = 0,
            @get:JsonProperty("nValues") val nValues: Long = 0)


    @Protected
    @GetMapping("/objects/{objectId}/stats")
    fun getObjectStats(@UserId userId: Int,
                       @PathVariable("objectId") id: Long): SqlStatsS {
        val stats = sqlStatsRepository.findById(id).orElseThrow {
            ItemNotFoundException("object (id=$id)")
        }
        return SqlStatsS(stats.objectId!!.toInt(), stats.avgSamplePeriod.toFloat(), stats.lastTs!!)
    }

    @Protected
    @GetMapping("/objects/{objectId}/stats/counters")
    fun getObjectCounters(@UserId userId: Int,
                          @PathVariable("objectId") id: Long): SqlStatsC {
        val stats = sqlStatsRepository.findById(id).orElseThrow {
            ItemNotFoundException("object (id=$id)")
        }
        return SqlStatsC(stats.objectId!!.toInt(), stats.nReads, stats.nWrites)
    }
}
