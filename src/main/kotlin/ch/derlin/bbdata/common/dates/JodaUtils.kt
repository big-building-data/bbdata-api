package ch.derlin.bbdata.common.dates

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.*


/**
 * Taken from JodaUtils (gitlab)
 *
 * date: 07.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
object JodaUtils {

    val FMT_ISO_MINUTES = "yyyy-MM-dd'T'HH:mm'Z'"
    val FMT_ISO_SECONDS = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    val FMT_ISO_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    var defaultPattern: String = FMT_ISO_MILLIS
        set(value) {
            field = value
            defaultFormatter = getFormatter(field)
        }

    private var defaultFormatter = getFormatter(defaultPattern)
    private var minDate: DateTime? = null
    private var maxDate: DateTime? = null

// ------------------------------------- configure

    /**
     * Set the default timezone to UTC for both [java.util] and [org.joda.time].
     */
    fun setDefaultTimeZoneUTC() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        DateTimeZone.setDefault(DateTimeZone.UTC)
    }

    /**
     * Treat dates outside of the interval from-to as incorrect dates,
     * Further calls to [.parse] will fail
     * with an [IllegalArgumentException] in case the string is in the correct format but outside this range.
     *
     * @param from minimal correct date, inclusive.
     * @param to   maximal correct date, exclusive.
     */
    fun setAcceptableDateRange(from: String, to: String? = null) {
        minDate = parseOrNull(from)
        if (to != null) maxDate = parseOrNull(to)
    }

// -------------------------------------- parse

    /**
     * @param epoch millis since epoch
     * @return the date
     * @see .parse
     */
    fun parseOrNull(epoch: Long): DateTime? {
        try {
            return parse(epoch)
        } catch (e: IllegalArgumentException) {
            return null
        }

    }

    /**
     * @param iso the date string in ISO format
     * @return the date
     * @see .parse
     */
    fun parseOrNull(iso: String): DateTime? {
        try {
            return parse(iso)
        } catch (e: IllegalArgumentException) {
            return null
        }

    }

    /**
     * @param iso an iso formatted string (see [DateTime] constructor).
     * @return the date represented by iso
     * @throws IllegalArgumentException in case the date is incorrect.
     * Incorrect means either a bad format or a date outside the acceptable range ([.acceptableDateRange].
     */
    fun parse(iso: String): DateTime {
        val dateTime = DateTime(iso.trim { it <= ' ' })
        checkDateRange(dateTime)
        return dateTime
    }

    /**
     * @param millis a date as the number of milliseconds since January 1rst, 1970.
     * @return the date represented by `iso`
     * @throws IllegalArgumentException in case the date is incorrect.
     * Incorrect means either a bad format or a date outside the acceptable range ([.acceptableDateRange].
     */
    fun parse(millis: Long): DateTime {
        val dateTime = DateTime(millis)
        checkDateRange(dateTime)
        return dateTime
    }

// ---------------------------------- range

    /**
     * @param dt a date
     * @return true if `dt` is in the acceptable range, false otherwise (see [.acceptableDateRange])
     */
    fun isInRange(dt: DateTime): Boolean {
        return (minDate == null || dt.isBefore(minDate)) && (maxDate == null || dt.isAfter(maxDate))
    }

    /**
     * @param dt a date
     * @throws IllegalArgumentException if `dt` is not in the acceptable range
     */
    fun checkDateRange(dt: DateTime) {
        minDate?.let { min ->
            if (dt.isBefore(min))
                throw IllegalArgumentException("Date '${format(dt)}' before the minimal acceptable date ${format(min)}")
        }
        maxDate?.let { max ->
            if (dt.isAfter(max))
                throw IllegalArgumentException("Date '${format(dt)}' after the maximal acceptable date ${format(max)}")
        }
    }

// ---------------------------------- format

    /**
     * @param pattern the pattern to use for the formatter
     * @return a new formatter configured to use the given format pattern
     */
    fun getFormatter(pattern: String = defaultPattern): DateTimeFormatter = DateTimeFormat.forPattern(pattern)


    /**
     * @param dt a date
     * @return the date as an ISO string, using the default format (see [.defaultFormat])
     */
    fun format(dt: DateTime): String = defaultFormatter.print(dt)


    /**
     * @param dt the date as millis since epoch
     * @return the formatted date
     * see [.format]
     */
    fun format(dt: Long): String = defaultFormatter.print(dt)


    /**
     * @param dt the date
     * @return the formatted date
     * see [.format]
     */
    fun format(dt: Date): String = defaultFormatter.print(DateTime(dt))

}