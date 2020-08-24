package ch.derlin.bbdata.input

import ch.derlin.bbdata.NO_KAFKA
import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.UNSECURED_REGULAR
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [UNSECURED_REGULAR, NO_KAFKA])
@ActiveProfiles(Profiles.UNSECURED, Profiles.SQL_STATS)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class SqlInputApiTest: InputApiTest()