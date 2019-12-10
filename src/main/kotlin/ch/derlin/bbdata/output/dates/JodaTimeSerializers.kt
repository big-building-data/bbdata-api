package ch.derlin.bbdata.output.dates

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.joda.JodaModule
import org.joda.time.DateTime
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component


/**
 * date: 07.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

// ============== Jackson / JSON

@Configuration
class AppConfig {
    /**
     * Make all dates work with the format registered as default to the JodaUtils module.
     * This will override the default formatter of joda-time.
     */
    @Bean
    fun serializingObjectMapper(): ObjectMapper {
        // register custom joda-time (de-)serializers
        val jodaModule = JodaModule()
        jodaModule.addSerializer(DateTime::class.java, JodaDateTimeSerializer())
        jodaModule.addDeserializer(DateTime::class.java, JodaDateTimeDeserializer())

        // register it to the global object mapper
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(jodaModule)
        return objectMapper
    }
}


class JodaDateTimeSerializer : JsonSerializer<DateTime>() {

    override fun serialize(value: DateTime, gen: JsonGenerator, serializers: SerializerProvider) =
            gen.writeString(JodaUtils.format(value))

}

class JodaDateTimeDeserializer : JsonDeserializer<DateTime>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): DateTime =
            JodaUtils.parse(p!!.valueAsString)

}

// ============== @RequestParam support

@Component
class DateUtilToDateSQLConverter : Converter<String, DateTime> {
    override fun convert(source: String): DateTime? = JodaUtils.parse(source)
}