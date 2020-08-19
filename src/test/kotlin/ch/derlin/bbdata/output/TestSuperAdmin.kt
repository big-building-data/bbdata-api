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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestSuperAdmin {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        var userId: Int = -1
        lateinit var additionalHeaders: Pair<String, Int>
    }

    @Test
    fun `0-0 create regular user in group bb`() {
        // == create a user, added to no group at all
        val putResponse = restTemplate.putWithBody("/users",
                """{"name": "TestUser${Random.nextInt()}", "password": "testtest", "email": "lala@lulu.com"}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)
        val json = JsonPath.parse(putResponse.body)

        userId = json.read<Int>("$.id")
        additionalHeaders = ("bbuser" to userId)
    }

    @Test
    fun `0-1 test regular user access (to nothing)`() {
        listOf(
                "/me/userGroups",
                "/objectGroups",
                "/objects"
        ).map { url ->
            val (status, json) = restTemplate.getQueryJson(url, additionalHeaders)
            assertEquals(HttpStatus.OK, status)
            val cnt = json.read<List<Any>>("$").size
            assertEquals(0, cnt,"$url: $cnt resources returned, expected 0")
        }
    }

    @Test
    fun `1-0 test set admin`() {
        val resp = restTemplate.putQueryString("/userGroups/1/users/${userId}?admin=true")
        assertEquals(HttpStatus.OK, resp.statusCode)
    }

    @Test
    fun `1-1 test superAdmin user read access (to all)`() {
        listOf(
                ("/me/userGroups" to 3),
                ("/objectGroups" to 3),
                ("/objects" to 9)
        ).map { (url, minCount) ->
            val (status, json) = restTemplate.getQueryJson(url, additionalHeaders)
            assertEquals(HttpStatus.OK, status)
            val cnt = json.read<List<Any>>("$").size
            assertTrue(cnt >= minCount, "$url: $cnt resources returned, expected >= $minCount")
        }


    }

    @Test
    fun `1-2 test superAdmin user write access (to all)`() {
        // can edit an object not owned by admin
        var resp = restTemplate.postWithBody("/objects/6",
                """{"description": "changed ${Random.nextInt()}"}""", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode)

        // can create an object in group aa
        resp = restTemplate.putWithBody("/objects",
                """{"name": "new ${Random.nextInt()}", "owner": 3, "unitSymbol": "lx"}""", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val newObjectId = JsonPath.parse(resp.body).read<Int>("$.id")

        // can create an object group in group aa
        resp = restTemplate.putWithBody("/objectGroups",
                """{"name": "new ${Random.nextInt()}", "owner": 3}""", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val newObjectGroupId = JsonPath.parse(resp.body).read<Int>("$.id")

        // can add the object to the objectGroup
        resp = restTemplate.putQueryString("/objectGroups/$newObjectGroupId/objects/$newObjectId", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode)

        // can remove the object to the objectGroup
        resp = restTemplate.deleteQueryString("/objectGroups/$newObjectGroupId/objects/$newObjectId", additionalHeaders)
        assertEquals(HttpStatus.OK, resp.statusCode)

        //  can remove objectGroup
        resp = restTemplate.deleteQueryString("/objectGroups/$newObjectGroupId")
        assertEquals(HttpStatus.OK, resp.statusCode)

        // ... etc ...

    }

}