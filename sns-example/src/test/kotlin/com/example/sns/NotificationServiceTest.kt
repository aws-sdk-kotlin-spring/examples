package com.example.sns

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.createQueue
import aws.sdk.kotlin.services.sqs.getQueueAttributes
import aws.sdk.kotlin.services.sqs.model.QueueAttributeName
import aws.sdk.kotlin.services.sqs.receiveMessage
import aws.sdk.kotlin.services.sqs.setQueueAttributes
import com.example.sns.internal.AwsNotificationService
import io.github.awskotlin.core.autoconfigure.AwsKotlinAutoConfiguration
import io.github.awskotlin.sns.SnsNotificationParser
import io.github.awskotlin.sns.SnsNotificationType
import io.github.awskotlin.sns.autoconfigure.SnsAutoConfiguration
import io.github.awskotlin.sqs.autoconfigure.SqsAutoConfiguration
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(classes = [NotificationServiceTest.TestConfig::class])
@Testcontainers
@EnabledIfEnvironmentVariable(named = "LOCALSTACK_AUTH_TOKEN", matches = ".+")
class NotificationServiceTest @Autowired constructor(
    val notificationService: NotificationService,
    val sqsClient: SqsClient,
) {
    companion object {
        @JvmField
        @Container
        val localstack: LocalStackContainer =
            LocalStackContainer(DockerImageName.parse("localstack/localstack:4"))
                .withServices("sns", "sqs")
                .apply {
                    System.getenv("LOCALSTACK_AUTH_TOKEN")?.takeIf { it.isNotBlank() }
                        ?.let { withEnv("LOCALSTACK_AUTH_TOKEN", it) }
                }

        @JvmStatic
        @DynamicPropertySource
        fun localstackProperties(registry: DynamicPropertyRegistry) {
            registry.add("awskotlin.endpoint") { localstack.endpoint.toString() }
        }
    }

    @Configuration
    @Import(AwsNotificationService::class)
    @ImportAutoConfiguration(
        AwsKotlinAutoConfiguration::class,
        SnsAutoConfiguration::class,
        SqsAutoConfiguration::class,
    )
    internal class TestConfig

    // JUnit 6 supports suspend test functions natively.
    @Test
    suspend fun `publish delivers the SNS envelope to a subscribed queue`() {
        val fanOut = newFanOut()
        notificationService.subscribeQueue(fanOut.topicArn, fanOut.queueArn)

        notificationService.publish(fanOut.topicArn, "Hello, SNS!", subject = "greeting")

        val notification = SnsNotificationParser.parse(receiveBody(fanOut.queueUrl))
        notification.type shouldBe SnsNotificationType.Notification
        notification.message shouldBe "Hello, SNS!"
        notification.topicArn shouldBe fanOut.topicArn
        notification.subject shouldBe "greeting"
    }

    @Test
    suspend fun `raw message delivery bypasses the envelope`() {
        val fanOut = newFanOut()
        notificationService.subscribeQueue(fanOut.topicArn, fanOut.queueArn, rawMessageDelivery = true)

        notificationService.publish(fanOut.topicArn, "raw-body")

        receiveBody(fanOut.queueUrl) shouldBe "raw-body"
    }

    private suspend fun newFanOut(): FanOut {
        val suffix = UUID.randomUUID().toString().substring(0, 8)
        val topicArn = notificationService.createTopic("example-fanout-$suffix")
        val queueUrl = sqsClient.createQueue { queueName = "example-fanout-$suffix" }.queueUrl!!
        val queueArn = sqsClient.getQueueAttributes {
            this.queueUrl = queueUrl
            attributeNames = listOf(QueueAttributeName.QueueArn)
        }.attributes!![QueueAttributeName.QueueArn]!!

        // Real AWS (and policy-enforcing emulators) require the queue to allow the topic to send to it.
        sqsClient.setQueueAttributes {
            this.queueUrl = queueUrl
            attributes = mapOf(QueueAttributeName.Policy to sendPolicy(topicArn, queueArn))
        }
        return FanOut(topicArn, queueUrl, queueArn)
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

    /** Fan-out delivery is asynchronous, so long-poll until a message arrives. */
    private suspend fun receiveBody(queueUrl: String): String {
        var body: String? = null
        eventually(60.seconds) {
            val message = sqsClient.receiveMessage {
                this.queueUrl = queueUrl
                waitTimeSeconds = 5
                maxNumberOfMessages = 1
            }.messages?.firstOrNull()
            body = message?.body
            body shouldNotBe null
        }
        return body!!
    }

    private data class FanOut(val topicArn: String, val queueUrl: String, val queueArn: String)
}
