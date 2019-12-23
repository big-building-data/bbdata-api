package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.api.objects.ObjectRepository
import ch.derlin.bbdata.output.api.objects.Objects
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups", description = "Manage object groups")
class ObjectGroupsObjectController(
        private val objectGroupsRepository: ObjectGroupsRepository,
        private val objectRepository: ObjectRepository) {

    @Protected
    @GetMapping("/{id}/objects")
    fun getObjectsOfGroup(@UserId userId: Int, @PathVariable(value = "id") id: Long): MutableList<Objects> {

        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }
        return ogrp.objects
    }

    @Protected
    @PutMapping("/{id}/objects")
    fun addObjectToGroup(@UserId userId: Int,
                         @PathVariable(value = "id") id: Long,
                         @RequestParam("objectId", required = true) objectId: Long): ResponseEntity<Unit> {

        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }

        if (ogrp.objects.find { it.id == objectId } != null) {
            return ResponseEntity(HttpStatus.NOT_MODIFIED)
        }

        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }

        ogrp.objects.add(obj)
        objectGroupsRepository.save(ogrp)
        return ResponseEntity(HttpStatus.OK)
    }

    @Protected
    @DeleteMapping("/{id}/objects")
    fun removeObjectFromGroup(@UserId userId: Int,
                              @PathVariable(value = "id") id: Long,
                              @RequestParam("objectId", required = true) objectId: Long): ResponseEntity<Unit> {

        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }

        var ret = HttpStatus.NOT_MODIFIED
        ogrp.objects.find { it.id == objectId }?.let {
            ogrp.objects.remove(it)
            objectGroupsRepository.save(ogrp)
            ret = HttpStatus.OK

        }
        return ResponseEntity(ret)
    }
}