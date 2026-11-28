package com.example.secretsmanager.internal

import com.example.secretsmanager.SecretsManagementService
import com.example.secretsmanager.SecretsManagementServiceException
import io.github.awskotlin.secretsmanager.SecretsManagerTemplate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AwsSecretsManagementService(private val template: SecretsManagerTemplate,) : SecretsManagementService {
    companion object {
        private val log = LoggerFactory.getLogger(SecretsManagementService::class.java)
    }

    override suspend fun write(secretName: String, secretValue: String, secretDescription: String?): String? {
        log.debug("writing secrets: $secretName, $secretValue, $secretDescription")
        val response = try {
            template.secret(secretName).create(secretValue) {
                forceOverwriteReplicaSecret = true
                secretDescription?.let { this.description = it }
            }
        } catch (e: Exception) {
            log.error("error occurred when writing secrets: ${e.message}")
            throw SecretsManagementServiceException(e.message ?: "Failed to write secrets:")
        }
        return response.arn
    }

    override suspend fun read(secretName: String): String? {
        log.debug("read secrets by name: $secretName")
        return try {
            template.secret(secretName).get().secretString
        } catch (e: Exception) {
            log.error("error occurred when reading secrets: ${e.message}")
            throw SecretsManagementServiceException(e.message ?: "Failed to read secrets")
        }
    }
}
