package ch.derlin.bbdata.input

import ch.derlin.bbdata.*
import ch.derlin.bbdata.common.dates.JodaUtils
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR, NO_KAFKA])
@ActiveProfiles(Profiles.UNSECURED)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class InputApiTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    // asynchronous stats update, hence wait a bit (ms)
    val statsWait: Long = 100

    companion object {
        val OBJ = 1
        val RANDOM_VALUE = "${Random.nextInt(10000)}.0" // float
        lateinit var NOW: String // don't use it twice on the same objectId, it will override Cassandra record (but not Kafka)
        val URL = "/objects/values"
        var writeCounter: Int = 0

        fun now() = DateTime.now(DateTimeZone.UTC)
        fun tsFmt(d: DateTime? = null) = JodaUtils.getFormatter(JodaUtils.FMT_ISO_MILLIS).print(d ?: now())

        fun getMeasureBody(
                objectId: Int = OBJ, token: String? = TOKEN(objectId),
                value: Any = RANDOM_VALUE, ts: String? = null, partial: Boolean = false): String {
            val body = """{
            |"objectId": $objectId,
            |"token": ${if (token == null) "null" else "\"$token\""},
            |"timestamp": ${if (ts == null) "null" else "\"$ts\""},
            |"value": "$value"
            |}""".trimMargin()
            return if (partial) body else "[$body]"
        }

        fun getMeasureBodyNoTs(
                objectId: Int = OBJ, token: String? = null,
                value: Any = RANDOM_VALUE, partial: Boolean = false): String {
            val body = """{
            |"objectId": $objectId,
            |"token": "${token ?: TOKEN(objectId)}",
            |"value": "$value"
            |}""".trimMargin()
            return if (partial) body else "[$body]"
        }

        val failedStatuses = listOf(HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND)
    }

    @Test
    fun `1-0 test submit measure fail`() {
        writeCounter = getWriteCounter()

        // test bad timestamps
        var resp = restTemplate.postWithBody(URL, getMeasureBody(ts = "10-01-12T01:00"))
        assertTrue(failedStatuses.contains(resp.statusCode),  "year 10AD: ${resp.statusCode}, ${resp.body}")
        resp = restTemplate.postWithBody(URL, getMeasureBody(ts = ""))
        assertTrue(failedStatuses.contains(resp.statusCode),  "empty ts string: ${resp.statusCode}, ${resp.body}")
        resp = restTemplate.postWithBody(URL, getMeasureBody(ts = "null"))
        assertTrue(failedStatuses.contains(resp.statusCode), "null ts string: ${resp.statusCode}, ${resp.body}")
        resp = restTemplate.postWithBody(URL, getMeasureBody(ts = "2080-02-06T12:00"))
        assertTrue(failedStatuses.contains(resp.statusCode),"ts in the future: ${resp.statusCode}, ${resp.body}")
        // test empty value
        resp = restTemplate.postWithBody(URL, getMeasureBody(value = ""))
        assertTrue(failedStatuses.contains(resp.statusCode),"empty value: ${resp.statusCode}, ${resp.body}")
        // test wrong login
        resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 231451345, token = TOKEN(1)))
        assertTrue(failedStatuses.contains(resp.statusCode), "wrong objectId: ${resp.statusCode}, ${resp.body}")
        resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 2, token = TOKEN(1)))
        assertTrue(failedStatuses.contains(resp.statusCode),"wrong credentials: ${resp.statusCode}, ${resp.body}")
        resp = restTemplate.postWithBody(URL, getMeasureBody(token = "00000000000000000000000000000000"))
        assertTrue(failedStatuses.contains(resp.statusCode),"wrong token: ${resp.statusCode}, ${resp.body}")
        resp = restTemplate.postWithBody(URL, getMeasureBody(token = null))
        assertTrue(failedStatuses.contains(resp.statusCode),"null token: ${resp.statusCode}, ${resp.body}")
        resp = restTemplate.postWithBody(URL, getMeasureBody(token = ""))
        assertTrue(failedStatuses.contains(resp.statusCode),"empty token")

        val wc = getWriteCounter()
        assertEquals(writeCounter, wc, "Counter incremented on failed measures.: ${resp.statusCode}, ${resp.body}")
    }


    @Test
    fun `1-1 test submit one measure ok`() {
        listOf(
                // timestamp if "null" => generated by server
                { _: String -> getMeasureBody(ts = null) to false },
                // timestamp is not present => generated by server
                { _: String -> getMeasureBodyNoTs() to false },
                // timestamp provided
                { now: String -> getMeasureBody(ts = now) to true }
        ).forEach { fn ->
            writeCounter = getWriteCounter()
            NOW = tsFmt()

            val (measureBody, exactTsMatch) = fn(NOW)
            val resp = restTemplate.postWithBody(URL, measureBody)
            assertEquals(HttpStatus.OK, resp.statusCode, resp.body)

            val json = JsonPath.parse(resp.body)
            assertFalse(resp.body!!.contains("token"))
            assertEquals(RANDOM_VALUE, json.read<String>("$[0].value"))
            assertEquals("V", json.read<String>("$[0].unitSymbol"))
            assertEquals("volt", json.read<String>("$[0].unitName"))
            assertEquals(REGULAR_USER_ID, json.read<Int>("$[0].owner"))

            // compare timestamps. If server-side generated, allow for a latency of 150 ms
            val ts = json.read<String>("$[0].timestamp")
            if (exactTsMatch) {
                assertEquals(ts, NOW)
            } else {
                val now = JodaUtils.parse(NOW)
                assertTrue(JodaUtils.parse(ts).millis - now.millis < 150, "$ts - $now > 150")

            }

            // ensure write counter was incremented
            assertEquals(writeCounter + 1, getWriteCounter(), "write counter was not incremented")

        }
    }

    @Test
    fun `1-2 test get latest measure`() {
        val (status, json) = restTemplate.getQueryJson("/objects/$OBJ/values/latest")
        assertEquals(HttpStatus.OK, status)

        val latestValue = json.read<Map<String, Any>>("$.[0]")
        val ts = latestValue.get("timestamp") as String

        assertEquals(OBJ, latestValue.get("objectId"))
        assertEquals(NOW, ts, "$NOW <> $ts")
        assertEquals(RANDOM_VALUE, latestValue.get("value"))
        assertNull(latestValue.get("comment"))
    }

    @Test
    fun `1-3 test duplicate`() {
        // same timestamp than previous test. Should fail
        val resp = restTemplate.postWithBody(URL, getMeasureBody(ts = NOW))
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }


    @Test
    fun `2-1 test types`() {
        var resp: ResponseEntity<String>

        // test bad floats
        for (v in listOf("true", "1.2.3")) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 1, value = v))
            assertTrue(failedStatuses.contains(resp.statusCode),"bad float: $v. ${resp.statusCode}, ${resp.body}")
        }
        // test good float
        resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 1, value = "1"))
        assertEquals(HttpStatus.OK, resp.statusCode, "good float: 1. ${resp.body}")


        // test bad ints
        for (v in listOf("1.5", "1.", 1.4)) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 3, value = v))
            assertTrue(failedStatuses.contains(resp.statusCode),"bad int: $v. ${resp.statusCode} ${resp.body}")
        }
        // test good int
        resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 3, value = "1"))
        assertEquals(HttpStatus.OK, resp.statusCode, "good int: 1. ${resp.body}")


        // test bad booleans
        for (v in listOf("1.", "2", "oui", "xxx", 1.45)) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 4, value = v))
            assertTrue(failedStatuses.contains(resp.statusCode),"bad bool: $v. ${resp.body}")
        }
        // test good booleans: true
        for (v in listOf("TRUE", "true", "1", "on", true)) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 4, value = v))
            assertEquals(HttpStatus.OK, resp.statusCode, "good bool true: $v. ${resp.body}")
            assertEquals("true", JsonPath.parse(resp.body).read<String>("$[0].value"))
        }
        // test good booleans: false
        for (v in listOf("False", "off", "OFF", "0", false)) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 4, value = v))
            assertEquals(HttpStatus.OK, resp.statusCode, "good bool false: $v. ${resp.body}")
            assertEquals("false", JsonPath.parse(resp.body).read<String>("$[0].value"))
        }

        // test good string
        for (v in listOf("False", "12.0", """{\"json\": \"data\", \"works\": 1}""", "  ", true)) {
            resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 5, value = v))
            assertEquals(HttpStatus.OK, resp.statusCode, "good string: $v. ${resp.body}")
            assertEquals(v.toString().replace("\\\"", "\""), JsonPath.parse(resp.body).read<String>("$[0].value"))
        }
    }

    @Test
    fun `3-1 test submit multiple measures fail`() {
        // wrong body (ensure validation still works on lists)
        var body = """[{"objectId": -1, "token": "1"}]"""
        var resp = restTemplate.postWithBody(URL, body)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode, "invalid body returned ${resp.body}")
        assertTrue(resp.body!!.contains("positive"), "body should contain objectId error ${resp.body}")
        assertTrue(resp.body!!.contains("32"), "body should contain token length error ${resp.body}")

        // duplicate objectId
        body = "[" + (1..2).map { getMeasureBodyNoTs(partial = true) }.toList().joinToString(",") + "]"
        resp = restTemplate.postWithBody(URL, body)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode, "duplicate objectId/timestamp (no ts) returned ${resp.body}")
        assertTrue(resp.body!!.contains("same timestamp"), "body should contain timestamp related error ${resp.body}")

        // one wrong token
        val now = tsFmt(now())
        body = "[" + listOf(
                getMeasureBody(ts = now, partial = true),
                getMeasureBody(ts = now, token = TOKEN(OBJ + 1), partial = true)
        ).joinToString(",") + "]"
        resp = restTemplate.postWithBody(URL, body)
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode, "wrong token returned ${resp.body}")
        assertTrue(resp.body!!.contains("token"), "body should contain token related error ${resp.body}")

        // assert the correct measures are not saved
        val (status, json) = restTemplate.getQueryJson("/objects/$OBJ/values?from=$now&to=$now")
        assertEquals(HttpStatus.OK, status, "get values $OBJ $now returned ${json.jsonString()}")
        assertEquals(0, json.read<List<Any>>("$[*]").size, "Failed bulk insert saved a value ! ${json.jsonString()}")

    }

    @Test
    fun `3-2 test submit multiple measures ok`() {

        val now = tsFmt(now())
        val oids = listOf(1, 2)
        val oid = oids.random()
        val writeCounter = getWriteCounter(oid)

        val body = "[" + oids.map { getMeasureBody(objectId = it, ts = now, partial = true) }.joinToString(",") + "]"
        val resp = restTemplate.postWithBody(URL, body)
        assertEquals(HttpStatus.OK, resp.statusCode, "submit multiple measures returned ${resp.body}")

        // assert the measures are saved
        val (status, json) = restTemplate.getQueryJson("/objects/$oid/values?from=$now&to=$now")
        assertEquals(HttpStatus.OK, status, "get values $oid $now returned ${json.jsonString()}")
        assertEquals(1, json.read<List<Any>>("$[*]").size, "bulk insert missing value ${json.jsonString()}")

        // object 1 should have incremented by one
        assertEquals(writeCounter + 1, getWriteCounter(oid), "write counter was not incremented for $oid")
    }

    @Test
    fun `3-3 test submit multiple measures with same id ok`() {

        val now = tsFmt(now()).dropLast(2)
        val writeCounter = getWriteCounter()
        val n = 4

        val body = "[" + (1..n).map { getMeasureBody(objectId = OBJ, ts = "$now$it", partial = true) }.joinToString(",") + "]"
        val resp = restTemplate.postWithBody(URL, body)
        assertEquals(HttpStatus.OK, resp.statusCode, "submit multiple measures returned ${resp.body}")

        // assert the measures are saved
        val (status, json) = restTemplate.getQueryJson("/objects/$OBJ/values?from=${now}1&to=$now$n")
        assertEquals(HttpStatus.OK, status, "get values $OBJ $now returned ${json.jsonString()}")
        assertEquals(n, json.read<List<Any>>("$[*]").size, "bulk insert missing value ${json.jsonString()}")

        // object should have incremented by n
        assertEquals(writeCounter + n, getWriteCounter(OBJ), "write counter was not incremented for $OBJ")
    }

    private fun getWriteCounter(oid: Int = OBJ): Int {
        if(statsWait > 0) Thread.sleep(statsWait) // the stats are updated asynchronously... wait a bit
        val (status, json) = restTemplate.getQueryJson("/objects/$oid/stats")
        assertEquals(HttpStatus.OK, status, "Counter: returned a strange status code $status: $json")
        return json.read<Int>("$.nWrites")
    }
}