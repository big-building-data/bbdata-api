package ch.derlin.bbdata.output

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("unsecured")
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectCreate {

    @LocalServerPort
    private val port = 0
    var restTemplate = TestRestTemplate()
    val id = 1

    @Test
    @Throws(Exception::class)
    fun `0-1 create object`() {
        // == create
        val putResponse = restTemplate.putForEntity(url("/objects"),
                """{"name": "hello", "owner": 1, "unitSymbol": "V"}""", String::class.java)
        assertEquals(putResponse.statusCode, HttpStatus.OK)

        // == get
        val id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        val getResponse = restTemplate.getForEntity(url("/objects/${id}"), String::class.java)
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertTrue(json.read<String>("$.creationdate").matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}".toRegex()))
        assertTrue(json.read<String>("$.unit.type").equals("float"))
        assertNotNull(json.read<String>("$.owner.name"))
        assertNull(json.read<String>("$.description"))
    }


    private fun url(uri: String): String {
        return "http://localhost:$port$uri"
    }
}