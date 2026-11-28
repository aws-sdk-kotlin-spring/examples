package com.example.sns.internal

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.createQueue
import aws.sdk.kotlin.services.sqs.getQueueAttributes
import aws.sdk.kotlin.services.sqs.model.QueueAttributeName
import aws.sdk.kotlin.services.sqs.setQueueAttributes
import com.example.sns.NotificationService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Wires the SNS -> SQS fan-out at startup. The listener container fails on missing
 * queues, so the topic, queue, queue policy and subscription must exist before it starts.
 */
@Component
class FanOutInitializer(private val notificationService: NotificationService, private val sqsClient: SqsClient,) {
    companion object {
        private val log = LoggerFactory.getLogger(FanOutInitializer::class.java)
        const val TOPIC_NAME = "notifications"
        const val QUEUE_NAME = "notifications"
    }

    @PostConstruct
    fun init() = runBlocking {
        val topicArn = notificationService.createTopic(TOPIC_NAME)
        val queueUrl = sqsClient.createQueue { queueName = QUEUE_NAME }.queueUrl!!
        val queueArn = sqsClient.getQueueAttributes {
            this.queueUrl = queueUrl
            attributeNames = listOf(QueueAttributeName.QueueArn)
        }.attributes!![QueueAttributeName.QueueArn]!!

        // The queue must allow the topic to send to it (sqs:SendMessage with a SourceArn condition).
        sqsClient.setQueueAttributes {
            this.queueUrl = queueUrl
            attributes = mapOf(QueueAttributeName.Policy to sendPolicy(topicArn, queueArn))
        }
        notificationService.subscribeQueue(topicArn, queueArn)
        log.debug("fan-out ready: topic $topicArn -> queue $queueUrl")
    }

    private fun sendPolicy(topicArn: String, queueArn: String): String =
        """
        {
          "Version": "2012-10-17",
          "Statement": [{
            "Effect": "Allow",
            "Principal": {"Service": "sns.amazonaws.com"},
            "Action": "sqs:SendMessage",
            "Resource": "$queueArn",
            "Condition": {"ArnEquals": {"aws:SourceArn": "$topicArn"}}
          }]
        }
        """.trimIndent()
}
