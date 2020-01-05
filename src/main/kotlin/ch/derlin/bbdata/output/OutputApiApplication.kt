package ch.derlin.bbdata.output

import ch.derlin.bbdata.output.dates.JodaUtils
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.annotation.PostConstruct


@SpringBootApplication
@ComponentScan(excludeFilters = arrayOf(
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = arrayOf(ExcludePackageFilter::class))
))
@OpenAPIDefinition( // see https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations
        info = Info(
                title = "BBData",
                version = "1.0.1",
                description = """
This document describes the different endpoints available through the bbdata output api endpoint, 
a json REST api to let you manage, view, and consult objects and values. Find more information, including
common errors codes and more, by visiting <a href="/#more-info">our landing page</a>.""",
                contact = Contact(url = "http://icosys.ch", name = "Lucy Linder", email = "lucy.derlin@gmail.com")
        )
)
@SecurityScheme(name = "auth", type = SecuritySchemeType.HTTP, scheme = "basic")
class OutputApiApplication {

    @PostConstruct
    fun init() {
        // Setting Spring Boot SetTimeZone
        JodaUtils.setDefaultTimeZoneUTC()
        JodaUtils.defaultPattern(JodaUtils.Format.ISO_SECONDS)
        JodaUtils.acceptableDateRange(from = "2016-01-01")
    }
}

// CORS configuration: allow everything from all origin
@Configuration
class CORSConfigurer : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/*")
                .allowedOrigins("*")
    }
}

fun main(args: Array<String>) {
    runApplication<OutputApiApplication>(*args)
}
