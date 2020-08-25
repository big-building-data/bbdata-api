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
        var id: Int = -1
    }

    @Test
    fun `1-0 test create object group fail`() {
        // == create no name
        var putResponse = restTemplate.putWithBody("/objectGroups",
                """{"owner": $REGULAR_USER_ID, "description": "test"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create short name
        putResponse = restTemplate.putWithBody("/objectGroups",
                """{"name": "a", "owner": $REGULAR_USER_ID, "description": "test"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create no owner
        putResponse = restTemplate.putWithBody("/objectGroups",
                """{"name": "$name"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create wrong owner
        putResponse = restTemplate.putWithBody("/objectGroups",
                """{"name": "$name", "owner": -1, "description": "test"}""")
        assertEquals(HttpStatus.NOT_FOUND, putResponse.statusCode)
    }

    @Test
    fun `1-1 test create object group`() {
        // == create
        val putResponse = restTemplate.putWithBody("/objectGroups",
                """{"name": "$name", "owner": $REGULAR_USER_ID}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == store variables
        id = JsonPath.parse(putResponse.body).read<Int>("$.id")

        // == get
        val getResponse = restTemplate.getQueryString("/objectGroups/${id}")
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertEquals(REGULAR_USER.get("group"), json.read<String>("$.owner.name"))
    }
    
    @Test
    fun `1-2 test edit object group`(){
        // == change name + description
        var postResponse = restTemplate.postWithBody("/objectGroups/$id",
                """{"name": "xxx", "description": "test"}""")
        assertEquals(HttpStatus.OK, postResponse.statusCode)

        var json = JsonPath.parse(postResponse.body)
        assertEquals("xxx", json.read<String>("$.name"))
        assertEquals("test", json.read<String>("$.description"))

        // == change name only
        postResponse = restTemplate.postWithBody("/objectGroups/$id", """{"name": "$name"}""")
        assertEquals(HttpStatus.OK, postResponse.statusCode)

        json = JsonPath.parse(postResponse.body)
        assertEquals(name, json.read<String>("$.name"))
        assertEquals("test", json.read<String>("$.description"))
    }

    @Test
    fun `2-1 test add object`() {
        val putResponse = restTemplate.putQueryString("/objectGroups/$id/objects/1")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        val json = restTemplate.getQueryJson("/objectGroups/${id}/objects").second
        assertTrue(json.read<List<Any>>("$[?(@.id == 1)]").size > 0)
    }

    @Test
    fun `2-2 test get objects`() {
        val json = restTemplate.getQueryJson("/objectGroups/${id}?withObjects=true").second
        val objs1 = json.read<List<String>>("$.objects")
        assertTrue(objs1.size > 0)

        val objs2 = restTemplate.getQueryString("/objectGroups/${id}/objects").body!!
        JSONAssert.assertEquals(objs1.toString(), objs2, false)
    }

    @Test
    fun `2-4 test object withObjects`() {
        val respFalse = restTemplate.getQueryString("/objectGroups/$id?withObjects=false")
        assertFalse(respFalse.body!!.contains("\"objects\""))

        val respTrue = restTemplate.getQueryString("/objectGroups/$id?withObjects=true")
        assertTrue(respTrue.body!!.contains("\"objects\""))
    }

    @Test
    fun `2-5 test remove object`() {
        val putResponse = restTemplate.deleteQueryString("/objectGroups/$id/objects/1")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        val putResponse2 = restTemplate.deleteQueryString("/objectGroups/$id/objects/1")
        assertEquals(HttpStatus.NOT_MODIFIED, putResponse2.statusCode)
    }

    @Test
    fun `3-1 test objectGroup withObjects`() {
        val respFalse = restTemplate.getQueryString("/objectGroups?withObjects=false")
        assertFalse(respFalse.body!!.contains("\"objects\""))

        val respTrue = restTemplate.getQueryString("/objectGroups?withObjects=true")
        assertTrue(respTrue.body!!.contains("\"objects\""))
    }


    @Test
    fun `3-1 test add permission`() {
        val putResp1 = restTemplate.putQueryString("/objectGroups/$id/userGroups/1")
        assertEquals(HttpStatus.OK, putResp1.statusCode)
        val putResp2 = restTemplate.putQueryString("/objectGroups/$id/userGroups/1")
        assertEquals(HttpStatus.NOT_MODIFIED, putResp2.statusCode)

        val json = restTemplate.getQueryJson("/objectGroups/$id/userGroups").second
        assertTrue(json.read<List<Any>>("$[?(@.id == 1)]").size > 0)
    }

    @Test
    fun `3-1 test remove permission`() {
        val putResp1 = restTemplate.deleteQueryString("/objectGroups/$id/userGroups/1")
        assertEquals(HttpStatus.OK, putResp1.statusCode)
        val putResp2 = restTemplate.deleteQueryString("/objectGroups/$id/userGroups/1")
        assertEquals(HttpStatus.NOT_MODIFIED, putResp2.statusCode)

        val json = restTemplate.getQueryJson("/objectGroups/$id/permissions").second
        assertTrue(json.read<List<Any>>("$[?(@.id == 1)]").size == 0)
    }

    @Test
    fun `5-0 test remove object group`() {
        val putResponse = restTemplate.deleteQueryString("/objectGroups/$id")
        assertEquals(putResponse.statusCode, HttpStatus.OK)

        val putResponse2 = restTemplate.deleteQueryString("/objectGroups/$id")
        assertEquals(putResponse2.statusCode, HttpStatus.NOT_MODIFIED)

        val getStatus = restTemplate.getQueryJson("/objectGroups/$id").first
        assertEquals(HttpStatus.NOT_FOUND, getStatus)
    }
}