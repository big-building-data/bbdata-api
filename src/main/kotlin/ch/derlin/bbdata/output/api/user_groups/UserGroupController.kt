package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.output.Constants
import ch.derlin.bbdata.output.api.users.User
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.output.exceptions.AppException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
            getOne(id, userId).getUsers() // TODO: should we be admins for that ?


    /*
    @PutMapping("/userGroups/{id}/users")
    fun addOrUpdateUser(@PathVariable(value = "id") id: Int,
                        @RequestHeader(value = Constants.HEADER_USER) userId: Int,
                        @RequestParam(name = "userId", required = true) newUserId: Int,
                        @RequestParam(name = "admin", required = false) admin: Boolean
    ): ResponseEntity<Unit> {
        userRepository.findById(newUserId).orElseThrow {
            AppException.itemNotFound("The user with id '${newUserId}' does not exist.")
        }
        val ugrp = userGroupRepository.findMine(userId, id, admin = true).orElseThrow {
            AppException.itemNotFound()
        }
        // TODO: what if a user removes its own admin rights ? Or there are no admins left ?
        if (ugrp.addUser(newUserId, admin)) {
            userGroupRepository.saveAndFlush(ugrp)
            return ResponseEntity(HttpStatus.OK)
        } else {
            return ResponseEntity(HttpStatus.NOT_MODIFIED)
        }

    }
     */

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
        userRepository.findById(newUserId).orElseThrow {
            AppException.itemNotFound("The user with id '${newUserId}' does not exist.")
        }

        val optional = userGroupMappingRepository.findById(UserUgrpMappingId(newUserId, id))
        if (optional.isPresent()) {
            val mapping = optional.get()
            if (mapping.isAdmin == admin) {
                return ResponseEntity(HttpStatus.NOT_MODIFIED)
            } else {
                mapping.isAdmin = admin
                userGroupMappingRepository.save(mapping)
                return ResponseEntity(HttpStatus.OK)
            }
        }
        userGroupMappingRepository.save(UserUgrpMapping(userId = newUserId, groupId = id, isAdmin = admin))
        return ResponseEntity(HttpStatus.OK)
    }

    @DeleteMapping("/userGroups/{id}/users")
    fun deleteUserMapping(@PathVariable(value = "id") id: Int,
                          @RequestHeader(value = Constants.HEADER_USER) userId: Int,
                          @RequestParam(name = "userId", required = true) userIdToDelete: Int
    ): ResponseEntity<Unit> {

        val optional = userGroupMappingRepository.findById(UserUgrpMappingId(userIdToDelete, id))
        if (optional.isPresent()) {
            userGroupMappingRepository.delete(optional.get())
            return ResponseEntity(HttpStatus.OK)
        }
        return ResponseEntity(HttpStatus.NOT_MODIFIED)
    }
}