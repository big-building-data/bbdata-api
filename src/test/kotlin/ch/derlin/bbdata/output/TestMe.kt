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
 * date: 28.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR])
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestMe {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate


    @Test
    fun `1-1 test get me`() {
        val (status, json) = restTemplate.getQueryJson("/me")
        assertEquals(HttpStatus.OK, status, "/me returned ${json.jsonString()}")

        assertEquals(REGULAR_USER_ID, json.read<Int>("$.id"), "/me: wrong userId")
        assertEquals(REGULAR_USER.get("name"), json.read<String>("$.name"), "/me: wrong name")
        assertTrue(json.read<String>("$.creationdate").isBBDataDatetime(), "/me: improper datetime format")
    }

    @Test
    fun `2-1 test get my groups`() {
        val (status, json) = restTemplate.getQueryJson("/me/userGroups")
        assertEquals(HttpStatus.OK, status, "get /me/userGroups returned ${json.jsonString()}")

        val myGroupMatches = json.read<List<Map<String,Any>>>("$[?(@.id == $REGULAR_USER_ID)]")
        assertEquals(1, myGroupMatches.size, "get /me/userGroups: missing group #$REGULAR_USER_ID")
        val myGroup = myGroupMatches.first()
        assertEquals(REGULAR_USER.get("group"), myGroup.get("name"), "get /me/userGroups #$REGULAR_USER_ID: wrong name")
        assertEquals(true, myGroup.get("admin"), "get /me/userGroups #$REGULAR_USER_ID: wrong admin flat")
    }
}