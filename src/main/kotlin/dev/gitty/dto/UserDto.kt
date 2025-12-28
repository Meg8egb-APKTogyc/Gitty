package dev.gitty.dto

import java.time.Instant

data class UserDto(
    val id: Long,
    val githubUsername: String,
    val email: String,
    val role: String,
    val createdAt: Instant
)

data class CreateUserRequest(
    val githubId: String,
    val githubUsername: String,
    val email: String,
    val githubAccessToken: String?
)