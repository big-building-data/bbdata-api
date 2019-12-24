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


import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import javax.persistence.*
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size


@Entity
@Table(name = "types")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "type")
@JsonIdentityReference(alwaysAsId = true)
data class BaseType(

        @Id
        @NotEmpty
        @Size(min = 1, max = 45)
        @Column(name = "name")
        var type: String = "",

        @OneToMany(cascade = [CascadeType.ALL], mappedBy = "type", fetch = FetchType.LAZY)
        private var units: Collection<Unit> = listOf()
)
