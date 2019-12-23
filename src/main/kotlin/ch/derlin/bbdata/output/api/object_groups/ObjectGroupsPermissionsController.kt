package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.api.user_groups.UserGroup
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups Permissions", description = "Manage object groups permissions")
class ObjectGroupsPermissionsController(
        private val objectGroupsRepository: ObjectGroupsRepository,
        private val userGroupRepository: UserGroupRepository) {


    @Protected
    @GetMapping("/{id}/permissions")
    fun getPermissions(@UserId userId: Int, @PathVariable(value = "id") id: Long): List<UserGroup> {

        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("objectGroup (${id})")
        }
        return ogrp.allowedUserGroups
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PutMapping("/{id}/permissions")
    fun addPermission(@UserId userId: Int,
                      @PathVariable(value = "id") id: Long,
                      @RequestParam("userGroup", required = true) userGroupId: Int): HttpEntity<Unit> {
        return addRemovePerms(userId, id, userGroupId, add = true)
    }


    @Protected(SecurityConstants.SCOPE_WRITE)
    @DeleteMapping("/{id}/permissions")
    fun removePermission(@UserId userId: Int,
                         @PathVariable(value = "id") id: Long,
                         @RequestParam("userGroup", required = true) userGroupId: Int): HttpEntity<Unit> {
        return addRemovePerms(userId, id, userGroupId, delete = true)
    }


    private fun addRemovePerms(userId: Int, id: Long, userGroupId: Int, add: Boolean = false, delete: Boolean = false): HttpEntity<Unit> {
        // get resources
        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("objectGroup (${id})")
        }
        val ugrp = userGroupRepository.findById(userGroupId).orElseThrow {
            ItemNotFoundException("userGroup ($userGroupId)")
        }
        // check if already allowed
        val found = ogrp.allowedUserGroups.find { it.id == ugrp.id }

        if (add) {
            if (found != null) return ResponseEntity(HttpStatus.NOT_MODIFIED)
            else ogrp.allowedUserGroups.add(ugrp)
        } else if (delete) {
            if (found == null) return ResponseEntity(HttpStatus.NOT_MODIFIED)
            else ogrp.allowedUserGroups.remove(found)
        }

        // save changes
        objectGroupsRepository.save(ogrp)
        return ResponseEntity(HttpStatus.OK)
    }
}