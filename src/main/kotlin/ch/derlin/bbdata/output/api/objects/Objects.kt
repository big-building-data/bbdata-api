package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.output.api.object_groups.ObjectGroup
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joda.time.DateTime
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size


/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@Entity
data class Objects(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Basic(optional = false)
        @Column(name = "id")
        val id: Long? = null,

        @Basic(optional = false)
        @NotNull
        @Size(min = 1, max = 60)
        @Column(name = "name")
        val name: String? = null,

        @Size(max = 255)
        @Column(name = "description")
        val description: String? = null,

        //@Column(name = "disabled")
        //val disabled: Boolean = false,

        @Column(name = "creationdate", insertable = false, updatable = false)
        val creationdate: DateTime? = null,

        @JsonIgnore
        @ManyToMany(mappedBy = "objects", fetch = FetchType.LAZY)
        val objectGroups: List<ObjectGroup>? = null,

        @JsonIdentityReference(alwaysAsId = true)
        @OneToMany(cascade = arrayOf(CascadeType.ALL), fetch = FetchType.EAGER, orphanRemoval = true)
        @JoinColumn(name = "object_id", updatable = false)
        val tags: Set<Tag>,

        @OneToMany(fetch = FetchType.LAZY, cascade = arrayOf())
        @JoinColumn(name = "object_id", insertable = false, updatable = false)
        protected val userPerms: List<ObjectsPerms>
)