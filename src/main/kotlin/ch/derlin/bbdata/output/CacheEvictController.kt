package ch.derlin.bbdata.output

import ch.derlin.bbdata.CacheProperties
import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.CacheConstants
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.output.api.CommonResponses
import io.swagger.v3.oas.annotations.Hidden
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
class CacheEvictController(
        private val cacheProperties: CacheProperties,
        private val cacheManager: CacheManager) {

    @Hidden
    @GetMapping("/cache-evict")
    fun cacheEvict(@RequestParam("key", required = false) key: String?): ResponseEntity<String> {
        if (cacheProperties.matches(key)) {
            cacheManager.getCache(CacheConstants.CACHE_NAME)?.clear()
            return CommonResponses.ok("Cache evicted.")
        } else {
            throw ForbiddenException("This endpoint is not available.")
        }
    }
}