package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.output.Constants
import ch.derlin.bbdata.output.api.users.User
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.security.ApikeyWrite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

/**
 * date: 06.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@RestController
class UserGroupController(
        private val userGroupRepository: UserGroupRepository) {

    @GetMapping("/userGroups")
    fun getAll(@RequestHeader(value = Constants.HEADER_USER) userId: Int): List<UserGroup> =
            userGroupRepository.findAll()

    @GetMapping("/mine/groups")
    fun getMines(@PathVariable(value = "id") id: Int,
                 @RequestHeader(value = Constants.HEADER_USER) userId: Int,
                 @RequestParam(name = "admin", required = false) admin: Boolean): List<UserGroup> =
            userGroupRepository.findMines(userId, admin)


    @GetMapping("/userGroups/{id}")
    fun getOne(@PathVariable(value = "id") id: Int,
               @RequestHeader(value = Constants.HEADER_USER) userId: Int): UserGroup =
            // TODO: admins only ? return list of users ?
            userGroupRepository.findById(id).orElseThrow {
                AppException.itemNotFound("No usergroup with id '${id}'")
            }


    @GetMapping("/userGroups/{id}/users")
    fun getUsers(@PathVariable(value = "id") id: Int,
                 @RequestHeader(value = Constants.HEADER_USER) userId: Int): List<UserUgrpMapping> =
            getOne(id, userId).userMappings // TODO: should we be admins for that ?
}

@RestController
class UserGroupMappingController(
        private val userGroupMappingRepository: UserGroupMappingRepository,
        private val userRepository: UserRepository) {

    @PutMapping("/userGroups/{id}/users")
    fun addOrUpdateUserMapping(@PathVariable(value = "id") id: Int,
                               @RequestHeader(value = Constants.HEADER_USER) userId: Int,
                               @RequestParam(name = "userId", required = true) newUserId: Int,
                               @RequestParam(name = "admin", required = false) admin: Boolean
    ): ResponseEntity<Unit> {
        canUserModifyGroup(userId, id) // ensure the user has the right to update members of this group
        // ensure the user we want to update exists
        userRepository.findById(newUserId).orElseThrow {
            AppException.itemNotFound("The user with id '${newUserId}' does not exist.")
        }
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

    @DeleteMapping("/userGroups/{id}/users")
    fun deleteUserMapping(@PathVariable(value = "id") id: Int,
                          @RequestHeader(value = Constants.HEADER_USER) userId: Int,
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

    @ApikeyWrite
    @PutMapping("/userGroups/{id}/users/new")
    fun createUser(@Valid @RequestBody newUser: User.NewUser,
                   @PathVariable(value = "id") id: Int,
                   @RequestHeader(value = Constants.HEADER_USER) userId: Int,
                   @RequestParam(name = "admin", required = false) admin: Boolean): User {
        // ensure the user has the right to add a member to the group
        val mapping = canUserModifyGroup(userId, id)
        // create both user and mapping (group permission)
        if (!mapping.isAdmin) throw AppException.forbidden("You must be admin to add users.")
        val user = userRepository.saveAndFlush(newUser.toUser()) // use flush to get the generated ID
        userGroupMappingRepository.save(UserUgrpMapping(userId = user.id!!, groupId = id, isAdmin = admin))
        return user
    }

    fun canUserModifyGroup(userId: Int, groupId: Int): UserUgrpMapping =
            // ensure the user has the right to add a member to the group
            userGroupMappingRepository.findById(UserUgrpMappingId(userId, groupId)).orElseThrow {
                AppException.itemNotFound("UserGroup '${groupId}' not found or not accessible.")
            }
}