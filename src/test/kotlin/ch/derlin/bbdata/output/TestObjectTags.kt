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
        val tag1 = "tag1-${Random.nextInt(10000)}"
        val tag2 = "tag2-${Random.nextInt(10000)}"
    }

    @Test
    @Throws(Exception::class)
    fun `1-1 add tags`() {
        // == add tags
        val url = "/objects/${id}/tags?tags=${tag1},${tag2}"
        val resp = restTemplate.putWithBody(url, "")
        assertEquals(HttpStatus.OK, resp.statusCode, "put $url returned ${resp.body}")

        val json = restTemplate.getQueryJson("/objects/$id").second
        listOf(tag1, tag2).forEach { tag ->
            assertTrue(json.read<List<String>>("$.tags").contains(tag),
                    "get /objects/$id: should have tag $tag")
        }

        // == add tags bis
        val response = restTemplate.putQueryString(url)
        assertEquals(HttpStatus.NOT_MODIFIED, response.statusCode, "put $url (2) returned ${resp.body}")
    }

    @Test
    @Throws(Exception::class)
    fun `1-2 get objects by tag`() {

        listOf(
                "/objects?tags=${tag1}",
                "/objects?tags=${tag1},${tag2}",
                "/objects?tags=${tag1},lala"
        ).forEach { url ->
            val (status, json) = restTemplate.getQueryJson(url)
            assertEquals(HttpStatus.OK, status, "get $url returned ${json.jsonString()}")
            val objs = json.read<List<Map<String, Any>>>("$")
            assertEquals(1, objs.size, "get $url should have one object")
            assertEquals(1, objs.first().get("id") as Int, "get $url should have object #$id")
        }
    }

    @Test
    @Throws(Exception::class)
    fun `1-3 remove first tag`() {
        // == remove first tag
        val url = "/objects/${id}/tags?tags=${tag1}"
        val resp = restTemplate.deleteQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "delete $url returned ${resp.body}")

        val json = restTemplate.getQueryJson("/objects/$id").second
        assertFalse(json.read<List<String>>("$.tags").contains(tag1), "tag $tag1 should NOT be present ${json.jsonString()}")
        assertTrue(json.read<List<String>>("$.tags").contains(tag2), "tag $tag1 should be present ${json.jsonString()}")
    }

    @Test
    @Throws(Exception::class)
    fun `1-4 remove second tag`() {
        // == remove second tag
        val url = "/objects/${id}/tags?tags=${tag2}"
        val resp = restTemplate.deleteQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "delete $url returned ${resp.body}")

        val json = restTemplate.getQueryJson("/objects/$id").second
        assertFalse(json.read<List<String>>("$.tags").contains(tag1), "tag $tag1 should NOT be present ${json.jsonString()}")
        assertFalse(json.read<List<String>>("$.tags").contains(tag2), "tag $tag2 should NOT be present ${json.jsonString()}")
    }
}