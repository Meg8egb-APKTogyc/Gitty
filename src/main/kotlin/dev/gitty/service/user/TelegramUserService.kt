package dev.gitty.service.user

import dev.gitty.model.User
import dev.gitty.model.UserRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TelegramUserService(
    private val userService: UserService
) {
    fun getOrCreateTelegramUser(telegramUserId: Long, chatId: Long): User {
        val telegramUsername = "telegram_$telegramUserId"
        val githubId = telegramUserId.toString()

        return userService.findByGithubUsername(telegramUsername)
            ?: userService.findByGithubId(githubId)
            ?: createNewTelegramUser(telegramUserId, chatId)
    }

    private fun createNewTelegramUser(telegramUserId: Long, chatId: Long): User {
        val telegramUsername = "telegram_$telegramUserId"

        val newUser = User(
            githubId = telegramUserId.toString(),
            githubUsername = telegramUsername,
            email = "telegram_${telegramUserId}@example.com",
            role = UserRole.DEVELOPER
        )

        return userService.save(newUser)
    }
}