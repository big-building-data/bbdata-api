package ch.derlin.bbdata

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


/**
 * date: 29.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@RestController
@Tag(name = "About", description = "API Status")
// the following annotation is for tests to run. I dunno why, but in tests InfoEndpoint is not available,
// but in normal mode the /about endpoint is registered even if management.endpoints.web.exposure.include doesn't
// include info ...
@ConditionalOnAvailableEndpoint(endpoint = InfoEndpoint::class)
class AboutController(private val infoEndpoint: InfoEndpoint) {
    /**
     * Since the management interface (actuator) usually runs on another port,
     * add the /about endpoint to the API using a "proxy": just call the actuator and return the result.
     * This means everything configured in CustomInfoContributor will still hold.
     */
    @GetMapping("/about")
    @Operation(description = "Get generic information about the running API instance.")
    fun getInfo(): Map<String, Any> = infoEndpoint.info()
}