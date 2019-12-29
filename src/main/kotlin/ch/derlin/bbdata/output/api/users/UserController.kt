package ch.derlin.bbdata.output.api.users

import ch.derlin.bbdata.output.api.user_groups.UserGroup
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
class UserController(val userRepository: UserRepository) {
    @Protected
    @GetMapping("/me")
    fun getMe(@UserId userId: Int): User = userRepository.getOne(userId)

    @Protected
    @GetMapping("/me/userGroups") // TODO: add flag admin to return list
    fun getMyGroups(@UserId userId: Int): List<UserGroup> = userRepository.getOne(userId).groups!!

    @Protected
    @GetMapping("/users")
    fun getAll(): List<User> = userRepository.findAll() // TODO: try to comment this endpoint... HATEOAS ! /search
}