package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.output.api.users.User
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

// Composite key class must implement Serializable
// and have defaults.
class UserUgrpMappingId(
        val userId: Int = 0,
        val groupId: Int = 0
) : Serializable

@Entity
@Table(name = "users_ugrps")
@IdClass(UserUgrpMappingId::class)
data class UserUgrpMapping(
        @Id
        @Basic(optional = false)
        @NotNull
        @Column(name = "user_id")
        val userId: Int,

        @Id
        @Basic(optional = false)
        @NotNull
        @Column(name = "ugrp_id")
        val groupId: Int,

        @Column(name = "is_admin")
        var isAdmin: Boolean,

        @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        private val user: User,

        @JoinColumn(name = "ugrp_id", referencedColumnName = "id", insertable = false, updatable = false)
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private val group: UserGroup
)