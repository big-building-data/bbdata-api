package ch.derlin.bbdata

/**
 * date: 05.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


fun String.isBBDataDatetime(): Boolean = this.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z".toRegex())

fun String.csv2map(): List<Map<String, String>> {
    val splits: List<List<String>> = this.lines().filter { it.isNotEmpty() }.map { it.split(',') }
    val headers = splits.first()
    return splits.drop(1).map { (headers zip it).toMap() }
}