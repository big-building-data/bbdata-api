package ch.derlin.bbdata.output.api.users

import ch.derlin.bbdata.output.api.user_groups.UserGroupMappingRepository
import ch.derlin.bbdata.output.api.user_groups.UsergroupMapping
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * date: 28.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@Tag(name = "Me", description = "Get information about you (current user)")
class MeController(private val userRepository: UserRepository,
                   private val userGroupMappingRepository: UserGroupMappingRepository) {

    class UsergroupInfo(mapping: UsergroupMapping) {
        val id = mapping.groupId
        val name = mapping.group!!.name
        val admin = mapping.isAdmin
    }

    @Protected
    @GetMapping("/me")
    fun getMe(@UserId userId: Int): User = userRepository.getOne(userId)

    @Protected
    @GetMapping("/me/userGroups")
    fun getMyGroups(@UserId userId: Int): List<UsergroupInfo> =
            userGroupMappingRepository.getByUserId(userId).map { UsergroupInfo(it) }

}