package ch.derlin.bbdata.output.dates

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.joda.time.DateTime
import org.springframework.boot.jackson.JsonComponent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component


/**
 * date: 07.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

// ============== Jackson / JSON

/*
@Component
@ConfigurationProperties("spring.jackson")
class JacksonProperties {
    lateinit var serialization: MutableMap<SerializationFeature, Boolean>
}

@Configuration
class AppConfig {

    @Autowired
    private lateinit var jacksonProperties: JacksonProperties

    @Bean
    fun noHateoasObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        jacksonProperties.serialization.map { mapper.configure(it.key, it.value) }
        val jodaModule = JodaModule()
        jodaModule.addSerializer(DateTime::class.java, JodaDateTimeSerializer())
        jodaModule.addDeserializer(DateTime::class.java, JodaDateTimeDeserializer())
        mapper.registerModule(jodaModule)
        return mapper
    }
}
*/

@JsonComponent
class JodaDateTimeSerializer : JsonSerializer<DateTime>() {

    override fun serialize(value: DateTime, gen: JsonGenerator, serializers: SerializerProvider) =
            gen.writeString(JodaUtils.format(value))

}

@JsonComponent
class JodaDateTimeDeserializer : JsonDeserializer<DateTime>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): DateTime =
            JodaUtils.parse(p!!.valueAsString)

}

// ============== @RequestParam support

@Component
class StringToJodaTimeConverter : Converter<String, DateTime> {
    override fun convert(source: String): DateTime? = JodaUtils.parse(source)
}

@Component
class LongToJodaTimeConverter : Converter<Long, DateTime> {
    override fun convert(source: Long): DateTime? = JodaUtils.parse(source)
}