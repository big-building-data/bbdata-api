package ch.derlin.bbdata.output.api.auth

import ch.derlin.bbdata.output.Constants
import ch.derlin.bbdata.output.api.users.User
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.security.NoHeaderRequired
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
class AuthController {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var authRepository: AuthRepository

    @PostMapping("/login")
    @NoHeaderRequired
    fun login(username: String, password: String): Apikey {
        val u: User = userRepository.findByName(username)
        u.id?.let { return authRepository.login(it, password) }
        throw AppException.forbidden(msg = "User not found.")
    }

    @PostMapping("/logout")
    fun logout(
            @RequestHeader(value = Constants.HEADER_USER) userId: Int,
            @RequestHeader(value = Constants.HEADER_TOKEN) apikey: String
    ): ResponseEntity<Any> {
        val ok = authRepository.logout(userId, apikey)
        val status_code = if (ok) HttpStatus.OK else HttpStatus.NOT_MODIFIED
        return ResponseEntity(status_code)
    }
}