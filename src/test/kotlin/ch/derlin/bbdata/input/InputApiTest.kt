package ch.derlin.bbdata.input

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.dates.JodaUtils
import ch.derlin.bbdata.getQueryJson
import ch.derlin.bbdata.postWithBody
import com.jayway.jsonpath.JsonPath
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Assertions
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
        val NOW = JodaUtils.getFormatter(JodaUtils.Format.ISO_SECONDS).print(DateTime.now(DateTimeZone.UTC))
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
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode, "year 10BC")
        resp = restTemplate.postWithBody(URL, getMeasureBody(ts = ""))
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode, "empty ts")
        resp = restTemplate.postWithBody(URL, getMeasureBody(ts = "2080-02-06T12:00"))
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode, "ts in the future")
        // test empty value
        resp = restTemplate.postWithBody(URL, getMeasureBody(value = ""))
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode, "empty value")
        // test wrong login
        resp = restTemplate.postWithBody(URL, getMeasureBody(objectId = 2))
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode, "wrong objectId")
        resp = restTemplate.postWithBody(URL, getMeasureBody(token = "00000000000000000000000000000000"))
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode, "wrong token")
    }


    @Test
    fun `1-1 test submit measure ok`() {
        writeCounter = getWriteCounter()
        val resp = restTemplate.postWithBody(URL, getMeasureBody())
        Assertions.assertEquals(HttpStatus.OK, resp.statusCode)

        val json = JsonPath.parse(resp.body)
        Assertions.assertFalse(resp.body!!.contains("token"))
        Assertions.assertEquals(RANDOM_VALUE, json.read<String>("$.value"))
        Assertions.assertEquals(NOW, json.read<String>("$.timestamp"))
        Assertions.assertEquals("V", json.read<String>("$.unitSymbol"))
        Assertions.assertEquals("volt", json.read<String>("$.unitName"))
        Assertions.assertEquals(1, json.read<Int>("$.owner"))
    }

    @Test
    fun `1-2 test get measure`() {
        val (status, json) = restTemplate.getQueryJson("/objects/$OBJ/values/latest", "accept" to "application/json")
        Assertions.assertEquals(HttpStatus.OK, status)

        val latestValue = json.read<Map<String,Any>>("$.[0].values.[0]")
        Assertions.assertEquals(NOW, latestValue.get("timestamp"))
        Assertions.assertEquals(RANDOM_VALUE, latestValue.get("value"))
        Assertions.assertNull(latestValue.get("comment"))

    }

    @Test
    fun `1-3 test write counter incremented`() {
        val wc = getWriteCounter()
        Assertions.assertEquals(writeCounter+1, wc)
    }

    private fun getWriteCounter(): Int {
        val (status, json) = restTemplate.getQueryJson("/objects/$OBJ/stats/counters")
        Assertions.assertEquals(HttpStatus.OK, status)
        return json.read<Int>("$.nValues")
    }
}