package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.exceptions.WrongParamsException
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * date: 06.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@RestController
@Tag(name = "UserGroups", description = "Manage user groups")
class UserGroupController(
        private val userGroupAccessManager: UserGroupAccessManager) {

    class NewUserGroup {
        @NotNull
        @Size(min = UserGroup.NAME_MIN, max = UserGroup.NAME_MAX)
        val name: String? = null
    }

    @Protected
    @Operation(description = "Get the list of existing user groups.")
    @GetMapping("/userGroups")
    fun getUserGroups(@UserId userId: Int): List<UserGroup> =
            userGroupAccessManager.userGroupRepository.findAll()


    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Create a new user group. You will automatically be made admin of it.")
    @Transactional
    @PutMapping("/userGroups")
    fun createUserGroup(@UserId userId: Int,
                        @Valid @NotNull @RequestBody newUserGroupBody: NewUserGroup): UserGroup {
        // create
        val ugrp = userGroupAccessManager.userGroupRepository.saveAndFlush(UserGroup(name = newUserGroupBody.name!!))
        // add permissions
        userGroupAccessManager.userGroupMappingRepository.save(UsergroupMapping(userId = userId, groupId = ugrp.id!!, isAdmin = true))
        return ugrp
    }

    @Protected
    @Operation(description = "Get the details of a user group.<br>" +
            "_NOTE_: To get the users part of it, use /userGroups/ugID/users instead.")
    @GetMapping("/userGroups/{userGroupId}")
    fun getUserGroup(@UserId userId: Int,
                     @PathVariable(value = "userGroupId") id: Int): UserGroup =
            // TODO: admins only ? return list of users ?
            userGroupAccessManager.userGroupRepository.findById(id).orElseThrow { ItemNotFoundException("usergroup (${id})") }

    @Protected
    @Operation(description = "Get the users belonging to a user group, along with their role (admin or not).")
    @GetMapping("/userGroups/{userGroupId}/users")
    fun getUsersInGroup(@UserId userId: Int,
                        @PathVariable(value = "userGroupId") id: Int): List<UsergroupMapping> =
            userGroupAccessManager.getAccessibleGroup(userId, id, admin = false).orElseThrow {
                ItemNotFoundException("userGroup ($id)")
            }.userMappings // TODO: return users instead ? only for admins ?


    @Protected(SecurityConstants.SCOPE_WRITE)
    // checked exception won't trigger rollback by default...
    @Transactional(rollbackFor = [WrongParamsException::class])
    @SimpleModificationStatusResponse
    @Operation(description = "Delete a user group you are admin of.<br>" +
            "__IMORTANT__: only user groups who DO NOT own resources (objects, object groups) can be deleted. " +
            "That is, groups whose purpose is to give _permissions_ to some resources, for a limited amount of time. " +
            "Note also that userGroup with id 1 (SUPERADMIN) cannot be deleted.")
    @DeleteMapping("/userGroups/{userGroupId}")
    fun deleteUserGroup(@UserId userId: Int,
                        @PathVariable("userGroupId") id: Int): ResponseEntity<String> {
        if (id == 1) {
            throw ForbiddenException("Deleting SUPERADMIN group is forbidden.")
        }
        if (!userGroupAccessManager.userGroupRepository.findById(id).isPresent) {
            return CommonResponses.notModifed()
        }
        val ugrp = userGroupAccessManager.getAccessibleGroup(userId, id, admin = true).orElseThrow {
            ForbiddenException("Only admins can delete usergroups")
        }

        // first remove all user mappings, then delete group
        try {
            // do "flush" inside the try/catch so the exception (if any)
            // is thrown now and not at the end of the method invocation
            userGroupAccessManager.userGroupMappingRepository.deleteByGroupId(ugrp.id!!)
            userGroupAccessManager.userGroupMappingRepository.flush()
            userGroupAccessManager.userGroupRepository.delete(ugrp)
            userGroupAccessManager.userGroupRepository.flush()
        } catch (e: Throwable) {
            throw WrongParamsException(
                    "This usergroup owns resources. It cannot be deleted. Ask you admin for support.")
        }

        return CommonResponses.ok()
    }

}

