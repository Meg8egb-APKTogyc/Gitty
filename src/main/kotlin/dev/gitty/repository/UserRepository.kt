package dev.gitty.repository

import dev.gitty.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByGithubId(githubId: String): Optional<User>
    fun findByGithubUsername(githubUsername: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
}