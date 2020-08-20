package ch.derlin.bbdata.common.cassandra

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.io.OutputStream
import javax.servlet.http.HttpServletResponse

/**
 * date: 11.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

interface StreamableCsv {
    fun csvValues(vararg args: Any): List<Any?>
}


@Component
class CassandraObjectStreamer(private val mapper: ObjectMapper) {

    fun stream(contentType: String?,
               response: HttpServletResponse,
               csvHeaders: List<String>,
               values: Iterable<StreamableCsv>) {
        if (contentType != null && contentType.contains("text")) {
            response.contentType = "text/csv"
            streamCsv(response.outputStream, csvHeaders, values)
        } else {
            response.contentType = "application/json"
            streamJson(response.outputStream, values)
        }
    }

    fun streamJson(outputStream: OutputStream,
                   values: Iterable<Any>,
                   single: Boolean = false) {
        mapper.factory.createGenerator(outputStream).use { jsonGenerator ->
            jsonGenerator.useDefaultPrettyPrinter()
            jsonGenerator.writeStartArray()
            values.forEach { mapper.writeValue(jsonGenerator, it) }
            jsonGenerator.writeEndArray()
        }
    }

    fun streamCsv(outputStream: OutputStream,
                  headers: List<String>,
                  values: Iterable<StreamableCsv>) {
        outputStream.writer().use { w ->
            w.write(headers.joinToString(",") + "\n")
            values.forEach {
                w.write(it.csvValues().joinToString(",") + "\n")
            }
        }
    }
}
