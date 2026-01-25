package dev.gitty.controller

import dev.gitty.service.github.GithubWebhookService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/webhook")
class WebhookController(
    private val githubWebhookService: GithubWebhookService
) {
    private val logger: Logger = LogManager.getLogger(WebhookController::class.java)

    @PostMapping("/github")
    fun handleGitHubWebhook(
        @RequestBody payload: Map<String, Any>,
        @RequestHeader("X-GitHub-Event") event: String
    ): ResponseEntity<String> {
        logger.info("Received GitHub webhook event: $event")

        if (event == "push") {
            githubWebhookService.handleGitHubWebhook(payload)
            return ResponseEntity.ok("Webhook processed successfully")
        }

        logger.info("Ignoring unsupported event type: $event")
        return ResponseEntity.ok("Event ignored")
    }

    @PostMapping("/telegram")
    fun handleTelegramWebhook(@RequestBody update: Map<String, Any>): ResponseEntity<String> {
        logger.info("Received Telegram webhook")
        return ResponseEntity.ok("OK")
    }

    @GetMapping("/test")
    fun testEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("Webhook endpoint is working")
    }
}