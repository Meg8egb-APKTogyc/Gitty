package dev.gitty.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class N8nIntegrationServ(
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate
) {
    private val logger: Logger = LogManager.getLogger(N8nIntegrationService::class.java)

    @Value("\${n8n.webhook-url:}")
    private lateinit var n8nWebhookUrl: String

    fun analyzeCommit(commitMessage: String, code: String): String {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
        }

        val requestBody = mapOf(
            "commit_message" to commitMessage,
            "code" to code
        )

        logger.info("📤 Отправка запроса в n8n: $n8nWebhookUrl")

        val entity = HttpEntity(requestBody, headers)

        return try {
            val response: ResponseEntity<String> = restTemplate.exchange(
                n8nWebhookUrl,
                HttpMethod.POST,
                entity,
                String::class.java
            )

            logger.info("📥 Получен ответ от n8n. Статус: ${response.statusCode}")

            if (response.statusCode == HttpStatus.OK && response.body != null) {
                val responseBody = response.body!!
                logger.info("📄 Длина ответа: ${responseBody.length} символов")
                logger.info("📊 Префикс ответа: ${responseBody.take(200)}...")

                parseN8nResponse(responseBody)
            } else {
                logger.warn("⚠️ n8n вернул неожиданный статус: ${response.statusCode}")
                "⚠️ Ошибка анализа: HTTP ${response.statusCode}"
            }
        } catch (e: Exception) {
            logger.error("❌ Ошибка соединения с n8n: ${e.message}", e)
            "⚠️ Ошибка соединения с анализатором: ${e.message}"
        }
    }

    private fun parseN8nResponse(response: String): String {
        return try {
            val jsonNode = objectMapper.readTree(response)

            logger.info("✅ Успешно распарсен JSON от n8n")
            logger.info("📊 Структура JSON:")
            logger.info("  - Есть поле 'summary': ${jsonNode.has("summary")}")
            logger.info("  - Есть поле 'ai_review': ${jsonNode.has("ai_review")}")
            logger.info("  - Есть поле 'files': ${jsonNode.has("files")}")

            if (jsonNode.has("summary") && jsonNode.has("ai_review")) {
                formatAnalysisResponse(jsonNode)
            } else {
                logger.warn("⚠️ Неожиданный формат ответа от n8n")
                cleanPlainTextResponse(response)
            }
        } catch (e: Exception) {
            logger.error("❌ Ошибка парсинга JSON от n8n: ${e.message}")
            cleanPlainTextResponse(response)
        }
    }

    private fun formatAnalysisResponse(jsonNode: JsonNode): String {
        val result = StringBuilder()

        val summary = jsonNode.get("summary")
        val overallScore = summary.get("overallScore")?.asInt() ?: 75
        val complexity = summary.get("complexity")?.asText() ?: "UNKNOWN"
        val riskLevel = summary.get("riskLevel")?.asText() ?: "MEDIUM"

        result.append("📊 **Анализ кода**\n\n")
        result.append("**Общая оценка:** $overallScore/100\n")
        result.append("**Сложность:** $complexity\n")
        result.append("**Уровень риска:** $riskLevel\n\n")

        jsonNode.get("ai_review")?.asText()?.let { aiReview ->
            result.append("🤖 **AI обзор:**\n")
            result.append("$aiReview\n\n")
        }

        if (jsonNode.has("files") && jsonNode.get("files").isArray) {
            val files = jsonNode.get("files")
            if (files.size() > 0) {
                result.append("📁 **Проанализированные файлы:**\n")
                files.forEachIndexed { index, file ->
                    val fileName = file.get("file")?.asText() ?: "Неизвестный файл"
                    val score = file.get("score")?.asInt() ?: 0
                    result.append("  ${index + 1}. $fileName - $score/100\n")

                    // Добавляем issues для файла
                    if (file.has("issues") && file.get("issues").isArray) {
                        file.get("issues").forEach { issue ->
                            val issueType = issue.get("type")?.asText() ?: "ISSUE"
                            val description = issue.get("description")?.asText() ?: ""
                            val emoji = when (issueType) {
                                "SECURITY" -> "🔒"
                                "STYLE" -> "🎨"
                                "MAINTAINABILITY" -> "🔧"
                                else -> "ℹ️"
                            }
                            result.append("     $emoji $description\n")
                        }
                    }
                }
                result.append("\n")
            }
        }

        if (jsonNode.has("generalIssues") && jsonNode.get("generalIssues").isArray) {
            val generalIssues = jsonNode.get("generalIssues")
            if (generalIssues.size() > 0) {
                result.append("⚠️ **Общие рекомендации:**\n")
                generalIssues.forEachIndexed { index, issue ->
                    val description = issue.get("description")?.asText() ?: ""
                    val suggestion = issue.get("suggestion")?.asText() ?: ""
                    result.append("  ${index + 1}. $description\n")
                    result.append("     💡 $suggestion\n")
                }
            }
        }

        return result.toString()
    }

    private fun cleanPlainTextResponse(text: String): String {
        var cleaned = text.trim()
        if (cleaned.length > 2000) {
            cleaned = cleaned.take(2000) + "..."
        }
        return cleaned
    }
}