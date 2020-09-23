package ch.derlin.bbdata.output.api.apikeys

import ch.derlin.bbdata.common.Beans
import ch.derlin.bbdata.common.dates.DurationParser
import ch.derlin.bbdata.common.dates.JodaUtils
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.joda.time.MutablePeriod
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size


/**
 * date: 27.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@Tag(name = "Authentication", description = "Login/Logout and manage API keys")
class ApikeyController(
        private val apikeyRepository: ApikeyRepository,
        private val userRepository: UserRepository) {

    private val log: Logger = LoggerFactory.getLogger(ApikeyController::class.java)

    class LoginBody {
        @NotNull
        val username: String? = null

        @NotNull
        val password: String? = null
    }

    class EditableApikeyFields {
        val readOnly: Boolean? = null

        val expirationDate: String? = null

        @Size(max = Beans.DESCRIPTION_MAX)
        val description: String? = null
    }

    @PostMapping("/login")
    @Operation(description = "Login to the API using username/password. " +
            "It will create and return a writable apikey valid for $AUTOLOGIN_EXPIRE_HOURS hours.<br> " +
            "To access the other endpoints, use your user ID and the 32-char apikey secret returned.")
    fun login(@Valid @NotNull @RequestBody loginBody: LoginBody): Apikey {
        val optionalUserId = userRepository.findByName(loginBody.username!!).map { it.id }
        if (optionalUserId.isPresent && apikeyRepository.canLogin(optionalUserId.get(), loginBody.password!!) > 0) {
            return apikeyRepository.saveAndFlush(Apikey(
                    userId = optionalUserId.get(),
                    readOnly = false,
                    description = AUTOLOGIN_DESCRIPTION,
                    secret = TokenGenerator.generate(),
                    expirationDate = DateTime().plus(AUTOLOGIN_EXPIRE)
            ))
        }
        log.info("invalid login for username='${loginBody.username}' password='${loginBody.password}'")
        throw ForbiddenException("Wrong username or password.")
    }

    @Protected
    @Operation(description = "Logout from the API by deleting the apikey used for the request.")
    @PostMapping("/logout")
    fun logout(@UserId userId: Int, @RequestAttribute(SecurityConstants.HEADER_TOKEN) apikey: String) {
        apikeyRepository.findValid(userId, apikey).map { apikeyRepository.delete(it) }
    }

    @GetMapping("/apikeys")
    @Operation(description = "Get all your apikeys. " +
            "__IMPORTANT__: while being a get request, this requires endpoint a _writable_ apikey.")
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun getApikeys(@UserId userId: Int): List<Apikey> = apikeyRepository.findByUserId(userId)

    @PutMapping("/apikeys")
    @Operation(description = "Create an apikey, writable or read-only.<br>" +
            "`expirationDate`can either be an ISO datetime string (UTC) or a duration (1d, 1d-3h, 3h, etc.). " +
            "For no expirationDate, either don't use the parameter or set it _explicitly_ to `null`.")
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun createApikey(
            @UserId userId: Int, // TODO: writable => readOnly
            @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean,
            @RequestParam("expirationDate", required = false) rawExpire: String?,
            @Valid @RequestBody descriptionBean: Beans.Description?): Apikey {

        return apikeyRepository.saveAndFlush(Apikey(
                userId = userId,
                readOnly = !writable,
                description = descriptionBean?.description,
                secret = TokenGenerator.generate(),
                expirationDate = rawExpire?.let { parseDateOrDuration(it) }
        ))
    }

    @PostMapping("/apikeys/{apikeyId}")
    @Operation(description = "Edit an apikey. Missing or blank (null) fields won't be updated. " +
            "`expirationDate`can either be an ISO datetime string (UTC) or a duration (1d, 1d-3h, 3h, etc.). " +
            "To __unset__ `expirationDate`, pass the explicit string value `\"null\"`.")
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun editApikey(
            @UserId userId: Int,
            @PathVariable("apikeyId") id: Int,
            @Valid @NotNull @RequestBody bean: EditableApikeyFields): Apikey {

        val apikey = apikeyRepository.findByIdAndUserId(id, userId).orElseThrow { ItemNotFoundException("apikey (id=$id)") }

        bean.expirationDate?.let { apikey.expirationDate = parseDateOrDuration(it) }
        bean.readOnly?.let { apikey.readOnly = it }
        bean.description?.let { apikey.description = it }

        return apikeyRepository.saveAndFlush(apikey)
    }


    @DeleteMapping("/apikeys/{apikeyId}")
    @SimpleModificationStatusResponse
    @Operation(description = "Delete an apikey using the apikey ID.<br>" +
            "_NOTE_: to delete the active apikey, using the endpoint `/logout` is preferred.")
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun deleteApikey(@UserId userId: Int, @PathVariable("apikeyId") id: Int): ResponseEntity<String> {
        val apikey = apikeyRepository.findByIdAndUserId(id, userId)
        if (apikey.isPresent) {
            apikeyRepository.delete(apikey.get())
            return CommonResponses.ok()
        } else {
            return CommonResponses.notModifed()
        }
    }

    companion object {
        const val AUTOLOGIN_DESCRIPTION = "auto_login"
        const val AUTOLOGIN_EXPIRE_HOURS = 13

        val AUTOLOGIN_EXPIRE = MutablePeriod(AUTOLOGIN_EXPIRE_HOURS, 0, 0, 0).toPeriod()

        fun parseDateOrDuration(raw: String): DateTime? {
            if (raw.trim().toLowerCase() == "null") return null
            val dt = JodaUtils.parseOrNull(raw.replace("Z", ""))
            return dt ?: DurationParser.parseIntoDate(raw)
        }
    }
}