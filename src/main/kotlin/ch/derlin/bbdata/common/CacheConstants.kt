package ch.derlin.bbdata.common

/**
 * date: 05.08.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
object CacheConstants {
    // name of the cache used by the input api to store metadata
    const val CACHE_NAME = "metas"
    // separator to create compound keys, eg: <objectId>:<token>
    const val KEY_SEP = ":"
}