package ch.derlin.bbdata.actuators

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * date: 28.09.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@Component
class CustomMetrics(private val meterRegistry: MeterRegistry) {

    /** Count the number of rejected measures in input api*/
    fun reject(objectId: Long) = meterRegistry
            .summary("ch.derlin.input.rejected",
                    "object_id", objectId.toString()
            ).record(1.0)

    /** Count the number of failed login /login */
    fun loginFailed(username: String) = meterRegistry
            .summary("ch.derlin.login.failed",
                    "username", username
            ).record(1.0)

    /** Count the number of failed authentication (wrong bbuser+bbtoken, not recording read-only apikeys on write endpoints) */
    fun authFailed(userId: Int) = meterRegistry
            .summary("ch.derlin.auth.failed",
                    "user_id", userId.toString()
            ).record(1.0)
}