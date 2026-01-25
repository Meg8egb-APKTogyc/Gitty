package dev.gitty.telegram

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import jakarta.annotation.PostConstruct
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Service

@Service
class TelegramBotService(
    private val telegramCommandHandler: TelegramCommandHandler
) {

    private val logger: Logger = LogManager.getLogger(TelegramBotService::class.java)

    private lateinit var bot: com.github.kotlintelegrambot.Bot

    @PostConstruct
    fun init() {
        val botToken = System.getenv("TELEGRAM_BOT_TOKEN")
            ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN environment variable is required")

        logger.info("Initializing Telegram bot with token: ${botToken.take(5)}...")

        bot = bot {
            token = botToken

            dispatch {
                command("start") {
                    logger.info("Received /start command from chatId: ${message.chat.id}")
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = """
                        🚀 Добро пожаловать в Gitty Code Reviewer Bot!
                        
                        Доступные команды:
                        /connect <github_repo_url> - Подключить репозиторий
                        /list - Список подключенных репозиториев
                        /subscribe - Подписаться на уведомления
                        /unsubscribe - Отписаться от уведомлений
                        /analyze - Анализировать код напрямую
                        
                        Бот будет автоматически анализировать коммиты в подключенных репозиториях.
                        """.trimIndent()
                    )
                }

                command("help") {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = """
                        Доступные команды:
                        /start - Запустить бота
                        /help - Помощь
                        /connect <url> - Подключить репозиторий
                        /list - Мои репозитории
                        /subscribe - Подписаться
                        /unsubscribe - Отписаться
                        /analyze - Анализ кода
                        """
                    )
                }

                command("connect") {
                    val response = telegramCommandHandler.handleConnect(
                        chatId = message.chat.id,
                        args = message.text?.substringAfter("/connect")?.trim() ?: "",
                        telegramUserId = message.from?.id ?: 0
                    )
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = response
                    )
                }

                command("subscribe") {
                    val response = telegramCommandHandler.handleSubscribe(
                        telegramUserId = message.from?.id ?: 0,
                        chatId = message.chat.id
                    )
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = response
                    )
                }

                command("unsubscribe") {
                    val response = telegramCommandHandler.handleUnsubscribe(message.chat.id)
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = response
                    )
                }

                command("list") {
                    val response = telegramCommandHandler.handleList(message.chat.id)
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = response
                    )
                }

                command("analyze") {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "⏳ Эта функция в разработке. Скоро можно будет анализировать код напрямую!"
                    )
                }
            }
        }

        logger.info("Starting Telegram bot polling...")
        bot.startPolling()
        logger.info("Telegram bot started successfully")
    }

    fun sendCodeReview(chatId: Long, commitInfo: String, analysisResult: String) {
        logger.info("Sending code review to chatId: $chatId")
        try {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = """
                📊 **Анализ коммита**
                
                $commitInfo
                
                $analysisResult
                
                ---
                Подробнее: ${System.getenv("APP_URL") ?: "http://localhost:8080"}/commits
                """.trimIndent()
            )
            logger.info("Code review sent successfully to chatId: $chatId")
        } catch (e: Exception) {
            logger.error("Error sending message to chatId $chatId: ${e.message}", e)
        }
    }
}