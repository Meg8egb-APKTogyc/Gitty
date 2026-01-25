package dev.gitty.service.repository

import dev.gitty.model.Repository
import dev.gitty.model.User
import dev.gitty.model.UserRole
import dev.gitty.repository.RepositoryRepository
import dev.gitty.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RepositoryConnectionService(
    private val repositoryRepository: RepositoryRepository,
    private val userRepository: UserRepository,
    private val repositoryService: RepositoryService
) {
    data class RepoInfo(val owner: String, val repoName: String, val fullName: String)

    fun extractRepoInfo(url: String): RepoInfo? {
        val pattern = """github\.com/([^/]+)/([^/]+)""".toRegex()
        val match = pattern.find(url)

        return match?.let {
            val owner = it.groupValues[1]
            val repoName = it.groupValues[2]
            RepoInfo(owner, repoName, "$owner/$repoName")
        }
    }

    fun connectRepositoryForTelegram(
        githubRepoId: String,
        name: String,
        ownerName: String,
        telegramUserId: Long,
        chatId: Long
    ): Repository {
        val user = userRepository.findByGithubUsername("telegram_$telegramUserId")
            .orElseGet {
                val newUser = User(
                    githubId = telegramUserId.toString(),
                    githubUsername = "telegram_$telegramUserId",
                    email = "telegram_${telegramUserId}@example.com",
                    role = UserRole.DEVELOPER
                )
                userRepository.save(newUser)
            }

        return repositoryService.connectRepository(
            githubRepoId = githubRepoId,
            name = name,
            ownerName = ownerName,
            userId = user.id
        )
    }
}