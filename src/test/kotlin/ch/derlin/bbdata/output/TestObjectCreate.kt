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
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectCreate {


    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        // id of the last created object
        private var id: Int = -1
        private val name = "object-create ${Random.nextInt()}"
    }

    @Test
    fun `1-1 create object fail`() {
        // empty name
        var resp = restTemplate.putWithBody("/objects",
                """{"name": "", "owner": $REGULAR_USER_ID, "unitSymbol": "V"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        // wrong unit
        resp = restTemplate.putWithBody("/objects",
                """{"name": "", "owner": $REGULAR_USER_ID, "unitSymbol": "@badUnit"}""")
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
                """{"name": "$name", "owner": $REGULAR_USER_ID, "unitSymbol": "V"}""")
        assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == get
        id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        val getResponse = restTemplate.getForEntity("/objects/$id", String::class.java)
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        assertTrue(json.read<String>("$.creationdate").isBBDataDatetime())
        assertTrue(json.read<String>("$.unit.type").equals("float"))
        assertNotNull(json.read<String>("$.owner.name"))
        assertNull(json.read<String>("$.description"))

        // ensure it is not part of any group
        val (statusCode, js) = restTemplate.getQueryJson("/objects/$id/objectGroups")
        assertEquals(HttpStatus.OK, statusCode)
        assertEquals(0, js.read<Int>("$.length()"))
    }

    @Test
    fun `1-3 edit object`() {

        val newName = "object-create new ${Random.nextInt(10000)}"
        val newDescr = newName

        // == change name + description
        var postResponse = restTemplate.postWithBody("/objects/$id",
                """{"name": "$newName", "description": "$newDescr"}""")
        assertEquals(HttpStatus.OK, postResponse.statusCode)

        var json = JsonPath.parse(postResponse.body)
        assertEquals(newName, json.read<String>("$.name"))
        assertEquals(newDescr, json.read<String>("$.description"))

        // == change name only
        postResponse = restTemplate.postWithBody("/objects/$id",
                """{"name": "$name"}""")
        assertEquals(HttpStatus.OK, postResponse.statusCode)

        json = JsonPath.parse(postResponse.body)
        assertEquals(name, json.read<String>("$.name"))
        assertEquals(newDescr, json.read<String>("$.description"))
    }

}