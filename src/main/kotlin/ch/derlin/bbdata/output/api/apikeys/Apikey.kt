package ch.derlin.bbdata.output.api.apikeys

import org.joda.time.DateTime
import javax.persistence.*
import javax.validation.constraints.Size


/**
 * date: 26.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Entity
@Table(name = "apikeys")
data class Apikey(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        var id: Int? = null,

        @Size(min = 1, max = 32)
        @Column(name = "secret")
        var secret: String? = null,

        @Column(name = "expirationdate")
        var expirationDate: DateTime? = null,

        @Column(name = "user_id")
        var userId: Int = 0,

        @Column(name = "readonly")
        var isReadOnly: Boolean = false,

        @Column(name = "description")
        var description: String? = null
)
