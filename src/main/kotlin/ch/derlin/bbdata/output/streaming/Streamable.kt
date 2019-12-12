package ch.derlin.bbdata.output.streaming

import java.io.IOException
import com.fasterxml.jackson.core.JsonGenerator
import java.io.Writer


/**
 * date: 11.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

object Streamable {

    val COL_SEP = ","
    val LINE_SEP = "\n"

    fun toCsv(w: Writer, elements: Iterable<CSV>?, withHeader: Boolean, vararg args: Any) {
        if (elements == null) return
        if (withHeader) w.write(elements.first().csvHeaders() + LINE_SEP)
        elements.forEach { it.to(w, *args); w.write(LINE_SEP) }
    }

    interface Json {
        fun to(generator: JsonGenerator, vararg args: Any)
    }

    interface CSV {
        fun csvHeaders(): String
        fun to(w: Writer, vararg args: Any)
    }

    interface All : Json, CSV
}
