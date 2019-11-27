package ch.derlin.bbdata.output.api.auth

import ch.derlin.bbdata.output.api.users.User
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.output.exceptions.AppException
import org.joda.time.DateTime
import org.joda.time.MutablePeriod
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.persistence.ParameterMode
import javax.persistence.PersistenceContext


/**
 * date: 26.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@Component
class AuthFacade {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @Throws(AppException::class)
    fun login(userId: Int, pass: String): Apikey {
        val storedProcedure = em.createStoredProcedureQuery("login")
        // set parameters
        storedProcedure.registerStoredProcedureParameter("userId", Int::class.java, ParameterMode.IN)
        storedProcedure.registerStoredProcedureParameter("clearpass", String::class.java, ParameterMode.IN)
        storedProcedure.registerStoredProcedureParameter("ok", Boolean::class.java, ParameterMode.OUT)
        storedProcedure.setParameter("userId", userId)
        storedProcedure.setParameter("clearpass", pass)
        // execute SP
        storedProcedure.execute()
        // get result
        val ok = storedProcedure.getOutputParameterValue("ok") as Boolean

        if (!ok) {
            throw AppException.forbidden("AuthenticationFailure",
                    "Wrong username|password.")
        }
        // create a temporary token
        return createApikey(userId, true, DateTime().plus(EXPIRE), "auto_login")
    }

    companion object {
        val EXPIRE = MutablePeriod(13, 0, 0, 0)
    }

    fun createApikey(userId: Int, writable: Boolean, expirationDate: DateTime,
                     description: String): Apikey {
        // create the apikey object
        val apikey = Apikey()
        apikey.userId = userId
        apikey.isReadOnly = !writable
        apikey.expirationdate = expirationDate
        apikey.description = description

        // generate the apikey secret
        apikey.secret = TokenGenerator.generate(32)

        // save it to database
        em.persist(apikey)
        return apikey
    }

    fun login(username: String, password: String): Apikey {
        val u: User = userRepository.findByName(username)
        u.id?.let { return login(it, password) }
        throw AppException.forbidden(msg = "User not found.")
    }

    fun checkApikey(userId: Int, apikey: String): Apikey? {
        val resultList = em.createNamedQuery("Apikey.Check")
                .setParameter("userId", userId)
                .setParameter("apikey", apikey)
                .resultList

        return if (resultList.size == 1) resultList[0] as Apikey else null
    }
}
