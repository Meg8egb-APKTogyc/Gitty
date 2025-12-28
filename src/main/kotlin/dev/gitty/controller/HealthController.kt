package dev.gitty.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
class HealthController {

    @GetMapping
    fun healthCheck(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "service" to "Gitty",
            "version" to "0.0.1",
            "timestamp" to System.currentTimeMillis()
        )
    }
}