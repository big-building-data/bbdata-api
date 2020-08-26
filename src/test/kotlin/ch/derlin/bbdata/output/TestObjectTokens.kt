package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR])
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectTokens {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val objectId: Int = 1
        var ids: MutableList<Int> = mutableListOf()
        val url = "/objects/$objectId/tokens"
    }

    @Test
    fun `1-1 test create token`() {
        // == create
        var resp = restTemplate.putQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "put $url returned ${resp.body}")

        // == store variables
        val id = JsonPath.parse(resp.body).read<Int>("$.id")
        ids.add(id)

        // == get
        resp = restTemplate.getQueryString("$url/$id")
        JSONAssert.assertEquals(resp.body, resp.body, false)

        // check some json variables
        val json = JsonPath.parse(resp.body)
        assertEquals(objectId, json.read<Int>("$.objectId"), "put $url/$id: wrong objectId")
        assertNull(json.read<String>("$.description"), "put $url/$id: description should be null")
        assertEquals(32, json.read<String>("$.token").length, "put $url/$id: token should be 32 chars")
    }

    @Test
    fun `1-2 test create token with description`() {
        // == create
        val descr = "hello token"
        val resp = restTemplate.putWithBody(url, """{"description": "$descr"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "put $url with body returned ${resp.body}")

        // == store variables
        val id = JsonPath.parse(resp.body).read<Int>("$.id")
        ids.add(id)

        // == get
        val getResponse = restTemplate.getForEntity("$url/$id", String::class.java)
        JSONAssert.assertEquals(resp.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertEquals(objectId, json.read<Int>("$.objectId"), "put $url/$id with body: wrong objectId")
        assertEquals(descr, json.read<Int>("$.description"), "put $url/$id with body: wrong description")
    }

    @Test
    fun `1-3 test edit token`() {
        val newDescr = "hello token new descr"
        val id = ids.last()

        val resp = restTemplate.postWithBody("$url/$id", """{"description": "$newDescr"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "post $url/$id returned ${resp.body}")

        // == get
        val getResponse = restTemplate.getQueryString("$url/$id")
        JSONAssert.assertEquals(resp.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertEquals(objectId, json.read<Int>("$.objectId"), "edit $url/$id: wrong objectId")
        assertEquals(newDescr, json.read<Int>("$.description"), "edit $url/$id: description didn't change")
    }

    @Test
    fun `1-4 test get tokens`() {
        val json = restTemplate.getQueryJson(url).second
        ids.map { id ->
            assertNotEquals(0, json.read<List<Any>>("$[?(@.id == $id)]").size,
                    "get $url should have token #$id, ${json.jsonString()}")
        }
    }

    @Test
    fun `1-5 test delete tokens`() {
        ids.map { id ->
            var resp = restTemplate.deleteQueryString("$url/$id")
            assertEquals(HttpStatus.OK, resp.statusCode, "delete $url/$id returned ${resp.body}")

            resp = restTemplate.deleteQueryString("$url/$id")
            assertEquals(HttpStatus.NOT_MODIFIED, resp.statusCode, "delete $url/$id (2) returned ${resp.body}")
        }

        val json = restTemplate.getQueryJson(url).second
        ids.map { id ->
            assertEquals(0, json.read<List<Any>>("$[?(@.id == $id)]").size,
                    "get $url should NOT have token #$id, ${json.jsonString()}")
        }

    }

}