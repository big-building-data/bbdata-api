package ch.derlin.bbdata.output

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
class TestUsers {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val name = "user-${Random.nextInt(10000)}"
        val password = "x1234567y"
        val email = "lala@lulu.xxx"

        // id of the last created user
        var id: Int? = -1
    }

    @Test
    fun `1-0 test create user fail`() {
        val url = "/users"
        // == create empty name
        var resp = restTemplate.putWithBody(url, """{"name": ""}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put $url with only an empty name returned ${resp.body}")

        // == create no email / password
        resp = restTemplate.putWithBody(url, """{"name": "$name"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put $url with only a name returned ${resp.body}")

        // == create short password
        resp = restTemplate.putWithBody(url, """{"name": "$name", "password": "x", "email": "$email"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put $url with a short password (1 char) returned ${resp.body}")

        // == create no email
        resp = restTemplate.putWithBody(url, """{"name": "$name", "password": "$password"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put $url with no email returned ${resp.body}")

        // == create wrong email
        resp = restTemplate.putWithBody(url, """{"name": "$name", "password": "$password", "email": "nope"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode,
                "put $url with an improper email returned ${resp.body}")
    }

    @Test
    fun `1-1 test create user`() {
        // == create
        val resp = restTemplate.putWithBody("/users",
                """{"name": "$name", "password": "$password", "email": "$email"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "put /users returned ${resp.body}")
        val json = JsonPath.parse(resp.body)

        // == store variables
        id = json.read<Int>("$.id")

        // == check response
        // same name
        assertEquals(name, json.read<String>("$.name"), "put /users: wrong name, ${json.jsonString()}")
        // neither password not email sent back
        assertFalse(resp.body!!.contains("password"), "put /users: a password has been returned, ${json.jsonString()}")
        assertFalse(resp.body!!.contains("email"), "put /users: an email has been returned, ${json.jsonString()}")
    }

    @Test
    fun `1-2 test create user duplicated`() {
        // == no duplicate names allowed
        val resp = restTemplate.putWithBody("/users",
                """{"name": "$name", "password": "$password", "email": "$email"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode, "put /users (2) ${resp.body}")
    }

    @Test
    fun `1-3 get users`() {
        // == no duplicate names allowed
        val (status, json) = restTemplate.getQueryJson("/users")
        assertEquals(HttpStatus.OK, status, "get /users returned ${json.jsonString()}")
        assertEquals(1, json.read<List<Any>>("$[?(@.name == '$name')]").size,
                "get /users: expecting user '$name' to be present ${json.jsonString()}")
    }

    @Test
    fun `1-4 get user`() {
        // == no duplicate names allowed
        val (status, json) = restTemplate.getQueryJson("/users/$id")
        assertEquals(HttpStatus.OK, status, "get /users/$id returned ${json.jsonString()}")
        assertEquals(name, json.read<String>("$.name"), "get /users/$id: wrong name")
    }


    @Test
    fun `2-1 test create user with userGroup`() {
        // == create
        var url = "/users?userGroupId=$REGULAR_USER_ID"
        val resp = restTemplate.putWithBody(url,
                """{"name": "$name-withGroup", "password": "$password", "email": "$email"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "put $url returned ${resp.body}")
        id = JsonPath.parse(resp.body).read<Int>("$.id")

        // == get using user
        url = "/userGroups/$REGULAR_USER_ID/users/$id"
        val (status, json) = restTemplate.getQueryJson(url)
        assertEquals(HttpStatus.OK, status, "get $url returned ${json.jsonString()}")
        assertEquals(id!!, json.read<Int>("$.id"), "get $url: wrong id")
    }

    @Test
    fun `3-1 test delete user fail (regular user)`() {
        val resp = restTemplate.deleteQueryString("/users/$id")
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode, "delete /users/$id returned ${resp.body}")
    }

    @Test
    fun `3-1 test delete user (root user)`() {
        var resp = restTemplate.deleteQueryString("/users/$id", HU to ROOT_ID)
        assertEquals(HttpStatus.OK, resp.statusCode, "delete /users/$id returned ${resp.body}")

        resp = restTemplate.deleteQueryString("/users/$id", HU to ROOT_ID)
        assertEquals(HttpStatus.NOT_MODIFIED, resp.statusCode, "delete /users/$id returned ${resp.body}")
    }

    @Test
    fun `3-2 test get deleted user`() {
        val resp = restTemplate.getQueryString("/users/$id")
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode, "get /users/$id returned ${resp.body}")

        val url = "/userGroups/$REGULAR_USER_ID/users"
        val (status, json) = restTemplate.getQueryJson(url)
        assertEquals(HttpStatus.OK, status, "get $url returned ${json.jsonString()}")
        assertEquals(0, json.read<List<Any>>("$[?(@.id == '$id')]").size,
                "get $url: user #$id should NOT to be present ${json.jsonString()}")
    }
}