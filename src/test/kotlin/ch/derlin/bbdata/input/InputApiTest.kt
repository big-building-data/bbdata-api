package ch.derlin.bbdata.input

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.common.dates.JodaUtils
import ch.derlin.bbdata.postWithBody
import com.jayway.jsonpath.JsonPath
import org.joda.time.DateTime
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
@ActiveProfiles(Profiles.INPUT_ONLY)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class InputApiTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val OBJ = 1
        val TOKEN = "012345678901234567890123456789ab"

        fun getMeasureBody(
                objectId: Int = OBJ, token: String = TOKEN,
                value: String = "3444", ts: String? = null): String = """{
            |"objectId": $objectId,
            |"token": "$token",
            |"value": "$value",
            |"timestamp": "${ts ?: JodaUtils.format(DateTime.now())}"
            |}""".trimMargin()

    }

    @Test
    fun `1-2 test submit measure fail`() {
        // test bad timestamps
        var resp = restTemplate.postWithBody("/measures", getMeasureBody(ts = "10-01-12T01:00"))
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode)
        resp = restTemplate.postWithBody("/measures", getMeasureBody(ts = ""))
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode)
        // test empty value
        resp = restTemplate.postWithBody("/measures", getMeasureBody(value = ""))
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode)
        // test wrong login
        resp = restTemplate.postWithBody("/measures", getMeasureBody(objectId = 2))
        Assertions.assertNotEquals(HttpStatus.OK, resp.statusCode)
    }

    @Test
    fun `1-2 test submit measure ok`() {
        val value = Random.nextInt(10000).toString()
        val ts = JodaUtils.format(DateTime.now())

        val resp = restTemplate.postWithBody("/measures", getMeasureBody(value = value, ts = ts))
        Assertions.assertEquals(HttpStatus.OK, resp.statusCode)

        val json = JsonPath.parse(resp.body)
        Assertions.assertFalse(resp.body!!.contains("token"))
        Assertions.assertEquals(value, json.read<String>("$.value"))
        Assertions.assertEquals(ts, json.read<String>("$.timestamp"))
        Assertions.assertEquals("V", json.read<String>("$.unitSymbol"))
        Assertions.assertEquals("volt", json.read<String>("$.unitName"))
        Assertions.assertEquals(1, json.read<Int>("$.owner"))
    }
}