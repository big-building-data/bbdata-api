package ch.derlin.bbdata.output.exceptions

import org.hibernate.exception.ConstraintViolationException
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.sql.SQLIntegrityConstraintViolationException


class AppException private constructor(
        val statusCode: HttpStatus,
        val exceptionType: String,
        val msg: Any) : Exception() {

    val errorAttributes: Map<String, Any> = mapOf(
            "exception" to exceptionType,
            "details" to msg
    )

    companion object {
        fun create(status: HttpStatus, exceptionType: String, msg: Any) =
                AppException(
                        statusCode = status,
                        exceptionType = exceptionType,
                        msg = msg
                )

        fun fromThrowable(ex: Throwable, status: HttpStatus? = null): AppException =
                AppException(
                        statusCode = status ?: HttpStatus.INTERNAL_SERVER_ERROR,
                        exceptionType = ex.javaClass.name.split('.').last(),
                        msg = ex.message ?: "")

        fun badApiKey(msg: String = "The apikey you provided does not exist or has expired.") =
                AppException(HttpStatus.UNAUTHORIZED, "BadApiKey", msg)

        fun badRequest(name: String = "BadRequest", msg: Any) =
                AppException(HttpStatus.BAD_REQUEST, name, msg)

        fun forbidden(msg: String = "This resource is protected.") =
                AppException(HttpStatus.FORBIDDEN, "Forbidden", msg)

        fun itemNotFound(msg: String = "The resource was not found or you don't have access to it.") =
                AppException(HttpStatus.NOT_FOUND, "NotFound", msg)
    }
}

// for errors thrown at the server-level (404)
@Component
class ErrorAttributesCustom : DefaultErrorAttributes() {

    override fun getErrorAttributes(webRequest: WebRequest, includeStackTrace: Boolean): Map<String, Any> {
        val ex = getError(webRequest)
        return AppException.fromThrowable(ex).errorAttributes
    }
}

// for Exception thrown at the code level
@RestControllerAdvice
class ErrorHandler : ResponseEntityExceptionHandler() {

    // known exceptions
    @ExceptionHandler(AppException::class)
    fun handleAppException(e: AppException): ResponseEntity<Map<String, Any>> = ResponseEntity(e.errorAttributes, e.statusCode)

    /*@ExceptionHandler(value = [
        javax.validation.ConstraintViolationException::class,
        org.hibernate.exception.ConstraintViolationException::class,
        org.springframework.dao.DataIntegrityViolationException::class])
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityException(ex: DataIntegrityViolationException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity(AppException.fromThrowable(
                (ex.cause as? ConstraintViolationException)?.sqlException ?: ex
        ).errorAttributes, HttpStatus.BAD_REQUEST)
    }
     */

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityException(ex: DataIntegrityViolationException): ResponseEntity<Map<String, Any>> {
        val appEx = (ex.cause as? ConstraintViolationException)?.sqlException?.let {
            val sqlMsg = it.message ?: ""
            val (name, msg) =
                    if ("foreign key constraint fails" in sqlMsg) {
                        "ForeignKeyException" to "A field references a non-existing resource."
                    } else if ("Duplicate entry" in sqlMsg) {
                        "DuplicateField" to "value ${sqlMsg.split(" ")[2]} already exists."
                    } else {
                        "SqlException" to sqlMsg
                    }
            AppException.badRequest(name, msg)
        } ?: AppException.fromThrowable(ex)

        return ResponseEntity(appEx.errorAttributes, appEx.statusCode)
    }

    // unknown exceptions
    override fun handleExceptionInternal(ex: java.lang.Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        val appEx = when (ex) {
            is MethodArgumentNotValidException -> AppException.badRequest(name = "WrongParameters",
                    msg = formatValidationErrors(ex.bindingResult.allErrors))
            is BindException -> AppException.badRequest(name = "WrongParameters",
                    msg = formatValidationErrors(ex.allErrors))
            else -> AppException.fromThrowable(ex, status)
        }
        return ResponseEntity(appEx.errorAttributes, status)
    }

    companion object {
        fun formatValidationErrors(errors: List<ObjectError>) =
                errors.associateBy({ (it as FieldError).getField() }, { it.defaultMessage })
    }

}