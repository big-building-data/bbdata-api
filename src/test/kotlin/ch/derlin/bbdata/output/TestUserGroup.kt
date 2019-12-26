package ch.derlin.bbdata.output

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.*
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestUserGroup {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val name = "usergroup-${Random.nextInt(10000)}"
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
    fun `1-0 test create user group fail`() {
        // == create no name
        var putResponse = restTemplate.putWithBody("/userGroups",
                """{"name": ""}""", String::class.java)
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create no owner
        putResponse = restTemplate.putWithBody("/userGroups",
                """{"name": "a"}""", String::class.java)
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)
    }

    @Test
    fun `1-1 test create user group`() {
        // == create
        val putResponse = restTemplate.putWithBody("/userGroups",
                """{"name": "$name"}""", String::class.java)
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == store variables
        id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        tpl = restTemplate

        // == get
        val getResponse = restTemplate.getForEntity("/userGroups/${id}", String::class.java)
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)
        val json = restTemplate.getQueryJson("/userGroups/${id}/users", String::class.java).second
        Assertions.assertEquals(1, json.read<List<Any>>("$").size) // Ensure one user id added
        Assertions.assertTrue(json.read<Boolean>("$.[0].admin"))
    }

    @Test
    fun `2-1 test add user`() {
        val putResponse = restTemplate.putQueryString("/userGroups/$id/users?userId=2")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        val json = restTemplate.getQueryJson("/userGroups/${id}/users", String::class.java).second
        Assertions.assertEquals(2, json.read<List<Any>>("$").size)

        val u = json.read<List<Boolean>>("$[?(@.id == 2)].admin")
        Assertions.assertEquals(1, u.size)
        Assertions.assertFalse(u[0]) // not admin
    }

    @Test
    fun `2-2 test edit user admin`() {
        val putResponse = restTemplate.putQueryString("/userGroups/$id/users?userId=2&admin=true")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        val putResponseNM = restTemplate.putQueryString("/userGroups/$id/users?userId=2&admin=true")
        Assertions.assertEquals(HttpStatus.NOT_MODIFIED, putResponseNM.statusCode)

        val json = restTemplate.getQueryJson("/userGroups/${id}/users", String::class.java).second
        val u = json.read<List<Boolean>>("$[?(@.id == 2)].admin")
        Assertions.assertEquals(1, u.size)
        Assertions.assertTrue(u[0]) // now admin
    }


    @Test
    fun `2-3 test remove user`() {
        val putResponse = restTemplate.deleteQueryString("/userGroups/$id/users?userId=2")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        val putResponse2 = restTemplate.deleteQueryString("/userGroups/$id/users?userId=2")
        Assertions.assertEquals(HttpStatus.NOT_MODIFIED, putResponse2.statusCode)
    }


    @Test
    fun `3-0 test remove user group`() {
        val putResponse = restTemplate.deleteQueryString("/userGroups/$id")
        Assertions.assertEquals(putResponse.statusCode, HttpStatus.OK)

        val putResponse2 = restTemplate.deleteQueryString("/userGroups/$id")
        Assertions.assertEquals(putResponse2.statusCode, HttpStatus.NOT_MODIFIED)

        val getStatus = restTemplate.getQueryJson("/userGroups/$id").first
        Assertions.assertEquals(HttpStatus.NOT_FOUND, getStatus)
    }
}