package ch.derlin.bbdata.output

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.isBBDataDatetime
import ch.derlin.bbdata.putWithBody
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


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectCreate {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `1-1 create object fail`(){
        // empty name
        var resp = restTemplate.putWithBody("/objects",
                """{"name": "", "owner": 1, "unitSymbol": "V"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        // wrong unit
        resp = restTemplate.putWithBody("/objects",
                """{"name": "", "owner": 1, "unitSymbol": "@badUnit"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        // missing owner
        resp = restTemplate.putWithBody("/objects",
                """{"name": "", "unitSymbol": "V"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        // wrong owner
        resp = restTemplate.putWithBody("/objects",
                """{"name": "", "owner": 19123187, "unitSymbol": "V"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    @Test
    fun `1-2 create object`() {
        // == create
        val putResponse = restTemplate.putWithBody("/objects",
                """{"name": "hello", "owner": 1, "unitSymbol": "V"}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == get
        val id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        val getResponse = restTemplate.getForEntity("/objects/${id}", String::class.java)
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertTrue(json.read<String>("$.creationdate").isBBDataDatetime())
        assertTrue(json.read<String>("$.unit.type").equals("float"))
        assertNotNull(json.read<String>("$.owner.name"))
        assertNull(json.read<String>("$.description"))
    }

}