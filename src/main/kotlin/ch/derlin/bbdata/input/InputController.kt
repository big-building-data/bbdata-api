package ch.derlin.bbdata.input

import ch.derlin.bbdata.HiddenEnvironmentVariables
import ch.derlin.bbdata.common.cassandra.RawValueRepository
import ch.derlin.bbdata.common.stats.StatsLogic
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.exceptions.WrongParamsException
import ch.derlin.bbdata.output.api.types.BaseType
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
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
@RestController
@Tag(name = "Objects Values", description = "Submit and query objects values")
class InputController(
        private val inputRepository: InputRepository,
        private val rawValueRepository: RawValueRepository,
        private val statsLogic: StatsLogic,
        private val mapper: ObjectMapper,
        private val kafkaTemplate: KafkaTemplate<String, String> // note: ignore jetbrains [false] warning
) {

    private final val log = LoggerFactory.getLogger(InputController::class.java)

    // Just for tests, if you don't want to have kafka running, do:
    // export BB_NO_KAFKA=true
    @Value("\${${HiddenEnvironmentVariables.NO_KAFKA}:false}")
    private val NO_KAFKA: Boolean = false

    private val MAX_LAG: Long = 2000 // in millis

    data class NewValueAugmented(
            val objectId: Long,
            val timestamp: DateTime,
            val value: String,
            val comment: String?,
            val unitName: String,
            val unitSymbol: String,
            val type: String,
            val owner: Int
    ) {

        companion object {
            fun create(v: NewValue, m: InputRepository.MeasureMeta) = NewValueAugmented(
                    objectId = v.objectId!!,
                    timestamp = v.timestamp!!,
                    value = v.value!!,
                    comment = v.comment,
                    unitName = m.unitName,
                    unitSymbol = m.unitSymbol,
                    type = m.type,
                    owner = m.owner
            )
        }
    }

    @PostMapping("objects/values")
    @Operation(description = "Submit new measures. " +
            "Each objectId/timestamp couple must be unique, both in the body and the database. Hence, any duplicate will make the request fail. " +
            "If you omit to provide a timestamp for any measure, it will be added automatically (server time). " +
            "This request is *atomic*: either *all* measures are valid and saved, or none. ")
    fun postNewMeasures(@Valid @NotNull @RequestBody rawMeasures: List<NewValue>,
                        @RequestParam("simulate", defaultValue = "false") sim: Boolean): List<NewValueAugmented> {

        val now = DateTime()

        val augmentedJsons = mutableListOf<NewValueAugmented>()
        val valueKeys = mutableSetOf<String>()

        val (measures, rawValues) = rawMeasures.map { rawMeasure ->
            val measure = if (rawMeasure.timestamp != null) {
                // check that date is in the past
                if (rawMeasure.timestamp.millis > now.millis + MAX_LAG) {
                    log.info("REJECTED: wrong timestamp: ${rawMeasure}")
                    throw WrongParamsException("objectId ${rawMeasure.objectId}: date should be in the past. input='${rawMeasure.timestamp}', now='$now'")
                }
                rawMeasure
            } else {
                // no timestamp provided: set it to now
                rawMeasure.copy(timestamp = now)
            }

            // get metadata
            val meta = inputRepository.getMeasureMeta(measure.objectId!!, measure.token!!).orElseThrow {
                log.info("REJECTED: wrong token: $measure")
                ItemNotFoundException(msg = "objectId ${measure.objectId}: the pair <objectId, token> does not exist")
            }
            // ensure the object is enabled. This is just a double check, as tokens cannot be created on disabled objects
            if (meta.disabled) {
                log.info("REJECTED: object is disabled: $rawMeasure")
                throw ForbiddenException("objectId ${rawMeasure.objectId} is disabled.")
            }
            // ensure the measure matches the type declared, and parse it
            val parsedValue = BaseType.parseType(measure.value!!, meta.type)
            if(parsedValue == null) {
                log.info("REJECTED: wrong value given the unit: $measure, $meta")
                throw WrongParamsException("objectId ${rawMeasure.objectId}: the value '${measure.value}' does not match " +
                        "the unit ${meta.unitSymbol} (${meta.type}) declared in the object definition.")
            }

            measure.value = parsedValue.toString()

            // create rawValue for cassandra
            val rawValue = measure.toRawValue()
            val key = "${measure.objectId}-${measure.timestamp}"

            // ensure no duplicate objectId/timestamp in the data sent
            if (valueKeys.contains(key)) {
                log.info("REJECTED: duplicate in body: $measure")
                throw WrongParamsException("objectId ${measure.objectId}: two or more values with the same timestamp")
            }
            valueKeys.add(key)

            // ensure it doesn't already exist in cassandra TODO: find a better way ?
            if (rawValueRepository.existsById(rawValue.key)) {
                log.info("REJECTED: duplicate in cassandra: $measure")
                throw WrongParamsException("objectId ${rawMeasure.objectId}: " +
                        "a value with the same timestamp (${measure.timestamp}) already exists for this object.")
            }

            // create augmented measure (to post to kafka)
            augmentedJsons.add(NewValueAugmented.create(measure, meta))
            // measure for stats
            measure to rawValue
        }.unzip()


        if (sim) {
            // simulation mode: do not save anything !
            return augmentedJsons
        }

        // save TODO transaction ???
        rawValueRepository.saveAll(rawValues)

        // update stats
        val future = statsLogic.updateAllStatsAsync(measures)

        // publish to Kafka
        if (!NO_KAFKA) {
            augmentedJsons.map {
                kafkaTemplate.sendDefault(mapper.writer().writeValueAsString(it))
            }//.last().get() // wait for the last ack to get back
        }

        // finally, return the augmented values
        return augmentedJsons
    }
}