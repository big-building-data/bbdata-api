package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
import ch.derlin.bbdata.output.api.objects.Objects
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(name = "ogrps")
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
        var objects: List<Objects>
)
