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

/**
 * date: 28.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestLoginLogout {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val user = ROOT_USER
        var id: Int = 0
        var secret: String = ""
    }

    @Test
    fun `1-0 test not login`() {
        val resp = restTemplate.getQueryString("/objects")
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode, "get /objects returned ${resp.body}")
    }

    @Test
    fun `1-1 test login`() {
        // == login
        var resp = restTemplate.postWithBody("/login",
                """{"username": "${user.get("name")}", "password": "${user.get("password")}"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "/login returned ${resp.body}")
        val json = JsonPath.parse(resp.body)

        // == store variables
        id = json.read<Int>("$.id")
        secret = json.read<String>("$.secret")

        // check some json variables
        assertEquals(false, json.read("$.readOnly"), "/login should be readOnly")
        assertNotNull(json.read<String>("$.description"), "/login should have a description")
        assertNotNull(json.read<String>("$.expirationDate"), "/login should have an expiration date")
        assertEquals(32, secret.length, "/login token is not 32 chars long")

        resp = restTemplate.getQueryString("/objects", HU to user.get("id"), HA to secret)
        assertEquals(HttpStatus.OK, resp.statusCode, "get /objects returned ${resp.body}")
    }

    @Test
    fun `1-1 test logout`() {
        // == logout
        var resp = restTemplate.postQueryString("/logout", HU to user.get("id"), HA to secret)
        assertEquals(HttpStatus.OK, resp.statusCode, "/logout returned ${resp.body}")

        // == ensure it does not work anymore
        resp = restTemplate.getQueryString("/objects", HU to user.get("id"), HA to secret)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode, "get /objects returned ${resp.body}")
    }
}