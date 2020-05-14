package ch.derlin.bbdata.output.api

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * date: 23.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@ApiResponses(
        ApiResponse(responseCode = "200", description = "Success."),
        ApiResponse(responseCode = "304", description = "Not modified.")
)
annotation class SimpleModificationStatusResponse


object CommonResponses {
    // TODO: https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5, 304 should not have a body
    fun notModifed(msg: String? = null) = ResponseEntity(msg, HttpStatus.NOT_MODIFIED)

    fun ok(msg: String? = null) = ResponseEntity(msg, HttpStatus.OK)
}