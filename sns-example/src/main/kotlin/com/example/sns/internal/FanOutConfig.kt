package com.example.sns.internal

import io.github.awskotlin.sns.SnsSqsFanOut
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Declares the SNS -> SQS fan-out bridge as a topic/queue pair. The starter's initializer
 * creates the topic, queue, queue policy and subscription before the listener container starts.
 */
@Configuration
class FanOutConfig {
    companion object {
        const val QUEUE_NAME = "notifications"
    }

    @Bean
    fun notificationsFanOut(): SnsSqsFanOut = SnsSqsFanOut(topic = "notifications", queue = QUEUE_NAME)
}
