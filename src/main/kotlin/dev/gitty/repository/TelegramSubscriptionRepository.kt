package dev.gitty.repository

import dev.gitty.model.TelegramSubscription
import org.springframework.data.jpa.repository.JpaRepository

interface TelegramSubscriptionRepository : JpaRepository<TelegramSubscription, Long> {
    fun findByChatId(chatId: Long): List<TelegramSubscription>
    fun findByRepositoryId(repositoryId: Long): List<TelegramSubscription>
    fun findByTelegramUserId(telegramUserId: Long): List<TelegramSubscription>
    fun existsByChatIdAndRepositoryId(chatId: Long, repositoryId: Long): Boolean
    fun deleteByChatIdAndRepositoryId(chatId: Long, repositoryId: Long)
    fun deleteByChatId(chatId: Long)
}