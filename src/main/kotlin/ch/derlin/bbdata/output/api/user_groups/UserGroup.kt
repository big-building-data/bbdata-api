package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.output.api.NoUpdateOnCreateEntity
import ch.derlin.bbdata.output.api.object_groups.ObjectGroup
import ch.derlin.bbdata.output.api.objects.Objects
import ch.derlin.bbdata.output.api.users.User
import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.validator.constraints.Length
import javax.persistence.*

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Entity
@Table(name = "ugrps")
data class UserGroup(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private val id: Int? = null,

        @Column(name = "name")
        @field:Length(min = NAME_MIN, max = NAME_MAX)
        val name: String = "",

        @ManyToMany(mappedBy = "allowedUserGroups", fetch = FetchType.LAZY)
        private val accessibleObjectGroups: List<ObjectGroup> = listOf(),

        @OneToMany(cascade = [], mappedBy = "owner", fetch = FetchType.LAZY)
        private val ownedObjectGroups: List<ObjectGroup> = listOf(),

        @OneToMany(cascade = [], mappedBy = "owner", fetch = FetchType.LAZY)
        private val ownedObjects: List<Objects> = listOf(),

        @JsonIgnore
        @OneToMany(cascade = [CascadeType.PERSIST, CascadeType.ALL], orphanRemoval = true)
        @JoinColumn(name = "ugrp_id")
        val userMappings: MutableList<UsergroupMapping> = mutableListOf(),

        @JoinTable(
                name = "users_ugrps",
                joinColumns = [JoinColumn(name = "ugrp_id", referencedColumnName = "id")],
                inverseJoinColumns = [JoinColumn(name = "user_id", referencedColumnName = "id")]
        )
        @ManyToMany(fetch = FetchType.LAZY)
        private val users: List<User> = listOf()

) : NoUpdateOnCreateEntity<Int?>() {
    override fun getId(): Int? = id

    companion object {
        const val NAME_MIN = 3
        const val NAME_MAX = 45
    }
}
