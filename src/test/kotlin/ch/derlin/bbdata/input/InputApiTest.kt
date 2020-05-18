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
class InputApiTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate


    companion object {
        val OBJ = 1
        val TOKEN = "012345678901234567890123456789ab"
        val RANDOM_VALUE = Random.nextInt(10000).toString()
        val NOW = JodaUtils.getFormatter(JodaUtils.FMT_ISO_SECONDS).print(DateTime.now(DateTimeZone.UTC))
        val URL = "/objects/values"

        var writeCounter: Int = 0

        fun getMeasureBody(
                objectId: Int = OBJ, token: String = TOKEN,
                value: String = RANDOM_VALUE, ts: String = NOW): String = """{
            |"objectId": $objectId,
            |"token": "$token",
            |"value": "$value",
            |"timestamp": "$ts"
            |}""".trimMargin()
    }

    @Test
    fun `1-0 test submit measure fail`() {
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
        resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 2))
        assertNotEquals(HttpStatus.OK, resp.statusCode, "wrong objectId")
        resp = restTemplate.postWithBody(URL, getMeasureBody(token = "00000000000000000000000000000000"))
        assertNotEquals(HttpStatus.OK, resp.statusCode, "wrong token")
    }


    @Test
    fun `1-1 test submit measure ok`() {
        writeCounter = getWriteCounter()
        val resp = restTemplate.postWithBody(URL, getMeasureBody())
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

        val latestValue = json.read<Map<String,Any>>("$.[0]")
        assertEquals(OBJ, latestValue.get("objectId"))
        assertTrue((latestValue.get("timestamp") as String).startsWith(NOW.dropLast(1)))
        assertEquals(RANDOM_VALUE, latestValue.get("value"))
        assertNull(latestValue.get("comment"))

    }

    @Test
    fun `1-3 test write counter incremented`() {
        val wc = getWriteCounter()
        assertEquals(writeCounter+1, wc)
    }

    private fun getWriteCounter(): Int {
        val (status, json) = restTemplate.getQueryJson("/objects/$OBJ/stats/counters")
        assertEquals(HttpStatus.OK, status)
        return json.read<Int>("$.nValues")
    }
}