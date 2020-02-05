package ch.derlin.bbdata

/**
 * date: 05.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


fun String.isBBDataDatetime(): Boolean = this.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}".toRegex())
