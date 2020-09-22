package ch.derlin.bbdata.output.api.types

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
/**
 * Context: BBData Project (daplab.ch)
 * Date: summer 2016
 *
 * @author Lucy Linder
 */


import ch.derlin.bbdata.common.truncate
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.hibernate.validator.constraints.Length
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.persistence.*
import javax.validation.constraints.NotEmpty


@Entity
@Table(name = "types")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "type")
@JsonIdentityReference(alwaysAsId = true)
data class BaseType(

        @Id
        @NotEmpty
        @field:Length(min = 1, max = TYPE_MAX)
        @Column(name = "name")
        var type: String = "",

        @OneToMany(cascade = [CascadeType.ALL], mappedBy = "type", fetch = FetchType.LAZY)
        private var units: Collection<Unit> = listOf()
) {


    companion object {
        const val TYPE_MAX = 45

        private val log: Logger = LoggerFactory.getLogger(BaseType::class.java)

        fun parseType(value: String, type: String): Any? {
            try {
                when (type) {
                    "string" -> return value
                    "float" -> return value.toFloat()
                    "int" -> return value.toInt()
                    "bool" -> {
                        if (value.toLowerCase() in listOf("true", "on", "yes", "1"))
                            return true
                        if (value.toLowerCase() in listOf("false", "off", "no", "0"))
                            return false
                        return null
                    }
                    else -> log.error("got an unknown unit type '$type' for value '$value'")
                }
            } catch (e: Exception) {
                log.debug("exception while parsing value ${value.truncate()} of type $type: ${e.javaClass.simpleName} ${e.message}")
            }
            return null
        }
    }
}
