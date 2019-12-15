package ch.derlin.bbdata.output.security


import ch.derlin.bbdata.output.api.auth.AuthRepository
import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.security.SecurityConstants.HEADER_TOKEN
import ch.derlin.bbdata.output.security.SecurityConstants.HEADER_USER
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.Charset
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class AuthInterceptor : HandlerInterceptor {

    @Autowired
    lateinit var authRepository: AuthRepository


    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {

        if (handler !is HandlerMethod) return true // static resources, do nothing

        // allow options method to support CORS requests
        if (request.method.equals("options", ignoreCase = true)) {
            response.status = HttpStatus.OK.value()
            return false
        }

        // allow non-bbdata endpoints, such as doc
        if (!handler.beanType.packageName.contains("bbdata")) {
            return true
        }

        // "free access" endpoints, do nothing
        // todo: handler.method.getAnnotation(io.swagger.v3.oas.annotations.security.SecurityRequirement::class.java).scopes.contains("write")
        if (handler.method.getAnnotation(NoHeaderRequired::class.java) != null) {
            return true
        }

        extractAuth(request)
        val bbuser = request.getAttribute(HEADER_USER) as String
        val bbtoken = request.getAttribute(HEADER_TOKEN) as String

        // missing one of the two headers...
        if (bbuser == "" || bbtoken == "") {
            response.getWriter().write("This resource is protected. "
                    + "Missing authorization headers: %s=<user_id:int>, %s=<token:string>");
            response.status = HttpStatus.UNAUTHORIZED.value()
            return false
        }

        bbuser.toIntOrNull()?.let { userId ->
            // check valid tokens
            authRepository.checkApikey(userId, bbtoken)?.let {
                // check if write access is necessary
                if (it.isReadOnly && handler.method.getAnnotation(ApikeyWrite::class.java) != null) {
                    // check write permissions
                    throw AppException.forbidden("Access denied for user ${userId} : this apikey is read-only")
                }
                // every checks passed !
                return true
            }
            // apikey is null
            throw AppException.badApiKey("Access denied for user ${userId} : bad apikey")
        }
        // bbuser is not an int
        throw AppException.badApiKey("Wrong header $HEADER_USER=${bbuser}. Should be an integer")

    }

    private fun extractAuth(request: HttpServletRequest) {
        // Basic Authorization header has the format: "Basic <base64-encoded user:pass>"
        val auth = request.getHeader("Authorization")
        if (auth != null && auth.startsWith("Basic")) {
            val decoded = String(
                    Base64.getDecoder().decode(auth.replaceFirst("Basic ", "").toByteArray()),
                    charset = UTF_8_CHARSET
            ).split(":")

            if (decoded.size == 2) {
                request.setAttribute(HEADER_USER, decoded[0])
                request.setAttribute(HEADER_TOKEN, decoded[1])
                return
            }
        }
        // If not working, extract from the headers
        request.setAttribute(HEADER_USER, request.getHeader(HEADER_USER) ?: "")
        request.setAttribute(HEADER_TOKEN, request.getHeader(HEADER_TOKEN) ?: "")
    }

    companion object {
        val UTF_8_CHARSET = Charset.forName("utf-8")
    }

}

@Configuration
class WebMvcConfiguration : WebMvcConfigurer {

    @Autowired
    lateinit var authInterceptor: AuthInterceptor

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
    }
}