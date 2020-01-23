package ch.derlin.bbdata.output

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
import kotlin.random.Random

/**
 * date: 28.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestCreateUnit {

    // TODO: also check with security on that only admins can create units !
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val symbol = "V${Random.nextInt(10000)}"
    }

    @Test
    fun `1-1 test create unit fail`() {
        val putWrongType = restTemplate.postWithBody("/units",
                """{"symbol": "$symbol", "name": "test", "type": "hello"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putWrongType.statusCode)

        val putNoSymbol = restTemplate.postWithBody("/units",
                """{"symbol": "", "name": "test", "type": "float"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putNoSymbol.statusCode)

        val putDupName = restTemplate.postWithBody("/units",
                """{"symbol": "$symbol", "name": "volt", "type": "float"}""")
        assertEquals(HttpStatus.BAD_REQUEST, putDupName.statusCode)
    }

    @Test
    fun `1-2 test create unit`() {
        val put1 = restTemplate.postWithBody("/units",
                """{"symbol": "$symbol", "name": "$symbol", "type": "float"}""")
        assertEquals(HttpStatus.OK, put1.statusCode)

        val put2 = restTemplate.postWithBody("/units",
                """{"symbol": "$symbol", "name": "$symbol", "type": "float"}""")
        assertNotEquals(HttpStatus.OK, put2.statusCode)
    }
}