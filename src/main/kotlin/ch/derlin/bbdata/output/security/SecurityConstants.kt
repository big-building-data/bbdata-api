package ch.derlin.bbdata.output.security

/**
 * date: 27.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
object SecurityConstants {
    // security headers (-H "<header>:<value>")
    const val HEADER_USER = "bbuser"
    const val HEADER_TOKEN = "bbtoken"

    // security scopes (e.g. @Protected(SecurityConstants.SCOPE_WRITE))
    // to annotate controller requests...
    const val SCOPE_READ = "read" // ... needing readonly apikeys
    const val SCOPE_WRITE = "write" // ... needing read/write apikeys

    // SUDO group: admins of this userGroup have full access to ANY resource in read/write
    const val SUPERADMIN_GROUP = 1
}
