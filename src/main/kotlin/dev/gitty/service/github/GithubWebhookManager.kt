package dev.gitty.service.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Service
class GithubWebhookManager(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger: Logger = LogManager.getLogger(GithubWebhookManager::class.java)

    private val githubApiBase = "https://api.github.com"

    /**
     * Создает вебхук для репозитория
     */
    fun createWebhook(
        owner: String,
        repo: String,
        githubToken: String,
        webhookUrl: String
    ): GithubWebhookResponse? {
        val url = "$githubApiBase/repos/$owner/$repo/hooks"

        val headers = HttpHeaders().apply {
            set("Authorization", "token $githubToken")
            set("Accept", "application/vnd.github.v3+json")
            set("User-Agent", "Gitty-Code-Analyzer")
        }

        val webhookConfig = mapOf(
            "url" to webhookUrl,
            "content_type" to "json",
            "insecure_ssl" to "0",
            "secret" to generateWebhookSecret()
        )

        val requestBody = mapOf(
            "name" to "web",
            "active" to true,
            "events" to listOf("push", "pull_request"),
            "config" to webhookConfig
        )

        logger.info("Creating webhook for $owner/$repo to $webhookUrl")

        val entity = HttpEntity(requestBody, headers)

        return try {
            val response: ResponseEntity<String> = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String::class.java
            )

            if (response.statusCode.is2xxSuccessful) {
                val responseBody = response.body ?: return null
                val webhookResponse = objectMapper.readValue(responseBody, GithubWebhookResponse::class.java)
                logger.info("Webhook created successfully: ${webhookResponse.id}")
                return webhookResponse
            } else {
                logger.error("Failed to create webhook: ${response.statusCode} - ${response.body}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error creating webhook: ${e.message}", e)
            null
        }
    }

    /**
     * Удаляет вебхук
     */
    fun deleteWebhook(
        owner: String,
        repo: String,
        githubToken: String,
        webhookId: Long
    ): Boolean {
        val url = "$githubApiBase/repos/$owner/$repo/hooks/$webhookId"

        val headers = HttpHeaders().apply {
            set("Authorization", "token $githubToken")
            set("Accept", "application/vnd.github.v3+json")
        }

        val entity = HttpEntity<Void>(headers)

        return try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void::class.java)
            logger.info("Webhook $webhookId deleted successfully")
            true
        } catch (e: Exception) {
            logger.error("Error deleting webhook: ${e.message}", e)
            false
        }
    }

    /**
     * Получает список вебхуков репозитория
     */
    fun listWebhooks(
        owner: String,
        repo: String,
        githubToken: String
    ): List<GithubWebhookResponse> {
        val url = "$githubApiBase/repos/$owner/$repo/hooks"

        val headers = HttpHeaders().apply {
            set("Authorization", "token $githubToken")
            set("Accept", "application/vnd.github.v3+json")
        }

        val entity = HttpEntity<Void>(headers)

        return try {
            val response: ResponseEntity<String> = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String::class.java
            )

            if (response.statusCode.is2xxSuccessful) {
                val responseBody = response.body ?: return emptyList()
                objectMapper.readValue(responseBody, Array<GithubWebhookResponse>::class.java).toList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error listing webhooks: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Проверяет, существует ли уже вебхук для нашего URL
     */
    fun webhookExists(
        owner: String,
        repo: String,
        githubToken: String,
        webhookUrl: String
    ): GithubWebhookResponse? {
        val webhooks = listWebhooks(owner, repo, githubToken)
        return webhooks.firstOrNull { it.config.url == webhookUrl }
    }

    /**
     * Генерирует секретный ключ для вебхука
     */
    private fun generateWebhookSecret(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GithubWebhookResponse(
        val id: Long,
        val url: String,
        val config: WebhookConfig,
        val events: List<String>,
        val active: Boolean
    ) {
        data class WebhookConfig(
            val url: String,
            val content_type: String,
            val insecure_ssl: String,
            val secret: String?
        )
    }
}