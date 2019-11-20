package ch.derlin.bbdata.api.output.entities.objects

import ch.derlin.bbdata.api.output.entities.object_groups.ObjectGroup
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
        //@Temporal(TemporalType.TIMESTAMP)
        val creationdate: DateTime? = null,

        @ManyToMany(mappedBy = "objects", fetch = FetchType.LAZY)
        private val objectGroups: List<ObjectGroup>? = null
)