package dev.gitty.service.repository

import dev.gitty.model.Repository
import dev.gitty.repository.RepositoryRepository
import dev.gitty.service.github.GithubWebhookManager
import dev.gitty.service.user.UserService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RepositoryService(
    private val repositoryRepository: RepositoryRepository,
    private val userService: UserService,
    private val githubWebhookManager: GithubWebhookManager
) {
    private val logger: Logger = LogManager.getLogger(RepositoryService::class.java)

    fun connectRepository(
        githubRepoId: String,
        name: String,
        ownerName: String,
        userId: Long,
        githubToken: String? = null,
        webhookUrl: String? = null
    ): Repository {
        val owner = userService.findById(userId)
            ?: throw RuntimeException("User not found with id: $userId")

        repositoryRepository.findByGithubRepoId(githubRepoId)
            .ifPresent { throw RuntimeException("Repository already connected") }

        val repository = Repository(
            githubRepoId = githubRepoId,
            name = name,
            ownerName = ownerName,
            owner = owner
        )

        val savedRepository = repositoryRepository.save(repository)

        if (!githubToken.isNullOrEmpty() && !webhookUrl.isNullOrEmpty()) {
            try {
                logger.info("Пытаюсь создать вебхук для $ownerName/$name")
                logger.info("Webhook URL: $webhookUrl")
                logger.info("Token starts with: ${githubToken.take(10)}...")

                val webhookResponse = githubWebhookManager.createWebhook(
                    owner = ownerName,
                    repo = name,
                    githubToken = githubToken,
                    webhookUrl = webhookUrl
                )

                if (webhookResponse != null) {
                    savedRepository.webhookId = webhookResponse.id.toString()
                    repositoryRepository.save(savedRepository)
                    logger.info("✅ Вебхук создан! ID: ${webhookResponse.id}")
                } else {
                    logger.error("❌ GitHub API вернул null при создании вебхука")
                }
            } catch (e: Exception) {
                logger.error("❌ Ошибка создания вебхука: ${e.message}", e)
            }
        } else {
            logger.warn("⚠️ Токен или URL вебхука не указаны. Вебхук не будет создан.")
        }

        return savedRepository
    }

    fun updateWebhookId(repositoryId: Long, webhookId: String): Repository {
        val repository = repositoryRepository.findById(repositoryId)
            .orElseThrow { RuntimeException("Repository not found with id: $repositoryId") }

        repository.webhookId = webhookId
        return repositoryRepository.save(repository)
    }

    fun getUserRepositories(userId: Long): List<Repository> {
        return repositoryRepository.findByOwnerId(userId)
    }

    fun findById(id: Long): Repository? {
        return repositoryRepository.findById(id).orElse(null)
    }

    fun findByGithubRepoId(githubRepoId: String): Repository? {
        return repositoryRepository.findByGithubRepoId(githubRepoId).orElse(null)
    }

    fun addTeamMember(repositoryId: Long, userId: Long): Repository {
        val repository = repositoryRepository.findById(repositoryId)
            .orElseThrow { RuntimeException("Repository not found with id: $repositoryId") }

        val user = userService.findById(userId)
            ?: throw RuntimeException("User not found with id: $userId")

        repository.teamMembers.add(user)
        return repositoryRepository.save(repository)
    }

    fun removeTeamMember(repositoryId: Long, userId: Long): Repository {
        val repository = repositoryRepository.findById(repositoryId)
            .orElseThrow { RuntimeException("Repository not found with id: $repositoryId") }

        val user = userService.findById(userId)
            ?: throw RuntimeException("User not found with id: $userId")

        repository.teamMembers.remove(user)
        return repositoryRepository.save(repository)
    }
}