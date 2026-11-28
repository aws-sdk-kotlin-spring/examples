package com.example.sqs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SqsExampleApplication

fun main(args: Array<String>) {
    runApplication<SqsExampleApplication>(*args)
}
