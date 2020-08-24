package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.common.Beans.DESCRIPTION_MAX
import ch.derlin.bbdata.output.api.object_groups.ObjectGroup
import ch.derlin.bbdata.output.api.types.Unit
import ch.derlin.bbdata.output.api.user_groups.UserGroup
import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.Generated
import org.hibernate.annotations.GenerationTime
import org.hibernate.validator.constraints.Length
import org.joda.time.DateTime
import javax.persistence.*


/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@Entity
@Table(name = "objects")
data class Objects(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        val id: Long? = null,

        @field:Length(min = NAME_MIN, max = NAME_MAX)
        @Column(name = "name")
        var name: String? = null,

        @field:Length(max = DESCRIPTION_MAX)
        @Column(name = "description")
        var description: String? = null,

        @JoinColumn(name = "unit_symbol", referencedColumnName = "symbol")
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        val unit: Unit,

        @Column(name = "disabled")
        var disabled: Boolean = false,

        @Column(name = "creationdate", insertable = false, updatable = false)
        @Generated(GenerationTime.INSERT)
        val creationdate: DateTime? = null,

        @JoinColumn(name = "ugrp_id", referencedColumnName = "id")
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        val owner: UserGroup,

        @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
        @JoinColumn(name = "object_id", updatable = false)
        var tags: MutableSet<Tag> = mutableSetOf(),

        @OneToMany(cascade = [])
        @field:JsonIgnore
        @JoinColumn(name = "object_id", insertable = false, updatable = false)
        val tokens: List<Token> = listOf(),

        @OneToMany(cascade = [], fetch = FetchType.LAZY, orphanRemoval = true)
        @JoinColumn(name = "object_id", updatable = false)
        @field:JsonIgnore
        val comments: List<Comment> = listOf(),

        @ManyToMany(mappedBy = "objects", fetch = FetchType.LAZY)
        private val objectGroups: List<ObjectGroup>? = null,

        @OneToMany(fetch = FetchType.LAZY, cascade = [])
        @JoinColumn(name = "object_id", insertable = false, updatable = false)
        protected val userPerms: List<ObjectsPerms>? = listOf()

) {
    fun addTag(tag: String): Boolean = this.tags.add(Tag(tag, this.id!!))
    fun removeTag(tag: String): Boolean = this.tags.removeIf { it.name == tag }

    fun getToken(id: Int): Token? = tokens.find { it.id == id }

    @JsonIgnore
    fun getObjectGroups(): List<ObjectGroup>? = objectGroups

    companion object {
        const val NAME_MIN = 1
        const val NAME_MAX = 60
    }
}