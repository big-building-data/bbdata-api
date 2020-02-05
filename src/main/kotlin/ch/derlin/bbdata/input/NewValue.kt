package ch.derlin.bbdata.input

import ch.derlin.bbdata.common.cassandra.RawValue
import ch.derlin.bbdata.common.cassandra.RawValuePK
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
    @NotEmpty
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