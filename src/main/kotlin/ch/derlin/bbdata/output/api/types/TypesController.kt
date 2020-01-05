package ch.derlin.bbdata.output.api.types

import ch.derlin.bbdata.output.exceptions.WrongParamsException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
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
                      private val baseTypeRepository: BaseTypeRepository) {

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

    @GetMapping("/types")
    fun getTypes(): List<BaseType> = baseTypeRepository.findAll()

    @GetMapping("/units")
    fun getUnits(): List<Unit> = unitRepository.findAll()

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PostMapping("/units")
    fun addUnit(@Valid @NotNull @RequestBody newUnit: NewUnit): Unit {
        val type = baseTypeRepository.findById(newUnit.type).orElseThrow {
            WrongParamsException("The type '${newUnit.type}' is not valid.")
        }
        return unitRepository.saveAndFlush(Unit(symbol = newUnit.symbol, name = newUnit.name, type = type))
    }
}
