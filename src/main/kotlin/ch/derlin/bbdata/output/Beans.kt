package ch.derlin.bbdata.output

import javax.validation.constraints.Size

/**
 * date: 07.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

object Beans {
    open class NameDescription {
        @Size(min = 3, max = 60)
        val name: String = ""

        @Size(max = 255)
        val description: String? = null
    }
}