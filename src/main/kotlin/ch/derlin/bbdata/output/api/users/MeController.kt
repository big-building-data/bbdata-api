package ch.derlin.bbdata.output.api.users

import ch.derlin.bbdata.output.api.user_groups.UserGroupMappingRepository
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.output.api.user_groups.UsergroupMapping
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * date: 28.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@Tag(name = "Me", description = "Get information about you (current user)")
class MeController(private val userRepository: UserRepository,
                   private val userGroupRepository: UserGroupRepository,
                   private val userGroupMappingRepository: UserGroupMappingRepository) {

    data class UsergroupInfo(val id: Int, val name: String, val admin: Boolean) {
        companion object {
            fun fromUserGroupMapping(mapping: UsergroupMapping) = UsergroupInfo(
                    id = mapping.groupId,
                    name = mapping.group!!.name,
                    admin = mapping.isAdmin)
        }
    }

    @Protected
    @Operation(description = "Get details about you.")
    @GetMapping("/me")
    fun getMe(@UserId userId: Int): User = userRepository.getOne(userId)

    @Protected
    @Operation(description = "Get all the user groups you belong to, along with your role (admin or not).")
    @GetMapping("/me/userGroups")
    fun getMyGroups(@UserId userId: Int,
                    @RequestParam("admin", required = false) isAdmin: Boolean = false
    ): List<UsergroupInfo> {
        // if is SUPERADMIN, return all
        if (userGroupMappingRepository.isSuperAdmin(userId)) {
            return userGroupRepository.findAll().map { UsergroupInfo(id = it.id!!, name = it.name, admin = true) }
        }
        val ugrps = userGroupMappingRepository.getByUserId(userId).map { UsergroupInfo.fromUserGroupMapping(it) }
        return if (isAdmin) ugrps.filter { it.admin == true }
        else ugrps
    }

}