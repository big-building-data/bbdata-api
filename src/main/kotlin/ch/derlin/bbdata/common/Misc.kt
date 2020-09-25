package ch.derlin.bbdata.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * date: 07.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


object Beans {
    /**
     * Common bean when just a description is needed + default max description size
     */
    const val DESCRIPTION_MAX = 255

    open class Description {

        @Size(max = DESCRIPTION_MAX)
        val description: String? = null
    }
}

class ValidatedList<E> {
    /**
     * See my answer at https://stackoverflow.com/a/64060909
     *
     * By default, spring-boot cannot validate lists, as they are generic AND do not conform to the Java Bean definition.
     * This is one work-around: create a wrapper that fits the Java Bean definition, and use Jackson annotations to
     * make the wrapper disappear upon (de)serialization.
     * Do not change anything (such as making the _value field private) or it won't work anymore !
     *
     * Usage:
     * ```
     * @PostMapping("/something")
     * fun someRestControllerMethod(@Valid @RequestBody pojoList: ValidatedList<SomePOJOClass>)
     * ```
     */

    @JsonValue
    @Valid
    @NotNull
    @Size(min = 1, message = "array body must contain at least one item.")
    var _values: List<E>? = null

    val values: List<E>
        get() = _values!!

    @JsonCreator
    constructor(vararg list: E) {
        this._values = list.asList()
    }
}

/**
 * Get the client's IP from the request
 */
fun HttpServletRequest.getIp(): String {
    val ip = this.getHeader("X-Forwarded-For") ?: this.remoteAddr
    return if (ip == "0:0:0:0:0:0:0:1") "127.0.0.1" else ip

}

/**
 * Ellipsise a string. E.G "1234".truncate(4) = "1234", "1234567".truncate(4) = "1234[...]"
 */
fun String?.truncate(maxLength: Int = 30, ellipsis: String = "[${Typography.ellipsis}]"): String? =
        if (this != null && this.length > maxLength) this.take(maxLength).plus(ellipsis) else this