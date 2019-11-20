package ch.derlin.bbdata.api.output.entities.object_groups

import ch.derlin.bbdata.api.output.entities.object_groups.ObjectGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface ObjectGroupsRepository : JpaRepository<ObjectGroup, Long>