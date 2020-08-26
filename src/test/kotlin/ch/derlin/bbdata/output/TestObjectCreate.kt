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
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode, "put /objects no name returned ${resp.body}")
        // wrong unit
        resp = restTemplate.putWithBody("/objects",
                """{"name": "$name", "owner": $REGULAR_USER_ID, "unitSymbol": "@badUnit"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode, "put /objects wrong unit returned ${resp.body}")
        // missing owner
        resp = restTemplate.putWithBody("/objects",
                """{"name": "$name", "unitSymbol": "V"}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode, "put /objects missing owner returned ${resp.body}")
        // wrong owner
        resp = restTemplate.putWithBody("/objects",
                """{"name": "$name", "owner": 19123187, "unitSymbol": "V"}""")
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode, "put /objects inexistant owner returned ${resp.body}")
    }

    @Test
    fun `1-2 create object`() {
        // == create
        var resp = restTemplate.putWithBody("/objects",
                """{"name": "$name", "owner": $REGULAR_USER_ID, "unitSymbol": "V"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "put /objects returned ${resp.body}")

        // == get
        id = JsonPath.parse(resp.body).read<Int>("$.id")
        resp = restTemplate.getForEntity("/objects/$id", String::class.java)
        JSONAssert.assertEquals(resp.body, resp.body, false)

        // check some json variables
        val json = JsonPath.parse(resp.body)
        assertTrue(json.read<String>("$.creationdate").isBBDataDatetime(), "get /object/$id: improper creation date")
        assertTrue(json.read<String>("$.unit.type").equals("float"), "get /object/$id: wrong unit")
        assertEquals(REGULAR_USER.get("group"), json.read<String>("$.owner.name"), "get /object/$id: wrong owner name")
        assertNull(json.read<String>("$.description"), "get /object/$id: description should be null")

        // ensure it is not part of any group
        val (statusCode, json2) = restTemplate.getQueryJson("/objects/$id/objectGroups")
        assertEquals(HttpStatus.OK, statusCode)
        assertEquals(0, json2.read<Int>("$.length()"),
                "get /objects/$id/objectGroups should be empty, ${json2.jsonString()}")
    }

    @Test
    fun `1-3 edit object`() {
        val url = "/objects/$id"
        val newName = "object-create new ${Random.nextInt(10000)}"
        val newDescr = newName

        // == change name + description
        var resp = restTemplate.postWithBody(url,
                """{"name": "$newName", "description": "$newDescr"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "edit $url all fields returned ${resp.body}")

        var json = JsonPath.parse(resp.body)
        assertEquals(newName, json.read<String>("$.name"), "edit $url all fields: name not modified")
        assertEquals(newDescr, json.read<String>("$.description"), "edit $url all fields: description not modified")

        // == change name only
        resp = restTemplate.postWithBody(url,
                """{"name": "$name"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "edit $url name only returned ${resp.body}")

        json = JsonPath.parse(resp.body)
        assertEquals(name, json.read<String>("$.name"), "edit $url name only: name not modified")
        assertEquals(newDescr, json.read<String>("$.description"), "edit $url name only: description modified")
    }

}