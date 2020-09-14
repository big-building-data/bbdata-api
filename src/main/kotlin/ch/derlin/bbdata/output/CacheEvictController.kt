package ch.derlin.bbdata.output

import ch.derlin.bbdata.CacheEvictProperties
import ch.derlin.bbdata.Constants
import ch.derlin.bbdata.common.OnCacheEnabled
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.output.api.CommonResponses
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.cache.CacheManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * date: 27.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@OnCacheEnabled
class CacheEvictController(
        private val cacheEvictProperties: CacheEvictProperties,
        private val cacheManager: CacheManager) {

    @Hidden
    @GetMapping("/cache-evict")
    fun cacheEvict(@RequestParam("key", required = false) key: String?): ResponseEntity<String> {
        if (cacheEvictProperties.matches(key)) {
            cacheManager.getCache(Constants.META_CACHE)?.clear()
            return CommonResponses.ok("Cache evicted.")
        } else {
            throw ForbiddenException("This endpoint is not available.")
        }
    }
}