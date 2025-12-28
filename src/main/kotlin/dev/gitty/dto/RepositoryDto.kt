package dev.gitty.dto

import java.time.Instant

data class RepositoryDto(
    val id: Long,
    val name: String,
    val ownerName: String,
    val githubRepoId: String,
    val createdAt: Instant,
    val teamMemberCount: Int
)

data class ConnectRepositoryRequest(
    val githubRepoId: String,
    val name: String,
    val ownerName: String,
    val userId: Long
)