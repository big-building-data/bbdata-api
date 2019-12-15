package ch.derlin.bbdata.output.api.types

import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.Size

/**
 * date: 30.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@RestController
class TypesController(private val unitRepository: UnitRepository,
                      private val baseTypeRepository: BaseTypeRepository) {

    class NewUnit { // do not use a data class !
        @Size(min = 1, max = 10)
        val symbol: String = ""

        @Size(min = 1, max = 20)
        val name: String = ""

        @Size(min = 1, max = 45)
        val type: String = ""
    }

    @GetMapping("/types")
    fun getBaseTypes(): List<BaseType> = baseTypeRepository.findAll()

    @GetMapping("/units")
    fun getUnits(): List<Unit> = unitRepository.findAll()

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PostMapping("/units")
    fun addUnit(@Valid @RequestBody newUnit: NewUnit) {
        baseTypeRepository.findById(newUnit.type).map {
            unitRepository.save(Unit(symbol = newUnit.symbol, name = newUnit.name, type = it))
        }.orElseThrow { AppException.badRequest(name = "WrongType", msg = "The type '${newUnit.type}' is not valid.") }
    }


}