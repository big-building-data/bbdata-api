package ch.derlin.bbdata.output.dates

import org.joda.time.DateTime

import org.joda.time.MutablePeriod

import org.joda.time.format.PeriodFormatterBuilder
import java.util.*


/**
 * date: 27.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
object DurationParser {

    private val parser = PeriodFormatterBuilder()
            .appendDays().appendSuffix("d")
            .appendHours().appendSuffix("h")
            .appendMinutes().appendSuffix("m")
            .appendSeconds().appendSuffix("s")
            .toParser()

    /**
     * Parse a duration string.
     *
     * @param periodString the duration string
     * @return the period object, or a zero period if the parsing failed.
     */
    fun parse(durationString: String): MutablePeriod {
        val periodString = durationString.replace("[- ]".toRegex(), "")
        val period = MutablePeriod()
        val offset = parser.parseInto(period, periodString, 0, Locale.getDefault())
        return if (offset != periodString.length) MutablePeriod() else period
    }

    /**
     * Check if the period is null, i.e. of 0.
     *
     * @param period the period to check
     * @return true if the period is 0, false otherwise
     */
    fun isZeroPeriod(period: MutablePeriod): Boolean = period.toString() == "PT0S"

    /**
     * Create a [DateTime] by adding the given duration to the current
     * time.
     *
     * @param periodString the duration string
     * @return the date object, or null if the period is invalid or 0
     */
    fun parseIntoDateOrNull(periodString: String): DateTime? {
        val period = parse(periodString)
        return if (isZeroPeriod(period)) null else DateTime().plus(period)
    }

    /**
     * Create a [DateTime] by adding the given duration to the current
     * time.
     *
     * @param periodString the duration string
     * @return the date object
     * @throws IllegalArgumentException if the specified period is 0
     */
    @Throws(IllegalArgumentException::class)
    fun parseIntoDate(periodString: String): DateTime {
        return parseIntoDateOrNull(periodString)
                ?: throw IllegalArgumentException("periodString '$periodString' : invalid format. Example: 1d-4h.")
    }
}
