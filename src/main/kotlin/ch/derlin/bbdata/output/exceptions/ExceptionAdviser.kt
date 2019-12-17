package ch.derlin.bbdata.output.exceptions

import org.hibernate.exception.ConstraintViolationException
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Component
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


/**
 * date: 15.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

fun Any.lastName(): String = this.javaClass.simpleName //this.javaClass.name.split('.').last()


class ExceptionBody(val exception: String, val details: Any?) {
    companion object {
        fun fromThrowable(ex: Throwable) = ExceptionBody(
                exception = ex.lastName(),
                details = ex.message ?: ""
        )
    }
}

// for errors thrown at the server-level (404)
@Component
class ErrorAttributes : DefaultErrorAttributes() {
    override fun getErrorAttributes(webRequest: WebRequest, includeStackTrace: Boolean): Map<String, Any?> {
        val attrs = super.getErrorAttributes(webRequest, false)
        return mapOf(
                "exception" to attrs.get("error"),
                "details" to attrs.get("message"))
    }
}

// for all other errors
@RestControllerAdvice
class GlobalControllerExceptionHandler : ResponseEntityExceptionHandler() {
    // === known exceptions

    @ExceptionHandler(ItemNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleItemNotFound(ex: ItemNotFoundException): ExceptionBody = ex.body()

    @ExceptionHandler(ForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbidden(ex: ItemNotFoundException): ExceptionBody = ex.body()


    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDataIntegrityException(ex: DataIntegrityViolationException): ExceptionBody = ex.body()


    // === unknown exceptions

    override fun handleExceptionInternal(
            ex: Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest
    ): ResponseEntity<Any> =
            ResponseEntity(when (ex) {
                is HttpMessageNotReadableException -> ExceptionBody(
                        ex.lastName(), ex.message?.let { it.split(" nested exception is")[0] })
                is MethodArgumentNotValidException -> ex.bindingResult.allErrors.body()
                is BindException -> ex.allErrors.body()
                else -> ExceptionBody.fromThrowable(ex)
            }, status)


    companion object {

        fun AppException.body(): ExceptionBody = ExceptionBody(this.name, this.details)

        fun List<ObjectError>.body(): ExceptionBody = ExceptionBody(
                exception = WrongParamsException::class.simpleName.toString(),
                details = this.associateBy({ (it as FieldError).getField() }, { it.defaultMessage })
        )

        fun DataIntegrityViolationException.body(): ExceptionBody =
                (this.cause as? ConstraintViolationException)?.sqlException?.let {
                    val sqlMsg = it.message ?: ""
                    val (name, msg) =
                            if ("foreign key constraint fails" in sqlMsg) {
                                "ForeignKeyException" to "A field references a non-existing resource."
                            } else if ("Duplicate entry" in sqlMsg) {
                                "DuplicateFieldException" to "value ${sqlMsg.split(" ")[2]} already exists."
                            } else {
                                "SqlException" to sqlMsg
                            }
                    ExceptionBody(name, msg)
                } ?: ExceptionBody.fromThrowable(this)
    }

}