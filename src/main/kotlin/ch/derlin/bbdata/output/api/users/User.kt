package ch.derlin.bbdata.output.api.users

import ch.derlin.bbdata.output.api.apikeys.Apikey
import ch.derlin.bbdata.output.api.apikeys.PasswordDigest
import ch.derlin.bbdata.output.api.user_groups.UserGroup
import ch.derlin.bbdata.output.api.user_groups.UsergroupMapping
import ch.derlin.bbdata.output.exceptions.AppException
import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.Generated
import org.hibernate.annotations.GenerationTime
import org.joda.time.DateTime
import javax.persistence.*
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size


/**
 * date: 26.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Entity
@Table(name = "users")
data class User(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        var id: Int? = null,

        @NotEmpty
        @Size(min = 1, max = 45)
        @Column(name = "name")
        var name: String = "",

        @Basic(fetch = FetchType.LAZY)
        @Size(min = 5)
        @Column(name = "password")
        private var password: String? = null,

        // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
        @Size(min = 1, max = 45)
        @Column(name = "email")
        var email: String? = null,

        @Column(name = "creationdate", insertable = false, updatable = false)
        @Generated(GenerationTime.INSERT)
        val creationdate: DateTime? = null,

        @OneToMany(cascade = arrayOf(CascadeType.ALL), fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", referencedColumnName = "id")
        @JsonIgnore
        var apikeys: List<Apikey>? = null,


        @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users")
        private val groups: List<UserGroup>? = null,

        @OneToMany(cascade = arrayOf(CascadeType.ALL), mappedBy = "user", fetch = FetchType.LAZY)
        private var userToGroupMappings: List<UsergroupMapping>? = null


) {

    // TODO: where to put NewX classes ? controller or model ?
    class NewUser {
        @NotNull
        @Size(min = 1, max = 45)
        val name: String? = null

        @NotNull
        @Size(min = 5)
        val password: String? = null

        @NotNull
        @Pattern(regexp = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message = "Invalid email")
        val email: String? = null

        fun toUser(): User = User(name = name!!, password = hashPassword(password!!), email = email!!)

    }

    companion object {

        fun hashPassword(password: String): String {
            try {
                return PasswordDigest.toMD5(password)
            } catch (ex: Exception) {
                throw AppException("Could not hash password")
            }

        }
    }
}
