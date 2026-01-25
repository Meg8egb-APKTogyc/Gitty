package dev.gitty.controller

import dev.gitty.repository.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/debug")
class DebugController(
    private val userRepository: UserRepository,
    private val repositoryRepository: RepositoryRepository,
    private val subscriptionRepository: TelegramSubscriptionRepository,
    private val commitRepository: GitCommitRepository
) {

    @GetMapping("/stats")
    fun getStats(): Map<String, Any> {
        return mapOf(
            "users" to userRepository.count(),
            "repositories" to repositoryRepository.count(),
            "subscriptions" to subscriptionRepository.count(),
            "commits" to commitRepository.count()
        )
    }

    @GetMapping("/users")
    fun getUsers(): List<Map<String, Any>> {
        return userRepository.findAll().map { user ->
            mapOf(
                "id" to user.id,
                "githubUsername" to user.githubUsername,
                "email" to user.email,
                "role" to user.role.name,
                "repositoriesCount" to user.repositories.size
            )
        }
    }

    @GetMapping("/repos")
    fun getRepositories(): List<Map<String, Any>> {
        return repositoryRepository.findAll().map { repo ->
            mapOf(
                "id" to repo.id,
                "name" to repo.name,
                "owner" to repo.ownerName,
                "githubId" to repo.githubRepoId,
                "webhookId" to repo.webhookId,
                "ownerUserId" to repo.owner.id
            ) as Map<String, Any>
        }
    }

    @GetMapping("/subscriptions")
    fun getSubscriptions(): List<Map<String, Any>> {
        return subscriptionRepository.findAll().map { sub ->
            mapOf(
                "id" to sub.id,
                "chatId" to sub.chatId,
                "telegramUserId" to sub.telegramUserId,
                "repositoryId" to sub.repositoryId,
                "repositoryName" to sub.repositoryName,
                "isActive" to sub.isActive
            )
        }
    }
}