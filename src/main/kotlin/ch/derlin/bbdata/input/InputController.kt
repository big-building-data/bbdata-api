package ch.derlin.bbdata.input

import ch.derlin.bbdata.common.cassandra.*
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.exceptions.WrongParamsException
import ch.derlin.bbdata.output.api.types.BaseType
import ch.derlin.bbdata.output.api.types.Unit
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
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
        private val objectStatsRepository: ObjectStatsRepository,
        private val objectStatsCounterRepository: ObjectStatsCounterRepository,
        private val mapper: ObjectMapper,
        private val kafkaTemplate: KafkaTemplate<String, String> // note: ignore jetbrains [false] warning
) {

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
    fun postNewMeasure(@Valid @NotNull @RequestBody measure: NewValue,
                       @RequestParam("simulate", defaultValue = "false") sim: Boolean): String {

        // check that date is in the past
        val now = DateTime()
        if (measure.timestamp!!.getMillis() > now.millis + MAX_LAG) {
            throw WrongParamsException("date should be in the past: input='${measure.timestamp}', now='$now'")
        }

        // get metadata
        val meta = inputRepository.getMeasureMeta(measure.objectId!!, measure.token!!).orElseThrow {
            ItemNotFoundException(msg = "The pair <objectId (${measure.objectId}), token> does not exist")
        }
        // ensure the object is enabled. This is just a double check, as tokens cannot be created on disabled objects
        if (meta.disabled) {
            throw ForbiddenException("Object ${measure.objectId} is disabled.")
        }
        // ensure the measure matches the type declared, and parse it
        val parsedValue = BaseType.parseType(measure.value!!, meta.type)
        if (parsedValue == null) {
            throw WrongParamsException("The value '${measure.value}' does not match " +
                    "the unit ${meta.unitSymbol} (${meta.type}) declared in the object definition.")
        }
        measure.value = parsedValue.toString()

        // create augmented measure (to post to kafka)
        val augmentedJson = mapper.writer().writeValueAsString(NewValueAugmented.create(measure, meta))

        if (sim) {
            // simulation mode: do not save anything !
            return augmentedJson
        }

        // save TODO transaction !!!!!!!!
        rawValueRepository.save(measure.toRawValue())

        // get old stats
        val objectId = measure.objectId.toInt()
        val objectStats = objectStatsRepository.findById(objectId).orElse(ObjectStats())
        val objectStatsCounter = objectStatsCounterRepository.findById(objectId).orElse(ObjectStatsCounter())

        // compute new stats
        val deltaMs = Math.abs(measure.timestamp.millis - (objectStats.lastTimestamp ?: measure.timestamp).millis)
        val nRecords = objectStatsCounter.nValues
        val newSamplePeriod = (objectStats.avgSamplePeriod * (nRecords - 1) + deltaMs) / nRecords

        // save new stats
        objectStatsRepository.update(measure.objectId.toInt(), newSamplePeriod, measure.timestamp)
        objectStatsCounterRepository.updateWriteCounter(measure.objectId.toInt())

        // publish to Kafka
        val ack = kafkaTemplate.sendDefault(augmentedJson).get()
        return ack.producerRecord.value()
    }
}