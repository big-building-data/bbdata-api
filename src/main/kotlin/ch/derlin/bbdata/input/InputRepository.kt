package ch.derlin.bbdata.input

import ch.derlin.bbdata.common.CacheConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.*
import javax.persistence.EntityManager

/**
 * date: 26.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@Component
class InputRepository {

    class MeasureMeta(fields: Array<Any>) : Serializable {
        val unitName: String = fields[0] as String
        val unitSymbol: String = fields[1] as String
        val type: String = fields[2] as String
        val owner: Int = fields[3] as Int
        val disabled: Boolean = fields[4] as Boolean

        companion object {
            val NATIVE_QUERY: String = """
            SELECT u.name AS "unitName", u.symbol AS "unitSymbol", u.type, o.ugrp_id AS "owner", o.disabled
            FROM objects o INNER JOIN tokens t ON o.id = t.object_id INNER JOIN units u ON u.symbol = o.unit_symbol
            WHERE o.id = :objectId AND t.token = :token
            """.trimIndent()
        }
    }


    @Autowired
    private lateinit var em: EntityManager

    @Cacheable(CacheConstants.CACHE_NAME, key = "#objectId.toString().concat(':').concat(#token)", unless = "#result == null")
    fun getMeasureMeta(objectId: Long, token: String): Optional<MeasureMeta> {
        val query = em.createNativeQuery(MeasureMeta.NATIVE_QUERY)

        query.setParameter("objectId", objectId)
        query.setParameter("token", token)

        val resList = query.resultList
        return if (resList.isEmpty())
            Optional.empty()
        else
            Optional.of(MeasureMeta(resList.first() as Array<Any>))
    }
}