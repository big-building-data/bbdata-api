package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.user_groups.UserGroup
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups Permissions", description = "Manage which user group has access to which object group")
class ObjectGroupsPermissionsController(
        private val objectGroupAccessManager: ObjectGroupAccessManager,
        private val userGroupRepository: UserGroupRepository) {


    @Protected
    @Operation(description = "Get the list of user groups having access to an object group you own.")
    @GetMapping("/{objectGroupId}/userGroups")
    fun getPermissions(@UserId userId: Int, @PathVariable(value = "objectGroupId") id: Long): List<UserGroup> {

        val ogrp = objectGroupAccessManager.findOne(userId, id, writable = true).orElseThrow {
            ItemNotFoundException("objectGroup (${id})")
        }
        return ogrp.allowedUserGroups
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Give a user group access to an object group you own. " +
            "If the user group already has access, it simply returns NOT MODIFIED.")
    @SimpleModificationStatusResponse
    @PutMapping("/{objectGroupId}/userGroups/{userGroupId}")
    fun addPermission(@UserId userId: Int,
                      @PathVariable(value = "objectGroupId") id: Long,
                      @PathVariable("userGroupId") userGroupId: Int): ResponseEntity<String> {
        return addRemovePerms(userId, id, userGroupId, add = true)
    }


    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Revoke the access of a user group to an object group you own. " +
            "If the user group doesn't have access, it simply returns NOT MODIFIED.")
    @SimpleModificationStatusResponse
    @DeleteMapping("/{objectGroupId}/userGroups/{userGroupId}")
    fun removePermission(@UserId userId: Int,
                         @PathVariable(value = "objectGroupId") id: Long,
                         @PathVariable("userGroupId") userGroupId: Int): ResponseEntity<String> {
        return addRemovePerms(userId, id, userGroupId, delete = true)
    }


    private fun addRemovePerms(userId: Int, id: Long, userGroupId: Int, add: Boolean = false, delete: Boolean = false): ResponseEntity<String> {
        // get resources
        val ogrp = objectGroupAccessManager.findOne(userId, id, writable = true).orElseThrow {
            ItemNotFoundException("objectGroup (${id})")
        }
        val ugrp = userGroupRepository.findById(userGroupId).orElseThrow {
            ItemNotFoundException("userGroup ($userGroupId)")
        }
        // check if already allowed
        val found = ogrp.allowedUserGroups.find { it.id == ugrp.id }

        if (add) {
            if (found != null) return CommonResponses.notModifed()
            else ogrp.allowedUserGroups.add(ugrp)
        } else if (delete) {
            if (found == null) return CommonResponses.notModifed()
            else ogrp.allowedUserGroups.remove(found)
        }

        // save changes
        objectGroupAccessManager.objectGroupsRepository.save(ogrp)
        return CommonResponses.ok()
    }
}
