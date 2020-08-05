package ch.derlin.bbdata.input

import ch.derlin.bbdata.*
import com.jayway.jsonpath.JsonPath
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
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * date: 05.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.CACHING)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
open class CachingTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var cacheManager: CacheManager

    init {
        System.setProperty("BB_NO_KAFKA", "true")
    }

    companion object {
        val CACHE_NAME = "metas"
        val OBJ_ID = 5
        val TOKEN = InputApiTest.TOKEN_P + OBJ_ID

        // dynamically create token
        var tokenId: Int = -1
        var tokenString: String = ""
    }

    @Test
    fun `0-1 setup object`() {
        // ensure object is enabled
        restTemplate.postQueryString("/object/$OBJ_ID/enable")
        // create token
        createToken()
    }

    @Test
    fun `1-0 test cacheable`() {
        // add two entries to the cache
        submitAndCheckCache(TOKEN)
        submitAndCheckCache(tokenString)
    }

    @Test
    fun `1-1 test cache evict`() {
        // delete token
        val resp = restTemplate.deleteQueryString("/objects/$OBJ_ID/tokens/$tokenId")
        assertEquals(HttpStatus.OK, resp.statusCode)

        // check cache
        assertNull(cacheEntry(tokenString))
        assertNotNull(cacheEntry(TOKEN)) // this one should not have changed

        // check input api
        val resp2 = submitMeasure(tokenString)
        assertNotEquals(HttpStatus.OK, resp2.statusCode)
    }

    @Test
    fun `1-2 test cache evict all`() {
        // add two values in the cache, for different objects
        // create token
        createToken()
        submitAndCheckCache(TOKEN)
        // use another object
        val tk = InputApiTest.TOKEN_P + InputApiTest.OBJ
        submitAndCheckCache(tk, InputApiTest.OBJ)

        // disable one object (should evict all entries in the cache
        val resp = restTemplate.postQueryString("/objects/$OBJ_ID/disable")
        assertEquals(HttpStatus.OK, resp.statusCode)

        // check cache
        assertNull(cacheEntry(tk, InputApiTest.OBJ))
        assertNull(cacheEntry(TOKEN))

        // check input api
        val resp2 = submitMeasure(tokenString)
        assertNotEquals(HttpStatus.OK, resp2.statusCode)
    }

    // -----------

    private fun createToken() {
        val response = restTemplate.putQueryString("/objects/$OBJ_ID/tokens")
        assertEquals(HttpStatus.OK, response.statusCode)
        val json = JsonPath.parse(response.body)
        tokenId = json.read<Int>("$.id")
        tokenString = json.read<String>("$.token")
    }

    private fun submitMeasure(token: String, objectId: Int = OBJ_ID): ResponseEntity<String> =
            restTemplate.postWithBody(InputApiTest.URL, InputApiTest.getMeasureBody(objectId = objectId, token = token))

    private fun submitAndCheckCache(token: String, objectId: Int = OBJ_ID) {
        // post a measure
        val resp = submitMeasure(token, objectId)
        assertEquals(HttpStatus.OK, resp.statusCode)

        // assert value in the cache
        assertTrue(cacheManager.cacheNames.contains(CACHE_NAME))
        assertNotNull(cacheEntry(token, objectId))
    }

    private fun cacheEntry(token: String, oid: Int = OBJ_ID) =
            cacheManager.getCache(CACHE_NAME)?.get("$oid:$token")
}