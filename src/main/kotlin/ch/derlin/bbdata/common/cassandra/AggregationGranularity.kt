package ch.derlin.bbdata.common.cassandra

import ch.derlin.bbdata.common.dates.JodaUtils
import org.joda.time.DateTime
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.support.FormattingConversionService
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport


/**
 * date: 05.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

enum class AggregationGranularity(val minutes: Int) {
    // the uppercase is ugly in the swagger UI... So just for once, do not follow the conventions
    // Note: casing should be in sync the function StringToGranularityConverter.convert !
    quarters(15),
    hours(60);

    companion object {
        val aceptableValues = values().map { it.name }.toList()
    }
}


@Component
class StringToGranularityConverter : Converter<String, AggregationGranularity?> {
    // by declaring the @QueryParam as type "AggregationGranularity?", this converter will be applied.
    // he is case-insensitive and simply returns null if the value could not be mapped to an enum
    override fun convert(source: String): AggregationGranularity? = source.toLowerCase().trim().let {
        if (AggregationGranularity.aceptableValues.contains(it)) AggregationGranularity.valueOf(it)
        else null
    }
}