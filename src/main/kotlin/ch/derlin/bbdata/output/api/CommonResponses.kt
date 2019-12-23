package ch.derlin.bbdata.output.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * date: 23.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

object CommonResponses {
    // TODO: https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5, 304 should not have a body
    fun notModifed(msg: String = "No modification.") = ResponseEntity(msg, HttpStatus.NOT_MODIFIED)
    fun ok(msg: String = "Success.") = ResponseEntity(msg, HttpStatus.OK)
}