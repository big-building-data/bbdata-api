package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.random.Random


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR])
@ActiveProfiles(Profiles.UNSECURED)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectDelete {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate


    @Test
    fun `1-1 delete object fail`() {
        // non existant object
        var url = "/objects/61231234"
        var resp = restTemplate.deleteQueryString(url)
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode,
                "delete $url inexistant object returned ${resp.body}")

        // wrong permission
        url = "/objects/$REGULAR_USER_ID"
        resp = restTemplate.deleteQueryString(url, HU to (REGULAR_USER_ID + 1))
        assertTrue(resp.statusCode in listOf(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN),
            "delete $url wrong apikey returned ${resp.body}")

        // object with associated values
        url = "/objects/1"
        resp = restTemplate.deleteQueryString(url)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode,
                "delete $url on used object returned ${resp.body}")
    }

    @Test
    fun `1-2 delete object`() {
        // == create
        val id = restTemplate.createObject(owner = REGULAR_USER_ID)
        val url = "/objects/$id"

        // == get
        var resp = restTemplate.getQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "get $url returned ${resp.body}")

        // == delete
        resp = restTemplate.deleteQueryString(url)
        assertEquals(HttpStatus.OK, resp.statusCode, "delete $url returned ${resp.body}")

        // == check
        resp = restTemplate.getQueryString("/objects/$id")
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode, "delete $url (2) returned ${resp.body}")
    }


}