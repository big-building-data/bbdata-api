package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.objects.Objects
import ch.derlin.bbdata.output.api.objects.ObjectsAccessManager
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups", description = "Manage object groups")
class ObjectGroupsObjectController(
        private val objectGroupAccessManager: ObjectGroupAccessManager,
        private val objectsAccessManager: ObjectsAccessManager) {

    @Protected
    @Operation(description = "Get all objects belonging to an object group.")
    @GetMapping("/{objectGroupId}/objects")
    fun getObjectsOfGroup(@UserId userId: Int, @PathVariable(value = "objectGroupId") id: Long): MutableList<Objects> {

        val ogrp = objectGroupAccessManager.findOne(userId, id).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }
        return ogrp.objects
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Add an object to an object group you own. " +
            "If the object is already present, it simply returns NOT MODIFIED.")
    @SimpleModificationStatusResponse
    @PutMapping("/{objectGroupId}/objects/{objectId}")
    fun addObjectToGroup(@UserId userId: Int,
                         @PathVariable(value = "objectGroupId") id: Long,
                         @PathVariable("objectId") objectId: Long): ResponseEntity<String> {

        val ogrp = objectGroupAccessManager.findOne(userId, id, writable = true).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }

        if (ogrp.objects.find { it.id == objectId } != null) {
            return CommonResponses.notModifed()
        }

        val obj = objectsAccessManager.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }

        ogrp.objects.add(obj)
        objectGroupAccessManager.objectGroupsRepository.save(ogrp)
        return CommonResponses.ok()
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Remove an object to an object group you own. " +
            "If the object is not present, it simply returns NOT MODIFIED.")
    @SimpleModificationStatusResponse
    @DeleteMapping("/{objectGroupId}/objects/{objectId}")
    fun removeObjectFromGroup(@UserId userId: Int,
                              @PathVariable(value = "objectGroupId") id: Long,
                              @PathVariable("objectId") objectId: Long): ResponseEntity<String> {

        val ogrp = objectGroupAccessManager.findOne(userId, id, writable = true).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }
        val found = ogrp.objects.find { it.id == objectId }

        if (found != null) {
            ogrp.objects.remove(found)
            objectGroupAccessManager.objectGroupsRepository.save(ogrp)
            return CommonResponses.ok()

        }
        return CommonResponses.notModifed()
    }
}