package dev.gitty.repository

import dev.gitty.model.GitCommit
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface GitCommitRepository : JpaRepository<GitCommit, Long> {
    fun findByCommitSha(commitSha: String): Optional<GitCommit>
    fun findByRepositoryId(repositoryId: Long): List<GitCommit>
    fun findByRepositoryIdAndBranchName(repositoryId: Long, branchName: String): List<GitCommit>
}