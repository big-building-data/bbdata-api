package ch.derlin.bbdata.input

import ch.derlin.bbdata.Profiles
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * date: 25.05.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.SQL_STATS)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class SqlInputApiTest: InputApiTest()