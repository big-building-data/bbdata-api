package ch.derlin.bbdata

/**
 * date: 19.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

object Profiles {

    // USED IN CODE
    // Makes sense for the output api only: "unsecured" will disable the bbuser/bbtoken check and
    // automatically set bbuser=1
    const val UNSECURED = "unsecured"
    const val NOT_UNSECURED = "!$UNSECURED"

    // CONFIGURED THROUGH PROPERTIES FILES (but can be used in tests)
    // Turn off the cassandra entrypoints (this means neither /values nor input endpoints)
    const val NO_CASSANDRA = "noc" // constant used in tests
    const val CASSANDRA = "!$NO_CASSANDRA"

    // Do not register components under bbdata.api.input
    const val OUTPUT_ONLY = "output"
    // Do not register components under bbdata.api.output
    const val INPUT_ONLY = "input"

    // where do you store statistics about objects
    const val SQL_STATS = "sqlstats"
    const val CASSANDRA_STATS = "!$SQL_STATS"

    // optionally enable caching
    const val CACHING = "caching"
}