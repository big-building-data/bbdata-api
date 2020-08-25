package ch.derlin.bbdata

import ch.derlin.bbdata.output.TestStats
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.JsonPathAssertions
import kotlin.random.Random

/**
 * date: 05.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

// ======== String utils

fun String.isBBDataDatetime(): Boolean = this.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z".toRegex())

fun String.csv2map(): List<Map<String, String>> {
    val splits: List<List<String>> = this.lines().filter { it.isNotEmpty() }.map { it.split(',') }
    val headers = splits.first()
    return splits.drop(1).map { (headers zip it).toMap() }
}

// ======== create test resources

fun TestRestTemplate.createObject(owner: Int, name: String = "test-${Random.nextInt()}", unit: String = "V"): Int {
    val resp = this.putWithBody("/objects",
            """{"name": "$name", "owner": $owner, "unitSymbol": "$unit"}""")
    assertEquals(HttpStatus.OK, resp.statusCode)

    val id = JsonPath.parse(resp.body).read<Int>("$.id")
    println("Created object with id=$id (name: $name)")
    return id
}

fun TestRestTemplate.createToken(objectId: Int): String {
    val resp = this.putQueryString("/objects/$objectId/tokens")
    assertEquals(HttpStatus.OK, resp.statusCode)

    val json = JsonPath.parse(resp.body)
    println("Created token for object=$objectId with id=${json.read<Int>("$.id")}")
    return json.read<String>("$.token")
}