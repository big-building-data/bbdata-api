package ch.derlin.bbdata.output.dates

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import java.util.Date
import java.util.TimeZone


/**
 * Taken from JodaUtils (gitlab)
 *
 * date: 07.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
class JodaUtils {

    /**
     * available formats: they are standard ISO, but without the Z at the end. The date is assumed to be in UTC
     * (see [.setDefaultTimeZoneUTC].
     */
    enum class Format constructor(
            /**
             * @return the format as a string.
             */
            val value: String) {
        /**
         * minutes granularity. Format: `yyyy-MM-dd'T'HH:mm`
         */
        ISO_MINUTES("yyyy-MM-dd'T'HH:mm"),
        /**
         * seconds granularity. Format: `yyyy-MM-dd'T'HH:mm:ss`
         */
        ISO_SECONDS("yyyy-MM-dd'T'HH:mm:ss"),
        /**
         * milliseconds granularity. Format: `yyyy-MM-dd'T'HH:mm:ss.SSS`
         */
        ISO_MILLIS("yyyy-MM-dd'T'HH:mm:ss.SSS")
    }

    /**
     * @return the format currently used.
     */
    fun defaultFormat(): Format {
        return defaultPattern
    }

    /* *****************************************************************
     * interval
     * ****************************************************************/

    /**
     * Utility class to represent a pair of dates, with from &lt;= to.
     */
    class FromToPair
    /**
     * @param isoFrom an ISO string
     * @param isoTo   an ISO string
     * @throws IllegalArgumentException if a string is not a valid ISO date format (see [DateTime] constructor) or if `from > to`
     */
    @Throws(IllegalArgumentException::class)
    constructor(isoFrom: String, isoTo: String) {

        val from: DateTime
        val to: DateTime

        init {
            from = parse(isoFrom)
            to = parse(isoTo)
            if (from.isAfter(to)) {
                throw IllegalArgumentException(String.format("Date range error: From '%s' > To '%s'",
                        isoFrom, isoTo))
            }
        }
    }

    companion object {

        private var defaultPattern = Format.ISO_MILLIS
        private var defaultFormatter = getFormatter(defaultPattern)
        private var minDate: DateTime? = DateTime("0000-01-01")
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
         * Change the default pattern used. This method should be called at initialization time, not afterwards.
         *
         * @param pattern the new pattern to use by default.
         */
        fun defaultPattern(pattern: Format) {
            defaultPattern = pattern
            defaultFormatter = getFormatter(pattern)
        }

        /**
         * Treat dates outside of the interval from-to as incorrect dates,
         * Further calls to [.parse] will fail
         * with an [IllegalArgumentException] in case the string is in the correct format but outside this range.
         *
         * @param from minimal correct date, inclusive.
         * @param to   maximal correct date, exclusive.
         */
        fun acceptableDateRange(from: String, to: String? = null) {
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
            minDate?.let {
                if (it.isBefore(minDate))
                    throw IllegalArgumentException(String.format("Date '%s' before the minimal acceptable date %s",
                            format(dt), format(it)))
            }
            maxDate?.let {
                if (it.isAfter(maxDate))
                    throw IllegalArgumentException(String.format("Date '%s' after the maximal acceptable date %s",
                            format(dt), format(it)))
            }
        }

        // ---------------------------------- format

        /**
         * @param pattern the pattern to use for the formatter
         * @return a new formatter configured to use the given format pattern
         */
        fun getFormatter(pattern: Format = defaultPattern): DateTimeFormatter {
            return DateTimeFormat.forPattern(pattern.value)
        }

        /**
         * @param dt a date
         * @return the date as an ISO string, using the default format (see [.defaultFormat])
         */
        fun format(dt: DateTime): String {
            return defaultFormatter.print(dt)
        }

        /**
         * @param dt the date as millis since epoch
         * @return the formatted date
         * see [.format]
         */
        fun format(dt: Long): String {
            return defaultFormatter.print(dt)
        }

        /**
         * @param dt the date
         * @return the formatted date
         * see [.format]
         */
        fun format(dt: Date): String {
            return defaultFormatter.print(DateTime(dt))
        }
    }
}
/**
 * Treat dates before `from` as incorrect dates, i.e. further calls to [.parse] will fail
 * with an [IllegalArgumentException] in case the string is in the correct format but before the minDate.
 *
 * @param from  the minimal acceptable date
 */
