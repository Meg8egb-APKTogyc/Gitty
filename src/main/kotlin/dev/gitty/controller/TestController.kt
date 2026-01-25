package dev.gitty.controller

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/test")
class TestController {
    private val logger: Logger = LogManager.getLogger(TestController::class.java)

    @GetMapping("/health")
    fun healthCheck(): Map<String, Any> {
        logger.info("Health check called")
        return mapOf(
            "status" to "UP",
            "service" to "Gitty Code Analyzer",
            "timestamp" to System.currentTimeMillis()
        )
    }

    @GetMapping("/env")
    fun checkEnvironment(): Map<String, String> {
        return mapOf(
            (("TELEGRAM_BOT_TOKEN" to System.getenv("TELEGRAM_BOT_TOKEN")?.let {
                if (it.isNotEmpty()) "***${it.takeLast(4)}" else "NOT_SET"
            }) ?: "NOT_SET") as Pair<String, String>,
            (("N8N_WEBHOOK_URL" to System.getenv("N8N_WEBHOOK_URL")) ?: "NOT_SET") as Pair<String, String>,
            (("NGROK_URL" to System.getenv("NGROK_URL")) ?: "NOT_SET") as Pair<String, String>
        )
    }
}