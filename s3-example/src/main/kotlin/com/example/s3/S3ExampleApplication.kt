package com.example.s3

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class S3ExampleApplication

fun main(args: Array<String>) {
    runApplication<S3ExampleApplication>(*args)
}
