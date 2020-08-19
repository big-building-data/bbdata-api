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
import org.junit.jupiter.api.Assertions.*


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = arrayOf(UNSECURED_REGULAR))
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectTokens {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val objectId: Int = 1
        var ids: MutableList<Int> = mutableListOf()
        var tpl: TestRestTemplate? = null

        @AfterAll
        @JvmStatic
        fun cleanup() {
            tpl?.let { tpl ->
                ids.map {
                    try {
                        tpl.deleteQueryString("/objects/$objectId/tokens/$it")
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    @Test
    fun `1-1 test create token`() {
        // == create
        val putResponse = restTemplate.putQueryString("/objects/$objectId/tokens")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == store variables
        val id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        ids.add(id)
        tpl = restTemplate

        // == get
        val getResponse = restTemplate.getQueryString("/objects/$objectId/tokens/$id")
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertEquals(objectId, json.read<Int>("$.objectId"))
        assertNull(json.read<String>("$.description"))
        assertEquals(32, json.read<String>("$.token").length)
    }

    @Test
    fun `1-2 test create token with description`() {
        // == create
        val descr = "hello token"
        val putResponse = restTemplate.putWithBody("/objects/$objectId/tokens",
                """{"description": "$descr"}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == store variables
        val id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        ids.add(id)
        tpl = restTemplate

        // == get
        val getResponse = restTemplate.getForEntity("/objects/$objectId/tokens/$id", String::class.java)
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertEquals(objectId, json.read<Int>("$.objectId"))
        assertEquals(descr, json.read<Int>("$.description"))
    }

    @Test
    fun `1-3 test edit token`() {
        val newDescr = "hello token new descr"
        val id = ids.last()

        val putResponse = restTemplate.postWithBody(
                "/objects/$objectId/tokens/$id",
                """{"description": "$newDescr"}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == get
        val getResponse = restTemplate.getQueryString("/objects/$objectId/tokens/$id")
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertEquals(objectId, json.read<Int>("$.objectId"))
        assertEquals(newDescr, json.read<Int>("$.description"))
    }

    @Test
    fun `1-4 test get tokens`() {
        val json = restTemplate.getQueryJson("/objects/$objectId/tokens").second
        ids.map { id -> assertNotEquals(0, json.read<List<Any>>("$[?(@.id == $id)]").size) }
    }

    @Test
    fun `1-5 test delete tokens`() {
        ids.map { id ->
            val deleteResponse1 = restTemplate.deleteQueryString("/objects/$objectId/tokens/$id")
            assertEquals(HttpStatus.OK, deleteResponse1.statusCode)

            val deleteResponse2 = restTemplate.deleteQueryString("/objects/$objectId/tokens/$id")
            assertEquals(HttpStatus.NOT_MODIFIED, deleteResponse2.statusCode)
        }

        val json = restTemplate.getQueryJson("/objects/$objectId/tokens").second
        ids.map { id -> assertEquals(0, json.read<List<Any>>("$[?(@.id == $id)]").size) }

    }

}