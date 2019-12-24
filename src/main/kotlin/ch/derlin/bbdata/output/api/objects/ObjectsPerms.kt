package ch.derlin.bbdata.output.api.objects

import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

/**
 * date: 25.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

// Composite key class must implement Serializable
// and have defaults.
data class ObjectsPermsId(
        @Id
        @NotNull
        @Column(name = "user_id")
        val userId: Int = 0,

        @Id
        @NotNull
        @Column(name = "object_id")
        val objectId: Int = 0
) : Serializable

@Entity
@Table(name = "userperms")
@IdClass(ObjectsPermsId::class)
data class ObjectsPerms(
        @Id
        val userId: Int,

        @Id
        val objectId: Int,

        @NotNull
        @Column(name = "writable")
        val writable: Boolean
)