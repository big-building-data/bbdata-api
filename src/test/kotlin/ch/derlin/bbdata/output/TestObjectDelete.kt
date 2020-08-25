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
        var resp = restTemplate.deleteQueryString("/objects/61231234")
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)

        // wrong permission
        resp = restTemplate.deleteQueryString("/objects/$REGULAR_USER_ID", HU to (REGULAR_USER_ID + 1))
        assertTrue(resp.statusCode in listOf(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN))

        // object with associated values
        resp = restTemplate.deleteQueryString("/objects/1" )
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
    }

    @Test
    fun `1-2 delete object`() {
        // == create
        val id = restTemplate.createObject(owner = REGULAR_USER_ID)

        // == get
        var resp = restTemplate.getQueryString("/objects/$id")
        assertEquals(HttpStatus.OK, resp.statusCode)

        // == delete
        resp = restTemplate.deleteQueryString("/objects/$id")
        assertEquals(HttpStatus.OK, resp.statusCode)

        // == check
        resp = restTemplate.getQueryString("/objects/$id")
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }


}