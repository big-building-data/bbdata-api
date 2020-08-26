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
import kotlin.random.Random

/**
 * date: 20.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR])
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectGroup {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val name = "test-${Random.nextInt(10000)}"
        val userAdd = 1 // user added to group to test permissions
        var id: Int = -1
    }

    @Test
    fun `1-0 test create object group fail`() {
        // == create no name
        var resp = restTemplate.putWithBody("/objectGroups",
                """{"owner": $REGULAR_USER_ID, "description": "test"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put /objectGroups no name returned ${resp.body}")

        // == create short name
        resp = restTemplate.putWithBody("/objectGroups",
                """{"name": "a", "owner": $REGULAR_USER_ID, "description": "test"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put /objectGroups name too short returned ${resp.body}")

        // == create no owner
        resp = restTemplate.putWithBody("/objectGroups",
                """{"name": "$name"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put /objectGroups no owner returned ${resp.body}")

        // == create wrong owner
        resp = restTemplate.putWithBody("/objectGroups",
                """{"name": "$name", "owner": -1, "description": "test"}""")
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode,
                "put /objectGroups wrong owner returned ${resp.body}")
    }

    @Test
    fun `1-1 test create object group`() {
        // == create
        val resp = restTemplate.putWithBody("/objectGroups",
                """{"name": "$name", "owner": $REGULAR_USER_ID}""")
        assertEquals(HttpStatus.OK, resp.statusCode,
                "put /objectGroups ok returned ${resp.body}")

        // == store variables
        id = JsonPath.parse(resp.body).read<Int>("$.id")

        // == get
        val getResponse = restTemplate.getQueryString("/objectGroups/$id")
        JSONAssert.assertEquals(resp.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertEquals(REGULAR_USER.get("group"), json.read<String>("$.owner.name"),
                "get /objectGroups/$id has wrong owner name")
    }

    @Test
    fun `1-2 test edit object group`() {
        // == change name + description
        var resp = restTemplate.postWithBody("/objectGroups/$id",
                """{"name": "xxx", "description": "test"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "edit /objectGroups/$id (all fields) returned ${resp.body}")

        var json = JsonPath.parse(resp.body)
        assertEquals("xxx", json.read<String>("$.name"),
                "edit /objectGroups/$id all fields: name didn't change")
        assertEquals("test", json.read<String>("$.description"),
                "edit /objectGroups/$id all fields: description didn't change")

        // == change name only
        resp = restTemplate.postWithBody("/objectGroups/$id", """{"name": "$name"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "edit /objectGroups/$id (name only) returned ${resp.body}")

        json = JsonPath.parse(resp.body)
        assertEquals(name, json.read<String>("$.name"),
                "edit /objectGroups/$id name only: name didn't change")
        assertEquals("test", json.read<String>("$.description"),
                "edit /objectGroups/$id name only: description did change")
    }

    @Test
    fun `2-1 test add object`() {
        val resp = restTemplate.putQueryString("/objectGroups/$id/objects/1")
        assertEquals(HttpStatus.OK, resp.statusCode,
                "add object 1 to /objectGroups/$id returned ${resp.body}")

        val json = restTemplate.getQueryJson("/objectGroups/$id/objects").second
        assertTrue(json.read<List<Any>>("$[?(@.id == 1)]").size > 0,
                "get objects in objectGroups/$id missing object 1, ${json.jsonString()}")
    }

    @Test
    fun `2-2 test get objects`() {
        val json = restTemplate.getQueryJson("/objectGroups/$id?withObjects=true").second
        val objs1 = json.read<List<String>>("$.objects")
        assertTrue(objs1.size > 0,
                "/objectGroups/$id?withObjects returned zero objects (should be 1), ${json.jsonString()}")

        val objs2 = restTemplate.getQueryString("/objectGroups/$id/objects").body!!
        JSONAssert.assertEquals(objs1.toString(), objs2, false)
    }

    @Test
    fun `2-4 test object withObjects`() {
        val url = "/objectGroups/$id?withObjects"
        var resp = restTemplate.getQueryString("$url=false")
        assertFalse(resp.body!!.contains("\"objects\""),
                "get $url=false shouldn't have objects property, ${resp.body}")

        resp = restTemplate.getQueryString("$url=true")
        assertTrue(resp.body!!.contains("\"objects\""),
                "get $url=true should have objects property, ${resp.body}")
    }

    @Test
    fun `2-5 test remove object`() {
        val url = "/objectGroups/$id/objects/1"
        var resp = restTemplate.deleteQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "delete $url returned ${resp.body}")

        resp = restTemplate.deleteQueryString(url)
        assertEquals(HttpStatus.NOT_MODIFIED, resp.statusCode, "delete $url (2) returned ${resp.body}")
    }

    @Test
    fun `3-1 test objectGroup withObjects`() {
        val url = "/objectGroups?withObjects"
        var resp = restTemplate.getQueryString("$url=false")
        assertFalse(resp.body!!.contains("\"objects\""),
                "get $url=false shouldn't have objects property, ${resp.body}")

        resp = restTemplate.getQueryString("/objectGroups?withObjects=true")
        assertTrue(resp.body!!.contains("\"objects\""),
                "get $url=true should have objects property, ${resp.body}")
    }


    @Test
    fun `3-1 test add permission`() {
        val url = "/objectGroups/$id/userGroups"
        var resp = restTemplate.putQueryString("$url/$userAdd")
        assertEquals(HttpStatus.OK, resp.statusCode, "put $url/$userAdd returned ${resp.body}")
        resp = restTemplate.putQueryString("$url/1")
        assertEquals(HttpStatus.NOT_MODIFIED, resp.statusCode, "put $url/$userAdd (2) returned ${resp.body}")

        val json = restTemplate.getQueryJson(url).second
        assertTrue(json.read<List<Any>>("$[?(@.id == $userAdd)]").size > 0,
                "get $url should have user #$userAdd ${json.jsonString()}")
    }

    @Test
    fun `3-1 test remove permission`() {
        val url = "/objectGroups/$id/userGroups"
        var resp = restTemplate.deleteQueryString("$url/$userAdd")
        assertEquals(HttpStatus.OK, resp.statusCode, "delete $url/$userAdd returned ${resp.body}")
        resp = restTemplate.deleteQueryString("$url/$userAdd")
        assertEquals(HttpStatus.NOT_MODIFIED, resp.statusCode, "delete $url/$userAdd (2) returned ${resp.body}")

        val json = restTemplate.getQueryJson("/objectGroups/$id/permissions").second
        assertTrue(json.read<List<Any>>("$[?(@.id == 1)]").size == 0,
                "get $url should NOT have user #$userAdd ${json.jsonString()}")
    }

    @Test
    fun `5-0 test remove object group`() {
        val url = "/objectGroups/$id"
        var resp = restTemplate.deleteQueryString(url)
        assertEquals(resp.statusCode, HttpStatus.OK, "delete $url returned ${resp.body}")

        resp = restTemplate.deleteQueryString(url)
        assertEquals(resp.statusCode, HttpStatus.NOT_MODIFIED, "delete $url (2) returned ${resp.body}")

        val (status, json) = restTemplate.getQueryJson(url)
        assertEquals(HttpStatus.NOT_FOUND, status, "get $url after delete returned ${json.jsonString()}")
    }
}