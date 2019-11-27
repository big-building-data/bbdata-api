package ch.derlin.bbdata.output.exceptions

import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

class AppException private constructor(
        val statusCode: HttpStatus,
        val exceptionType: String,
        val msg: Any) : Exception() {

    val errorAttributes: Map<String, Any> = mapOf(
            "exception" to exceptionType,
            "details" to msg
    )

    companion object {
        fun create(status: HttpStatus, exceptionType: String, msg: Any, vararg args: Any) =
                AppException(
                        statusCode = status,
                        exceptionType = exceptionType,
                        msg = if (msg is String) String.format(msg, *args) else msg
                )

        fun fromThrowable(ex: Throwable, status: HttpStatus? = null): AppException =
                AppException(
                        statusCode = status ?: HttpStatus.INTERNAL_SERVER_ERROR,
                        exceptionType = ex.javaClass.name.split('.').last(),
                        msg = ex.message ?: "")

        fun badApiKey(msg: String = "The apikey you provided does not exist or has expired.", vararg args: Any?) =
                create(HttpStatus.UNAUTHORIZED, "BadApiKey", msg, args)

        fun badRequest(name: String = "BadRequest", msg: String, vararg args: Any?) =
                create(HttpStatus.BAD_REQUEST, name, msg, args)

        fun forbidden(msg: String = "This resource is protected.", vararg args: Any?) =
                create(HttpStatus.FORBIDDEN, "Forbidden", msg, args)

        fun itemNotFound(msg: String = "The resource was not found or you don't have access to it.", vararg args: Any?) =
                create(HttpStatus.NOT_FOUND, "NotFound", msg, args)
    }
}

// for errors thrown at the server-level (404)
@Component
class ErrorAttributesCustom : DefaultErrorAttributes() {

    override fun getErrorAttributes(webRequest: WebRequest, includeStackTrace: Boolean): Map<String, Any> {
        val map = super.getErrorAttributes(webRequest, includeStackTrace)
        val msg = map["trace"]?.let { (it as String).split("\n").get(0) }.orEmpty()
        return hashMapOf<String, Any>(
                //"code" to map["status"].toString(),
                "details" to map["error"].toString(),
                "exception" to msg
        )
    }
}

// for Exception thrown at the code level
@RestControllerAdvice
class ErrorHandler : ResponseEntityExceptionHandler() {

    // known exceptions
    @ExceptionHandler(AppException::class)
    fun handleAppException(e: AppException): ResponseEntity<Map<String, Any>> = ResponseEntity(e.errorAttributes, e.statusCode)

    // unknown exceptions
    override fun handleExceptionInternal(ex: java.lang.Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> =
            ResponseEntity(AppException.fromThrowable(ex, status).errorAttributes, status)

}