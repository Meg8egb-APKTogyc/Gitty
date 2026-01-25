package dev.gitty.service.telegram

import dev.gitty.model.TelegramSubscription
import dev.gitty.repository.TelegramSubscriptionRepository
import dev.gitty.repository.RepositoryRepository
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TelegramSubscriptionService(
    private val subscriptionRepository: TelegramSubscriptionRepository,
    private val repositoryRepository: RepositoryRepository
) {
    private val logger: Logger = LogManager.getLogger(TelegramSubscriptionService::class.java)

    fun subscribe(chatId: Long, repositoryId: Long, telegramUserId: Long) {
        if (!subscriptionRepository.existsByChatIdAndRepositoryId(chatId, repositoryId)) {
            val repository = repositoryRepository.findById(repositoryId)
                .orElseThrow { RuntimeException("Repository not found with id: $repositoryId") }

            val subscription = TelegramSubscription(
                chatId = chatId,
                telegramUserId = telegramUserId,
                repositoryId = repository.id,
                repositoryName = repository.name,
                ownerName = repository.ownerName
            )

            subscriptionRepository.save(subscription)
            logger.info("User $telegramUserId subscribed to repository $repositoryId")
        }
    }

    fun unsubscribe(chatId: Long, repositoryId: Long) {
        subscriptionRepository.deleteByChatIdAndRepositoryId(chatId, repositoryId)
        logger.info("User unsubscribed from repository $repositoryId")
    }

    fun unsubscribeAll(chatId: Long) {
        subscriptionRepository.deleteByChatId(chatId)
        logger.info("User unsubscribed from all repositories")
    }

    fun subscribeToAllRepositories(telegramUserId: Long, chatId: Long) {
        val userRepositories = repositoryRepository.findAll().filter {
            it.owner.githubUsername == "telegram_$telegramUserId"
        }

        userRepositories.forEach { repository ->
            subscribe(chatId, repository.id, telegramUserId)
        }
    }

    fun getSubscriptionsForChat(chatId: Long): List<TelegramSubscription> {
        return subscriptionRepository.findByChatId(chatId)
    }

    fun getSubscriptionsForRepository(repositoryId: Long): List<TelegramSubscription> {
        return subscriptionRepository.findByRepositoryId(repositoryId)
    }

    fun getSubscriptionsForUser(telegramUserId: Long): List<TelegramSubscription> {
        return subscriptionRepository.findByTelegramUserId(telegramUserId)
    }
}