package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import ch.derlin.bbdata.common.dates.JodaUtils
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectComments {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val objectId: Int = 1

        val from = "2020-02-01T23:30"
        val to = "2020-02-03T08:30"

        var id: Int? = null
        var tpl: TestRestTemplate? = null

        @AfterAll
        @JvmStatic
        fun cleanup() {
            tpl?.let { tpl ->
                id?.let {
                    try {
                        tpl.deleteQueryString("/objects/$objectId/comments/$it")
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    @Test
    fun `1-1 test create comment fail`() {

        val putResponseNoComment = restTemplate.putWithBody("/objects/$objectId/comments",
                """{"from": "$from", "to": "$to"}""")
        assertNotEquals(HttpStatus.OK, putResponseNoComment.statusCode)

        val putResponseEmptyComment = restTemplate.putWithBody("/objects/$objectId/comments",
                """{"from": "$from", "to": "$to", "comment": ""}""")
        assertNotEquals(HttpStatus.OK, putResponseEmptyComment.statusCode)

        val putResponseNoFrom = restTemplate.putWithBody("/objects/$objectId/comments",
                """{"to": "$to", "comment": "hello"}""")
        assertNotEquals(HttpStatus.OK, putResponseNoFrom.statusCode)

        val putResponseWrongFrom = restTemplate.putWithBody("/objects/$objectId/comments",
                """{"from": "not-a-date", "to": "$to", "comment": "hello"}""")
        assertNotEquals(HttpStatus.OK, putResponseWrongFrom.statusCode)

        val putResponseWrongFrom2 = restTemplate.putWithBody("/objects/$objectId/comments",
                """{"from": "1900-13-32", "to": "$to", "comment": "hello"}""")
        assertNotEquals(HttpStatus.OK, putResponseWrongFrom2.statusCode)

        val putResponseFromGreaterThanTo = restTemplate.putWithBody("/objects/$objectId/comments",
                """{"from": "$to", "to": "$from", "comment": "hello"}""")
        assertNotEquals(HttpStatus.OK, putResponseFromGreaterThanTo.statusCode)

    }

    @Test
    fun `1-2 test create comment`() {
        // == create
        val putResponse = restTemplate.putWithBody("/objects/$objectId/comments",
                """{"from": "$from", "to": "$to", "comment": "comment"}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == store variables
        id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        tpl = restTemplate

        // == get
        val getResponse = restTemplate.getQueryString("/objects/$objectId/comments/$id")
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertEquals(objectId, json.read<Int>("$.objectId"))
        assertTrue(json.read<String>("$.from").isBBDataDatetime())
    }


    @Test
    fun `1-3 test get comments`() {
        val json = restTemplate.getQueryJson("/objects/$objectId/comments").second
        assertEquals(1, json.read<List<Any>>("$[?(@.id == $id)]").size)
    }

    @Test
    fun `1-4 test get comments for date`() {
        val toDate = JodaUtils.parse(to)
        val between = JodaUtils.format(toDate.plusMinutes(-30))
        val notBetween = JodaUtils.format(toDate.plusMinutes(30))

        val jsonBetween = restTemplate.getQueryJson("/objects/$objectId/comments?forDate=$between").second
        assertEquals(1, jsonBetween.read<List<Any>>("$[?(@.id == $id)]").size)

        val jsonLimit = restTemplate.getQueryJson("/objects/$objectId/comments?forDate=$to").second
        assertEquals(1, jsonLimit.read<List<Any>>("$[?(@.id == $id)]").size)

        val jsonNotBetween = restTemplate.getQueryJson("/objects/$objectId/comments?forDate=$notBetween").second
        assertEquals(0, jsonNotBetween.read<List<Any>>("$[?(@.id == $id)]").size)


    }

    @Test
    fun `1-5 test delete comments`() {
        val deleteResponse1 = restTemplate.deleteQueryString("/objects/$objectId/comments/$id")
        assertEquals(HttpStatus.OK, deleteResponse1.statusCode)

        val deleteResponse2 = restTemplate.deleteQueryString("/objects/$objectId/comments/$id")
        assertEquals(HttpStatus.NOT_MODIFIED, deleteResponse2.statusCode)

        val json = restTemplate.getQueryJson("/objects/$objectId/comments").second
        assertEquals(0, json.read<List<Any>>("$[?(@.id == $id)]").size)

    }

}