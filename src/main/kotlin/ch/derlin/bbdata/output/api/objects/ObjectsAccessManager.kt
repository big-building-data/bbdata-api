package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.output.api.user_groups.UserGroupMappingRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.*

/**
 * date: 19.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Component
class ObjectsAccessManager(
        val objectRepository: ObjectRepository,
        private val userGroupMappingRepository: UserGroupMappingRepository
) {

    fun findAll(userId: Int, writable: Boolean, search: String = "",
                pageable: Pageable = Pageable.unpaged()): List<Objects> {
        return if (userGroupMappingRepository.isSuperAdmin(userId)) objectRepository.findAll(search, pageable)
        else objectRepository.findAll(userId, writable, search, pageable)
    }

    fun findAllByTag(tags: List<String>, userId: Int, writable: Boolean, search: String = "",
                     pageable: Pageable = Pageable.unpaged()): List<Objects> {
        return if (userGroupMappingRepository.isSuperAdmin(userId)) objectRepository.findAllByTag(tags, search, pageable)
        else objectRepository.findAllByTag(tags, userId, writable, search, pageable)
    }

    fun findById(id: Long, userId: Int, writable: Boolean): Optional<Objects> {
        return if (userGroupMappingRepository.isSuperAdmin(userId)) objectRepository.findById(id)
        else objectRepository.findById(id, userId, writable)
    }
}