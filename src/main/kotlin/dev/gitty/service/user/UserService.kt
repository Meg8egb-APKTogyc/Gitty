package dev.gitty.service.user

import dev.gitty.model.User
import dev.gitty.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {
    fun findById(userId: Long): User? {
        return userRepository.findById(userId).orElse(null)
    }

    fun findByGithubUsername(username: String): User? {
        return userRepository.findByGithubUsername(username).orElse(null)
    }

    fun findByGithubId(githubId: String): User? {
        return userRepository.findByGithubId(githubId).orElse(null)
    }

    fun save(user: User): User {
        return userRepository.save(user)
    }
}