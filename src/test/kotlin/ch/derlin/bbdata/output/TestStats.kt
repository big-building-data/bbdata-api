package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import ch.derlin.bbdata.input.InputApiTest
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
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
import java.lang.Double.isNaN
import kotlin.random.Random

/**
 * date: 28.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR, NO_KAFKA])
@ActiveProfiles(Profiles.UNSECURED)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestStats {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        var OID: Int = -1
        var token: String = ""
        var nWrites = 0
        var nReads = 0

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            // reset counters, as this class is used in subclasses
            nWrites = 0
            nReads = 0
        }
    }

    @Test
    fun `0-1 create object`() {
        var resp = restTemplate.putWithBody("/objects",
                """{"name": "Stats-${Random.nextInt()}", "owner": $REGULAR_USER_ID, "unitSymbol": "V"}""")
        assertEquals(HttpStatus.OK, resp.statusCode)
        OID = JsonPath.parse(resp.body).read<Int>("$.id")

        resp = restTemplate.putQueryString("/objects/$OID/tokens")
        assertEquals(HttpStatus.OK, resp.statusCode)
        token = JsonPath.parse(resp.body).read<String>("$.token")
    }

    @Test
    fun `1-1 test no values`() {
        // get current stats
        val (status, json) = restTemplate.getQueryJson("/objects/$OID/stats")
        assertEquals(HttpStatus.OK, status)

        listOf(
                "objectId" to OID,
                "nReads" to 0,
                "nWrites" to 0,
                "avgSamplePeriod" to .0,
                "lastTs" to null
        ).forEach { (key, expected) ->
            assertEquals(expected, json.read("$.$key"))
        }
    }

    @Test
    fun `1-2 test one value`() {
        // post a new measure
        val json = submitAndCheck("12.2")
        assertEquals(.0, json.readPeriod())
    }

    @Test
    fun `1-3 test another value`() {
        // post a new measure
        val json = submitAndCheck("6.")
        assertTrue(json.readPeriod() > .0)
    }

    @Test
    fun `1-4 test nReads`() {
        val response = restTemplate.getQueryString("/objects/$OID/values?from=${InputApiTest.tsFmt()}")
        assertEquals(HttpStatus.OK, response.statusCode)
        nReads = +1
        submitAndCheck("6.")

    }

    @Test
    fun `2-1 test wrong object`() {
        // get object with no write
        val (status, _) = restTemplate.getQueryJson("/objects/12352/stats")
        assertNotEquals(HttpStatus.OK, status)
    }

    // ----------

    private fun submitAndCheck(value: String): DocumentContext {
        // post a new measure
        val ts = InputApiTest.tsFmt()
        val resp = restTemplate.postWithBody(InputApiTest.URL,
                InputApiTest.getMeasureBody(objectId = OID, token = token, value = value, ts = ts))
        assertEquals(HttpStatus.OK, resp.statusCode)
        nWrites += 1
        // get updated stats
        val (status, json) = restTemplate.getQueryJson("/objects/$OID/stats")
        assertEquals(HttpStatus.OK, status)
        listOf(
                "objectId" to OID,
                "nReads" to nReads,
                "nWrites" to nWrites,
                "lastTs" to ts
        ).forEach { (key, expected) ->
            assertEquals(expected, json.read("$.$key"), "for property $key")
        }
        assertFalse(isNaN(json.readPeriod()))
        return json
    }

    private fun DocumentContext.readPeriod(): Double = this.read<Double>("$.avgSamplePeriod")
}