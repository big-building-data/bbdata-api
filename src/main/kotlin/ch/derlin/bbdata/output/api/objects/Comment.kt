package ch.derlin.bbdata.output.api.objects

import org.hibernate.annotations.Type
import org.joda.time.DateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*


/**
 * date: 24.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface CommentRepository : JpaRepository<Comment, Int> {
    @Query("SELECT c FROM Comment c WHERE c.objectId = :objectId " +
            "AND :forDate BETWEEN c.from AND c.to")
    fun findForDate(objectId: Long, forDate: DateTime): List<Comment>

    @Query("SELECT c FROM Comment c WHERE c.objectId = :objectId " +
            "AND (:dfrom IS NULL or c.from >= :dfrom) AND (:dto IS NULL or c.to <= :dto)")
    fun findBetween(objectId: Long, dfrom: DateTime? = null, dto: DateTime? = null): List<Comment>

    fun findByIdAndObjectId(id: Int, objectId: Long): Optional<Comment>
}

@Entity
@Table(name = "comments")
data class Comment(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        val id: Int? = null,

        @Column(name = "object_id")
        val objectId: Long,

        @Column(name = "dfrom")
        val from: DateTime,

        @Column(name = "dto")
        val to: DateTime,

        @Column(name = "comment")
        @Type(type = "text")
        val comment: String,

        @OneToMany(fetch = FetchType.LAZY, cascade = arrayOf())
        @JoinColumn(name = "object_id", insertable = false, updatable = false)
        protected val userPerms: List<ObjectsPerms>? = listOf()
)