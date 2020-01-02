package ch.derlin.bbdata.output.api.users

import ch.derlin.bbdata.output.api.user_groups.UserGroup
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

/**
 * date: 28.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@Tag(name = "Me", description = "Get information about you (current user)")
class UserController(val userRepository: UserRepository) {

    // TODO: where to put NewX classes ? controller or model ?
    class NewUser {
        @NotNull
        @Size(min = User.NAME_MIN, max = User.NAME_MAX)
        val name: String? = null

        @NotNull
        @Size(min = User.PASSWORD_MIN, max = User.PASSWORD_MAX)
        val password: String? = null

        @NotEmpty
        @Size(max = User.EMAIL_MAX)
        @Pattern(regexp = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message = "Invalid email")
        val email: String? = null

        fun toUser(): User = User(name = name!!, password = User.hashPassword(password!!), email = email!!)

    }
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