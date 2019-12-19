package ch.derlin.bbdata.output

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.random.Random


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StudentControllerTests {

    @LocalServerPort
    private val port = 0
    var restTemplate = TestRestTemplate()
    val id = 1

    @Test
    @Throws(Exception::class)
    fun testCreateObject() {
        // == create
        val entity = HttpEntity("""{"name": "hello", "owner": 1, "unitSymbol": "V"}""", headers)
        val putResponse = restTemplate.exchange(
                url("/objects"), HttpMethod.PUT, entity, String::class.java)
        assertTrue(putResponse.statusCode == HttpStatus.OK)

        // == get
        val id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        val getResponse = restTemplate.exchange(
                url("/objects/${id}"), HttpMethod.GET, null, String::class.java)
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)
        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertTrue(json.read<String>("$.creationdate").matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}".toRegex()))
        assertTrue(json.read<String>("$.unit.type").equals("float"))
        assertNotNull(json.read<String>("$.owner.name"))
        assertNull(json.read<String>("$.description"))
    }

    @Test
    @Throws(Exception::class)
    fun testTags() {
        val tag1 = "test-${Random.nextInt(10000)}"
        val tag2 = "test-${Random.nextInt(10000)}"


        // == add tags
        var response = restTemplate.exchange(url(
                "/objects/${1}/tags?tags=${tag1},${tag2}"), HttpMethod.PUT, emptyEntity, String::class.java)
        assertTrue(response.statusCode == HttpStatus.OK)

        var json = _getObject(id).second
        assertTrue(json.read<List<String>>("$.tags").contains(tag1))
        assertTrue(json.read<List<String>>("$.tags").contains(tag2))

        // == add tags bis
        response = restTemplate.exchange(url(
                "/objects/${1}/tags?tags=${tag1}"), HttpMethod.PUT, emptyEntity, String::class.java)
        assertEquals(HttpStatus.NOT_MODIFIED, response.statusCode) // TODO

        // == remove first tag
        response = restTemplate.exchange(url(
                "/objects/${1}/tags?tags=${tag1}"), HttpMethod.DELETE, emptyEntity, String::class.java)
        assertTrue(response.statusCode == HttpStatus.OK)

        json = _getObject(id).second
        assertFalse(json.read<List<String>>("$.tags").contains(tag1))
        assertTrue(json.read<List<String>>("$.tags").contains(tag2))

        // == remove second tag
        response = restTemplate.exchange(url(
                "/objects/${1}/tags?tags=${tag2}"), HttpMethod.DELETE, emptyEntity, String::class.java)
        assertTrue(response.statusCode == HttpStatus.OK)

        json = _getObject(id).second
        assertFalse(json.read<List<String>>("$.tags").contains(tag1))
        assertFalse(json.read<List<String>>("$.tags").contains(tag2))

    }


//    @Test
//    @Throws(Exception::class)
//    fun testRetrieveStudent() {
//        val entity = HttpEntity<String>(null, headers)
//        val response = restTemplate.exchange(
//                createURLWithPort("/objects/1"), HttpMethod.GET, entity, String::class.java)
//        val expected = """
//        {
//            "id":1,
//            "name":"volts box 1",
//            "description": null,
//                "unit":{"name":"volt","type":"float","id":"V"},
//                "disabled":false,
//                "creationdate":"2019-12-15T10:23:21",
//                "owner":{"id":1,"name":"admin"},
//                "tags":[]
//        }
//        """.trimIndent()
//        JSONAssert.assertEquals(expected, response.body, false)
//    }

    private fun url(uri: String): String {
        return "http://localhost:$port$uri"
    }

    fun _getObject(id: Int = 1, expectedStatus: HttpStatus = HttpStatus.OK): Pair<ResponseEntity<String>, DocumentContext> {
        val getResponse = restTemplate.exchange(url("/objects/${id}"), HttpMethod.GET, emptyEntity, String::class.java)
        assertEquals(expectedStatus, getResponse.statusCode)
        // check some json variables
        return getResponse to JsonPath.parse(getResponse.body)
    }

    companion object {
        val headers: HttpHeaders = HttpHeaders()
        var emptyEntity: HttpEntity<String?>

        init {
            headers.add("Content-Type", "application/json")
            headers.add("bbuser", "1")
            headers.add("bbtoken", "wr1")
            emptyEntity = HttpEntity(null, headers)
        }

        fun <T> entity(body: T) = HttpEntity(body, headers)

    }
}