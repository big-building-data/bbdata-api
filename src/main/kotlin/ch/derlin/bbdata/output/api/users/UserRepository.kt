package ch.derlin.bbdata.output.api.users

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

/**
 * date: 26.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */



@Repository
interface UserRepository : JpaRepository<User, Int> {
    fun findByName(name: String): User
}