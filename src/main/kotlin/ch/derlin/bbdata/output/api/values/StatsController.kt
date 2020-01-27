package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.common.cassandra.ObjectStats
import ch.derlin.bbdata.common.cassandra.ObjectStatsCounter
import ch.derlin.bbdata.common.cassandra.ObjectStatsCounterRepository
import ch.derlin.bbdata.common.cassandra.ObjectStatsRepository
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.websocket.server.PathParam

/**
 * date: 25.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@RestController
@Tag(name = "Objects")
class StatsController(
        private val objectStatsRepository: ObjectStatsRepository,
        private val objectStatsCounterRepository: ObjectStatsCounterRepository
) {

    @Protected
    @GetMapping("/objects/{id}/stats")
    fun getObjectStats(@UserId userId: Int,
                       @PathParam("id") id: Int): ObjectStats? {
        return objectStatsRepository.findById(id).orElseThrow {
            ItemNotFoundException("object (id=$id)")
        }
    }

    @Protected
    @GetMapping("/objects/{id}/counters")
    fun getObjectCounters(@UserId userId: Int,
                       @PathParam("id") id: Int): ObjectStatsCounter? {
        return objectStatsCounterRepository.findById(id).orElseThrow {
            ItemNotFoundException("object (id=$id)")
        }
    }
}


