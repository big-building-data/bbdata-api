package ch.derlin.bbdata.output

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.deleteQueryString
import ch.derlin.bbdata.getQueryJson
import ch.derlin.bbdata.putWithBody
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.*
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestCreateUser {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val name = "user-${Random.nextInt(10000)}"
        val password = "x1234567y"
        val email = "lala@lulu.xxx"

        var id: Int? = -1
        var tpl: TestRestTemplate? = null

        @AfterAll
        @JvmStatic
        fun cleanup() {
            tpl?.let { tpl ->
                id?.let {
                    try {
                        tpl.deleteQueryString("/userGroups/$it")
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    @Test
    fun `1-0 test create user fail`() {
        val url = "/users"
        // == create empty name
        var putResponse = restTemplate.putWithBody(url, """{"name": ""}""")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create no email / password
        putResponse = restTemplate.putWithBody(url, """{"name": "$name"}""")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create short password
        putResponse = restTemplate.putWithBody(url, """{"name": "$name", "password": "x", "email": "$email"}""")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create no email
        putResponse = restTemplate.putWithBody(url, """{"name": "$name", "password": "$password"}""")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create wrong email
        putResponse = restTemplate.putWithBody(url, """{"name": "$name", "password": "$password", "email": "nope"}""")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)
    }

    @Test
    fun `1-1 test create user`() {
        // == create
        val putResponse = restTemplate.putWithBody("/users",
                """{"name": "$name", "password": "$password", "email": "$email"}""")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)
        val json = JsonPath.parse(putResponse.body)

        // == store variables
        id = json.read<Int>("$.id")
        tpl = restTemplate

        // == check response
        // same name
        Assertions.assertEquals(name, json.read<String>("$.name"))
        // neither password not email sent back
        Assertions.assertFalse(putResponse.body!!.contains("password"))
        Assertions.assertFalse(putResponse.body!!.contains("email"))
    }

    @Test
    fun `1-2 test create user bis`() {
        // == no duplicate names allowed
        val putResponse2 = restTemplate.putWithBody("/users",
                """{"name": "$name", "password": "$password", "email": "$email"}""")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse2.statusCode)
    }

    @Test
    fun `1-3 get user`() {
        // == no duplicate names allowed
        val (status, json) = restTemplate.getQueryJson("/users/$id")
        Assertions.assertEquals(HttpStatus.OK, status)
        Assertions.assertEquals(name, json.read<String>("$.name"))
    }
}