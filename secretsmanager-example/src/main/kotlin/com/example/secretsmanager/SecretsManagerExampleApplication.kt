package com.example.secretsmanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SecretsManagerExampleApplication

fun main(args: Array<String>) {
    runApplication<SecretsManagerExampleApplication>(*args)
}
