package com.example.sns.internal

import io.github.awskotlin.sns.SnsNotificationParser
import io.github.awskotlin.sqs.listener.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NotificationReceiver {
    companion object {
        private val log = LoggerFactory.getLogger(NotificationReceiver::class.java)
    }

    // Without raw message delivery the queue receives the SNS JSON envelope as a String.
    @SqsListener(FanOutInitializer.QUEUE_NAME)
    suspend fun onNotification(body: String) {
        val notification = SnsNotificationParser.parse(body)
        log.debug("received notification ${notification.messageId}: ${notification.message}")
        log.debug("like an event listener, do side effect...")
    }
}
