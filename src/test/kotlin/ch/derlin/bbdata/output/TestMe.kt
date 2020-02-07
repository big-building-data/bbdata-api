package ch.derlin.bbdata.output

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.getQueryJson
import ch.derlin.bbdata.isBBDataDatetime
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestMe {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate


    @Test
    fun `1-1 test get me`() {
        val (status, json) = restTemplate.getQueryJson("/me")
        assertEquals(HttpStatus.OK, status)

        assertEquals(1, json.read<Int>("$.id"))
        assertEquals("admin", json.read<String>("$.name"))
        assertTrue(json.read<String>("$.creationdate").isBBDataDatetime(), "proper datetime format")
    }

    @Test
    fun `2-1 test get my groups`() {
        val (status, json) = restTemplate.getQueryJson("/me/userGroups")
        assertEquals(HttpStatus.OK, status)
        val adminGroupMatches = json.read<List<Map<String,Any>>>("$[?(@.id == 1)]")
        assertEquals(1, adminGroupMatches.size)
        val adminGroup = adminGroupMatches.first()
        assertEquals("admin", adminGroup.get("name"))
        assertEquals(true, adminGroup.get("admin"))
    }
}