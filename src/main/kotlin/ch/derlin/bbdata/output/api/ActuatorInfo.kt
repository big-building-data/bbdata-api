package ch.derlin.bbdata.output.api

import org.joda.time.DateTime
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component


/**
 * date: 29.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Component
class TimeInfoContributor : InfoContributor {
    override fun contribute(builder: Info.Builder) {
        // this will be shown in the /info actuator endpoint (springboot-actuator)
        builder.withDetail("server-time", DateTime())
    }
}