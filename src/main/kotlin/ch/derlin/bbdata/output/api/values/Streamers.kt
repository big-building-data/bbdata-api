package ch.derlin.bbdata.output.api.values

import ch.derlin.bbdata.output.api.objects.ObjectRepository
import ch.derlin.bbdata.output.api.objects.Objects
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.io.OutputStream
import java.io.Writer
import javax.servlet.http.HttpServletResponse

/**
 * date: 11.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

interface StreamableCsv {

    fun csvHeaders(): List<String>
    fun csvValues(vararg args: Any): List<Any?>
}


@Component
class CassandraObjectStreamer(
        private val objectsRepository: ObjectRepository,
        private val mapper: ObjectMapper) {


    fun stream(contentType: String,
               response: HttpServletResponse,
               userId: Int, ids: List<Long>,
               valuesGenerator: (Int) -> Iterable<StreamableCsv>,
               writable: Boolean = false) {
        if (contentType != null && contentType.contains("text")) {
            streamCsv(response.outputStream, userId, ids, valuesGenerator, writable)
        } else {
            streamJson(response.outputStream, userId, ids, valuesGenerator, writable)
        }
    }

    fun streamJson(outputStream: OutputStream,
                   userId: Int, ids: List<Long>,
                   valuesGenerator: (Int) -> Iterable<Any>,
                   writable: Boolean = false) {
        mapper.factory.createGenerator(outputStream).use { jsonGenerator ->
            jsonGenerator.useDefaultPrettyPrinter()
            jsonGenerator.writeStartArray()
            ids.map { objectId ->
                val o = objectsRepository.findById(objectId, userId, writable = false).orElse(null)
                if (o != null) {
                    jsonGenerator.writeStartObject()
                    jsonGenerator.writeObjectField("objectId", objectId)
                    jsonGenerator.writeFieldName("unit")
                    mapper.writeValue(jsonGenerator, o.unit)
                    jsonGenerator.writeFieldName("values")
                    jsonGenerator.writeStartArray()
                    valuesGenerator(objectId.toInt()).forEach { mapper.writeValue(jsonGenerator, it) }
                    jsonGenerator.writeEndArray()
                    jsonGenerator.writeEndObject()
                } else {
                    jsonGenerator.writeStartObject()
                    jsonGenerator.writeObjectField("objectId", objectId)
                    jsonGenerator.writeStringField("error", "not found or not accessible")
                    jsonGenerator.writeEndObject()
                }
            }
            jsonGenerator.writeEndArray()
        }
    }

    fun streamCsv(outputStream: OutputStream,
                  userId: Int, ids: List<Long>,
                  valuesGenerator: (Int) -> Iterable<StreamableCsv>,
                  writable: Boolean = false) {
        outputStream.writer().use { w ->
            var first = true
            ids.map { objectId ->
                objectsRepository.findById(objectId, userId, writable = false).map {
                    valuesGenerator(objectId.toInt()).forEach {
                        if (first) {
                            w.write(it.csvHeaders().joinToString(",") + "\n")
                            first = false
                        }
                        w.write(it.csvValues().joinToString(",") + "\n")
                    }
                }
            }
        }
    }
}
