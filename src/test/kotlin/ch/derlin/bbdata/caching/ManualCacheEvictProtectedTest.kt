package ch.derlin.bbdata.caching

import ch.derlin.bbdata.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * date: 09.09.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = [UNSECURED_REGULAR, "spring.cache.type=simple", "cache.evict.secret-key=111"])
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class ManualCacheEvictProtectedTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var cacheEvictProperties: CacheEvictProperties

    @Test
    fun `1-1 test cache evict protected fail`() {
        // add something to the cache

        listOf("wrong", "", "lala").forEach { key ->
            // call manual cache evict with wrong keys
            val resp = restTemplate.getQueryString("/cache-evict?key=$key")
            Assertions.assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        }
    }

    @Test
    fun `1-1 test cache evict protected ok`() {
        // call manual cache evict with the right key
        val resp = restTemplate.getQueryString("/cache-evict?key=${cacheEvictProperties.secretKey}")
        Assertions.assertEquals(HttpStatus.OK, resp.statusCode)
    }
}