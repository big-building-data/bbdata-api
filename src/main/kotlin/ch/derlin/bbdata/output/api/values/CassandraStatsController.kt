package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.cassandra.ObjectStats
import ch.derlin.bbdata.common.cassandra.ObjectStatsCounter
import ch.derlin.bbdata.common.cassandra.ObjectStatsCounterRepository
import ch.derlin.bbdata.common.cassandra.ObjectStatsRepository
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * date: 25.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Profile(Profiles.CASSANDRA_STATS)
@RestController
@Tag(name = "Objects Statistics", description = "Get various statistics about object values")
class CassandraStatsController(
        private val objectStatsRepository: ObjectStatsRepository,
        private val objectStatsCounterRepository: ObjectStatsCounterRepository
) {

    @Protected
    @GetMapping("/objects/{objectId}/stats")
    fun getObjectStats(@UserId userId: Int,
                       @PathVariable("objectId") id: Int): ObjectStats? {
        return objectStatsRepository.findById(id).orElseThrow {
            ItemNotFoundException("object (id=$id)")
        }
    }

    @Protected
    @GetMapping("/objects/{objectId}/stats/counters")
    fun getObjectCounters(@UserId userId: Int,
                          @PathVariable("objectId") id: Int): ObjectStatsCounter? {
        return objectStatsCounterRepository.findById(id).orElseThrow {
            ItemNotFoundException("object (id=$id)")
        }
    }
}


