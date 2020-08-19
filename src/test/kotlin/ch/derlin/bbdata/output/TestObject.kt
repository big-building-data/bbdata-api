package ch.derlin.bbdata.output

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.beans.factory.annotation.Autowired


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = arrayOf(UNSECURED_REGULAR))
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObject {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val id = 2 // disabling an object will delete its tokens, so don't do it on object 1 used in other tests
    }

    @Test
    fun `1-0 ensure has tokens`() {
        // create a token
        val response = restTemplate.putQueryString("/objects/$id/tokens")
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `1-1 object disable`() {
        var response = restTemplate.postQueryString("/objects/$id/disable")
        assertEquals(HttpStatus.OK, response.statusCode)

        response = restTemplate.postQueryString("/objects/$id/disable")
        assertEquals(HttpStatus.NOT_MODIFIED, response.statusCode)
    }

    @Test
    fun `1-2 object tokens on disabled`() {
        // ensure all tokens have been wiped out
        val (status, json) = restTemplate.getQueryJson("/objects/$id/tokens")
        assertEquals(HttpStatus.OK, status)
        assertEquals(0, json.read<List<Any>>("$").size)
        // ensure we can't create new tokens
        val response = restTemplate.putQueryString("/objects/$id/tokens")
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `1-3 object enable`() {
        // enable once => OK
        val response1 = restTemplate.postQueryString("/objects/$id/enable")
        assertEquals(HttpStatus.OK, response1.statusCode)
        // enable twice => not modified
        val response2 = restTemplate.postQueryString("/objects/$id/enable")
        assertEquals(HttpStatus.NOT_MODIFIED, response2.statusCode)
    }
}