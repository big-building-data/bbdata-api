package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import com.jayway.jsonpath.DocumentContext
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
        var ids: MutableList<Int> = mutableListOf()
        val secrets: MutableList<String> = mutableListOf()

        val user = REGULAR_USER_ID
        val headers = arrayOf(HU to user, HA to APIKEY(user))
    }


    @Test
    fun `1-1 test create apikeys`() {
        createApikey()
        createApikey(expirationDate = "1d")
        createApikey(expirationDate = "1d-2m-3s")
        createApikey(description = "this is a test")
        createApikey(writable = true)
    }


    @Test
    fun `2-1 test apikeys`() {
        // all apikeys have read access
        secrets.map {
            val resp = restTemplate.getQueryString("/objectGroups", HU to user, HA to it)
            assertEquals(HttpStatus.OK, resp.statusCode, resp.body)
        }
        // only read/write apikey has write access
        val body ="""{"name": "apikeys-${Random.nextInt()}", "description": "xx", "owner": $user}"""
        secrets.take(secrets.size - 1).map {
            val resp = restTemplate.putWithBody("/objectGroups", body, HU to user, HA to it)
            assertEquals(HttpStatus.FORBIDDEN, resp.statusCode, resp.body)
        }
        val resp = restTemplate.putWithBody("/objectGroups", body, HU to user, HA to secrets.last())
        assertEquals(HttpStatus.OK, resp.statusCode, resp.body)
    }

    @Test
    fun `2-1 test get apikeys`() {
        val (status, json) = restTemplate.getQueryJson("/apikeys", *headers)
        assertEquals(HttpStatus.OK, status)
        assertEquals(1, json.read<List<String>>("$[?(@.secret=='${secrets.random()}')]").size)
    }

    @Test
    fun `3-1 test edit apikeys`() {
        val id = ids[0]

        // edit all fields
        var json = editApikey(id, """{"description": "hello", "readOnly": true, "expirationDate": "null"}""")
        assertEquals(true, json.read<Boolean>("$.readOnly"))
        assertNull(json.read<String>("$.expirationDate"))
        assertEquals("hello", json.read<String>("$.description"))

        // edit description only
        json = editApikey(id, """{"description": "newDescr"}""")
        assertEquals("newDescr", json.read<String>("$.description"))

        // edit readOnly only
        json = editApikey(id, """{"readOnly": false}""")
        assertEquals(false, json.read<Boolean>("$.readOnly"))

        // edit expirationDate only, using a date
        json = editApikey(id, """{"expirationDate": "2070-01-04Z"}""")
        assertEquals("2070-01-04T00:00:00.000Z", json.read<String>("$.expirationDate"))

        // edit expirationDate only, using a different date format
        json = editApikey(id, """{"expirationDate": "2070-01-04T10:00"}""")
        assertEquals("2070-01-04T10:00:00.000Z", json.read<String>("$.expirationDate"))

        // edit expirationDate only, using a duration TODO: test the result
        json = editApikey(id, """{"expirationDate": "1d-2h"}""")
        print(json.read<String>("$.expirationDate"))
    }

    @Test
    fun `4-1 test delete apikeys`() {
        ids.zip(secrets).map {
            val resp = restTemplate.deleteQueryString("/apikeys/${it.first}", *headers)
            assertEquals(HttpStatus.OK, resp.statusCode)
        }
    }

    @Test
    fun `4-2 test get apikeys after delete`() {
        val pair = restTemplate.getQueryJson("/apikeys", *headers)
        assertEquals(HttpStatus.OK, pair.first)
        assertEquals(0, pair.second.read<List<String>>("$[?(@.secret=='${secrets.random()}')]").size)
    }

    // -------

    fun createApikey(writable: Boolean = false, expirationDate: String? = null, description: String? = null): DocumentContext? {
        // create url
        val pathParams = mutableListOf<String>()
        if (writable) pathParams.add("writable=true")
        if (expirationDate != null) pathParams.add("expirationDate=$expirationDate")
        val url = "/apikeys" + (if (pathParams.size > 0) "?" + pathParams.joinToString("&") else "")

        // make query
        println(url)
        val response =
                if (description != null)
                    restTemplate.putWithBody(url, """{"description": "$description"}""", *headers)
                else
                    restTemplate.putQueryString(url, *headers)

        assertEquals(HttpStatus.OK, response.statusCode)
        val json = JsonPath.parse(response.body)

        // store variables
        ids.add(json.read<Int>("$.id"))
        secrets.add(json.read<String>("$.secret"))

        // check some json variables
        assertEquals(!writable, json.read("$.readOnly"))
        assertEquals(description, json.read<String>("$.description"))
        assertEquals(expirationDate == null, json.read<String>("$.expirationDate") == null)
        assertEquals(32, secrets.last().length)

        return json
    }

    fun editApikey(id: Int, body: String): DocumentContext {
        val response = restTemplate.postWithBody("/apikeys/$id", body, *headers)
        assertEquals(HttpStatus.OK, response.statusCode)
        return JsonPath.parse(response.body)
    }
}