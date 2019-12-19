package ch.derlin.bbdata.output.api.object_groups

import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull


/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

data class ObjectsGroupPermsId(
        @Id
        @Basic(optional = false)
        @NotNull
        @Column(name = "user_id")
        var userId: Int = 0,

        @Id
        @Basic(optional = false)
        @NotNull
        @Column(name = "ogrp_id")
        var objectGroupId: Int = 0
) : Serializable

// TODO: find a way to make it DRY

@Entity
@Table(name = "ogrps_read")
@IdClass(ObjectsGroupPermsId::class)
data class ObjectGroupReadPerms(
        @Id
        var userId: Int = 0,

        @Id
        var objectGroupId: Int = 0,

        @Basic(optional = false)
        @NotNull
        @Column(name = "is_admin")
        var writable: Boolean
)

@Entity
@Table(name = "ogrps_write")
@IdClass(ObjectsGroupPermsId::class)
data class ObjectGroupWritePerms(

        @Id
        var userId: Int = 0,

        @Id
        var objectGroupId: Int = 0,

        @Basic(optional = false)
        @NotNull
        @Column(name = "is_admin")
        var writable: Boolean
)