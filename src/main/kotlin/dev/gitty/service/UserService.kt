package dev.gitty.service

import dev.gitty.model.User
import dev.gitty.model.UserRole
import dev.gitty.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {

    fun findOrCreateUserFromGithub(
        githubId: String,
        githubUsername: String,
        email: String,
        githubAccessToken: String?
    ): User {
        return userRepository.findByGithubId(githubId)
            .orElseGet {
                val newUser = User(
                    githubId = githubId,
                    githubUsername = githubUsername,
                    email = email,
                    githubAccessToken = githubAccessToken
                )
                userRepository.save(newUser)
            }
    }

    fun findById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    fun findByGithubUsername(username: String): User? {
        return userRepository.findByGithubUsername(username).orElse(null)
    }

    fun updateUserRole(userId: Long, role: UserRole): User {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found with id: $userId") }

        // Пока поставим заглушку
        throw RuntimeException("Update role not implemented yet")
    }

    fun updateAccessToken(userId: Long, accessToken: String): User {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found with id: $userId") }

        // Пока поставим заглушку
        throw RuntimeException("Update access token not implemented yet")
    }
}
