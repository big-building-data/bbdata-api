package ch.derlin.bbdata.output

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
class OutputApiApplication

fun main(args: Array<String>) {
    runApplication<OutputApiApplication>(*args)
}
