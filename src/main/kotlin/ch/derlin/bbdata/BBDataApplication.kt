package ch.derlin.bbdata

import ch.derlin.bbdata.common.dates.JodaUtils
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Profile
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@SpringBootApplication
@ComponentScan(excludeFilters = arrayOf(
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = arrayOf(ExcludePackageFilter::class))
))
@EnableTransactionManagement
@OpenAPIDefinition( // see https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations
        info = Info(
                title = "BBData",
                version = "1.0.1",
                description = """
This document describes the different endpoints available through the bbdata API, 
a json REST api to let you manage, view, and consult objects and values. Find more information, including
common errors codes and more, by visiting <a href="/#more-info">our landing page</a>.""",
                contact = Contact(url = "http://icosys.ch", name = "Lucy Linder", email = "lucy.derlin@gmail.com")
        )
)
@SecurityScheme(name = "auth", type = SecuritySchemeType.HTTP, scheme = "basic")
@EnableConfigurationProperties
class BBDataApplication {

    init {
        // UTC timezone is central !
        JodaUtils.setDefaultTimeZoneUTC()
        // the following set the default datetime handling, but that can
        // be overriden using properties (see CustomConfigProperties)
        JodaUtils.defaultPattern = JodaUtils.FMT_ISO_MILLIS
        JodaUtils.setAcceptableDateRange(from = "2016-01-01")
    }

}

// this is only to turn off warnings "Spring Data Cassandra - Could not safely identify store assignment"
// to work, spring.data.cassandra.repositories.type=none must be set in the properties file
// if there is a problem, simply delete the class+annotation and remove the property in application.properties
@Configuration
@Profile(Profiles.CASSANDRA)
@EnableCassandraRepositories(basePackages = arrayOf("ch.derlin.bbdata.common.cassandra"))
class CassandraConfig

// CORS configuration: allow everything from all origin
@Configuration
class CORSConfigurer : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/*")
                .allowedOrigins("*")
    }
}

fun main(args: Array<String>) {
    runApplication<BBDataApplication>(*args)
}