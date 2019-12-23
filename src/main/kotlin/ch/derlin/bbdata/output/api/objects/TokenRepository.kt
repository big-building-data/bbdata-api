package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.output.api.objects.Token
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * date: 23.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Repository
interface TokenRepository : JpaRepository<Token, Int>