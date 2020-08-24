package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
import ch.derlin.bbdata.common.Beans.DESCRIPTION_MAX
import ch.derlin.bbdata.output.api.objects.Objects
import ch.derlin.bbdata.output.api.user_groups.UserGroup
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.Length
import javax.persistence.*

@Entity
@Table(name = "ogrps")
data class ObjectGroup(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        var id: Long? = null,

        @field:Length(min = NAME_MIN, max = NAME_MAX)
        @Column(name = "name")
        var name: String? = null,

        @field:Length(max = DESCRIPTION_MAX)
        @Column(name = "description")
        var description: String? = null,

        @JoinTable(
                name = "objects_ogrps",
                joinColumns = [JoinColumn(name = "ogrp_id", referencedColumnName = "id")],
                inverseJoinColumns = [JoinColumn(name = "object_id", referencedColumnName = "id")]
        )
        @ManyToMany(fetch = FetchType.LAZY)
        @JsonIgnore
        var objects: MutableList<Objects> = mutableListOf(),

        @JoinColumn(name = "ugrp_id", referencedColumnName = "id")
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        val owner: UserGroup,

        @JoinTable(
                name = "rights",
                joinColumns = [JoinColumn(name = "ogrp_id", referencedColumnName = "id")],
                inverseJoinColumns = [JoinColumn(name = "ugrp_id", referencedColumnName = "id")]
        )
        @ManyToMany(fetch = FetchType.LAZY)
        @JsonIgnore
        var allowedUserGroups: MutableList<UserGroup> = mutableListOf(),

        @OneToMany(fetch = FetchType.LAZY)
        @JoinColumn(name = "ogrp_id", insertable = false, updatable = false)
        @JsonIgnore
        val readPermissions: List<ObjectGroupReadPerms> = listOf(),

        @OneToMany(fetch = FetchType.LAZY)
        @JoinColumn(name = "ogrp_id", insertable = false, updatable = false)
        @JsonIgnore
        val writePermissions: List<ObjectGroupWritePerms> = listOf()
) {

    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("objects")
    @Transient
    var _objectsToReturn: List<Objects>? = null
        private set

    fun withObjects(): ObjectGroup {
        this._objectsToReturn = objects
        return this
    }

    companion object {
        const val NAME_MIN = 3
        const val NAME_MAX = 45
    }
}

