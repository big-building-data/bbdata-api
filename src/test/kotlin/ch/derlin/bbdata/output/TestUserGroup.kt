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
class TestUserGroup {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        var addedUserId: Int = -1 // will be created
        var id: Int = -1
    }

    @Test
    fun `1-0 test create user group fail`() {
        // == create no name
        var resp = restTemplate.putWithBody("/userGroups", """{"name": ""}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put /userGroups with an empty name returned ${resp.body}")

        // == create no owner
        resp = restTemplate.putWithBody("/userGroups", """{"name": "a"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put /userGroups with a short name returned ${resp.body}")
    }

    @Test
    fun `1-1 test create user group`() {
        // == create
        val putResp = restTemplate.putWithBody("/userGroups", """{"name": "usergroup-${Random.nextInt()}"}""")
        assertEquals(HttpStatus.OK, putResp.statusCode, "put /userGroups returned ${putResp.body}")

        // == store variables
        id = JsonPath.parse(putResp.body).read<Int>("$.id")

        // == get
        val getResp = restTemplate.getQueryString("/userGroups/$id")
        JSONAssert.assertEquals(putResp.body, getResp.body, false)
        val json = restTemplate.getQueryJson("/userGroups/$id/users").second
        assertEquals(1, json.read<List<Any>>("$").size,
                "put /userGroups: one user should have been added, ${json.jsonString()}")
        assertTrue(json.read<Boolean>("$.[0].admin"),
                "put /userGroups: after creation, one user must be admin ${json.jsonString()}")
    }

    @Test
    fun `1-2 test get user groups`() {
        val (status, json) = restTemplate.getQueryJson("/userGroups")
        assertEquals(HttpStatus.OK, status, "get /userGroups returned ${json.jsonString()}")
        assertEquals(1, json.read<List<Any>>("$[?(@.id == $id)]").size,
                "get /userGroups: missing newly created group #$id, ${json.jsonString()}")
    }

    @Test
    fun `1-3 test what user not in group sees`() {
        // create a user
        var resp = restTemplate.putWithBody("/users",
                """{"name": "user-${Random.nextInt()}", "email": "lasdf@sdfg.com", "password": "alsdkfj"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "put /users returned ${resp.body}")
        addedUserId = JsonPath.parse(resp.body).read<Int>("$.id")

        // user is not in group
        val (status, json) = restTemplate.getQueryJson("/me/userGroups", HU to addedUserId)
        assertEquals(HttpStatus.OK, status, "/me/userGroups returned ${json.jsonString()}")
        assertEquals(0, json.read<List<Any>>("$[?(@.id == $id)]").size,
                "get /me/userGroups: userGroup #$id should NOT be present ${json.jsonString()}")

        // user has still access to basic information about group
        var url = "/userGroups/$id"
        resp = restTemplate.getQueryString(url, HU to addedUserId)
        assertEquals(HttpStatus.OK, resp.statusCode, "get $url returned ${resp.body}")

        // user cannot see users in group
        url = "/userGroups/$id/users"
        resp = restTemplate.getQueryString(url, HU to addedUserId)
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode, "get $url returned ${resp.body}")

        // user cannot add users to group
        url = "/userGroups/$id/users/$addedUserId"
        resp = restTemplate.putQueryString(url, HU to addedUserId)
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode, "put $url returned ${resp.body}")
    }

    @Test
    fun `2-0 test add user`() {
        var url = "/userGroups/$id/users/$addedUserId"
        val resp = restTemplate.putQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "put $url returned ${resp.body}")

        url = "/userGroups/$id/users"
        val json = restTemplate.getQueryJson(url).second
        assertEquals(2, json.read<List<Any>>("$").size,
                "get $url: expecting two users (admin and #$addedUserId), ${json.jsonString()}")

        val u = json.read<List<Boolean>>("$[?(@.id == $addedUserId)].admin")
        assertEquals(1, u.size, "get $url: expecting #$addedUserId to be present, ${json.jsonString()}")
        assertFalse(u[0], "get $url: expecting #$addedUserId to NOT be admin, ${json.jsonString()}")
    }

    @Test
    fun `2-1 test what non-admins can see`() {
        // can see users
        var url = "/userGroups/$id/users"
        val (status, json) = restTemplate.getQueryJson(url, HU to addedUserId)
        assertEquals(HttpStatus.OK, status, "get $url returned ${json.jsonString()}")

        // cannot add users
        url = "/userGroups/$id/users/$addedUserId"
        val resp = restTemplate.putQueryString(url, HU to addedUserId)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode,
                "get $url returned ${json.jsonString()}")
    }

    @Test
    fun `3-0 test edit user admin`() {
        val url = "/userGroups/$id/users/$addedUserId?admin=true"
        var resp = restTemplate.putQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "put $url returned ${resp.body}")

        resp = restTemplate.putQueryString(url)
        assertEquals(HttpStatus.NOT_MODIFIED, resp.statusCode, "put $url (2) returned ${resp.body}")

        val json = restTemplate.getQueryJson("/userGroups/$id/users").second
        val u = json.read<List<Boolean>>("$[?(@.id == $addedUserId)].admin")
        assertEquals(1, u.size, "get $url: expecting #$addedUserId to be present, ${json.jsonString()}")
        assertTrue(u[0], "get $url: expecting #$addedUserId have been made admin, ${json.jsonString()}")
    }


    @Test
    fun `4-0 test remove user`() {
        val putResponse = restTemplate.deleteQueryString("/userGroups/$id/users/$addedUserId")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        val putResponse2 = restTemplate.deleteQueryString("/userGroups/$id/users/$addedUserId")
        assertEquals(HttpStatus.NOT_MODIFIED, putResponse2.statusCode)
    }


    @Test
    fun `5-0 test remove user group`() {
        val putResponse = restTemplate.deleteQueryString("/userGroups/$id")
        assertEquals(putResponse.statusCode, HttpStatus.OK)

        val putResponse2 = restTemplate.deleteQueryString("/userGroups/$id")
        assertEquals(putResponse2.statusCode, HttpStatus.NOT_MODIFIED)

        val getStatus = restTemplate.getQueryJson("/userGroups/$id").first
        assertEquals(HttpStatus.NOT_FOUND, getStatus)
    }
}