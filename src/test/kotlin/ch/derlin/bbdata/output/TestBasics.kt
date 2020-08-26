package ch.derlin.bbdata.output

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.UNSECURED_REGULAR
import ch.derlin.bbdata.getQueryJson
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
class TestBasics {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate


    @Test
    fun `1-1 test get units`() {
        val (status, json) = restTemplate.getQueryJson("/units")
        assertEquals(HttpStatus.OK, status, "get /units returned ${json.jsonString()}")
        assertNotEquals(0, json.read<List<Any>>("$.[?(@.type == 'float')]").size,
            "get /units returned 0 units of type float")
    }

    @Test
    fun `2-1 test get types`() {
        val (status, json) = restTemplate.getQueryJson("/types")
        assertEquals(HttpStatus.OK, status, "get /types returned ${json.jsonString()}")
        assertNotEquals(0, json.read<List<String>>("$").contains("float"),
            "get /types missing type float")
    }
}