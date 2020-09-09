package ch.derlin.bbdata.input

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.NotNull


/**
 * date: 24.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
// TODO: remove this controller after migration
@RestController
@Tag(name = "Objects Values [Deprecated]", description = "Please, use /objects/values instead.")
class InputControllerDeprecated(
        private val inputController: InputController) {

    @PostMapping("input/measures")
    @Operation(description = "**DEPRECATED**: this endpoint may disappear in newer versions. Please use `/objects/values` instead. <br>" +
            "Submit a new measure. This is similar to `/objects/values`, but takes only one measure in the body.")
    fun postNewMeasure(@Valid @NotNull @RequestBody rawMeasure: NewValue,
                       @RequestParam("simulate", defaultValue = "false") sim: Boolean): InputController.NewValueAugmented =
            inputController.postNewMeasures(listOf(rawMeasure), sim)[0]

    @PostMapping("input/measures/bulk")
    @Operation(description = "**DEPRECATED**: this endpoint may disappear in newer versions. Please use `/objects/values` instead.")
    fun postNewMeasureBulkOld(@Valid @NotNull @RequestBody rawMeasures: List<NewValue>,
                              @RequestParam("simulate", defaultValue = "false") sim: Boolean): List<InputController.NewValueAugmented> =
            inputController.postNewMeasures(rawMeasures, sim)

}