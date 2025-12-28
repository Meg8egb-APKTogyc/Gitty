package dev.gitty.service

import dev.gitty.model.Repository
import dev.gitty.repository.RepositoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional
class RepositoryService(
    private val repositoryRepository: RepositoryRepository,
    private val userService: UserService
) {

    fun connectRepository(
        githubRepoId: String,
        name: String,
        ownerName: String,
        userId: Long
    ): Repository {
        val owner = userService.findById(userId)
            ?: throw RuntimeException("User not found with id: $userId")

        // Проверяем, не подключен ли уже репозиторий
        repositoryRepository.findByGithubRepoId(githubRepoId)
            .ifPresent { throw RuntimeException("Repository already connected") }

        val repository = Repository(
            githubRepoId = githubRepoId,
            name = name,
            ownerName = ownerName,
            owner = owner
        )

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