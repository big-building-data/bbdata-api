package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.stats.Stats
import ch.derlin.bbdata.common.stats.StatsLogic
import ch.derlin.bbdata.output.api.objects.ObjectsAccessManager
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * date: 24.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Profile("${Profiles.CASSANDRA} || ${Profiles.SQL_STATS}")
@RestController
@Tag(name = "Objects Statistics", description = "Get various statistics about object values")
class StatsController(
        private val objectsAccessManager: ObjectsAccessManager,
        private val statsLogic: StatsLogic) {
    @Protected
    @Operation(description = "Get statistics about an object, such as the number of read/writes, the last timestamp submitted, etc. ")
    @GetMapping("/objects/{objectId}/stats")
    fun getObjectStats(@UserId userId: Int,
                       @PathVariable("objectId") id: Long): Stats {
        objectsAccessManager.findById(id, userId, writable = false).orElseThrow {
            ItemNotFoundException("object (id=$id)")
        }
        return statsLogic.getStats(objectId = id)
    }
}