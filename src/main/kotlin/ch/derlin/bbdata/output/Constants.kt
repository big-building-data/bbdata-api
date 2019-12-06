package ch.derlin.bbdata.output

import javax.validation.constraints.Size

/**
 * date: 27.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
object Constants {
    const val HEADER_USER = "bbuser"
    const val HEADER_TOKEN = "bbtoken"
}

object Beans {
    open class NameDescription {
        @Size(min = 3, max = 60)
        val name: String = ""

        @Size(max = 255)
        val description: String? = null
    }
}