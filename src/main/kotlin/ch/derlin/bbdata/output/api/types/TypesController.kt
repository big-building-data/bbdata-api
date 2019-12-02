package ch.derlin.bbdata.output.api.types

import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.security.NoHeaderRequired
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
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

    @GetMapping("/types_")
    @NoHeaderRequired
    fun getBaseTypes_(): Array<String> = baseTypeRepository.findAll().map { it.type }.toTypedArray()

    @GetMapping("/types")
    @NoHeaderRequired
    fun getBaseTypes(): List<BaseType> = baseTypeRepository.findAll()

    @GetMapping("/units")
    @NoHeaderRequired
    fun getUnits(): List<Unit> = unitRepository.findAll()

    @PostMapping("/units")
    fun addUnit(@Valid @RequestBody newUnit: NewUnit) { // TODO: @Valid !!!
        if (unitRepository.existsById(newUnit.symbol))
        // TODO JPA doesn't make a difference between create and save
            throw AppException.badRequest(name = "DuplicateField", msg = "A unit with symbol ${newUnit.symbol} already exists.")

        baseTypeRepository.findById(newUnit.type).map {
            unitRepository.save(Unit(symbol = newUnit.symbol, name = newUnit.name, type = it))
        }.orElseThrow { AppException.badRequest(name = "WrongType", msg = "The type ${newUnit.type} is not valid.") }
    }


}