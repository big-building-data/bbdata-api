package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.output.api.users.User
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.output.exceptions.ForbiddenException
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

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
    fun getAll(@UserId userId: Int): List<UserGroup> =
            userGroupRepository.findAll()

    @Protected
    @GetMapping("/mine/groups")
    fun getMines(@UserId userId: Int,
                 @RequestParam(name = "admin", required = false, defaultValue = "false") admin: Boolean): List<UserGroup> =
            userGroupRepository.findMines(userId, admin)

    @Protected
    @GetMapping("/userGroups/{id}")
    fun getOne(@UserId userId: Int,
               @PathVariable(value = "id") id: Int): UserGroup =
            // TODO: admins only ? return list of users ?
            userGroupRepository.findById(id).orElseThrow { ItemNotFoundException("usergroup (${id})") }

    @Protected
    @GetMapping("/userGroups/{id}/users")
    fun getUsers(@UserId userId: Int,
                 @PathVariable(value = "id") id: Int): List<UserUgrpMapping> =
            getOne(id, userId).userMappings // TODO: should we be admins for that ?
}

@RestController
@Tag(name = "UserGroups", description = "Manage user groups")
class UserGroupMappingController(
        private val userGroupMappingRepository: UserGroupMappingRepository,
        private val userRepository: UserRepository) {

    @Protected(SecurityConstants.SCOPE_WRITE)
    @ApiResponse(responseCode = "304", description = "Not modified.")
    @PutMapping("/userGroups/{id}/users")
    fun addOrUpdateUserMapping(@UserId userId: Int,
                               @PathVariable(value = "id") id: Int,
                               @RequestParam(name = "userId", required = true) newUserId: Int,
                               @RequestParam(name = "admin", required = false, defaultValue = "false") admin: Boolean
    ): ResponseEntity<Unit> {
        canUserModifyGroup(userId, id) // ensure the user has the right to update members of this group
        // ensure the user we want to update exists
        userRepository.findById(newUserId).orElseThrow { ItemNotFoundException("user ($newUserId)") }
        // do the deed, either updating or creating a new mapping
        val optional = userGroupMappingRepository.findById(UserUgrpMappingId(newUserId, id))
        if (optional.isPresent()) {
            // mapping exists, this is an update
            val mapping = optional.get()
            if (mapping.isAdmin == admin) {
                return ResponseEntity(HttpStatus.NOT_MODIFIED)
            } else {
                mapping.isAdmin = admin
                userGroupMappingRepository.save(mapping)
                return ResponseEntity(HttpStatus.OK)
            }
        }
        // mapping doesn't exist, create a new one
        userGroupMappingRepository.save(UserUgrpMapping(userId = newUserId, groupId = id, isAdmin = admin))
        return ResponseEntity(HttpStatus.OK)
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @ApiResponse(responseCode = "304", description = "Not modified.")
    @DeleteMapping("/userGroups/{id}/users")
    fun deleteUserMapping(@UserId userId: Int,
                          @PathVariable(value = "id") id: Int,
                          @RequestParam(name = "userId", required = true) userIdToDelete: Int
    ): ResponseEntity<Unit> {
        canUserModifyGroup(userId, id) // ensure the user has the right to delete a member from the group

        val optional = userGroupMappingRepository.findById(UserUgrpMappingId(userIdToDelete, id))
        if (optional.isPresent()) {
            userGroupMappingRepository.delete(optional.get())
            return ResponseEntity(HttpStatus.OK)
        }
        return ResponseEntity(HttpStatus.NOT_MODIFIED)
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PutMapping("/userGroups/{id}/users/new")
    fun createUser(@UserId userId: Int,
                   @Valid @RequestBody newUser: User.NewUser,
                   @PathVariable(value = "id") id: Int,
                   @RequestParam(name = "admin", required = false, defaultValue = "false") admin: Boolean): User {
        // ensure the user has the right to add a member to the group
        val mapping = canUserModifyGroup(userId, id)
        // create both user and mapping (group permission)
        if (!mapping.isAdmin) throw ForbiddenException("You must be admin to add users.")
        val user = userRepository.saveAndFlush(newUser.toUser()) // use flush to get the generated ID
        userGroupMappingRepository.save(UserUgrpMapping(userId = user.id!!, groupId = id, isAdmin = admin))
        return user
    }

    fun canUserModifyGroup(userId: Int, groupId: Int): UserUgrpMapping =
            // ensure the user has the right to add a member to the group
            userGroupMappingRepository.findById(UserUgrpMappingId(userId, groupId)).orElseThrow {
                ItemNotFoundException("usergroup (${groupId})")
            }
}