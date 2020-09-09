package ch.derlin.bbdata.caching

import ch.derlin.bbdata.*
import ch.derlin.bbdata.common.CacheConstants
import ch.derlin.bbdata.input.InputApiTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * date: 09.09.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = [UNSECURED_REGULAR, NO_KAFKA, "spring.cache.type=simple", "cache.evict.secret-key="])
@ActiveProfiles(Profiles.UNSECURED, Profiles.CACHING)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class ManualCacheEvictTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var cacheManager: CacheManager

    companion object {
        const val objectId = 1
    }

    @Test
    fun `1-1 test cache evict`() {
        val token = TOKEN(objectId)
        val cache = cacheManager.getCache(CacheConstants.CACHE_NAME)!!
        val cacheKey = "$objectId:$token"

        // check nothing in the cache
        assertNull(cache.get(cacheKey))

        // add something to the cache
        var resp = restTemplate.postWithBody(InputApiTest.URL,
                InputApiTest.getMeasureBody(objectId = objectId, token = token))
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertNotNull(cache.get(cacheKey))

        // call manual cache evict
        resp = restTemplate.getQueryString("/cache-evict")
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertNull(cache.get(cacheKey))
    }
}