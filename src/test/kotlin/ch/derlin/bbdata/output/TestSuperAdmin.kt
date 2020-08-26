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
 * date: 19.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR])
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestSuperAdmin {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        // id of the new admin user
        var userId: Int = -1

        // auth based on the new user
        lateinit var additionalHeaders: Pair<String, Int>
    }

    @Test
    fun `0-0 create regular user in group "regular"`() {
        // == create a user, added to no group at all
        val resp = restTemplate.putWithBody("/users",
                """{"name": "TestUser-${Random.nextInt()}", "password": "testtest", "email": "lala@lulu.com"}""",
                HU to ROOT_ID)
        assertEquals(HttpStatus.OK, resp.statusCode, "put /users returned ${resp.body}")
        val json = JsonPath.parse(resp.body)

        userId = json.read<Int>("$.id")
        additionalHeaders = (HU to userId)
    }

    @Test
    fun `0-1 test regular user resources (all empty)`() {
        listOf(
                "/me/userGroups",
                "/objectGroups",
                "/objects"
        ).map { url ->
            val (status, json) = restTemplate.getQueryJson(url, additionalHeaders)
            assertEquals(HttpStatus.OK, status, "get $url returned ${json.jsonString()}")
            val cnt = json.read<List<Any>>("$").size
            assertEquals(0, cnt, "get $url: $cnt resources returned, expected 0")
        }
    }

    @Test
    fun `0-2 test regular user accesses (to nothing)`() {
        listOf(
                "/objectGroups/1",
                "/objects/1",
                "/userGroups/1/users"
        ).map { url ->
            val resp = restTemplate.getQueryString(url, additionalHeaders)
            assertTrue(resp.statusCode in listOf(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN),
                "get $url returned ${resp.body}")
        }
    }

    @Test
    fun `1-0 test set admin`() {
        val url = "/userGroups/$ROOT_ID/users/${userId}?admin=true"
        val resp = restTemplate.putQueryString(url, HU to ROOT_ID)
        assertEquals(HttpStatus.OK, resp.statusCode, "put $url returned ${resp.body}")
    }

    @Test
    fun `1-1 test superAdmin read access (to all)`() {
        listOf(
                ("/me/userGroups" to 3),
                ("/objectGroups" to 3),
                ("/objects" to 9)
        ).map { (url, minCount) ->
            val (status, json) = restTemplate.getQueryJson(url, additionalHeaders)
            assertEquals(HttpStatus.OK, status, "get $url returned ${json.jsonString()}")
            val cnt = json.read<List<Any>>("$").size
            assertTrue(cnt >= minCount, "get $url: $cnt resources returned, expected >= $minCount")
        }
    }

    @Test
    fun `1-2 test superAdmin write access (to all)`() {
        // can edit an object not owned by admin
        var resp = restTemplate.postWithBody("/objects/6",
                """{"description": "changed ${Random.nextInt()}"}""", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode, "edit object not owned by admin returned ${resp.body}")

        // can create an object in group other
        resp = restTemplate.putWithBody("/objects",
                """{"name": "superadmin ${Random.nextInt()}", "owner": 3, "unitSymbol": "lx"}""", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode, "put object in an objectGroup not owned returned ${resp.body}")
        val newObjectId = JsonPath.parse(resp.body).read<Int>("$.id")

        // can create an object group in group other
        resp = restTemplate.putWithBody("/objectGroups",
                """{"name": "superadmin ${Random.nextInt()}", "owner": 3}""", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode, "create objectGroup with another owner returned ${resp.body}")
        val newObjectGroupId = JsonPath.parse(resp.body).read<Int>("$.id")

        // can add the object to the objectGroup
        resp = restTemplate.putQueryString("/objectGroups/$newObjectGroupId/objects/$newObjectId", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode, "put object in the new objectGroup returned ${resp.body}")

        // can remove the object to the objectGroup
        resp = restTemplate.deleteQueryString("/objectGroups/$newObjectGroupId/objects/$newObjectId", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode, "remove object from the new objectGroup returned ${resp.body}")

        //  can remove objectGroup
        resp = restTemplate.deleteQueryString("/objectGroups/$newObjectGroupId", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode, "remove the new objectGroup returned ${resp.body}")

        // ... etc ...

    }

}