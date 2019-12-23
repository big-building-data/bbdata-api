package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.output.Beans
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import com.sun.istack.NotNull
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

/**
 * date: 23.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@RestController
@RequestMapping("/objects")
@Tag(name = "Objects Tokens", description = "Manage object tokens")
class ObjectsTokenController(private val objectRepository: ObjectRepository,
                             private val tokenRepository: TokenRepository) {

    @Protected(SecurityConstants.SCOPE_WRITE)
    @GetMapping("{objectId}/tokens")
    fun getTokens(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long): List<Token> {
        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        return obj.tokens
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @GetMapping("{objectId}/tokens/{id}")
    fun getToken(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long,
            @PathVariable(value = "id") id: Int): Token {
        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        return obj.getToken(id) ?: throw ItemNotFoundException("token (oid=$objectId, id=$id)")
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PutMapping("{objectId}/tokens")
    fun addToken(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long,
            @Valid @RequestBody descriptionBody: Beans.Description?): Token {

        // ensure rights
        objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }

        return tokenRepository.saveAndFlush(Token.create(objectId, descriptionBody?.description))
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PostMapping("{objectId}/tokens/{id}")
    fun editToken(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long,
            @PathVariable(value = "id") id: Int,
            @Valid @NotNull @RequestBody descriptionBody: Beans.Description?): Token {

        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        val token = obj.getToken(id) ?: throw ItemNotFoundException("token (oid=$objectId, id=$id)")
        if (token.description != descriptionBody?.description) {
            token.description = descriptionBody?.description
            return tokenRepository.saveAndFlush(token)
        }
        return token
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @DeleteMapping("{objectId}/tokens/{id}")
    fun deleteToken(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long,
            @PathVariable(value = "id") id: Int): ResponseEntity<String> {
        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        val token = obj.getToken(id)
        if (token != null) {
            tokenRepository.delete(token)
            return CommonResponses.ok()
        }
        return CommonResponses.notModifed()
    }

}