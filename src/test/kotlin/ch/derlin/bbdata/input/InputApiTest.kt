package ch.derlin.bbdata.input

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.dates.JodaUtils
import ch.derlin.bbdata.getQueryJson
import ch.derlin.bbdata.postQueryString
import ch.derlin.bbdata.postWithBody
import com.jayway.jsonpath.JsonPath
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.random.Random

/**
 * date: 05.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
open class InputApiTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate


    companion object {
        val OBJ = 1
        val TOKEN_P = "012345678901234567890123456789a" // last digit is objectId
        val RANDOM_VALUE = "${Random.nextInt(10000)}.0" // float
        val NOW = nowTs() // don't use it twice on the same objectId, it will override Cassandra record (but not Kafka)
        val URL = "/objects/values"

        var writeCounter: Int = 0

        fun nowTs() = JodaUtils.getFormatter(JodaUtils.FMT_ISO_MILLIS).print(DateTime.now(DateTimeZone.UTC))

        fun getMeasureBody(
                objectId: Int = OBJ, token: String? = null,
                value: Any = RANDOM_VALUE, ts: String? = null): String = """{
            |"objectId": $objectId,
            |"token": "${token ?: TOKEN_P + objectId.toString()}",
            |"value": "$value",
            |"timestamp": "${ts ?: nowTs()}"
            |}""".trimMargin()
    }

    @Test
    fun `1-0 test submit measure fail`() {
        writeCounter = getWriteCounter()

        // test bad timestamps
        var resp = restTemplate.postWithBody(URL, getMeasureBody(ts = "10-01-12T01:00"))
        assertNotEquals(HttpStatus.OK, resp.statusCode, "year 10BC")
        resp = restTemplate.postWithBody(URL, getMeasureBody(ts = ""))
        assertNotEquals(HttpStatus.OK, resp.statusCode, "empty ts")
        resp = restTemplate.postWithBody(URL, getMeasureBody(ts = "2080-02-06T12:00"))
        assertNotEquals(HttpStatus.OK, resp.statusCode, "ts in the future")
        // test empty value
        resp = restTemplate.postWithBody(URL, getMeasureBody(value = ""))
        assertNotEquals(HttpStatus.OK, resp.statusCode, "empty value")
        // test wrong login
        resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 2, token = "${TOKEN_P}1"))
        assertNotEquals(HttpStatus.OK, resp.statusCode, "wrong objectId")
        resp = restTemplate.postWithBody(URL, getMeasureBody(token = "00000000000000000000000000000000"))
        assertNotEquals(HttpStatus.OK, resp.statusCode, "wrong token")

        val wc = getWriteCounter()
        assertEquals(writeCounter, wc, "Counter incremented on failed measures.")
    }


    @Test
    fun `1-1 test submit measure ok`() {
        writeCounter = getWriteCounter()
        val resp = restTemplate.postWithBody(URL, getMeasureBody(ts = NOW))
        assertEquals(HttpStatus.OK, resp.statusCode)

        val json = JsonPath.parse(resp.body)
        assertFalse(resp.body!!.contains("token"))
        assertEquals(RANDOM_VALUE, json.read<String>("$.value"))
        assertTrue(json.read<String>("$.timestamp").startsWith(NOW.dropLast(1)))
        assertEquals("V", json.read<String>("$.unitSymbol"))
        assertEquals("volt", json.read<String>("$.unitName"))
        assertEquals(1, json.read<Int>("$.owner"))
    }

    @Test
    fun `1-2 test get measure`() {
        val (status, json) = restTemplate.getQueryJson("/objects/$OBJ/values/latest", "accept" to "application/json")
        assertEquals(HttpStatus.OK, status)

        val latestValue = json.read<Map<String, Any>>("$.[0]")
        assertEquals(OBJ, latestValue.get("objectId"))
        assertTrue((latestValue.get("timestamp") as String).startsWith(NOW.dropLast(1)))
        assertEquals(RANDOM_VALUE, latestValue.get("value"))
        assertNull(latestValue.get("comment"))

    }

    @Test
    fun `1-3 test write counter incremented`() {
        val wc = getWriteCounter()
        assertEquals(writeCounter + 1, wc)
    }

    @Test
    fun `1-4 test duplicate`() {
        // same timestamp than previous test. Should fail
        val resp = restTemplate.postWithBody(URL, getMeasureBody(ts = NOW))
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    private fun getWriteCounter(): Int {
        val (status, json) = restTemplate.getQueryJson("/objects/$OBJ/stats/counters")
        return when (status) {
            HttpStatus.OK -> json.read<Int>("$.nValues")
            HttpStatus.NOT_FOUND -> 0
            else -> {
                assertEquals(HttpStatus.OK, status, "Counter: returned a strange status code $status: $json")
                0
            }
        }
    }

    @Test
    fun `2-1 test types`() {
        var resp: ResponseEntity<String>

        // test bad floats
        for (v in listOf("true", "1.2.3")) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 1, value = v))
            assertNotEquals(HttpStatus.OK, resp.statusCode, "bad float: $v. ${resp.body}")
        }
        // test good float
        resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 1, value = "1"))
        assertEquals(HttpStatus.OK, resp.statusCode, "good float: 1. ${resp.body}")


        // test bad ints
        for (v in listOf("1.5", "1.", 1.4)) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 3, value = v))
            assertNotEquals(HttpStatus.OK, resp.statusCode, "bad int: $v. ${resp.body}")
        }
        // test good int
        resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 3, value = "1"))
        assertEquals(HttpStatus.OK, resp.statusCode, "good int: 1. ${resp.body}")


        // test bad booleans
        for (v in listOf("1.", "2", "oui", "xxx", 1.45)) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 4, value = v))
            assertNotEquals(HttpStatus.OK, resp.statusCode, "bad bool: $v. ${resp.body}")
        }
        // test good booleans: true
        for (v in listOf("TRUE", "true", "1", "on", true)) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 4, value = v))
            assertEquals(HttpStatus.OK, resp.statusCode, "good bool true: $v. ${resp.body}")
            assertEquals("true", JsonPath.parse(resp.body).read<String>("$.value"))
        }
        // test good booleans: false
        for (v in listOf("False", "off", "OFF", "0", false)) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 4, value = v))
            assertEquals(HttpStatus.OK, resp.statusCode, "good bool false: $v. ${resp.body}")
            assertEquals("false", JsonPath.parse(resp.body).read<String>("$.value"))
        }
    }
}