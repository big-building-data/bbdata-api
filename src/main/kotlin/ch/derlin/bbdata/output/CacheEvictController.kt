package ch.derlin.bbdata.output

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.CacheConstants
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.output.api.CommonResponses
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * date: 27.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@Profile(Profiles.CACHING)
class CacheEvictController(private val cacheManager: CacheManager) {

    // is this property is defined, the cache will be cleared only if the right key is provided as a parameter
    @Value("\${cache.evict.secret-key:}")
    val secretKey: String = ""

    @Hidden
    @GetMapping("/cache-evict")
    fun cacheEvict(@RequestParam("key", required = false) key: String?): ResponseEntity<String> {
        if (secretKey.isBlank() || key?.equals(secretKey) ?: false) {
            cacheManager.getCache(CacheConstants.CACHE_NAME)?.clear()
            return CommonResponses.ok("Cache evicted.")
        } else {
            throw ForbiddenException("This endpoint is not available.")
        }
    }
}