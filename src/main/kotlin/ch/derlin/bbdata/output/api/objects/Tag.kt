package ch.derlin.bbdata.output.api.objects

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import javax.xml.bind.annotation.*


/**
 * date: 25.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

class TagId(
        @Basic(optional = false)
        @NotNull
        @Column(name = "name")
        @Size(max = 25)
        val name: String = "",

        @Basic(optional = false)
        @NotNull
        @Column(name = "object_id")
        val objectId: Int = 0
) : Serializable

@Entity
@Table(name = "tags")
@IdClass(TagId::class)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "name")
class Tag {

    @Id
    val name: String? = null

    @Id
    val objectId: Int = 0
}