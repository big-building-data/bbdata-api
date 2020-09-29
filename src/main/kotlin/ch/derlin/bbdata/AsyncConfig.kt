package ch.derlin.bbdata

import ch.derlin.bbdata.actuators.CustomMetrics
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.task.TaskExecutorBuilder
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import java.lang.reflect.Method
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor


/**
 * Enable async in Spring, used mostly by StatsLogic (update stats asynchronously)
 * More info at https://www.baeldung.com/spring-async.
 *
 * Note: as of spring-boot 2.2.1, there is no need to define our own taskExecutor anymore (see article).
 * Indeed, TaskExecutorBuilder uses a ThreadPoolTaskExecutor by default, that can be completely configured
 * using properties: spring.task.executor.*
 * Moreover, one can implement TaskExecutorCustomizer for additional settings (see below).
 *
 * date: 08.09.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ConditionalOnProperty(AsyncProperties.ENABLED, havingValue = "true", matchIfMissing = true)
annotation class OnAsyncEnabled


@OnAsyncEnabled
@EnableAsync
@Configuration
class AsyncConfig(
        private val taskExecutorBuilder: TaskExecutorBuilder,
        // here, the lazy is VERY IMPORTANT ! Without it, most of the core metrics (jvm, etc) will be missing...
        // making the dep lazy will let the MeterRegistry load normally before first use
        // see https://github.com/micrometer-metrics/micrometer/issues/823 and
        // https://stackoverflow.com/questions/52148543/spring-boot-2-actuator-doesnt-publish-jvm-metric
        @Lazy private val customMetrics: CustomMetrics
) : AsyncConfigurer {


    @Value("\${spring.task.execution.pool.queue-capacity}")
    val queueCapacity: Int = -1

    val logger: Logger = LoggerFactory.getLogger(AsyncConfig::class.java)

    /**
     * Add monitoring of executor using micrometer + use CallerRunsPolicy when the queue if full
     * (that is, the job is handled synchronously).
     * Note that if we didn't need to monitor the executor, the code could be put into a TaskExecutorCustomizer
     * (see e.g. commit b9cb1860eeecea53f5b371408649420b450db2e4)
     */
    override fun getAsyncExecutor(): Executor {
        // create executor based on default spring-boot properties
        val executor = taskExecutorBuilder.build()
        // if the queue is full, the caller will execute the task => ensure it always gets executed
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        // we need to initialize it before calling monitor
        executor.initialize()
        // log info
        logger.info("Async executor set: class=${executor.javaClass.simpleName}, " +
                "coreSize=${executor.corePoolSize}, maxSize=${executor.maxPoolSize}, queueCapacity=${queueCapacity}")
        // monitor the executor (so it is available in metrics) (must be wrapped)
        return customMetrics.monitoredThreadExecutor(executor.threadPoolExecutor)
    }

    /**
     * Register a custom exception handler to ensure exceptions thrown asynchronously are still logged.
     * Attention: this will be called only on async method with void return type !
     */
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler? {
        return AsyncExceptionHandler()
    }
}

class AsyncExceptionHandler : AsyncUncaughtExceptionHandler {
    /**
     * Actual logger used for asynchronous uncaught exceptions (methods with void return type only)
     */

    val logger: Logger = LoggerFactory.getLogger(AsyncExceptionHandler::class.java)

    override fun handleUncaughtException(throwable: Throwable, method: Method, vararg params: Any) {
        val niceParams = params.take(2).joinToString(",") { it.toString() }
        logger.error("in ${method.name} with ${params.size} params: $niceParams ...", throwable)
    }
}