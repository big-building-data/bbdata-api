package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.output.api.auth.TokenGenerator
import javax.persistence.*
import javax.validation.constraints.Size

/**
 * date: 23.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Entity
@Table(name = "tokens")
data class Token(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Basic(optional = false)
        @Column(name = "id")
        val id: Int? = null,

        @Basic(optional = false)
        @Column(name = "object_id")
        val objectId: Long,

        @Basic(optional = false)
        @Column(name = "token")
        @Size(min = 32, max = 32)
        private val token: String,

        @Column(name = "description")
        var description: String? = null
) {
    companion object {

        val TOKEN_LENGTH = 32

        fun create(objectId: Long, description: String? = null): Token = Token(
                objectId = objectId,
                token = TokenGenerator.generate(TOKEN_LENGTH),
                description = description
        )
    }
}