package ch.derlin.bbdata.output.api.auth

import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.exceptions.ForbiddenException
import org.joda.time.DateTime
import org.joda.time.MutablePeriod
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import javax.persistence.ParameterMode
import javax.persistence.PersistenceContext


/**
 * date: 26.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@Component
class AuthRepository {

    @PersistenceContext
    private lateinit var em: EntityManager

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
            throw ForbiddenException("Wrong username|password.")
        }
        // create a temporary token
        return createApikey(userId, true, DateTime().plus(EXPIRE), "auto_login")
    }

    fun logout(userId: Int, apikey: String): Boolean {
        val entity = checkApikey(userId, apikey)
        // TODO: really remove apikey ?
        entity?.let {
            em.remove(entity)
            return true
        }
        return false
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


    fun checkApikey(userId: Int, apikey: String): Apikey? {
        val resultList = em.createNamedQuery("Apikey.Check")
                .setParameter("userId", userId)
                .setParameter("apikey", apikey)
                .resultList

        return if (resultList.size == 1) resultList[0] as Apikey else null
    }

    companion object {
        val EXPIRE = MutablePeriod(13, 0, 0, 0)
    }
}
