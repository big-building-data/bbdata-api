package ch.derlin.bbdata.output.api.types

import ch.derlin.bbdata.output.api.NoUpdateOnCreateEntity
import org.hibernate.validator.constraints.Length
import javax.persistence.*


/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Entity
@Table(name = "units")
data class Unit(

        @Id
        @field:Length(min = 1, max = SYMBOL_MAX)
        @Column(name = "symbol")
        private var symbol: String = "",

        @field:Length(min = 1, max = NAME_MAX)
        @Column(name = "name", unique = true)
        var name: String = "",

        @JoinColumn(name = "type", referencedColumnName = "name")
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        var type: BaseType? = null

) : NoUpdateOnCreateEntity<String>() {
    override fun getId(): String = symbol

    companion object {
        const val SYMBOL_MAX = 10
        const val NAME_MAX = 20
    }
}