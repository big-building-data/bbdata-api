package ch.derlin.bbdata

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.logging.DeferredLog
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.filter.TypeFilter
import java.io.IOException

/**
 * This PostProcessor is my solution to the problem of defining different spring.autoconfigure.exclude in
 * different properties files (e.g. profiles).
 * The idea is to add a "KEY" to the property, e.g. `spring.autoconfigure.exclude.NOC=some.package.SomeClass` each
 * time it is defined. At runtime, the processor will gather all those properties and assemble the final
 * `spring.autoconfigure.exclude` property list.
 *
 * Advantages:
 * - one can override only part of `spring.autoconfigure.exclude`, using the key (an empty value is ignored)
 * - one can define multiple autoconfiguration classes to exclude from different properties files
 *
 * Ho to use:
 * In the file `resources/META-INF/spring.factories`, add the following:
 * ```
 * org.springframework.boot.env.EnvironmentPostProcessor=ch.derlin.bbdata.ExcludeAutoConfigPostProcessor
 * ```
 *
 * NOTE that this class is also automatically including/excluding RedisAutoConfiguration based on the active profile
 * and the value of `spring.cache.type`. I didn't find a better way to achieve that... TODO
 */
class ExcludeAutoConfigPostProcessor : EnvironmentPostProcessor {

    var logger: DeferredLog = DeferredLog()

    companion object {
        const val PROP = "spring.autoconfigure.exclude"
    }

    override fun postProcessEnvironment(env: ConfigurableEnvironment, application: SpringApplication) {

        val excludes = env.getPrefixedProperties(PROP)
        // also exclude Redis AutoConfiguration unless the caching profile is enabled and redis is selected
        if (!(env.activeProfiles.contains("caching") && env.getProperty("spring.cache.type") == "redis"))
            excludes.add(RedisAutoConfiguration::class.qualifiedName!!)
        // override $PROP
        env.propertySources.addFirst(MapPropertySource(PROP, mapOf(PROP to excludes)))
        logger.info("Assembled the new $PROP property: $PROP=${excludes.joinToString(" + ")}")

        // defer logging to after the application starts: see https://stackoverflow.com/a/61290206
        application.addInitializers(ApplicationContextInitializer<ConfigurableApplicationContext> {
            logger.replayTo(ExcludeAutoConfigPostProcessor::class.java)
        })
    }
}

/**
 * Dynamically exclude any component from a given subpackage.
 * Usage:
 * - in application.properties: component-scan.exclude.<KEY>=name.of.subpackage
 *   (KEY is to allow multiple definitions in different properties files)
 * - register this filter. Under your @SpringBootApplication annotation, add:
 *   ```
 *      @ComponentScan(excludeFilters = [
 *           ComponentScan.Filter(type = FilterType.CUSTOM, classes = [ExcludePackagesFilter::class])
 *      ])
 *   ```
 *
 * Derived from [this StackOverflow answer](https://stackoverflow.com/a/54381921)
 */
class ExcludePackagesFilter : TypeFilter, EnvironmentAware {

    var logger: Logger = LoggerFactory.getLogger(ExcludePackagesFilter::class.java)

    private var excludes: Set<String>? = null

    @Throws(IOException::class)
    override fun match(metadataReader: MetadataReader, metadataReaderFactory: MetadataReaderFactory): Boolean {
        if (excludes != null) {
            val cls = metadataReader.classMetadata.className
            return excludes?.any { cls.startsWith(it) } ?: false
        }
        return false
    }

    override fun setEnvironment(environment: Environment) {
        if (environment is ConfigurableEnvironment) {
            excludes = environment.getPrefixedProperties(ScanExcludeProperties.PREFIX)
            logger.info("Ready to exclude packages: ${excludes?.joinToString(" + ")}")
        } else {
            logger.warn("environment is not a ConfigurableEnvironment")
        }
    }
}


/**
 * Get all string properties beginning with a given prefix into one list of values.
 * Example:
 * Say we have this in *.properties:
 * ```
 * test.one=X
 * test.two=Y
 * test.three=Z
 * test=T
 * test.one=
 * ```
 * getPrefixedProperties("test") will return the list `["T", "Y", "Z"]
 * @param prefix: the properties prefix to match
 * @return a list of property values
 */
fun ConfigurableEnvironment.getPrefixedProperties(prefix: String): MutableSet<String> {
    val excludes = mutableSetOf<String>()
    this.propertySources.forEach { ps ->
        if (ps is EnumerablePropertySource<*>) {
            excludes.addAll(ps.propertyNames
                    .filter { it.startsWith(prefix) }
                    .flatMap { (ps.getProperty(it) as String).split(",") }
                    .filter { it.isNotBlank() })
        }
    }
    return excludes
}