package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import ch.derlin.bbdata.common.cassandra.AggregationGranularity
import ch.derlin.bbdata.common.dates.JodaUtils
import com.jayway.jsonpath.DocumentContext
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

/**
 * date: 06.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = arrayOf(UNSECURED_REGULAR))
@ActiveProfiles(Profiles.UNSECURED)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestValues {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val OID = 1
        val FROM = "2019-01-01T09:00"
        val TO = "2019-01-01T10:00" // inclusive !

        val URL_RAW = "/objects/$OID/values?from=$FROM&to=$TO"
        val URL_AGGR = "/objects/$OID/values/aggregated?from=$FROM&to=$TO"
        val URL_LATEST = "/objects/$OID/values/latest?before=$TO"
    }

    @Test
    fun `1-1 get raw values JSON`() {
        // get between
        val (status, json) = restTemplate.getQueryJson(URL_RAW,
                "accept" to "application/json")
        assertEquals(HttpStatus.OK, status)
        json.readList().checkRaw()
    }

    @Test
    fun `1-2 get raw values CSV`() {
        // get between
        val resp = restTemplate.getQueryString(URL_RAW)
        assertEquals(HttpStatus.OK, resp.statusCode)
        resp.body!!.csv2map().checkRaw()
    }

    @Test
    fun `2-1 get aggregated values JSON`() {
        // test the default: should be hours
        val (statusH, jsonH) = restTemplate.getQueryJson(URL_AGGR, "accept" to "application/json")
        assertEquals(HttpStatus.OK, statusH)
        jsonH.readList().checkAggr(granularity = AggregationGranularity.hours)

        // test the quarters
        val (statusQ, jsonQ) = restTemplate.getQueryJson("$URL_AGGR&granularity=quarters", "accept" to "application/json")
        assertEquals(HttpStatus.OK, statusQ)
        jsonQ.readList().checkAggr(granularity = AggregationGranularity.quarters)
    }

    @Test
    fun `2-2 get aggregated values CSV`() {
        // test the hours
        var resp = restTemplate.getQueryString("$URL_AGGR&granularity=hours")
        assertEquals(HttpStatus.OK, resp.statusCode)
        resp.body!!.csv2map().checkAggr(granularity = AggregationGranularity.hours)
        // test the quarters
        resp = restTemplate.getQueryString("$URL_AGGR&granularity=hours")
        assertEquals(HttpStatus.OK, resp.statusCode)
        resp.body!!.csv2map().checkAggr(granularity = AggregationGranularity.hours)
    }

    @Test
    fun `2-3 test granularity is case insensitive`() {
        var firstResp: String? = null
        for (g in listOf("hours", "HOURS", "hOuRs")) {
            val resp = restTemplate.getQueryString("$URL_AGGR&granularity=$g")
            assertEquals(HttpStatus.OK, resp.statusCode)
            if (firstResp == null) {
                firstResp = resp.body
            } else {
                assertTrue(firstResp == resp.body, "case change in granularity generates different bodies")
            }
        }
    }

    @Test
    fun `3-1 test values latest (json only)`() {
        val (status, json) = restTemplate.getQueryJson(URL_LATEST, "accept" to "application/json")
        assertEquals(HttpStatus.OK, status)
        val values = json.readList()
        assertEquals(1, values.count())
        assertEquals(OID, values[0].get("objectId") as Int)
        assertTrue((values[0].get("timestamp") as String).startsWith(TO.dropLast(1)))
    }

    // -------------------

    private fun DocumentContext.readList(): List<Map<String, Any>> = this.read<List<Map<String, Any>>>("$")


    private fun List<Map<String, Any>>.checkRaw() {
        assertEquals(5, this.count())
        assertEquals(OID.toString(), this.random().get("objectId").toString())
        assertEquals("null", this.random().get("comment").toString())
        assertEquals(JodaUtils.parse(FROM), JodaUtils.parse(this.first().get("timestamp") as String))
        assertEquals(JodaUtils.parse(TO), JodaUtils.parse(this.last().get("timestamp") as String))
    }

    private fun List<Map<String, Any>>.checkAggr(granularity: AggregationGranularity) {
        val nRecords = (60 / granularity.minutes) + 1 // 60 is the difference in minutes between FROM and TO
        val nValuesPerRecord = granularity.minutes / 15 // object 1 has one value each 15 minutes

        assertEquals(nRecords, this.count())
        assertEquals(OID.toString(), this.random().get("objectId").toString())
        assertEquals(nValuesPerRecord.toString(), this.random().get("count").toString())
        for (key in listOf("min", "max", "std", "sum", "mean")) {
            assertTrue(this.random().containsKey(key), "$key missing in at least one record")
        }
        for (key in listOf("timestamp", "lastTimestamp")) {
            assertTrue((this.random().get(key) as String).isBBDataDatetime(), "invalid date format")
        }
    }

}