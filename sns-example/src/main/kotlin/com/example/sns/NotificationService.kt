package com.example.sns

interface NotificationService {
    suspend fun createTopic(topicName: String): String

    suspend fun publish(topicArn: String, message: String, subject: String? = null): String?

    suspend fun subscribeQueue(topicArn: String, queueArn: String, rawMessageDelivery: Boolean = false): String?
}

data class NotificationServiceException(val error: String) : RuntimeException(error)
