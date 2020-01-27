package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
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
        private val userGroupRepository: UserGroupRepository) {

    @Protected
    @GetMapping("/userGroups")
    fun getUserGroups(@UserId userId: Int): List<UserGroup> =
            userGroupRepository.findAll()

    @Protected
    @GetMapping("/mine/groups") // TODO
    fun getMyUserGroups(@UserId userId: Int,
                 @RequestParam(name = "admin", required = false, defaultValue = "false") admin: Boolean): List<UserGroup> =
            userGroupRepository.findMines(userId, admin)

    @Protected
    @GetMapping("/userGroups/{id}")
    fun getUserGroup(@UserId userId: Int,
               @PathVariable(value = "id") id: Int): UserGroup =
            // TODO: admins only ? return list of users ?
            userGroupRepository.findById(id).orElseThrow { ItemNotFoundException("usergroup (${id})") }

    @Protected
    @GetMapping("/userGroups/{id}/users")
    fun getUsersInGroup(@UserId userId: Int,
                 @PathVariable(value = "id") id: Int): List<UsergroupMapping> =
            getUserGroup(userId, id).userMappings // TODO: return users instead ? only for admins ?

}

@RestController
@Tag(name = "UserGroups", description = "Manage user groups")
class AddDeleteUserGroupController(
        private val userGroupRepository: UserGroupRepository,
        private val userGroupMappingRepository: UserGroupMappingRepository) {

    class NewUserGroup {
        @NotNull
        @Size(min = UserGroup.NAME_MIN, max = UserGroup.NAME_MAX)
        val name: String? = null
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PutMapping("/userGroups")
    fun createUserGroup(@UserId userId: Int,
                        @Valid @NotNull @RequestBody newUserGroupBody: NewUserGroup): UserGroup {
        // create
        val ugrp = userGroupRepository.saveAndFlush(UserGroup(name = newUserGroupBody.name!!))
        // add permission
        userGroupMappingRepository.save(UsergroupMapping(userId = userId, groupId = ugrp.id!!, isAdmin = true))
        return ugrp
    }


    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @DeleteMapping("/userGroups/{id}")
    fun deleteUserGroup(@UserId userId: Int,
                        @PathVariable("id") id: Int): ResponseEntity<String> {
        if (!userGroupRepository.findById(id).isPresent) {
            return CommonResponses.notModifed()
        }
        val ugrp = userGroupRepository.findMine(userId, id, admin = true).orElseThrow {
            ForbiddenException("Only admins can delete usergroups")
        }

        // first remove all user mappings, then delete group
        // TODO: find a better way ?
        userGroupMappingRepository.deleteByGroupId(ugrp.id!!)
        userGroupMappingRepository.flush()
        userGroupRepository.delete(ugrp)

        return CommonResponses.ok()
    }

}

