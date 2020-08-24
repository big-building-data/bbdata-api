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
class TestCreateUser {

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
        var putResponse = restTemplate.putWithBody(url, """{"name": ""}""")
        assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create no email / password
        putResponse = restTemplate.putWithBody(url, """{"name": "$name"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create short password
        putResponse = restTemplate.putWithBody(url, """{"name": "$name", "password": "x", "email": "$email"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create no email
        putResponse = restTemplate.putWithBody(url, """{"name": "$name", "password": "$password"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create wrong email
        putResponse = restTemplate.putWithBody(url, """{"name": "$name", "password": "$password", "email": "nope"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)
    }

    @Test
    fun `1-1 test create user`() {
        // == create
        val putResponse = restTemplate.putWithBody("/users",
                """{"name": "$name", "password": "$password", "email": "$email"}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)
        val json = JsonPath.parse(putResponse.body)

        // == store variables
        id = json.read<Int>("$.id")

        // == check response
        // same name
        assertEquals(name, json.read<String>("$.name"))
        // neither password not email sent back
        assertFalse(putResponse.body!!.contains("password"))
        assertFalse(putResponse.body!!.contains("email"))
    }

    @Test
    fun `1-2 test create user duplicated`() {
        // == no duplicate names allowed
        val putResponse2 = restTemplate.putWithBody("/users",
                """{"name": "$name", "password": "$password", "email": "$email"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putResponse2.statusCode)
    }

    @Test
    fun `1-3 get users`() {
        // == no duplicate names allowed
        val (status, json) = restTemplate.getQueryJson("/users")
        assertEquals(HttpStatus.OK, status)
        assertEquals(1, json.read<List<Any>>("$[?(@.name == '$name')]").size)
    }

    @Test
    fun `1-4 get user`() {
        // == no duplicate names allowed
        val (status, json) = restTemplate.getQueryJson("/users/$id")
        assertEquals(HttpStatus.OK, status)
        assertEquals(name, json.read<String>("$.name"))
    }

    @Test
    fun `2-1 test create user with userGroup`() {
        // == create
        val putResponse = restTemplate.putWithBody("/users?userGroupId=$REGULAR_USER_ID",
                """{"name": "$name-withGroup", "password": "$password", "email": "$email"}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)
        id =  JsonPath.parse(putResponse.body).read<Int>("$.id")

        // == get using user
        val getResponse = restTemplate.getQueryJson("/userGroups/$REGULAR_USER_ID/users/$id")
        assertEquals(HttpStatus.OK, getResponse.first)
        assertEquals(id!!, getResponse.second.read<Int>("$.id"))
    }
}