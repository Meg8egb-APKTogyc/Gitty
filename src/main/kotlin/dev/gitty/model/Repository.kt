package dev.gitty.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.GenerationType
import jakarta.persistence.Column
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.Instant

@Entity
@Table(name = "repositories")
data class Repository(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "github_repo_id", nullable = false, unique = true)
    val githubRepoId: String,

    @Column(nullable = false)
    val name: String,

    @Column(name = "owner_name", nullable = false)
    val ownerName: String,

    @Column(name = "webhook_id")
    val webhookId: String? = null,

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
)
