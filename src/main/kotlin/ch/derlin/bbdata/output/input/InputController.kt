package ch.derlin.bbdata.output.input

import ch.derlin.bbdata.output.api.objects.ObjectRepository
import ch.derlin.bbdata.output.api.objects.Objects
import ch.derlin.bbdata.output.api.objects.TokenRepository
import ch.derlin.bbdata.output.api.values.*
import ch.derlin.bbdata.output.dates.JodaUtils.format
import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.exceptions.ForbiddenException
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.exceptions.WrongParamsException
import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.DateTime
import org.joda.time.YearMonth
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size


/**
 * date: 24.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
class InputController(
        private val objectsRepository: ObjectRepository,
        private val tokensRepository: TokenRepository,
        private val rawValueRepository: RawValueRepository,
        private val objectStatsRepository: ObjectStatsRepository,
        private val objectStatsCounterRepository: ObjectStatsCounterRepository,
        private val mapper: ObjectMapper
) {

    private val MAX_LAG: Long = 2000 // in millis


    class NewValue {

        @NotNull
        @Min(value = 0, message = "objectId must be positive.")
        val objectId: Long? = null

        @NotNull
        @Size(min = 32, max = 32, message = "wrong size: should be 32 chars long.")
        val token: String? = null

        @NotNull(message = "Invalid date. Format: YYYY-MM-ddTHH:mm[:ss], range: 2016-01-01T00:00 to 2050-01-01T00:00")
        val timestamp: DateTime? = null

        @NotNull
        val value: String? = null

        @Size(max = 1024, message = "too long. Maximum set to 1024.")
        val comment: String? = null

        fun toRawValue(): RawValue = RawValue(
                key = RawValuePK(
                        objectId = objectId!!.toInt(),
                        month = YearMonth(timestamp).toString(),
                        timestamp = timestamp),
                value = value!!,
                comment = comment
        )
    }

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
            fun create(v: NewValue, o: Objects) = NewValueAugmented(
                    objectId = o.id!!,
                    timestamp = v.timestamp!!,
                    value = v.value!!,
                    comment = v.comment,
                    unitName = o.unit.name,
                    unitSymbol = o.unit.id,
                    type = o.unit.type!!.type,
                    owner = o.owner.id!!
            )
        }

    }

    @PostMapping("/measures")
    fun postNewMeasure(@Valid @NotNull @RequestBody measure: NewValue) {

        // check that date is in the past
        // val now = DateTime()
        // if (measure.timestamp!!.getMillis() > now.millis + MAX_LAG) {
        //     throw WrongParamsException("date should be in the past: input='${measure.timestamp}', now='$now'")
        // }

        // get metadata
        val obj = objectsRepository.findById(measure.objectId!!).orElseThrow {
            ItemNotFoundException("This object does not exist")
        }
        tokensRepository.getByObjectIdAndToken(measure.objectId, measure.token!!).orElseThrow {
            ForbiddenException("This token is invalid.")
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
        objectStatsRepository.update(measure.objectId.toInt(), newSamplePeriod, measure.timestamp)
        objectStatsCounterRepository.updateCounter(measure.objectId.toInt())

        // publish to Kafka
        val json = mapper.writer().writeValueAsString(NewValueAugmented.create(measure, obj))
        print(json) // TODO
    }
}