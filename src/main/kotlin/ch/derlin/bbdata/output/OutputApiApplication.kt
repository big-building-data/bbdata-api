package ch.derlin.bbdata.output

import ch.derlin.bbdata.output.dates.JodaUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import java.util.TimeZone
import javax.annotation.PostConstruct


@SpringBootApplication
class OutputApiApplication {

    @PostConstruct
    fun init() {
        // Setting Spring Boot SetTimeZone
        JodaUtils.setDefaultTimeZoneUTC();
        JodaUtils.defaultPattern(JodaUtils.Format.ISO_SECONDS);
    }
}

fun main(args: Array<String>) {
    runApplication<OutputApiApplication>(*args)
}
