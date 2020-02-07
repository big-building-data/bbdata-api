package ch.derlin.bbdata

import ch.derlin.bbdata.common.dates.JodaUtils
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * date: 07.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Configuration
@ConfigurationProperties(prefix = "bbdata.datetime")
class BBDataDateTimeFormatProperties {

    /** Defines how datetime will be serialized when sent in a response. Note: default set in Application's init */
    var outputFormat: String? = null
        set(value) {
            field = value
            field?.let { JodaUtils.defaultPattern = it }
        }

    /** Defines the lower limit of a date to be valid when received from the user. Note: default set in Application's init*/
    var min: String? = null
        set(value) {
            field = value
            field?.let { JodaUtils.setAcceptableDateRange(from = it) }
        }
}