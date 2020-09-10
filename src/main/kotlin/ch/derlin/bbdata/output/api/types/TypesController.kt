package ch.derlin.bbdata.output.api.types

import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.WrongParamsException
import ch.derlin.bbdata.output.api.user_groups.UserGroupMappingRepository
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@RestController
@Tag(name = "Basics", description = "Manage generic types and units")
class TypesController(private val unitRepository: UnitRepository,
                      private val baseTypeRepository: BaseTypeRepository,
                      private val userGroupMappingRepository: UserGroupMappingRepository) {

    class NewUnit { // do not use a data class !
        @NotEmpty
        @Size(max = Unit.SYMBOL_MAX)
        val symbol: String = ""

        @NotEmpty
        @Size(max = Unit.NAME_MAX)
        val name: String = ""

        @NotEmpty
        @Size(max = BaseType.TYPE_MAX)
        val type: String = ""
    }

    @Operation(description = "Get the list of data types supported.")
    @GetMapping("/types")
    fun getTypes(): List<BaseType> = baseTypeRepository.findAll()

    @Operation(description = "Get the list of units supported.")
    @GetMapping("/units")
    fun getUnits(): List<Unit> = unitRepository.findAll()

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Create a new unit. " +
            "__IMPORTANT__: this endpoint is only accessible to the user group ADMIN (id=1).")
    @PostMapping("/units")
    fun addUnit(@UserId userId: Int, @Valid @NotNull @RequestBody newUnit: NewUnit): Unit {
        if (!userGroupMappingRepository.isSuperAdmin(userId))
            throw ForbiddenException("Only users part of the 'admin' group can add new units.")

        val type = baseTypeRepository.findById(newUnit.type).orElseThrow {
            WrongParamsException("The type '${newUnit.type}' is not valid.")
        }
        return unitRepository.saveAndFlush(Unit(symbol = newUnit.symbol, name = newUnit.name, type = type))
    }
}
