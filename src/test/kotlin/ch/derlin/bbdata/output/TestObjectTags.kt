package ch.derlin.bbdata.output

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import kotlin.random.Random
import ch.derlin.bbdata.output.putForEntity
import org.springframework.beans.factory.annotation.Autowired


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectTags {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val id = 1
        val tag1 = "test-${Random.nextInt(10000)}"
        val tag2 = "test-${Random.nextInt(10000)}"
    }

    @Test
    @Throws(Exception::class)
    fun `1-1 add tags`() {
        // == add tags
        val response = restTemplate.putForEntity("/objects/${1}/tags?tags=${tag1},${tag2}", "", String::class.java)
        assertEquals(response.statusCode, HttpStatus.OK)

        val json = restTemplate.getQueryJson("/objects/$id").second
        assertTrue(json.read<List<String>>("$.tags").contains(tag1))
        assertTrue(json.read<List<String>>("$.tags").contains(tag2))
    }

    @Test
    @Throws(Exception::class)
    fun `1-2 add tags not modified`() {
        // == add tags bis
        val response = restTemplate.putQueryString("/objects/${1}/tags?tags=${tag1},${tag2}")
        assertEquals(HttpStatus.NOT_MODIFIED, response.statusCode)
    }

    @Test
    @Throws(Exception::class)
    fun `1-3 remove first tag`() {
        // == remove first tag
        val response = restTemplate.deleteQueryString("/objects/${1}/tags?tags=${tag1}")
        assertEquals(HttpStatus.OK, response.statusCode)

        val json = restTemplate.getQueryJson("/objects/$id").second
        assertFalse(json.read<List<String>>("$.tags").contains(tag1))
        assertTrue(json.read<List<String>>("$.tags").contains(tag2))
    }

    @Test
    @Throws(Exception::class)
    fun `1-4 remove second tag`() {
        // == remove second tag
        val response = restTemplate.deleteQueryString("/objects/${1}/tags?tags=${tag2}")
        assertEquals(HttpStatus.OK, response.statusCode)

        val json = restTemplate.getQueryJson("/objects/$id").second
        assertFalse(json.read<List<String>>("$.tags").contains(tag1))
        assertFalse(json.read<List<String>>("$.tags").contains(tag2))
    }
}