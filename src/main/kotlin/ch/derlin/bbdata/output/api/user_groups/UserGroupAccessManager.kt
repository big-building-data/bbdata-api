package ch.derlin.bbdata.output.api.user_groups

import org.springframework.stereotype.Component
import java.util.*


/**
 * date: 18.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Component
class UserGroupAccessManager(
        val userGroupRepository: UserGroupRepository,
        val userGroupMappingRepository: UserGroupMappingRepository) {

    fun canAccessGroup(userId: Int, groupId: Int, admin: Boolean = false): Boolean {
        return if (userGroupMappingRepository.isSuperAdmin(userId)) true
        else userGroupRepository.findMine(userId, groupId, admin = admin).isPresent
    }

    fun getAccessibleGroup(userId: Int, groupId: Int, admin: Boolean = false): Optional<UserGroup> {
        return if (userGroupMappingRepository.isSuperAdmin(userId)) userGroupRepository.findById(groupId)
        else userGroupRepository.findMine(userId, groupId, admin = admin)
    }
}