package ch.derlin.bbdata.output.api.objects

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository


/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@Repository
interface ObjectRepository : JpaRepository<Objects, Long> {
    @Query("SELECT o FROM Objects o INNER JOIN o.userPerms p " +
            "WHERE p.userId = :userId " +
            "AND (p.writable = true OR p.writable = :writable) " +
            "AND LOCATE(:search, o.name) > 0")
    fun findAll(userId: Int, writable: Boolean, search: String = "",
                pageable: Pageable = Pageable.unpaged()): List<Objects>

    @Query("SELECT o FROM Objects o INNER JOIN o.userPerms p INNER JOIN o.tags t " +
            "WHERE t.name IN :tags " +
            "AND p.userId = :userId " +
            "AND (p.writable = true OR p.writable = :writable) " +
            "AND LOCATE(:search, o.name) > 0")
    fun findAllByTag(tags: List<String>, userId: Int, writable: Boolean, search: String = "",
                     pageable: Pageable = Pageable.unpaged()): List<Objects>

    @Query("SELECT o FROM Objects o INNER JOIN o.userPerms p " +
            "WHERE p.userId = :userId AND o.id = :id AND (p.writable = true OR p.writable = :writable)")
    fun findById(id: Long, userId: Int, writable: Boolean): Objects?
}