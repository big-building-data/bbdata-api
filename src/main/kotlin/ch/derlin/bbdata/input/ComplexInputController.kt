package ch.derlin.bbdata.input

import ch.derlin.bbdata.output.api.values.*
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.DateTime
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.NotNull

/**
 * date: 26.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
//@RestController
class ComplexInputController(
        private val inputRepository: InputRepository,
        private val rawValueRepository: RawValueRepository,
        private val objectStatsRepository: ObjectStatsRepository,
        private val objectStatsCounterRepository: ObjectStatsCounterRepository,
        private val kafkaTemplate: KafkaTemplate<String, String>, // note: ignore jetbrains [false] warning
        private val mapper: ObjectMapper) {

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

    @PostMapping("/measures")
    fun postNewMeasure(@Valid @NotNull @RequestBody measure: NewValue): String {

        // check that date is in the past
        // val now = DateTime()
        // if (measure.timestamp!!.getMillis() > now.millis + MAX_LAG) {
        //     throw WrongParamsException("date should be in the past: input='${measure.timestamp}', now='$now'")
        // }

        // get meta (will also check the token)
        val meta = inputRepository.getMeasureMeta(measure.objectId!!, measure.token!!).orElseThrow {
            throw ItemNotFoundException("The pair <objectId (${measure.objectId}), token> doesn't exist.")
        }

        // save
        rawValueRepository.save(measure.toRawValue())

        // get old stats
        val objectId = measure.objectId.toInt()
        val objectStats = objectStatsRepository.findById(objectId).orElse(ObjectStats())
        val objectStatsCounter = objectStatsCounterRepository.findById(objectId).orElse(ObjectStatsCounter())

        // compute new stats
        val deltaMs = Math.abs(measure.timestamp!!.millis - (objectStats.lastTimestamp ?: measure.timestamp).millis)
        val nRecords = objectStatsCounter.nValues
        val newSamplePeriod = (objectStats.avgSamplePeriod * (nRecords - 1) + deltaMs) / nRecords

        // save new stats
        objectStatsRepository.update(objectId, newSamplePeriod, measure.timestamp)
        objectStatsCounterRepository.updateCounter(objectId)

        // publish to Kafka
        val json = mapper.writer().writeValueAsString(NewValueAugmented.create(measure, meta))
        val ack = kafkaTemplate.sendDefault(json).get()
        return ack.producerRecord.value()
    }
}