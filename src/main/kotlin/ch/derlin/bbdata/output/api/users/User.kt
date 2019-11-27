package ch.derlin.bbdata.output.api.users

import ch.derlin.bbdata.output.api.auth.Apikey
import ch.derlin.bbdata.output.api.auth.PasswordDigest
import ch.derlin.bbdata.output.exceptions.AppException
import org.joda.time.DateTime
import org.springframework.http.HttpStatus
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import javax.xml.bind.annotation.XmlTransient


/**
 * date: 26.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Entity
@Table(name = "users")
data class User(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Basic(optional = false)
        @Column(name = "id")
        var id: Int? = null,

        @Basic(optional = false)
        @NotNull
        @Size(min = 1, max = 45)
        @Column(name = "name")
        var name: String? = null,

        @Basic(fetch = FetchType.LAZY)
        @NotNull
        @Size(min = 5)
        @Column(name = "password")
        private var password: String? = null,

        // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
        @Basic(optional = false)
        @NotNull
        @Size(min = 1, max = 45)
        @Column(name = "email")
        var email: String? = null,

        @Column(name = "creationdate", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
        var creationdate: DateTime? = null,

        @OneToMany(cascade = arrayOf(CascadeType.ALL), fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", referencedColumnName = "id")
        var apikeys: List<Apikey>? = null

        /*
        @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users")
        @get:XmlTransient
        val groups: Set<UserGroup>? = null,

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "user", fetch = FetchType.LAZY)
        @get:XmlTransient
        var userToGroupMappings: List<UserUserGroup>? = null
        */

) {


    @Throws(AppException::class)
    fun setPassword(password: String) {
        try {
            this.password = PasswordDigest.toMD5(password)
        } catch (ex: Exception) {
            throw AppException.create(HttpStatus.INTERNAL_SERVER_ERROR,
                    "SecurityException", "Could not hash password")
        }

    }


    companion object {

        private const val serialVersionUID = 1L
    }

}
