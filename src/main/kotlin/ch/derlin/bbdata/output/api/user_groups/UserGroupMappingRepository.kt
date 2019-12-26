package ch.derlin.bbdata.output.api.user_groups

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * date: 06.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface UserGroupMappingRepository : JpaRepository<UsergroupMapping, UserUgrpMappingId> {
    @Transactional
    fun deleteByGroupId(groupId: Int): Unit
}