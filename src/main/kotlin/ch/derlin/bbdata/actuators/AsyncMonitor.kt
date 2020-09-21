package ch.derlin.bbdata.actuators

import ch.derlin.bbdata.OnAsyncEnabled
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component

/**
 * date: 21.09.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Component
@OnAsyncEnabled
@WebEndpoint(id = "tasks")
class AsyncMonitor(private val taskExecutor: TaskExecutor? = null) {
    /** add an optional tasks actuator to monitor async executor (hidden) */

    @ReadOperation
    @Operation(description = "Actuator web endpoint 'tasks', monitor async tasks execution")
    @Hidden
    fun executorInfo(): Map<String, Any> {
        // use linkedMap to preserve insertion order in output
        val executorInfo = linkedMapOf<String, Any>()
        val tasksInfo = linkedMapOf<String, Any>()
        if (taskExecutor is ThreadPoolTaskExecutor)
            taskExecutor.threadPoolExecutor.let {
                executorInfo["pool-size"] = it.corePoolSize
                executorInfo["active-count"] = it.activeCount
                executorInfo["queue-size"] = it.queue.size
                tasksInfo["task-count"] = it.taskCount
                tasksInfo["completed-task-count"] = it.completedTaskCount
            }
        return linkedMapOf("executor" to executorInfo, "tasks" to tasksInfo)
    }
}

