package ch.derlin.bbdata

import ch.derlin.bbdata.output.security.SecurityConstants

/**
 * date: 05.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

const val ROOT_ID = 1
const val REGULAR_USER_ID = 2
const val NO_RIGHTS_USER_ID = 3

val ROOT_USER = mapOf("id" to ROOT_ID, "name" to "admin", "password" to "testtest", "group" to "SUPERADMIN")
val REGULAR_USER = mapOf("id" to REGULAR_USER_ID, "name" to "regular_user", "password" to "testtest", "group" to "regular")

fun APIKEY(userId: Int, ro: Boolean = false) = if (ro) "ro$userId" else "wr$userId"
fun TOKEN(userId: Int) = "012345678901234567890123456789a$userId"

const val UNSECURED_ROOT = "UNSECURED_BBUSER=$ROOT_ID"
const val UNSECURED_REGULAR = "UNSECURED_BBUSER=$REGULAR_USER_ID"
const val NO_KAFKA = "BB_NO_KAFKA=true"

const val HU = SecurityConstants.HEADER_USER
const val HA = SecurityConstants.HEADER_TOKEN


