package ch.derlin.bbdata.output.api.apikeys

import ch.derlin.bbdata.common.Beans
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.common.dates.DurationParser
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.joda.time.MutablePeriod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotNull


/**
 * date: 27.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@Tag(name = "Authentication", description = "Login/Logout and manage API keys")
class ApikeyController(
        private val apikeyRepository: ApikeyRepository,
        private val userRepository: UserRepository) {

    class LoginBody {
        @NotNull
        val username: String? = null

        @NotNull
        val password: String? = null
    }

    @PostMapping("/login")
    fun login(@Valid @NotNull @RequestBody loginBody: LoginBody): Apikey {
        val optionalUserId = userRepository.findByName(loginBody.username!!).map { it.id }
        if (optionalUserId.isPresent && apikeyRepository.canLogin(optionalUserId.get(), loginBody.password!!) > 0) {
            return apikeyRepository.saveAndFlush(Apikey(
                    userId = optionalUserId.get(),
                    isReadOnly = false,
                    description = AUTOLOGIN_DESCRIPTION,
                    secret = TokenGenerator.generate(),
                    expirationDate = DateTime().plus(AUTOLOGIN_EXPIRE)
            ))
        }
        throw ForbiddenException("Wrong username or password.")
    }

    @Protected
    @PostMapping("/logout")
    fun logout(@UserId userId: Int, @RequestAttribute(SecurityConstants.HEADER_TOKEN) apikey: String): Unit {
        apikeyRepository.findValid(userId, apikey).map { apikeyRepository.delete(it) }
    }

    @GetMapping("/apikeys")
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun getApikeys(@UserId userId: Int): List<Apikey> = apikeyRepository.findByUserId(userId)

    @PutMapping("/apikeys")
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun createApikey(
            @UserId userId: Int,
            @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean,
            @RequestParam("expire", required = false) rawExpire: String?,
            @Valid @RequestBody descriptionBean: Beans.Description): Apikey {

        return apikeyRepository.saveAndFlush(Apikey(
                userId = userId,
                isReadOnly = !writable,
                description = descriptionBean.description,
                secret = TokenGenerator.generate(),
                expirationDate = rawExpire?.let { DurationParser.parseIntoDate(it) }
        ))
    }


    @DeleteMapping("/apikeys/{id}")
    @SimpleModificationStatusResponse
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun deleteApikey(@UserId userId: Int, @PathVariable("id") id: Int): ResponseEntity<String> {
        val apikey = apikeyRepository.findByIdAndUserId(id, userId)
        if (apikey == null) return CommonResponses.notModifed()
        apikeyRepository.delete(apikey)
        return CommonResponses.ok()
    }

    companion object {
        val AUTOLOGIN_DESCRIPTION = "auto_login"
        val AUTOLOGIN_EXPIRE = MutablePeriod(13, 0, 0, 0).toPeriod()
    }
}