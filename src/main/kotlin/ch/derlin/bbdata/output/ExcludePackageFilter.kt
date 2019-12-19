package ch.derlin.bbdata.output

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.filter.TypeFilter
import org.springframework.stereotype.Component
import java.io.IOException

/**
 * Dynamically exclude any component from a given subpackage.
 * Usage:
 * - in application.properties: componentscan.exclude.package=name.of.subpackage
 * - register this filter. Under your @SpringBootApplication annotation, add:
 *   ```
 *      @ComponentScan(excludeFilters = arrayOf(
 *           ComponentScan.Filter(type = FilterType.CUSTOM, classes = arrayOf(ExcludePackageFilter::class))
 *      ))
 *   ```
 *
 * Derived from [this StackOverflow answer](https://stackoverflow.com/a/54381921)
 */
class ExcludePackageFilter : TypeFilter, EnvironmentAware {

    companion object {
        private const val PROP_NAME = "component-scan.exclude"
    }

    private var excludedPackages: Array<String>? = null

    @Throws(IOException::class)
    override fun match(metadataReader: MetadataReader, metadataReaderFactory: MetadataReaderFactory): Boolean {
        if (excludedPackages != null) {
            val cls = metadataReader.classMetadata.className
            return excludedPackages?.any { cls.startsWith(it) } ?: false
        }
        return false
    }

    override fun setEnvironment(environment: Environment) {
        excludedPackages = environment.getProperty(PROP_NAME, Array<String>::class.java)
    }
}