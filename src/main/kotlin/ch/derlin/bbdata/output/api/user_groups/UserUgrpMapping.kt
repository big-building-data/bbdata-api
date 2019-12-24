package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.output.api.NoUpdateOnCreateEntity
import ch.derlin.bbdata.output.api.users.User
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

// Composite key class must implement Serializable
// and have defaults.
data class UserUgrpMappingId(
        val userId: Int = 0,
        val groupId: Int = 0
) : Serializable

@Entity
@Table(name = "users_ugrps")
@IdClass(UserUgrpMappingId::class)
data class UserUgrpMapping(
        @Id
        @NotNull
        @Column(name = "user_id")
        @JsonIgnore
        val userId: Int,

        @Id
        @NotNull
        @Column(name = "ugrp_id")
        @JsonIgnore
        val groupId: Int,

        @Column(name = "is_admin")
        var isAdmin: Boolean,

        @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JsonUnwrapped
        val user: User? = null,

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "ugrp_id", referencedColumnName = "id", insertable = false, updatable = false)
        private val group: UserGroup? = null

) : NoUpdateOnCreateEntity<UserUgrpMappingId>() {
    @JsonIgnore
    override fun getId(): UserUgrpMappingId? = UserUgrpMappingId(userId, groupId)

}