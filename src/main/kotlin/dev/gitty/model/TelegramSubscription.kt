package dev.gitty.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "telegram_subscriptions")
data class TelegramSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "chat_id", nullable = false)
    val chatId: Long,

    @Column(name = "telegram_user_id", nullable = false)
    val telegramUserId: Long,

    @Column(name = "repository_id", nullable = false)
    val repositoryId: Long,

    @Column(name = "repository_name", nullable = false)
    val repositoryName: String,

    @Column(name = "owner_name", nullable = false)
    val ownerName: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)