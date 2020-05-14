package ch.derlin.bbdata.output.api.apikeys

import ch.derlin.bbdata.common.Beans
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.common.dates.DurationParser
import ch.derlin.bbdata.common.dates.JodaUtils
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.api.object_groups.ObjectGroup
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.joda.time.MutablePeriod
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

    class LoginBody {
        @NotNull
        val username: String? = null

        @NotNull
        val password: String? = null
    }

    class EditableFields {
        val readOnly: Boolean? = null

        val expirationDate: String? = null

        @Size(max = Beans.DESCRIPTION_MAX)
        val description: String? = null
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
    @Operation(description = "Create an apikey. " +
            "`expirationDate`can either be an ISO datetime string (UTC) or a duration (1d, 1d-3h, 3h, etc.). " +
            "For no expirationDate, either don't use the parameter or set it explicitly to null.")
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun createApikey(
            @UserId userId: Int, // TODO: writable => readOnly
            @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean,
            @RequestParam("expirationDate", required = false) rawExpire: String?,
            @Valid @RequestBody descriptionBean: Beans.Description?): Apikey {

        return apikeyRepository.saveAndFlush(Apikey(
                userId = userId,
                isReadOnly = !writable,
                description = descriptionBean?.description,
                secret = TokenGenerator.generate(),
                expirationDate = rawExpire?.let { parseDateOrDuration(it) }
        ))
    }

    @PostMapping("/apikeys/{apikeyId}")
    @Operation(description = "Edit an apikey. Missing or blank (null) fields won't be updated. " +
            "`expirationDate`can either be an ISO datetime string (UTC) or a duration (1d, 1d-3h, 3h, etc.). " +
            "__IMPORTANT__: to unset `expirationDate`, pass the explicit string value `\"null\"`.")
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun editApikey(
            @UserId userId: Int,
            @PathVariable("apikeyId") id: Int,
            @Valid @NotNull @RequestBody bean: EditableFields): Apikey {

        val apikey = apikeyRepository.findByIdAndUserId(id, userId).orElseThrow { ItemNotFoundException("apikey (id=$id)") }

        bean.expirationDate?.let { apikey.expirationDate = parseDateOrDuration(it) }
        bean.readOnly?.let { apikey.isReadOnly = it }
        bean.description?.let { apikey.description = it }

        return apikeyRepository.saveAndFlush(apikey)
    }


    @DeleteMapping("/apikeys/{apikeyId}")
    @SimpleModificationStatusResponse
    @Protected(SecurityConstants.SCOPE_WRITE)
    fun deleteApikey(@UserId userId: Int, @PathVariable("apikeyId") id: Int): ResponseEntity<String> {
        val apikey = apikeyRepository.findByIdAndUserId(id, userId)
        if (apikey.isEmpty) return CommonResponses.notModifed()
        apikeyRepository.delete(apikey.get())
        return CommonResponses.ok()
    }

    companion object {
        val AUTOLOGIN_DESCRIPTION = "auto_login"
        val AUTOLOGIN_EXPIRE = MutablePeriod(13, 0, 0, 0).toPeriod()

        fun parseDateOrDuration(raw: String): DateTime? {
            if (raw.trim().toLowerCase() == "null") return null
            val dt = JodaUtils.parseOrNull(raw.replace("Z", ""))
            return dt ?: DurationParser.parseIntoDate(raw)
        }
    }
}