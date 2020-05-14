package ch.derlin.bbdata.output

import ch.derlin.bbdata.JsonEntity
import ch.derlin.bbdata.Profiles
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
        val USER1 = arrayOf("bbuser" to 1, "bbtoken" to "wr1")
        val USER2 = arrayOf("bbuser" to 2, "bbtoken" to "wr2")
    }

    @Test
    fun `1-1 resources should be editable by owners only`() {

        val body1 = JsonEntity.create("{}", *USER1)
        val body2 = JsonEntity.create("{}", *USER2)

        val errors = arrayListOf<String>()

        listOf(
                "/objects/1" to HttpMethod.POST,
                "/objectGroups/1" to HttpMethod.POST,
                "/apikeys/1" to HttpMethod.POST,
                "/userGroups/1/users/2" to HttpMethod.PUT
        ).forEach { (url, method) ->

            // EDIT: user 1 should be "bad request"/"ok"/"not modified", but not "not found"
            var response = restTemplate.exchange(url, method, body1, String::class.java)
            if (response.statusCode == HttpStatus.NOT_FOUND)
                errors.add("EDIT $url with user1 returned ${response.statusCode} !")

            // EDIT: user 2 should be not found or forbidden
            response = restTemplate.exchange(url, method, body2, String::class.java)
            if (response.statusCode !in listOf(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN))
                errors.add("EDIT $url with user2 returned ${response.statusCode}, should not have access")

            // ACCESS: user 2 should have access too
            response = restTemplate.exchange(url, HttpMethod.GET, body2, String::class.java)
            if (response.statusCode == HttpStatus.NOT_FOUND)
                errors.add("GET: $url with user2 returned ${response.statusCode}, should have access")
        }

        assertTrue(errors.isEmpty(), errors.joinToString("\n") + "\n")
    }

    @Test
    fun `2-1 user 1 should not be touched`() {
        listOf(HttpMethod.PUT, HttpMethod.DELETE).forEach { method ->
            val response = restTemplate.exchange(
                    "/userGroups/2/users/1", method, JsonEntity.empty(*USER1), String::class.java)
            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertTrue(response.body!!.contains("Cannot .* SUPERUSER".toRegex()), "$method: ${response.body}")
        }
    }
}