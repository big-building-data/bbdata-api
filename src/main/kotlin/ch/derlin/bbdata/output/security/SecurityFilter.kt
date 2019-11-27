package ch.derlin.bbdata.output.security


import ch.derlin.bbdata.output.Constants
import ch.derlin.bbdata.output.api.auth.AuthFacade
import ch.derlin.bbdata.output.exceptions.AppException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/*
@Component
@Order(1)
public class TransactionFilter : Filter {

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val req: HttpServletRequest = request as HttpServletRequest
        chain?.doFilter(request, response)
    }
}
*/


@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class ApikeyWrite

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class NoHeaderRequired

@Component
class AuthInterceptor : HandlerInterceptor {

    @Autowired
    lateinit var authFacade: AuthFacade


    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val method = (handler as HandlerMethod).method

        // allow options method to support CORS requests
        if (request.method.equals("options", ignoreCase = true)) {
            response.status = HttpStatus.OK.value()
            return false
        }

        // "free access" endpoints, do nothing
        if (method.getAnnotation(NoHeaderRequired::class.java) != null) {
            return true
        }

        extractBasicAuth(request)
        val bbuser = request.getHeader(Constants.HEADER_USER)
        val bbtoken = request.getHeader(Constants.HEADER_TOKEN)

        // missing one of the two headers...
        if (bbuser == null || bbtoken == null) {
            response.getWriter().write("This resource is protected. "
                    + "Missing authorization headers: %s=<user_id:int>, %s=<token:string>");
            response.status = HttpStatus.UNAUTHORIZED.value()
            return false
        }

        bbuser.toIntOrNull()?.let { userId ->
            // check valid tokens
            authFacade.checkApikey(userId, bbtoken)?.let {
                // check if write access is necessary
                if (it.isReadOnly && method.getAnnotation(ApikeyWrite::class.java) != null) {
                    // check write permissions
                    throw AppException.forbidden("Access denied for user %d : this apikey is read-only", userId)
                }
                // every checks passed !
                return true
            }
            // apikey is null
            throw AppException.badApiKey("Access denied for user %d : bad apikey", userId)
        }
        // bbuser is not an int
        throw AppException.badApiKey("Wrong header %s=%s. Should be an integer", Constants.HEADER_USER, bbuser)

    }

    private fun extractBasicAuth(request: HttpServletRequest) {
        val auth = request.getHeader("Authorization")
        // Basic Authorization header has the format: "Basic <base64-encoded user:pass>"
        if (auth != null && auth.startsWith("Basic")) {
            val decoded = String(
                    Base64.getDecoder().decode(auth.replaceFirst("Basic ".toRegex(), "").toByteArray())
            ).split(":")

            if (decoded.size == 2) {
                request.setAttribute(Constants.HEADER_USER, decoded[0])
                request.setAttribute(Constants.HEADER_TOKEN, decoded[1])
            }
        }
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