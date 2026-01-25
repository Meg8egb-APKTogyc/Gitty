package dev.gitty.config

import jakarta.annotation.PostConstruct
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Component

@Component
class StartupCheck {
    private val logger: Logger = LogManager.getLogger(StartupCheck::class.java)

    @PostConstruct
    fun checkConfiguration() {
        logger.info("=== Проверка конфигурации при запуске ===")

        // Простая проверка через System.getenv
        val telegramToken = System.getenv("TELEGRAM_BOT_TOKEN")
        val n8nUrl = System.getenv("N8N_WEBHOOK_URL")
        val ngrokUrl = System.getenv("NGROK_URL")

        if (telegramToken.isNullOrEmpty()) {
            logger.error("❌ TELEGRAM_BOT_TOKEN не установлен! Бот не запустится.")
        } else {
            logger.info("✅ TELEGRAM_BOT_TOKEN: ***${telegramToken.takeLast(4)}")
        }

        if (n8nUrl.isNullOrEmpty()) {
            logger.warn("⚠️ N8N_WEBHOOK_URL не установлен, используется значение по умолчанию")
        } else {
            logger.info("✅ N8N_WEBHOOK_URL: $n8nUrl")
        }

        logger.info("✅ NGROK_URL: ${ngrokUrl ?: "http://localhost:8080 (по умолчанию)"}")

        logger.info("=== Проверка завершена ===")
    }
}