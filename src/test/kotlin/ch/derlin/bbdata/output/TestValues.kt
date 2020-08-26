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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR])
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
    }

    @Test
    fun `1-1 get raw values JSON`() {
        // get between
        val (status, json) = restTemplate.getQueryJson(URL_RAW,
                "accept" to "application/json")
        assertEquals(HttpStatus.OK, status, "get $URL_RAW returned ${json.jsonString()}")
        json.readList().checkRaw()
    }

    @Test
    fun `1-2 get raw values CSV`() {
        // get between
        val resp = restTemplate.getQueryString(URL_RAW)
        assertEquals(HttpStatus.OK, resp.statusCode, "get $URL_RAW returned ${resp.body}")
        resp.body!!.csv2map().checkRaw()
    }

    @Test
    fun `2-1 get aggregated values JSON`() {
        // test the default: should be hours
        var url = URL_AGGR
        val (statusH, jsonH) = restTemplate.getQueryJson(url, "accept" to "application/json")
        assertEquals(HttpStatus.OK, statusH, "get $url returned ${jsonH.jsonString()}")
        jsonH.readList().checkAggr(granularity = AggregationGranularity.hours)

        // test the quarters
        url = "$URL_AGGR&granularity=quarters"
        val (statusQ, jsonQ) = restTemplate.getQueryJson(url, "accept" to "application/json")
        assertEquals(HttpStatus.OK, statusQ, "get $url returned ${jsonQ.jsonString()}")
        jsonQ.readList().checkAggr(granularity = AggregationGranularity.quarters)
    }

    @Test
    fun `2-2 get aggregated values CSV`() {
        // test the hours
        var url = "$URL_AGGR&granularity=hours"
        var resp = restTemplate.getQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "get $url returned ${resp.body}")
        resp.body!!.csv2map().checkAggr(granularity = AggregationGranularity.hours)
        // test the quarters
        url = "$URL_AGGR&granularity=hours"
        resp = restTemplate.getQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "get $url returned ${resp.body}")
        resp.body!!.csv2map().checkAggr(granularity = AggregationGranularity.hours)
    }

    @Test
    fun `2-3 test granularity is case insensitive`() {
        var firstResp: String? = null
        for (g in listOf("hours", "HOURS", "hOuRs")) {
            val url = "$URL_AGGR&granularity=$g"
            val resp = restTemplate.getQueryString(url)
            assertEquals(HttpStatus.OK, resp.statusCode, "get $url returned ${resp.body}")
            if (firstResp == null) {
                firstResp = resp.body
            } else {
                assertTrue(firstResp == resp.body, "get $url: case change in granularity generates different bodies")
            }
        }
    }

    @Test
    fun `3-1 test values latest (json only)`() {
        val values = getLatest(OID, TO)
        assertEquals(1, values.count(), "get latest should return one value")
        assertEquals(OID, values[0].get("objectId") as Int, "get latest: wrong objectId")
        assertTrue((values[0].get("timestamp") as String).startsWith(TO.dropLast(1)), "get latest: wrong timestamp")
    }

    @Test
    fun `3-2 test values latest (json only, way in the past)`() {
        val oid = 2
        val values = getLatest(oid, "2020")
        assertEquals(1, values.count(), "get latest should return one value")
        assertEquals(oid, values[0].get("objectId") as Int, "get latest: wrong objectId")
        assertTrue((values[0].get("timestamp") as String).startsWith("2019-01-01"), "get latest: wrong timestamp")
    }

    @Test
    fun `3-3 test values latest (json only, no value)`() {
        val values = getLatest(3008)
        assertEquals(0, values.count(), "get latest should return no value")
    }

    // -------------------

    private fun DocumentContext.readList(): List<Map<String, Any>> = this.read<List<Map<String, Any>>>("$")


    private fun List<Map<String, Any>>.checkRaw() {
        assertEquals(5, this.count(),
                "checkRaw: number of raw values returned don't match")
        assertEquals(OID.toString(), this.random().get("objectId").toString(),
                "checkRaw: objectId don't match")
        assertEquals("null", this.random().get("comment").toString(),
                "checkRaw: comment should be null")
        assertEquals(JodaUtils.parse(FROM), JodaUtils.parse(this.first().get("timestamp") as String),
                "checkRaw: first timestamp should be $FROM")
        assertEquals(JodaUtils.parse(TO), JodaUtils.parse(this.last().get("timestamp") as String),
                "checkRaw: last timestamp should be $TO")
    }

    private fun List<Map<String, Any>>.checkAggr(granularity: AggregationGranularity) {
        val prefix = "checkAggr $granularity:"
        val nRecords = (60 / granularity.minutes) + 1 // 60 is the difference in minutes between FROM and TO
        val nValuesPerRecord = granularity.minutes / 15 // object 1 has one value each 15 minutes

        assertEquals(nRecords, this.count(), "$prefix number of records don't match")
        assertEquals(OID.toString(), this.random().get("objectId").toString(), "$prefix objectId don't match")
        assertEquals(nValuesPerRecord.toString(), this.random().get("count").toString(), "$prefix count don't match")
        for (key in listOf("min", "max", "std", "sum", "mean")) {
            assertTrue(this.random().containsKey(key),
                    "$prefix $key missing in at least one record")
        }
        for (key in listOf("timestamp", "lastTimestamp")) {
            assertTrue((this.random().get(key) as String).isBBDataDatetime(),
                    "$prefix invalid date format in at least one record")
        }
    }

    private fun getLatest(oid: Int, before: String? = null): List<Map<String, Any>> {
        val url = "/objects/$oid/values/latest" + (if (before != null) "?before=$before" else "")
        val (status, json) = restTemplate.getQueryJson(url, "accept" to "application/json")
        assertEquals(HttpStatus.OK, status, "get $url returned ${json.jsonString()}")
        return json.readList()
    }

}