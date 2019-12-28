package ch.derlin.bbdata.output.api.apikeys

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * date: 27.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface ApikeyRepository :  JpaRepository<Apikey, Int> {

    fun findByUserId(userId: Int): List<Apikey>
    fun findByIdAndUserId(id: Int, userId: Int): Apikey?

    @Query("SELECT a FROM Apikey a WHERE a.userId = :userId AND a.secret = :secret " +
            "AND (a.expirationDate IS NULL OR a.expirationDate > CURRENT_TIMESTAMP)")
    fun findValid(userId: Int, secret: String): Optional<Apikey>

    // a bug currently in Hibernate returns BigInt instead of boolean... so return Int to avoid cast exception
    @Query("SELECT COUNT(id) <> 0 FROM users WHERE id = :userId AND password = MD5(:clearpass)", nativeQuery = true)
    fun canLogin(userId: Int, clearpass: String): Int
}