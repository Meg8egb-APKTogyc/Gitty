package dev.gitty.service

import dev.gitty.dto.n8n.N8nAnalysisResponse
import dev.gitty.telegram.TelegramBotService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class NotificationDispatcher(
    private val telegramBotService: TelegramBotService
) {
    private val logger: Logger = LogManager.getLogger(NotificationDispatcher::class.java)

    @Async
    fun sendCommitNotification(
        chatId: Long,
        repositoryName: String,
        ownerName: String,
        branch: String,
        authorName: String,
        message: String,
        url: String,
        analysisResult: String
    ) {
        try {
            val commitInfo = """
                📌 **Репозиторий:** $ownerName/$repositoryName
                🌿 **Ветка:** $branch
                👤 **Автор:** $authorName
                💬 **Сообщение:** $message
                🔗 **Ссылка:** $url
            """.trimIndent()

            telegramBotService.sendCodeReview(chatId, commitInfo, analysisResult)
            logger.info("Notification sent to chat $chatId for commit: $message")
        } catch (e: Exception) {
            logger.error("Failed to send notification to chat $chatId: ${e.message}", e)
        }
    }
}