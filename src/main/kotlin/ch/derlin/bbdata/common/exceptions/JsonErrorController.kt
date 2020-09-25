package ch.derlin.bbdata.common.exceptions

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.ServletWebRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * date: 23.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
class JsonErrorController : ErrorController {

    @Autowired
    private lateinit var errorAttributes: DefaultErrorAttributes

    @RequestMapping(ERROR_PATH)
    fun error(request: HttpServletRequest, response: HttpServletResponse): Map<String, Any?> {
        // Appropriate HTTP response code (e.g. 404 or 500) is automatically set by Spring.
        // Here we just define response body, which is forced to be JSON (vs whitelabel error page in browser)
        val attrs = errorAttributes.getErrorAttributes(ServletWebRequest(request), ErrorAttributeOptions.of(
                ErrorAttributeOptions.Include.MESSAGE, ErrorAttributeOptions.Include.BINDING_ERRORS))
        return linkedMapOf(
                "exception" to attrs["error"],
                "details" to attrs["message"])
    }

    override fun getErrorPath(): String {
        return ERROR_PATH
    }

    companion object {
        private const val ERROR_PATH = "/error"
    }
}
