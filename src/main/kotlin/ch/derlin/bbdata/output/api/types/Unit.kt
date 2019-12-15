package ch.derlin.bbdata.output.api.types

import ch.derlin.bbdata.output.api.NoUpdateOnCreateEntity
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size


/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Entity
@Table(name = "units")
data class Unit(

        @Id
        @Basic(optional = false)
        @NotNull
        @Size(min = 1, max = 10)
        @Column(name = "symbol")
        private var symbol: String = "",

        @Basic(optional = false)
        @NotNull
        @Size(min = 1, max = 20)
        @Column(name = "name")
        var name: String = "",

        @JoinColumn(name = "type", referencedColumnName = "name")
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        var type: BaseType? = null

) : NoUpdateOnCreateEntity<String>() {
    override fun getId(): String = symbol
}