package ch.derlin.bbdata.actuators

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * date: 21.09.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Component
@ConfigurationProperties(prefix = "dynamic")
class CustomInfoContributor : InfoContributor {
    /**
     * Customize the actuator endpoint /info with dynamic properties and server time
     */

    // any field in the form dynamic.info.key=property
    // the property is the key of another property in the *.properties on the classpath
    var info: MutableMap<String, String>? = null

    private val staticDetails = mutableMapOf<String, Any>()

    @Autowired
    private lateinit var env: Environment

    @PostConstruct
    fun prepareDynamicValues() {
        info?.forEach { (k, ref) ->
            env.getProperty(ref)?.let { v ->
                if (v.isNotBlank()) staticDetails[k] = v
            }
        }
    }

    override fun contribute(builder: Info.Builder) {
        builder.withDetail("server-time", DateTime())
        builder.withDetails(staticDetails)

    }
}