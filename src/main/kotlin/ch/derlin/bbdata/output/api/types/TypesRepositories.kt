package ch.derlin.bbdata.output.api.types

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Repository
interface BaseTypeRepository : JpaRepository<BaseType, String>

@Repository
interface UnitRepository : JpaRepository<Unit, String>