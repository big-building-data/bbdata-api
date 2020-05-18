package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.common.Beans
import ch.derlin.bbdata.common.exceptions.AppException
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.exceptions.WrongParamsException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import com.sun.istack.NotNull
import io.swagger.v3.oas.annotations.Operation
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
    @Operation(description = "Get the list of tokens for an object.")
    @GetMapping("{objectId}/tokens")
    fun getObjectTokens(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long): List<Token> {
        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        return obj.tokens
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Get the details of a token.")
    @GetMapping("{objectId}/tokens/{tokenId}")
    fun getObjectToken(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long,
            @PathVariable(value = "tokenId") id: Int): Token {
        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        return obj.getToken(id) ?: throw ItemNotFoundException("token (oid=$objectId, id=$id)")
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Create a new token. " +
            "_NOTE_: The request body is optional, only used if you want to set a description.")
    @PutMapping("{objectId}/tokens")
    fun addObjectToken(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long,
            @Valid @RequestBody descriptionBody: Beans.Description?): Token {

        // ensure rights
        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        if (obj.disabled)
            throw WrongParamsException(msg = "Object $objectId is disabled.")

        return tokenRepository.saveAndFlush(Token.create(objectId, descriptionBody?.description))
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Edit a token description.")
    @PostMapping("{objectId}/tokens/{tokenId}")
    fun editObjectToken(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long,
            @PathVariable(value = "tokenId") id: Int,
            @Valid @NotNull @RequestBody descriptionBody: Beans.Description?): Token {
        // TODO: return ok/not modified ?
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
    @Operation(description = "Delete a token.")
    @SimpleModificationStatusResponse
    @DeleteMapping("{objectId}/tokens/{tokenId}")
    fun deleteObjectToken(
            @UserId userId: Int,
            @PathVariable(value = "objectId") objectId: Long,
            @PathVariable(value = "tokenId") id: Int): ResponseEntity<String> {
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