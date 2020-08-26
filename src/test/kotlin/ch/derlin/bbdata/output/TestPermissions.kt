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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.random.Random

/**
 * date: 08.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.OUTPUT_ONLY)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestPermissions {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val OWNER = arrayOf(HU to REGULAR_USER_ID, HA to APIKEY(REGULAR_USER_ID))
        val ILLEGAL = arrayOf(HU to NO_RIGHTS_USER_ID, HA to APIKEY(NO_RIGHTS_USER_ID))
    }

    @Test
    fun `1-1 resources should be editable by owners only`() {

        val owner_body = JsonEntity.create("{}", *OWNER)
        val illegal_body = JsonEntity.create("{}", *ILLEGAL)

        val errors = arrayListOf<String>()

        val urls = listOf(
                "/userGroups/2/users/3" to HttpMethod.PUT, // this will add "illegal" to group "regular", hence giving access
                "/objects/1" to HttpMethod.POST, // owned by regular
                "/objectGroups/1" to HttpMethod.POST, // owned by regular, having objects 1 & 2
                "/apikeys/3" to HttpMethod.POST // "wr2"
        )

        // --- "illegal" has been added to group regular with the first request, should have read access but no write access

        urls.forEach { (url, method) ->

            // EDIT: owner should be "bad request"/"ok"/"not modified", but not "not found"
            var response = restTemplate.exchange(url, method, owner_body, String::class.java)
            if (response.statusCode == HttpStatus.NOT_FOUND)
                errors.add("$method $url with owner returned ${response.statusCode} !")

            // EDIT: illegal should be not found or forbidden
            response = restTemplate.exchange(url, method, illegal_body, String::class.java)
            if (response.statusCode !in listOf(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN))
                errors.add("$method $url with illegal returned ${response.statusCode}, should not have access")

            // ACCESS: illegal should have access too (except for apikeys)
            if ("apikey" !in url) {
                response = restTemplate.exchange(url, HttpMethod.GET, illegal_body, String::class.java)
                if (response.statusCode != HttpStatus.OK)
                    errors.add("GET: $url with illegal returned ${response.statusCode}, should have access")
            }
        }

        // UNDO adding illegal to group regular
        restTemplate.deleteQueryString("/userGroups/2/users/3", *OWNER)
        assertTrue(errors.isEmpty(), errors.joinToString("\n") + "\n")

        // --- illegal has been removed from group "regular", should have no access left

        errors.clear()
        // now that ILLEGAL is removed from group, ensure all GET fail
        urls.forEach { (url, _) ->
            val response = restTemplate.exchange(url, HttpMethod.GET, illegal_body, String::class.java)
            if (response.statusCode == HttpStatus.OK)
                errors.add("GET after permissions removed: illegal has still access to {url}")
        }
        assertTrue(errors.isEmpty(), errors.joinToString("\n") + "\n")
    }

    @Test
    fun `2-1 object groups must be created by owners`() {
        // try to create an object group with user "illegal" but specifying usergroup he's not an admin of
        val name = "xxx${Random.nextInt(1000)}"
        val resp = restTemplate.putWithBody("/objectGroups",
                """{"name": "$name", "owner": $REGULAR_USER_ID}""", *ILLEGAL)

        if (resp.statusCode == HttpStatus.OK) {
            // FAILED: delete the group before making any assert
            JsonPath.parse(resp.body).read<Int?>("$.id").let { id ->
                restTemplate.delete("/objectGroups/$id")
            }
        }
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode,
                "creating an objectGroup without admin right returned ${resp.body}")
    }

    @Test
    fun `3-1 only super admin can add new units`() {
        // neither regular user nor no rights user is superadmin
        listOf(OWNER, ILLEGAL).forEach { headers ->
        val resp = restTemplate.postWithBody("/units",
                """{"type": "float", "name": "lala", "symbol": "L"}""", *headers)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode,
            "creating a unit without SUPERADMIN rights returned ${resp.body}")
        }
    }


}