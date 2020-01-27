package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import com.jayway.jsonpath.DocumentContext
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
class TestApikeys {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val userId: Int = 1
        var ids: MutableList<Int> = mutableListOf()
        val secrets: MutableList<String> = mutableListOf()
        var tpl: TestRestTemplate? = null

        @AfterAll
        @JvmStatic
        fun cleanup() {
            tpl?.let { tpl ->
                ids.map {
                    try {
                        tpl.deleteQueryString("/apikeys/$it", "bbuser" to 1, "bbtoken" to "wr1")
                    } catch (e: Exception) {
                        println("!! delete failed for $it")
                    }
                }
            }
        }
    }


    @Test
    fun `1-1 test create apikeys`() {
        tpl = restTemplate

        createApikey()
        createApikey(expire = "1d")
        createApikey(expire = "1d-2m-3s")
        createApikey(description = "this is a test")
        createApikey(writable = true)
    }

    @Test
    fun `1-2 test apikeys`() {
        secrets.map {
            val resp = restTemplate.getQueryString("/objectGroups", "bbuser" to userId, "bbtoken" to it)
            assertEquals(HttpStatus.OK, resp.statusCode)
        }
        secrets.take(secrets.size - 1).map {
            val resp = restTemplate.putQueryString("/objectGroups", "bbuser" to userId, "bbtoken" to it)
            assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        }
    }

    @Test
    fun `1-3 test get apikeys`() {
        val pair = restTemplate.getQueryJson("/apikeys", "bbuser" to 1, "bbtoken" to "wr1")
        assertEquals(HttpStatus.OK, pair.first)
        assertEquals(1, pair.second.read<List<String>>("$[?(@.secret=='${secrets.random()}')]").size)
    }

    @Test
    fun `1-4 test delete apikeys`() {
        ids.zip(secrets).map {
            val resp = restTemplate.deleteQueryString("/apikeys/${it.first}", "bbuser" to userId, "bbtoken" to "wr1")
            assertEquals(HttpStatus.OK, resp.statusCode)
        }
    }

    @Test
    fun `1-5 test get apikeys after delete`() {
        val pair = restTemplate.getQueryJson("/apikeys", "bbuser" to 1, "bbtoken" to "wr1")
        assertEquals(HttpStatus.OK, pair.first)
        assertEquals(0, pair.second.read<List<String>>("$[?(@.secret=='${secrets.random()}')]").size)
    }

    // -------

    fun createApikey(writable: Boolean = false, expire: String? = null, description: String? = null): DocumentContext? {
        // create url
        val pathParams = mutableListOf<String>()
        if (writable) pathParams.add("writable=true")
        if (expire != null) pathParams.add("expire=$expire")
        val url = "/apikeys" + (if (pathParams.size > 0) "?" + pathParams.joinToString("&") else "")

        // make query
        println(url)
        val response =
                if (description != null)
                    restTemplate.putWithBody(url, """{"description": "$description"}""",
                            "bbuser" to 1, "bbtoken" to "wr1")
                else
                    restTemplate.putQueryString(url, "bbuser" to 1, "bbtoken" to "wr1")

        assertEquals(HttpStatus.OK, response.statusCode)
        val json = JsonPath.parse(response.body)

        // store variables
        ids.add(json.read<Int>("$.id"))
        secrets.add(json.read<String>("$.secret"))

        // check some json variables
        assertEquals(!writable, json.read("$.readOnly"))
        assertEquals(description, json.read<String>("$.description"))
        assertEquals(expire == null, json.read<String>("$.expirationDate") == null)
        assertEquals(32, secrets.last().length)

        return json
    }
}