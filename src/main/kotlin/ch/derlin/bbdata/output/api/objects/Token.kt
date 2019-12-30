package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.output.api.apikeys.TokenGenerator
import org.hibernate.validator.constraints.Length
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import javax.persistence.*

/**
 * date: 23.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface TokenRepository : JpaRepository<Token, Int>

@Entity
@Table(name = "tokens")
data class Token(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        val id: Int? = null,

        @Column(name = "object_id")
        val objectId: Long,

        @Column(name = "token")
        @field:Length(min = 32, max = 32)
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