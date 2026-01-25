package dev.gitty.telegram

import dev.gitty.service.repository.RepositoryConnectionService
import dev.gitty.service.telegram.TelegramSubscriptionService
import dev.gitty.service.repository.RepositoryService
import dev.gitty.service.github.GithubWebhookManager
import dev.gitty.service.user.TelegramUserService
import dev.gitty.service.user.UserService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class TelegramCommandHandler(
    private val telegramSubscriptionService: TelegramSubscriptionService,
    private val repositoryConnectionService: RepositoryConnectionService,
    private val repositoryService: RepositoryService,
    private val githubWebhookManager: GithubWebhookManager,
    private val telegramUserService: TelegramUserService,
    private val userService: UserService,
    private val env: Environment
) {
    private val logger: Logger = LogManager.getLogger(TelegramCommandHandler::class.java)

    fun handleConnect(
        chatId: Long,
        args: String?,
        telegramUserId: Long
    ): String {
        if (args == null) {
            return """
                ❌ Укажите URL репозитория:
                /connect <github_repo_url>
                
                Пример:
                /connect https://github.com/username/repo
                
                Для создания вебхука укажите токен через пробел:
                /connect https://github.com/username/repo ghp_yourtoken123
                """.trimIndent()
        }

        try {
            val parts = args.trim().split(' ', limit = 2)
            logger.info("Parts after split: $parts")
            logger.info("Parts size: ${parts.size}")

            val repoUrl = parts[0]
            val token = parts.getOrNull(1)?.takeIf { it.isNotBlank() }

            logger.info("Extracted repoUrl: '$repoUrl'")
            logger.info("Extracted token: '${token?.take(5)}...' (length: ${token?.length})")

            val repoInfo = repositoryConnectionService.extractRepoInfo(repoUrl)
                ?: return "❌ Неверный формат URL. Пример: https://github.com/username/repo"

            val existingRepo = repositoryService.findByGithubRepoId(repoInfo.fullName)
            if (existingRepo != null) {
                return "✅ Репозиторий уже подключен!\n" +
                        "📁 ${existingRepo.name}\n" +
                        "👤 ${existingRepo.ownerName}\n" +
                        "Используйте /list для просмотра"
            }

            val ngrokUrl = System.getenv("NGROK_URL") ?: "http://localhost:8080"

            val webhookUrl = "$ngrokUrl/api/webhook/github"

            val user = getUserOrCreateForTelegram(telegramUserId, chatId)

            logger.info("""
                Подключаем репозиторий: ${repoInfo.fullName}
                Webhook URL: $webhookUrl
                Token present: ${!token.isNullOrEmpty()}
            """.trimIndent())

            val repository = repositoryService.connectRepository(
                githubRepoId = repoInfo.fullName,
                name = repoInfo.repoName,
                ownerName = repoInfo.owner,
                userId = user.id,
                githubToken = token,
                webhookUrl = webhookUrl
            )

            telegramSubscriptionService.subscribe(
                chatId = chatId,
                repositoryId = repository.id,
                telegramUserId = telegramUserId
            )

            val response = StringBuilder().apply {
                append("✅ Репозиторий подключен!\n")
                append("📁 Имя: ${repository.name}\n")
                append("👤 Владелец: ${repository.ownerName}\n")
                append("🔗 GitHub: ${repository.githubRepoId}\n")

                if (token != null) {
                    if (repository.webhookId != null) {
                        append("\n🔗 Вебхук создан успешно! (ID: ${repository.webhookId})\n")
                        append("GitHub будет отправлять уведомления при пушах.\n")
                    } else {
                        append("\n⚠️ Вебхук НЕ создан!\n")
                        append("Возможные причины:\n")
                        append("1. Неверный или просроченный токен\n")
                        append("2. Недостаточно прав у токена (нужны repo, admin:repo_hook)\n")
                        append("3. Ошибка сети или GitHub API\n")
                    }
                } else {
                    append("\nℹ️ Вебхук не создан (не указан токен)\n")
                    append("Чтобы получать уведомления, укажите токен:\n")
                    append("/connect <url> <ваш_github_token>\n")
                }

                append("\n✅ Вы подписаны на уведомления!")
            }

            return response.toString()
        } catch (e: Exception) {
            logger.error("Error connecting repository: ${e.message}", e)
            return "❌ Ошибка подключения репозитория: ${e.message}"
        }
    }

    fun handleSubscribe(telegramUserId: Long, chatId: Long): String {
        return try {
            telegramSubscriptionService.subscribeToAllRepositories(
                telegramUserId = telegramUserId,
                chatId = chatId
            )
            "✅ Вы подписались на все репозитории!"
        } catch (e: Exception) {
            logger.error("Error subscribing: ${e.message}", e)
            "❌ Ошибка подписки: ${e.message}"
        }
    }

    fun handleUnsubscribe(chatId: Long): String {
        return try {
            telegramSubscriptionService.unsubscribeAll(chatId)
            "✅ Вы отписались от всех репозиториев!"
        } catch (e: Exception) {
            logger.error("Error unsubscribing: ${e.message}", e)
            "❌ Ошибка отписки: ${e.message}"
        }
    }

    fun handleList(chatId: Long): String {
        return try {
            val subscriptions = telegramSubscriptionService.getSubscriptionsForChat(chatId)

            if (subscriptions.isEmpty()) {
                "📭 У вас нет подключенных репозиториев.\nИспользуйте /connect для подключения."
            } else {
                val reposList = subscriptions.joinToString("\n") { sub ->
                    "📁 ${sub.ownerName}/${sub.repositoryName}" +
                            if (sub.isActive) " ✅" else " ❌"
                }

                """
                📋 Ваши репозитории:
                
                $reposList
                
                Всего: ${subscriptions.size}
                """.trimIndent()
            }
        } catch (e: Exception) {
            logger.error("Error listing repositories: ${e.message}", e)
            "❌ Ошибка получения списка: ${e.message}"
        }
    }

    private fun getUserOrCreateForTelegram(telegramUserId: Long, chatId: Long): dev.gitty.model.User {
        return try {
            telegramUserService.getOrCreateTelegramUser(telegramUserId, chatId)
        } catch (e: Exception) {
            logger.error("Ошибка при получении/создании пользователя: ${e.message}", e)
            val telegramUsername = "telegram_$telegramUserId"
            userService.findByGithubUsername(telegramUsername)
                ?: userService.save(
                    dev.gitty.model.User(
                        githubId = telegramUserId.toString(),
                        githubUsername = telegramUsername,
                        email = "telegram_${telegramUserId}@example.com",
                        role = dev.gitty.model.UserRole.DEVELOPER
                    )
                )
        }
    }
}