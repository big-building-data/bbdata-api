package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.user_groups.UserGroup
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups Permissions", description = "Manage which user group has access to which object group")
class ObjectGroupsPermissionsController(
        private val objectGroupsRepository: ObjectGroupsRepository,
        private val userGroupRepository: UserGroupRepository) {


    @Protected
    @GetMapping("/{objectGroupId}/userGroups")
    fun getPermissions(@UserId userId: Int, @PathVariable(value = "objectGroupId") id: Long): List<UserGroup> {

        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("objectGroup (${id})")
        }
        return ogrp.allowedUserGroups
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @PutMapping("/{objectGroupId}/userGroups/{userGroupId}")
    fun addPermission(@UserId userId: Int,
                      @PathVariable(value = "objectGroupId") id: Long,
                      @PathVariable("userGroupId") userGroupId: Int): ResponseEntity<String> {
        return addRemovePerms(userId, id, userGroupId, add = true)
    }


    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @DeleteMapping("/{objectGroupId}/userGroups/{userGroupId}")
    fun removePermission(@UserId userId: Int,
                         @PathVariable(value = "objectGroupId") id: Long,
                         @PathVariable("userGroupId") userGroupId: Int): ResponseEntity<String> {
        return addRemovePerms(userId, id, userGroupId, delete = true)
    }


    private fun addRemovePerms(userId: Int, id: Long, userGroupId: Int, add: Boolean = false, delete: Boolean = false): ResponseEntity<String> {
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
            if (found != null) return CommonResponses.notModifed()
            else ogrp.allowedUserGroups.add(ugrp)
        } else if (delete) {
            if (found == null) return CommonResponses.notModifed()
            else ogrp.allowedUserGroups.remove(found)
        }

        // save changes
        objectGroupsRepository.save(ogrp)
        return CommonResponses.ok()
    }
}
