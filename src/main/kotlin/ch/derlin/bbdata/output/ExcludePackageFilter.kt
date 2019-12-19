package ch.derlin.bbdata.output

import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.filter.TypeFilter
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
    private lateinit var env: Environment

    @Throws(IOException::class)
    override fun match(metadataReader: MetadataReader, metadataReaderFactory: MetadataReaderFactory): Boolean {
        var match = false
        env.getProperty("componentscan.exclude.package")?.let { excludedPackage ->
            match = metadataReader.classMetadata.className.startsWith(excludedPackage)
        }
        return match
    }

    override fun setEnvironment(environment: Environment) {
        env = environment
    }

}