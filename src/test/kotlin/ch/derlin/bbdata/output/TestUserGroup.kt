package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
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
        val addedUserId = 2 // should not be 1, since 1 is the owner
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
        var putResponse = restTemplate.putWithBody("/userGroups", """{"name": ""}""")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create no owner
        putResponse = restTemplate.putWithBody("/userGroups", """{"name": "a"}""")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)
    }

    @Test
    fun `1-1 test create user group`() {
        // == create
        val putResponse = restTemplate.putWithBody("/userGroups", """{"name": "$name"}""")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == store variables
        id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        tpl = restTemplate

        // == get
        val getResponse = restTemplate.getQueryString("/userGroups/${id}")
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)
        val json = restTemplate.getQueryJson("/userGroups/${id}/users").second
        Assertions.assertEquals(1, json.read<List<Any>>("$").size) // Ensure one user id added
        Assertions.assertTrue(json.read<Boolean>("$.[0].admin"))
    }

    @Test
    fun `1-2 test get user groups`(){
        val (status, json) = restTemplate.getQueryJson("/userGroups")
        Assertions.assertEquals(HttpStatus.OK, status)
        Assertions.assertEquals(1, json.read<List<Any>>("$[?(@.id == $id)]").size)
    }

    @Test
    fun `1-3 test what user not in group sees`() {
        // user is not in group
        val (status, json) = restTemplate.getQueryJson("/me/userGroups", "bbuser" to addedUserId)
        Assertions.assertEquals(HttpStatus.OK, status)
        Assertions.assertEquals(0, json.read<List<Any>>("$[?(@.id == $id)]").size)

        // user has still access to basic information about group
        val getUserGroupResponse = restTemplate.getQueryString("/userGroups/$id", "bbuser" to addedUserId)
        Assertions.assertEquals(HttpStatus.OK, getUserGroupResponse.statusCode)

        // user cannot see users in group
        val getUsersInGroupResponse = restTemplate.getQueryString("/userGroups/$id/users", "bbuser" to addedUserId)
        Assertions.assertEquals(HttpStatus.NOT_FOUND, getUsersInGroupResponse.statusCode)

        // user cannot add users to group
        val putUserResponse = restTemplate.putQueryString("/userGroups/$id/users/$addedUserId", "bbuser" to addedUserId)
        Assertions.assertEquals(HttpStatus.NOT_FOUND, putUserResponse.statusCode)
    }

    @Test
    fun `2-0 test add user`() {
        val putResponse = restTemplate.putQueryString("/userGroups/$id/users/$addedUserId")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        val json = restTemplate.getQueryJson("/userGroups/${id}/users").second
        Assertions.assertEquals(2, json.read<List<Any>>("$").size)

        val u = json.read<List<Boolean>>("$[?(@.id == $addedUserId)].admin")
        Assertions.assertEquals(1, u.size)
        Assertions.assertFalse(u[0]) // not admin
    }

    @Test
    fun `2-1 test what non-admins can see`() {
        // can see users
        val (status, _) = restTemplate.getQueryJson("/userGroups/${id}/users", "bbuser" to addedUserId)
        Assertions.assertEquals(HttpStatus.OK, status)

        // cannot add users
        val putResponse = restTemplate.putQueryString("/userGroups/$id/users/$addedUserId", "bbuser" to addedUserId)
        Assertions.assertEquals(HttpStatus.FORBIDDEN, putResponse.statusCode)
    }

    @Test
    fun `3-0 test edit user admin`() {
        val putResponse = restTemplate.putQueryString("/userGroups/$id/users/$addedUserId?admin=true")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        val putResponseNM = restTemplate.putQueryString("/userGroups/$id/users/$addedUserId?admin=true")
        Assertions.assertEquals(HttpStatus.NOT_MODIFIED, putResponseNM.statusCode)

        val json = restTemplate.getQueryJson("/userGroups/${id}/users").second
        val u = json.read<List<Boolean>>("$[?(@.id == $addedUserId)].admin")
        Assertions.assertEquals(1, u.size)
        Assertions.assertTrue(u[0]) // now admin
    }


    @Test
    fun `4-0 test remove user`() {
        val putResponse = restTemplate.deleteQueryString("/userGroups/$id/users/$addedUserId")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        val putResponse2 = restTemplate.deleteQueryString("/userGroups/$id/users/$addedUserId")
        Assertions.assertEquals(HttpStatus.NOT_MODIFIED, putResponse2.statusCode)
    }


    @Test
    fun `5-0 test remove user group`() {
        val putResponse = restTemplate.deleteQueryString("/userGroups/$id")
        Assertions.assertEquals(putResponse.statusCode, HttpStatus.OK)

        val putResponse2 = restTemplate.deleteQueryString("/userGroups/$id")
        Assertions.assertEquals(putResponse2.statusCode, HttpStatus.NOT_MODIFIED)

        val getStatus = restTemplate.getQueryJson("/userGroups/$id").first
        Assertions.assertEquals(HttpStatus.NOT_FOUND, getStatus)
    }
}