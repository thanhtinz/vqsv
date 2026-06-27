package com.vqsv

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class VqsvApplication

fun main(args: Array<String>) {
    runApplication<VqsvApplication>(*args)
}
