package ch.derlin.bbdata.input

import ch.derlin.bbdata.common.cassandra.RawValue
import ch.derlin.bbdata.common.cassandra.RawValuePK
import ch.derlin.bbdata.common.truncate
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.joda.time.DateTime
import org.joda.time.YearMonth
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * date: 26.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

data class NewValue(

        @field:NotNull
        @field:Min(value = 0, message = "objectId must be positive.")
        val objectId: Long? = null,

        @field:NotNull
        @field:Size(min = 32, max = 32, message = "wrong size: should be 32 chars long.")
        val token: String? = null,

        //@NotNull(message = "Invalid date. Format: YYYY-MM-ddTHH:mm[:ss], range: 2016-01-01T00:00 to 2050-01-01T00:00")
        val timestamp: DateTime? = null,

        @field:NotNull
        @field:NotEmpty
        @field:JsonDeserialize(using = RawStringDeserializer::class)
        var value: String? = null,

        @field:Size(max = 1024, message = "too long. Maximum set to 1024.")
        val comment: String? = null
) {


    fun toRawValue(): RawValue = RawValue(
            key = RawValuePK(
                    objectId = objectId!!.toInt(),
                    month = YearMonth(timestamp).toString(),
                    timestamp = timestamp),
            value = value!!,
            comment = comment
    )

    override fun toString(): String {
        return "NewValue(objectId=$objectId, token=$token, timestamp=$timestamp, " +
                "value=${value.truncate()}, comment=${comment?.truncate(10)})"
    }
}