package ch.derlin.bbdata.output.api.auth

import org.joda.time.DateTime
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size


/**
 * date: 26.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Entity
@Table(name = "apikeys")
@NamedQueries(
        // check that apikey exists and is active. Params: userId, apikey
        NamedQuery(
                name = "Apikey.Check",
                query = "SELECT a FROM Apikey a WHERE a.userId = :userId AND a.secret = :apikey AND (a.expirationdate IS NULL OR a.expirationdate > CURRENT_TIMESTAMP)"),
        // get an apikey from userid and secret. Params: userId, apikey
        NamedQuery(
                name = "Apikey.FindBySecret",
                query = "SELECT a FROM Apikey a WHERE a.userId = :userId AND a.secret = :apikey"),
        // find one apikey. Params: userId, apikeyId
        NamedQuery(
                name = "Apikey.FindOne",
                query = "SELECT a FROM Apikey a WHERE a.userId = :userId AND a.id = :apikeyId")
        )
data class Apikey(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Basic(optional = false)
        @Column(name = "id")
        var id: Int? = null,

        @Basic(optional = false)
        @NotNull
        @Size(min = 1, max = 32)
        @Column(name = "secret")
        var secret: String? = null,

        @Column(name = "expirationdate")
        var expirationdate: DateTime? = null,

        @Column(name = "user_id")
        @NotNull
        @Basic(optional = false)
        var userId: Int = 0,

        @Column(name = "readonly")
        @NotNull
        @Basic(optional = false)
        var isReadOnly: Boolean = false,

        @Column(name = "description")
        var description: String? = null

)
