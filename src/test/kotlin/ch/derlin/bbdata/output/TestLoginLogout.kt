package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.AfterAll
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
        var ids: MutableList<Int> = mutableListOf()
        val secrets: MutableList<String> = mutableListOf()
        var tpl: TestRestTemplate? = null

        @AfterAll
        @JvmStatic
        fun cleanup() {
            tpl?.let { tpl ->
                ids.map {
                    try {
                        tpl.deleteQueryString("/apikeys/$it")
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    @Test
    fun `1-0 test not login`() {
        val resp = restTemplate.getQueryString("/objects")
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }

    @Test
    fun `1-1 test login`() {
        // == login
        val putResponse = restTemplate.postWithBody("/login",
                """{"username": "${user.get("name")}", "password": "${user.get("password")}"}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)
        val json = JsonPath.parse(putResponse.body)

        // == store variables
        val id = json.read<Int>("$.id")
        val secret = json.read<String>("$.secret")
        ids.add(id)
        secrets.add(secret)
        tpl = restTemplate

        // check some json variables
        assertEquals(false, json.read("$.readOnly"))
        assertNotNull(json.read<String>("$.description"))
        assertNotNull(json.read<String>("$.expirationDate"))
        assertEquals(32, secret.length)

        val resp = restTemplate.getQueryString("/objects", HU to user.get("id"), HA to secret)
        assertEquals(HttpStatus.OK, resp.statusCode)
    }

    @Test
    fun `1-1 test logout`() {
        // == logout
        val secret = secrets.first()
        val putResponse = restTemplate.postQueryString("/logout", HU to user.get("id"), HA to secret)
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        val resp = restTemplate.getQueryString("/objects", HU to user.get("id"), HA to secret)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
    }
}