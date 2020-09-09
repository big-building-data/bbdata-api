package ch.derlin.bbdata

import ch.derlin.bbdata.common.dates.JodaUtils
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * date: 07.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

object HiddenEnvironmentVariables {
    /** Environment variables names that can be set */

    /** boolean to turn off kafka */
    const val NO_KAFKA = "BB_NO_KAFKA"
    /** integer to override the default user when the unsecured profile is used */
    const val UNSECURED_USER = "UNSECURED_BBUSER"
}

@Configuration
@ConfigurationProperties(prefix = "scan")
class ScanExcludeProperties {
    /** this class is only defined to have the autocomplete on intellij
     * scan.exclude.XX=package1[,package2], see ExcludePackagesFilter */
    var exclude: Map<String, String> = mapOf()

    companion object {
        const val PREFIX = "scan.exclude"
    }
}

@Configuration
@ConfigurationProperties(prefix = "async")
class AsyncProperties {
    /** Whether or not to turn async on: see AsyncConfig */
    var enabled: Boolean = true
}

@Configuration
@ConfigurationProperties(prefix = "cache.evict")
class CacheProperties {
    /** Optional secret key to call cache evict: see CacheEvictController */
    var secretKey: String = ""

    fun matches(key: String?): Boolean = secretKey.isBlank() || key?.equals(secretKey) ?: false
}

@Configuration
@ConfigurationProperties(prefix = "datetime")
class DateTimeFormatProperties {

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