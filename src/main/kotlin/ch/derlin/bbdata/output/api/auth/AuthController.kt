package ch.derlin.bbdata.output.api.auth

import ch.derlin.bbdata.output.api.users.User
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.output.exceptions.ForbiddenException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RestController

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@Tag(name = "Authentication", description = "login/logout")
class AuthController {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var authRepository: AuthRepository

    @PostMapping("/login")
    fun login(username: String, password: String): Apikey {
        val u: User = userRepository.findByName(username)
        u.id?.let { return authRepository.login(it, password) }
        throw ForbiddenException("User not found.")
    }

    @Protected
    @PostMapping("/logout")
    fun logout(
            @UserId userId: Int,
            @RequestAttribute(value = SecurityConstants.HEADER_TOKEN) apikey: String
    ): ResponseEntity<Any> {
        val ok = authRepository.logout(userId, apikey)
        val status_code = if (ok) HttpStatus.OK else HttpStatus.NOT_MODIFIED
        return ResponseEntity(status_code)
    }
}