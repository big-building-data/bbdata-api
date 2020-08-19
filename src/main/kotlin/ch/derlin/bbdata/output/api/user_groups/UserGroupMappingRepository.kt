package ch.derlin.bbdata.output.api.user_groups

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * date: 06.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface UserGroupMappingRepository : JpaRepository<UsergroupMapping, UserUgrpMappingId> {
    @Transactional
    fun deleteByGroupId(groupId: Int): Unit

    fun getByUserId(userId: Int): List<UsergroupMapping>

    @Query("SELECT CASE WHEN count(u) > 0 THEN true ELSE false END " +
            "FROM UsergroupMapping u WHERE u.userId = :userId AND u.groupId = 1 AND u.isAdmin = true")
    fun isSuperAdmin(userId: Int): Boolean

}