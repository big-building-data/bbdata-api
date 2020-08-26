package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import ch.derlin.bbdata.common.dates.JodaUtils
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR])
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectComments {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val objectId: Int = 1
        val url = "/objects/$objectId/comments"

        val from = "2020-02-01T23:30"
        val to = "2020-02-03T08:30"

        // id of the last created comment
        var id: Int = -1
    }

    @Test
    fun `1-1 test create comment fail`() {
        var resp = restTemplate.putWithBody(url,
                """{"from": "$from", "to": "$to"}""")
        assertNotEquals(HttpStatus.OK, resp.statusCode, "put $url missing comment returned ${resp.body}")

        resp = restTemplate.putWithBody(url,
                """{"from": "$from", "to": "$to", "comment": ""}""")
        assertNotEquals(HttpStatus.OK, resp.statusCode, "put $url empty comment returned ${resp.body}")

        resp = restTemplate.putWithBody(url,
                """{"to": "$to", "comment": "hello"}""")
        assertNotEquals(HttpStatus.OK, resp.statusCode, "put $url missing from returned ${resp.body}")

        resp = restTemplate.putWithBody(url,
                """{"from": "not-a-date", "to": "$to", "comment": "hello"}""")
        assertNotEquals(HttpStatus.OK, resp.statusCode, "put $url improper from (not a date) returned ${resp.body}")

        resp = restTemplate.putWithBody(url,
                """{"from": "1900-13-32", "to": "$to", "comment": "hello"}""")
        assertNotEquals(HttpStatus.OK, resp.statusCode, "put $url improper from (1990) returned ${resp.body}")

        resp = restTemplate.putWithBody(url,
                """{"from": "$to", "to": "$from", "comment": "hello"}""")
        assertNotEquals(HttpStatus.OK, resp.statusCode, "put $url from > to returned ${resp.body}")

    }

    @Test
    fun `1-2 test create comment`() {
        // == create
        val resp = restTemplate.putWithBody(url,
                """{"from": "$from", "to": "$to", "comment": "comment"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "put $url ok returned ${resp.body}")

        // == store variables
        id = JsonPath.parse(resp.body).read<Int>("$.id")

        // == get
        val getResponse = restTemplate.getQueryString("$url/$id")
        JSONAssert.assertEquals(resp.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertEquals(objectId, json.read<Int>("$.objectId"), "get $url/$id: wrong objectId")
        assertTrue(json.read<String>("$.from").isBBDataDatetime(), "get $url/$id: improper datetime")
    }


    @Test
    fun `1-3 test get comments`() {
        val json = restTemplate.getQueryJson(url).second
        assertEquals(1, json.read<List<Any>>("$[?(@.id == $id)]").size, "get $url: missing comment #$id")
    }

    @Test
    fun `1-4 test get comments for date`() {
        val toDate = JodaUtils.parse(to)
        val between = JodaUtils.format(toDate.plusMinutes(-30))
        val notBetween = JodaUtils.format(toDate.plusMinutes(30))

        var resp = restTemplate.getQueryJson("$url?forDate=$between").second
        assertEquals(1, resp.read<List<Any>>("$[?(@.id == $id)]").size,
                "get $url between: missing comment #$id, ${resp.jsonString()}")

        resp = restTemplate.getQueryJson("$url?forDate=$to").second
        assertEquals(1, resp.read<List<Any>>("$[?(@.id == $id)]").size,
                "get $url forDate: missing comment #$id, ${resp.jsonString()}")

        resp = restTemplate.getQueryJson("$url?forDate=$notBetween").second
        assertEquals(0, resp.read<List<Any>>("$[?(@.id == $id)]").size,
                "get $url forDate: shouldn't have comment #$id, ${resp.jsonString()}")
    }

    @Test
    fun `1-5 test delete comments`() {
        var resp = restTemplate.deleteQueryString("$url/$id")
        assertEquals(HttpStatus.OK, resp.statusCode, "delete $url/$id returned ${resp.body}")

        resp = restTemplate.deleteQueryString("$url/$id")
        assertEquals(HttpStatus.NOT_MODIFIED, resp.statusCode, "delete $url/$id (2) returned ${resp.body}")

        val json = restTemplate.getQueryJson(url).second
        assertEquals(0, json.read<List<Any>>("$[?(@.id == $id)]").size, "get $url/$id after delete returned ${resp.body}")

    }

}