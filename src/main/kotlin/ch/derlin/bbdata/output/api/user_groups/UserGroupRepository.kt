package ch.derlin.bbdata.output.api.user_groups

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

/**
 * date: 06.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface UserGroupRepository : JpaRepository<UserGroup, Int?> {

    @Query("SELECT g FROM UserGroup g INNER JOIN g.userMappings u " +
            "WHERE u.userId = :userId AND (:admin = false OR u.isAdmin = :admin)")
    fun findMines(userId: Int, admin: Boolean = false): List<UserGroup>

    @Query("SELECT g FROM UserGroup g INNER JOIN g.userMappings u " +
            "WHERE g.id = :id AND u.userId = :userId AND (:admin = false OR u.isAdmin = :admin)")
    fun findMine(userId: Int, id: Int, admin: Boolean = false): Optional<UserGroup>
}