package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
import ch.derlin.bbdata.output.api.objects.Objects
import ch.derlin.bbdata.output.api.objects.Tag
import ch.derlin.bbdata.output.api.user_groups.UserGroup
import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import org.springframework.http.converter.json.MappingJacksonValue
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(name = "ogrps")
//@JsonFilter("noObjectsFilter")
data class ObjectGroup(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Basic(optional = false)
        @Column(name = "id")
        var id: Long? = null,

        @Basic(optional = false)
        @NotNull
        @Size(min = 1, max = 45)
        @Column(name = "name")
        var name: String? = null,

        @Size(max = 255)
        @Column(name = "description")
        var description: String? = null,

        @JoinTable(
                name = "objects_ogrps",
                joinColumns = arrayOf(JoinColumn(name = "ogrp_id", referencedColumnName = "id")),
                inverseJoinColumns = arrayOf(JoinColumn(name = "object_id", referencedColumnName = "id"))
        )
        @ManyToMany(fetch = FetchType.LAZY)
        @JsonIgnore
        var objects: MutableList<Objects> = mutableListOf(),

        @JoinColumn(name = "ugrp_id", referencedColumnName = "id")
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        val owner: UserGroup,

        @JoinTable(
                name = "rights",
                joinColumns = arrayOf(JoinColumn(name = "ogrp_id", referencedColumnName = "id")),
                inverseJoinColumns = arrayOf(JoinColumn(name = "ugrp_id", referencedColumnName = "id"))
        )
        @ManyToMany(fetch = FetchType.LAZY)
        @JsonIgnore
        var allowedUserGroups: List<UserGroup> = listOf(),

        @OneToMany(fetch = FetchType.LAZY)
        @JoinColumn(name = "ogrp_id", insertable = false, updatable = false)
        @JsonIgnore
        val readPermissions: List<ObjectGroupReadPerms> = listOf(),

        @OneToMany(fetch = FetchType.LAZY)
        @JoinColumn(name = "ogrp_id", insertable = false, updatable = false)
        @JsonIgnore
        val writePermissions: List<ObjectGroupWritePerms> = listOf()
) {
//    companion object {
//        fun asJacksonMapping(obj: Any, withObjects: Boolean = false): MappingJacksonValue {
//            val mapping = MappingJacksonValue(obj)
//            mapping.filters = SimpleFilterProvider().addFilter(
//                    "noObjectsFilter",
//                    SimpleBeanPropertyFilter.serializeAllExcept(if (withObjects) "_x_" else "objects"))
//            return mapping
//        }
//    }

    data class ObjectGroupExtended constructor(
            val id: Long,
            val name: String,
            val description: String?,
            val owner: UserGroup,
            val objects: List<Objects> = listOf() // make object part of the JSON
    ) {
        constructor(og: ObjectGroup) : this(
                id = og.id!!,
                name = og.name!!,
                description = og.description,
                owner = og.owner,
                objects = og.objects)
    }
}

