package ch.derlin.bbdata.common.cassandra

import org.joda.time.YearMonth
import org.joda.time.format.DateTimeFormatter

/**
 * date: 19.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


object CassandraUtils {
    val YM_FORMAT: DateTimeFormatter = org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM")

    fun xMonthsFrom(d1: YearMonth, x: Int): List<String> {
        val ym = YearMonth(d1)
        return monthsBetween(ym.minusMonths(x), ym)
    }

    fun monthsBetween(d1: YearMonth, optionalD2: YearMonth? = null): List<String> {
        val months = mutableListOf<String>()
        var d2 = optionalD2 ?: YearMonth.now()
        while (d1 <= d2) {
            months.add(YM_FORMAT.print(d2))
            d2 = d2.minusMonths(1)
        }
        return months
    }
}