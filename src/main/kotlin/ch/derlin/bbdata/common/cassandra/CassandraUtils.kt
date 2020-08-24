package ch.derlin.bbdata.common.cassandra

import org.joda.time.YearMonth

/**
 * date: 19.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


object CassandraUtils {
    val YM_FORMAT = org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM")

    fun xMonthsFrom(d1: YearMonth, x: Int): List<String> {
        val ym = YearMonth(d1)
        return monthsBetween(ym.minusMonths(x), ym)
    }

    fun monthsBetween(d1: YearMonth, d2: YearMonth? = null): List<String> {
        val months = mutableListOf<String>()
        var d2 = d2 ?: YearMonth.now()
        while (d1.compareTo(d2) <= 0) {
            months.add(YM_FORMAT.print(d2))
            d2 = d2.minusMonths(1)
        }
        return months
    }
}