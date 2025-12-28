package dev.gitty.repository

import dev.gitty.model.Repository
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RepositoryRepository : JpaRepository<Repository, Long> {
    fun findByGithubRepoId(githubRepoId: String): Optional<Repository>
    fun findByOwnerId(ownerId: Long): List<Repository>
    fun findByNameAndOwnerName(name: String, ownerName: String): Optional<Repository>
}