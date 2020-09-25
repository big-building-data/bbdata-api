package ch.derlin.bbdata.common.exceptions

import io.swagger.v3.oas.annotations.Hidden
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
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


open class ExceptionBody(open val exception: String, open val details: Any?) {
    companion object {
        fun fromThrowable(ex: Throwable) = ExceptionBody(
                exception = ex.lastName(),
                details = ex.message ?: ""
        )
    }
}

// for all other errors
@RestControllerAdvice
class GlobalControllerExceptionHandler : ResponseEntityExceptionHandler() {
    // === known exceptions

    @ExceptionHandler(ItemNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @Hidden
    fun handleItemNotFound(ex: ItemNotFoundException): ExceptionBody = ex.body()

    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @Hidden
    fun handleUnauthorized(ex: UnauthorizedException): ExceptionBody = ex.body()

    @ExceptionHandler(ForbiddenException::class, BadApikeyException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @Hidden
    fun handleForbidden(ex: AppException): ExceptionBody = ex.body()

    @ExceptionHandler(WrongParamsException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Hidden
    fun handleWrongParam(ex: WrongParamsException): ExceptionBody = ex.body()

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Hidden
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityException(ex: DataIntegrityViolationException): ExceptionBody = ex.body()

    /*
    @ExceptionHandler(javax.validation.ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Hidden
    fun handleConstraintViolationException(ex: javax.validation.ConstraintViolationException) =
            // this one is thrown when validating a List<?> in parameters, if you do not use @ValidatedList
            // but the @Validated on the controller class + @Valid @NotNull @RequestBody newObjects: List<@Valid CLS>...
            ExceptionBody(ex.lastName(), ex.constraintViolations.map { it.propertyPath.toString() to it.message }.toMap())
    */

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
                details = this.associateBy({ (it as FieldError).field }, { it.defaultMessage })
        )

        fun DataIntegrityViolationException.body(): ExceptionBody =
                (this.cause as? ConstraintViolationException)?.sqlException?.let {
                    val sqlMsg = it.message ?: ""
                    val (name, msg) = when {
                        "foreign key constraint fails" in sqlMsg -> {
                            "ForeignKeyException" to "A field references a non-existing resource."
                        }
                        "Duplicate entry" in sqlMsg -> {
                            "DuplicateFieldException" to "value ${sqlMsg.split(" ")[2]} already exists."
                        }
                        else -> {
                            "SqlException" to sqlMsg
                        }
                    }
                    ExceptionBody(name, msg)
                } ?: ExceptionBody.fromThrowable(this)
    }

}