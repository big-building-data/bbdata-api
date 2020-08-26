package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
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
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR])
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObject {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        var id = -1
    }

    @Test
    fun `1-0 create object and token`() {
        id = restTemplate.createObject(owner = REGULAR_USER_ID)
        val resp = restTemplate.putQueryString("/objects/$id/tokens")
        assertEquals(HttpStatus.OK, resp.statusCode, "put /objects/$id/tokens returned ${resp.body}")
    }

    @Test
    fun `1-1 object disable`() {
        val url = "/objects/$id/disable"
        var resp = restTemplate.postQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "post $url returned ${resp.body}")

        resp = restTemplate.postQueryString("/objects/$id/disable")
        assertEquals(HttpStatus.NOT_MODIFIED, resp.statusCode, "post $url (2) returned ${resp.body}")
    }

    @Test
    fun `1-2 object tokens on disabled`() {
        // ensure all tokens have been wiped out
        val (status, json) = restTemplate.getQueryJson("/objects/$id/tokens")
        assertEquals(HttpStatus.OK, status,
                "get /objects/$id/tokens (disabled object) returned ${json.jsonString()}")
        assertEquals(0, json.read<List<Any>>("$").size)
        // ensure we can't create new tokens
        val response = restTemplate.putQueryString("/objects/$id/tokens")
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode,
                "put /objects/$id/tokens (disabled object) returned ${json.jsonString()}")
    }

    @Test
    fun `1-3 object enable`() {
        val url = "/objects/$id/enable"
        // enable once => OK
        var resp = restTemplate.postQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "post $url returned ${resp.body}")
        // enable twice => not modified
        resp = restTemplate.postQueryString("/objects/$id/enable")
        assertEquals(HttpStatus.NOT_MODIFIED, resp.statusCode, "post $url (2) returned ${resp.body}")
    }
}