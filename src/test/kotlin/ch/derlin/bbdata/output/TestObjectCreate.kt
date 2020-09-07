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
        // invalid tag: too long
        resp = restTemplate.putWithBody("/objects",
                """{"name": "$name", "owner": $REGULAR_USER_ID, "unitSymbol": "V", "tags": ["ok", ${"x".repeat(100)}]}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode, "put /objects long tag returned ${resp.body}")
        // invalid tag: not a string
        resp = restTemplate.putWithBody("/objects",
                """{"name": "$name", "owner": $REGULAR_USER_ID, "unitSymbol": "V", "tags": [{}]}""")
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode, "put /objects object tag returned ${resp.body}")
    }

    @Test
    fun `1-2 create object`() {
        // == create
        var resp = restTemplate.putWithBody("/objects",
                """{"name": "$name", "owner": $REGULAR_USER_ID, "unitSymbol": "V"}""")
        assertEquals(HttpStatus.OK, resp.statusCode, "put /objects returned ${resp.body}")
        val putBody = resp.body

        // == get
        id = JsonPath.parse(resp.body).read<Int>("$.id")
        resp = restTemplate.getForEntity("/objects/$id", String::class.java)
        JSONAssert.assertEquals(putBody, resp.body, false)

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

    @Test
    fun `1-2 create objects in bulk`() {
        // == create
        val tag = "bulk-tag-${Random.nextInt()}"
        val names = listOf("bulk-${Random.nextInt()}", "bulk-${Random.nextInt()}")

        val bodyTemplate = """[
            |{"name": "${names[0]}", "owner": $REGULAR_USER_ID, "unitSymbol": "V", "tags": ["$tag"]},
            |{"name": "${names[1]}", "owner": %d, "unitSymbol": "V", "tags": ["$tag"]}
            |]""".trimMargin()

        // create with two different owners
        var resp = restTemplate.putWithBody("/objects/bulk", bodyTemplate.format(1))
        assertNotEquals(HttpStatus.OK, resp.statusCode, "put /objects/bulk with different owners returned ${resp.body}")

        // create ok
        resp = restTemplate.putWithBody("/objects/bulk", bodyTemplate.format(REGULAR_USER_ID))
        assertEquals(HttpStatus.OK, resp.statusCode, "put /objects/bulk returned ${resp.body}")
        val putBody = resp.body

        // == get
        resp = restTemplate.getForEntity("/objects?tags=$tag", String::class.java)
        JSONAssert.assertEquals(putBody, resp.body, false)

        // == check
        val json = JsonPath.parse(putBody)
        assertEquals(REGULAR_USER.get("group"),
                json.read<List<String>>("$[?(@.name == \"${names.random()}\")].owner.name")[0],
                "put in bulk: one owner group is wrong ${json.jsonString()}")

    }

}