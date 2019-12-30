package ch.derlin.bbdata.output.api.objects

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.hibernate.validator.constraints.Length
import java.io.Serializable
import javax.persistence.*


/**
 * date: 25.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

data class TagId(
        @Column(name = "name")
        @field:Length(max = 25)
        val name: String = "",

        @Column(name = "object_id")
        val objectId: Long = 0
) : Serializable

@Entity
@Table(name = "tags")
@IdClass(TagId::class)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "name")
@JsonIdentityReference(alwaysAsId = true)
data class Tag(
        @Id
        val name: String,
        @Id
        val objectId: Long
)