package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import org.junit.jupiter.api.Assertions.*
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
import kotlin.random.Random


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR])
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
        val response = restTemplate.putWithBody("/objects/${id}/tags?tags=${tag1},${tag2}", "")
        assertEquals(HttpStatus.OK, response.statusCode)

        val json = restTemplate.getQueryJson("/objects/$id").second
        assertTrue(json.read<List<String>>("$.tags").contains(tag1))
        assertTrue(json.read<List<String>>("$.tags").contains(tag2))
    }

    @Test
    @Throws(Exception::class)
    fun `1-2 add tags not modified`() {
        // == add tags bis
        val response = restTemplate.putQueryString("/objects/${id}/tags?tags=${tag1},${tag2}")
        assertEquals(HttpStatus.NOT_MODIFIED, response.statusCode)
    }

    @Test
    @Throws(Exception::class)
    fun `1-3 get objects by tag`() {
        // == get with one tag
        var resp = restTemplate.getQueryJson("/objects?tags=${tag1}")
        assertEquals(HttpStatus.OK, resp.first)
        var objs = resp.second.read<List<Map<String, Any>>>("$")
        assertEquals(1, objs.size)
        assertEquals(1, objs.first().get("id") as Int)
        // == get with two tag
        resp = restTemplate.getQueryJson("/objects?tags=${tag1},${tag2}")
        assertEquals(HttpStatus.OK, resp.first)
        objs = resp.second.read<List<Map<String, Any>>>("$")
        assertEquals(1, objs.size)
        assertEquals(1, objs.first().get("id") as Int)
        // == get with one only one tag matching (or, not and) TODO should it be and ?
        resp = restTemplate.getQueryJson("/objects?tags=${tag1},lala")
        assertEquals(HttpStatus.OK, resp.first)
        objs = resp.second.read<List<Map<String, Any>>>("$")
        assertEquals(1, objs.size)
    }

    @Test
    @Throws(Exception::class)
    fun `1-4 remove first tag`() {
        // == remove first tag
        val response = restTemplate.deleteQueryString("/objects/${id}/tags?tags=${tag1}")
        assertEquals(HttpStatus.OK, response.statusCode)

        val json = restTemplate.getQueryJson("/objects/$id").second
        assertFalse(json.read<List<String>>("$.tags").contains(tag1))
        assertTrue(json.read<List<String>>("$.tags").contains(tag2))
    }

    @Test
    @Throws(Exception::class)
    fun `1-5 remove second tag`() {
        // == remove second tag
        val response = restTemplate.deleteQueryString("/objects/${id}/tags?tags=${tag2}")
        assertEquals(HttpStatus.OK, response.statusCode)

        val json = restTemplate.getQueryJson("/objects/$id").second
        assertFalse(json.read<List<String>>("$.tags").contains(tag1))
        assertFalse(json.read<List<String>>("$.tags").contains(tag2))
    }
}