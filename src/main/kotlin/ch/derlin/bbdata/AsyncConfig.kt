package ch.derlin.bbdata

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.task.TaskExecutorCustomizer
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.lang.reflect.Method
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


@Component
class AsyncExecutorCustomizer : TaskExecutorCustomizer {
    /** Create a custom taskExecutor with CallerRuns policy */

    @Value("\${spring.task.execution.pool.queue-capacity}")
    val queueCapacity: Int = -1
    val logger: Logger = LoggerFactory.getLogger(AsyncExecutorCustomizer::class.java)

    override fun customize(taskExecutor: ThreadPoolTaskExecutor?) {
        taskExecutor?.let { executor ->
            // if the queue is full, the caller will execute the task => ensure it always gets executed
            executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            logger.info("Async executor set: class=${executor.javaClass.simpleName}, " +
                    "coreSize=${executor.corePoolSize}, maxSize=${executor.maxPoolSize}, queueCapacity=${queueCapacity}")
        }
    }
}

@OnAsyncEnabled
@EnableAsync
@Configuration
class AsyncConfig : AsyncConfigurer {
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