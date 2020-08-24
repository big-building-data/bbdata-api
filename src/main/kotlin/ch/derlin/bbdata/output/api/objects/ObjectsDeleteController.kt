package ch.derlin.bbdata.output.api.objects

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.cassandra.RawValueRepository
import ch.derlin.bbdata.common.cassandra.findLatestValue
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile(Profiles.CASSANDRA)
@RestController
@RequestMapping("/objects")
@Tag(name = "Objects", description = "Manage objects")
class ObjectsDeleteController(private val objectsAccessManager: ObjectsAccessManager,
                       private val rawValueRepository: RawValueRepository) {

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Delete an object. " +
            "This is only possible for objects that have never been used, that no value is associated to it. " +
            "Also note that the API has to do an expansive operation to check this precondition, so it may take some time. " +
            "Use it wisely !")
    @DeleteMapping("/{objectId}")
    fun deleteObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long) {
        val obj = objectsAccessManager.findById(id, userId, writable = true).orElseThrow { ItemNotFoundException("object") }
        // here, do not rely on stats.nWrites, we really want to ensure there are no values in cassandra !
        if (rawValueRepository.findLatestValue(objectId = id, from = obj.creationdate!!, to = DateTime.now()) != null)
            throw ForbiddenException("This object has values associated to it. It cannot be deleted.")
        objectsAccessManager.objectRepository.delete(obj)
    }
}