package com.example.sns.internal

import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.createTopic
import com.example.sns.NotificationService
import com.example.sns.NotificationServiceException
import io.github.awskotlin.sns.SnsTemplate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AwsNotificationService(private val template: SnsTemplate, private val client: SnsClient,) : NotificationService {
    companion object {
        private val log = LoggerFactory.getLogger(NotificationService::class.java)
    }

    // The template takes a topic ARN only, so keep the raw starter-provided client for creation.
    override suspend fun createTopic(topicName: String): String {
        log.debug("creating topic: $topicName")
        val response = try {
            client.createTopic { name = topicName }
        } catch (e: Exception) {
            log.error("error occurred when creating topic: ${e.message}")
            throw NotificationServiceException(e.message ?: "Failed to create topic: $topicName")
        }
        return response.topicArn ?: throw NotificationServiceException("CreateTopic returned no topicArn")
    }

    override suspend fun publish(topicArn: String, message: String, subject: String?): String? {
        log.debug("publishing to topic: $topicArn")
        val response = try {
            template.topic(topicArn).publish(message, subject)
        } catch (e: Exception) {
            log.error("error occurred when publishing notification: ${e.message}")
            throw NotificationServiceException(e.message ?: "Failed to publish notification")
        }
        return response.messageId
    }

    override suspend fun subscribeQueue(topicArn: String, queueArn: String, rawMessageDelivery: Boolean): String? {
        log.debug("subscribing queue $queueArn to topic $topicArn (rawMessageDelivery=$rawMessageDelivery)")
        val response = try {
            template.topic(topicArn).subscribeSqs(queueArn, rawMessageDelivery)
        } catch (e: Exception) {
            log.error("error occurred when subscribing queue: ${e.message}")
            throw NotificationServiceException(e.message ?: "Failed to subscribe queue: $queueArn")
        }
        return response.subscriptionArn
    }
}
