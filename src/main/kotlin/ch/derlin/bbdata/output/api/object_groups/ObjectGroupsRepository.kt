package ch.derlin.bbdata.output.api.object_groups

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface ObjectGroupsRepository : JpaRepository<ObjectGroup, Long> {

    @Query("SELECT DISTINCT o FROM ObjectGroup o INNER JOIN o.readPermissions p WHERE p.userId = :userId")
    fun findAll(userId: Int): List<ObjectGroup>

    @Query("SELECT DISTINCT o FROM ObjectGroup o INNER JOIN o.writePermissions p WHERE p.userId = :userId")
    fun findAllWritable(userId: Int): List<ObjectGroup>

    @Query("SELECT DISTINCT o FROM ObjectGroup o LEFT JOIN o.readPermissions r " +
            "LEFT JOIN o.writePermissions w WHERE o.id = :id AND (r.userId = :userId or w.userId = :userId)")
    fun findOne(userId: Int, id: Long): Optional<ObjectGroup>

    @Query("SELECT o FROM ObjectGroup o INNER JOIN o.writePermissions p WHERE o.id = :id AND p.userId = :userId")
    fun findOneWritable(userId: Int, id: Long): Optional<ObjectGroup>
}