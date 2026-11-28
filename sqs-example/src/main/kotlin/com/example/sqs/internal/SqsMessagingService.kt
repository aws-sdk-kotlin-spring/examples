package com.example.sqs.internal

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.GetQueueUrlRequest
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import com.example.sqs.MessagingService
import com.example.sqs.MessagingServiceException
import io.github.awskotlin.sqs.SqsMessageConverter
import io.github.awskotlin.sqs.SqsTemplate
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

@Service
class SqsMessagingService(
    private val template: SqsTemplate,
    private val sqsClient: SqsClient,
    private val converter: SqsMessageConverter,
) : MessagingService {
    companion object {
        private val log = LoggerFactory.getLogger(SqsMessagingService::class.java)
        const val DEFAULT_SQS_QUEUE_NAME = "default"
        const val MAX_NUMBER_OF_RECEIVED_MESSAGES = 5
    }

    @PostConstruct
    fun init() = runBlocking {
        log.debug("initializing SqsMessagingService...")
        val queueNameVal = createQueue(DEFAULT_SQS_QUEUE_NAME)
        log.debug("created queue: $queueNameVal")
    }

    override suspend fun createQueue(queueNameVal: String): String {
        return getQueue(queueNameVal) ?: run {
            log.debug("queue $queueNameVal does not exist, create one")
            val createQueueResponse =
                try {
                    template.queue(queueNameVal).create()
                } catch (e: Exception) {
                    throw MessagingServiceException(e.message ?: "Failed to create queue: $queueNameVal")
                }
            log.debug("created queue url ${createQueueResponse.queueUrl}")
            return createQueueResponse.queueUrl!!
        }
    }

    override suspend fun getQueue(queueNameVal: String): String? {
        log.debug("get queue by name: $queueNameVal")
        val getQueueUrlRequest =
            GetQueueUrlRequest {
                queueName = queueNameVal
            }

        val getQueueUrlResponse =
            withContext(Dispatchers.IO) {
                try {
                    sqsClient.getQueueUrl(getQueueUrlRequest)
                } catch (_: Exception) {
                    null
                }
            }
        return getQueueUrlResponse?.queueUrl
    }

    override suspend fun send(queueNameVal: String, message: Any) {
        val sendMessageResponse =
            try {
                template.queue(queueNameVal).send(message) { delay = 1.seconds }
            } catch (e: Exception) {
                throw MessagingServiceException(e.message ?: "Failed to send message")
            }
        log.debug("message ${sendMessageResponse.messageId} is sent out")
    }

    // Keep the raw client request for tolerant per-message conversion (skip bodies that fail
    // to deserialize) and leave messages in flight; the template's receive(...).list() converts
    // strictly and would throw on the first bad body.
    override suspend fun <T : Any> receive(queueNameVal: String, clazz: Class<out T>): List<T> {
        val getQueueUrl = getQueue(queueNameVal)

        val receiveMessageRequest =
            ReceiveMessageRequest {
                queueUrl = getQueueUrl
                maxNumberOfMessages = MAX_NUMBER_OF_RECEIVED_MESSAGES
            }

        val receiveMessageResponse =
            withContext(Dispatchers.IO) {
                try {
                    sqsClient.receiveMessage(receiveMessageRequest)
                } catch (e: Exception) {
                    throw MessagingServiceException(e.message ?: "Failed to receive message")
                }
            }
        return receiveMessageResponse
            .messages
            ?.mapNotNull { message ->
                log.debug("received message: $message")
                runCatching { converter.fromBody(message.body ?: "", clazz) }.getOrNull()
            } ?: emptyList()
    }
}
