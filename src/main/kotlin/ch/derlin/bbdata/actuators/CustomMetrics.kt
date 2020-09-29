package ch.derlin.bbdata.actuators

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

/**
 * date: 28.09.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Component
class CustomMetrics(private val meterRegistry: MeterRegistry) {

    private val rejectCounterBuilder = Counter
            .builder("ch.derlin.input.rejected")
            .description("The total umber of rejected measures by the input endpoints")

    private val loginFailedBuilder = Counter
            .builder("ch.derlin.login.failed")
            .description("Total number of failed logins (POST /login with wrong username/password)")

    private val authFailedBuilder = Counter
            .builder("ch.derlin.auth.failed")
            .description("Total number of failed authentication (wrong bbuser/bbtoken in headers)")

    /** Count the number of rejected measures in input api*/
    fun reject(objectId: Long) = rejectCounterBuilder
            .tag("object_id", objectId.toString())
            .register(meterRegistry)
            .increment()

    /** Count the number of failed login /login */
    fun loginFailed(username: String) = loginFailedBuilder
            .tag("username", username)
            .register(meterRegistry)
            .increment()

    /** Count the number of failed authentication (wrong bbuser+bbtoken, not recording read-only apikeys on write endpoints) */
    fun authFailed(userId: Int) = authFailedBuilder
            .tag("user_id", userId.toString())
            .register(meterRegistry)
            .increment()

    /** Wrap an executor so stats are available in metrics */
    fun monitoredThreadExecutor(executor: Executor): Executor =
           ExecutorServiceMetrics.monitor(meterRegistry, executor, "AsyncExecutor", "async")

}