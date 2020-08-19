package ch.derlin.bbdata.output.api.object_groups

import ch.derlin.bbdata.output.api.user_groups.UserGroupMappingRepository
import org.springframework.stereotype.Component
import java.util.*

/**
 * date: 19.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Component
class ObjectGroupAccessManager(
        val objectGroupsRepository: ObjectGroupsRepository,
        private val userGroupMappingRepository: UserGroupMappingRepository
) {

    fun findAll(userId: Int, writable: Boolean = false): List<ObjectGroup> {
        if (userGroupMappingRepository.isSuperAdmin(userId))
            return objectGroupsRepository.findAll()

        return if (writable) objectGroupsRepository.findAllWritable(userId)
        else objectGroupsRepository.findAll(userId)
    }

    fun findOne(userId: Int, id: Long, writable: Boolean = false): Optional<ObjectGroup> {
        if (userGroupMappingRepository.isSuperAdmin(userId))
            return objectGroupsRepository.findById(id)

        return if (writable) objectGroupsRepository.findOneWritable(userId, id)
        else objectGroupsRepository.findOne(userId, id)
    }
}