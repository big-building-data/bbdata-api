package ch.derlin.bbdata.output.security


import ch.derlin.bbdata.output.Profiles
import ch.derlin.bbdata.output.api.apikeys.ApikeyRepository
import ch.derlin.bbdata.output.exceptions.BadApikeyException
import ch.derlin.bbdata.output.exceptions.ForbiddenException
import ch.derlin.bbdata.output.security.SecurityConstants.HEADER_TOKEN
import ch.derlin.bbdata.output.security.SecurityConstants.HEADER_USER
import ch.derlin.bbdata.output.security.SecurityConstants.SCOPE_WRITE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
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

// ========================

@Component
@Profile(Profiles.UNSECURED)
class DummyAuthInterceptor : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        request.setAttribute(HEADER_USER, "1")
        return true
    }
}

@Configuration
@Profile(Profiles.UNSECURED)
class DummyWebMvcConfiguration : WebMvcConfigurer {

    @Autowired
    lateinit var authInterceptor: DummyAuthInterceptor

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
    }
}

// ========================

@Component
@Profile(Profiles.NOT_UNSECURED)
class AuthInterceptor : HandlerInterceptor {

    @Autowired
    lateinit var apikeyRepository: ApikeyRepository


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

        // get security annotation and scope
        val securityAnnotation = handler.method.getAnnotation(Protected::class.java)
        if (securityAnnotation == null) {
            // "free access" endpoints, do nothing
            return true
        }
        val writeRequired = securityAnnotation.value.contains(SCOPE_WRITE)

        // extract userId and token into request attributes
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
            val apikey = apikeyRepository.findValid(userId, bbtoken).orElseThrow {
                BadApikeyException("Access denied for user ${userId} : bad apikey")
            }
            // check if write access is necessary
            if (apikey.isReadOnly && writeRequired) {
                // check write permissions
                throw ForbiddenException("Access denied for user ${userId} : this apikey is read-only")
            }
            // every checks passed !
            return true
        }

        // bbuser is not an int
        throw BadApikeyException("Wrong header $HEADER_USER=${bbuser}. Should be an integer")

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
@Profile(Profiles.NOT_UNSECURED)
class WebMvcConfiguration : WebMvcConfigurer {

    @Autowired
    lateinit var authInterceptor: AuthInterceptor

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
    }
}