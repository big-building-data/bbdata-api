package ch.derlin.bbdata.output.api.auth

import java.security.MessageDigest
import java.security.SecureRandom


/**
 * date: 26.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

object PasswordDigest {

    @Throws(Exception::class)
    fun toMD5(password: String): String {
        val m = MessageDigest.getInstance("MD5")
        m.update(password.toByteArray(charset("UTF8")))
        val s = m.digest()
        var result = ""
        for (i in s.indices) {
            result += Integer.toHexString((0x000000ff and s[i].toInt()) or -0x100).substring(6)
        }
        return result
    }
}

object TokenGenerator {

    private val random = SecureRandom()

    /**
     * Generate a secured random token.
     * @param length the length of the token to generate
     * @return the token
     */
    fun generate(length: Int = 32): String {
        var token = ""
        while (token.length < length) {
            token += Integer.toHexString(random.nextInt())
        }
        return token.substring(0, length)
    }
}