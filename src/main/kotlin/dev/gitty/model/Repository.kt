package dev.gitty.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "repositories")
data class Repository(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "github_repo_id", nullable = false, unique = true)
    val githubRepoId: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "owner_name", nullable = false)
    val ownerName: String,

    @Column(name = "webhook_id")
    var webhookId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val owner: User,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "repository", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val commits: MutableList<GitCommit> = mutableListOf(),

    @ManyToMany
    @JoinTable(
        name = "repository_team_members",
        joinColumns = [JoinColumn(name = "repository_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    val teamMembers: MutableSet<User> = mutableSetOf()
) {
    fun getFullName(): String = "$ownerName/$name"

    fun hasWebhook(): Boolean = webhookId != null
}