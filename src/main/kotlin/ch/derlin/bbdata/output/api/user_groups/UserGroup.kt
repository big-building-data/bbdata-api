package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.output.api.NoUpdateOnCreateEntity
import ch.derlin.bbdata.output.api.object_groups.ObjectGroup
import ch.derlin.bbdata.output.api.objects.Objects
import ch.derlin.bbdata.output.api.users.User
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.transaction.annotation.Transactional
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import javax.xml.bind.annotation.XmlTransient
import kotlin.jvm.Transient

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Entity
@Table(name = "ugrps")
data class UserGroup(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Basic(optional = false)
        @Column(name = "id")
        private val id: Int? = null,

        @Basic(optional = false)
        @Size(min = 1, max = 45)
        @Column(name = "name")
        @NotNull
        val name: String = "",

        @ManyToMany(mappedBy = "allowedUserGroups", fetch = FetchType.LAZY)
        private val accessibleObjectGroups: List<ObjectGroup> = listOf(),

        @OneToMany(cascade = arrayOf(), mappedBy = "owner", fetch = FetchType.LAZY)
        private val ownedObjectGroups: List<ObjectGroup> = listOf(),

        @OneToMany(cascade = arrayOf(), mappedBy = "owner", fetch = FetchType.LAZY)
        private val ownedObjects: List<Objects> = listOf(),

        @JsonIgnore
        @OneToMany(cascade = arrayOf(CascadeType.PERSIST, CascadeType.ALL))
        @JoinColumn(name = "ugrp_id")
        val userMappings: MutableList<UserUgrpMapping> = mutableListOf(),

        @JoinTable(
                name = "users_ugrps",
                joinColumns = arrayOf(JoinColumn(name = "ugrp_id", referencedColumnName = "id")),
                inverseJoinColumns = arrayOf(JoinColumn(name = "user_id", referencedColumnName = "id"))
        )
        @ManyToMany(fetch = FetchType.LAZY)
        private val users: List<User> = listOf()

) : NoUpdateOnCreateEntity<Int?>() {
    override fun getId(): Int? = id
}
