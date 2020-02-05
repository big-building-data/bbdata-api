package ch.derlin.bbdata.output.api.users

import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.api.user_groups.UserGroup
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

/**
 * date: 28.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@Tag(name = "Users", description = "Get or create users")
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
    @GetMapping("/users")
    fun getUsers(): List<User> = userRepository.findAll() // TODO: try to comment this endpoint... HATEOAS ! /search

    @Protected
    @GetMapping("/users/{userId}")
    fun getUser(@PathVariable("userId") userId: Int): User = userRepository.findById(userId).orElseThrow {
        ItemNotFoundException("user ($userId)")
    }

    @Operation(description = "Create a new user. Note that as long as he is not part of any userGroup, he won't have access to resources.")
    @Protected(SecurityConstants.SCOPE_WRITE)
    @PutMapping("/users")
    fun createUser(@Valid @RequestBody newUser: NewUser): User {
        // Note: the user won't be part of any userGroup, so he has to be added through another call
        // to the ObjectGroupsPermissionController
        return userRepository.saveAndFlush(newUser.toUser())
    }
}