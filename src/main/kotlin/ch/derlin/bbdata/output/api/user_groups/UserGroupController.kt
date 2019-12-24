package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
                 @PathVariable(value = "id") id: Int): List<UsergroupMapping> =
            getOne(id, userId).userMappings // TODO: should we be admins for that ?
}

