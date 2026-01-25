package dev.gitty.service.github

import dev.gitty.dto.analysis.CodeAnalysisRequest
import dev.gitty.dto.analysis.*
import dev.gitty.model.AnalysisStatus
import dev.gitty.model.GitCommit
import dev.gitty.model.Repository
import dev.gitty.repository.GitCommitRepository
import dev.gitty.repository.RepositoryRepository
import com.fasterxml.jackson.databind.ObjectMapper
import dev.gitty.dto.n8n.N8nAnalysisResponse
import dev.gitty.service.N8nIntegrationService
import dev.gitty.service.NotificationDispatcher
import dev.gitty.service.telegram.TelegramSubscriptionService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.get

@Service
@Transactional
class GithubWebhookService(
    private val repositoryRepository: RepositoryRepository,
    private val gitCommitRepository: GitCommitRepository,
    private val n8nIntegrationService: N8nIntegrationService,
    private val telegramSubscriptionService: TelegramSubscriptionService,
    private val notificationDispatcher: NotificationDispatcher,
    private val objectMapper: ObjectMapper
) {
    private val logger: Logger = LogManager.getLogger(GithubWebhookService::class.java)

    private val processingLocks = ConcurrentHashMap<String, Any>()

    fun handleGitHubWebhook(payload: Map<String, Any>) {
        try {
            val ref = payload["ref"] as? String ?: return
            val repository = payload["repository"] as? Map<*, *> ?: return
            val commits = payload["commits"] as? List<*> ?: return

            val repoFullName = repository["full_name"] as? String ?: return
            val branch = ref.removePrefix("refs/heads/")

            val deliveryId = payload["zen"] as? String ?: "unknown"
            logger.info("Processing webhook $deliveryId for repository: $repoFullName, branch: $branch")

            val repo = repositoryRepository.findByGithubRepoId(repoFullName).orElse(null)
            if (repo == null) {
                logger.warn("Repository not found: $repoFullName")
                return
            }

            commits.forEach { commitObj ->
                val commit = commitObj as? Map<*, *> ?: return@forEach
                val commitSha = commit["id"] as? String ?: return@forEach

                val lock = processingLocks.computeIfAbsent(commitSha) { Any() }
                synchronized(lock) {
                    processCommit(commit, repo, branch)
                }
            }
        } catch(e: Exception) {
            logger.error("Error processing GitHub webhook: ${e.message}", e)
        }
    }

    private fun processCommit(commit: Map<*, *>, repository: Repository, branch: String) {
        val commitId = commit["id"] as? String ?: return
        val message = commit["message"] as? String ?: ""
        val url = commit["url"] as? String ?: ""
        val author = commit["author"] as? Map<*, *>
        val authorName = author?.get("name") as? String ?: "Unknown"
        val authorEmail = author?.get("email") as? String ?: ""

        logger.info("Processing commit: $commitId by $authorName")

        @Suppress("UNCHECKED_CAST")
        val added = (commit["added"] as? List<String>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val modified = (commit["modified"] as? List<String>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val removed = (commit["removed"] as? List<String>) ?: emptyList()

        val changes = buildString {
            if (added.isNotEmpty()) append("Added: ${added.joinToString()}\n")
            if (modified.isNotEmpty()) append("Modified: ${modified.joinToString()}\n")
            if (removed.isNotEmpty()) append("Removed: ${removed.joinToString()}\n")
        }

        val analysisResult = n8nIntegrationService.analyzeCommit(message, changes)

        saveCommitToDatabase(repository, commitId, message, authorName, authorEmail, branch, analysisResult)

        sendNotifications(repository, branch, authorName, message, url, analysisResult)
    }

    private fun createAnalysisRequest(
        commitId: String,
        commitMessage: String,
        authorName: String,
        authorEmail: String,
        authorUsername: String?,
        commitUrl: String,
        repository: Repository,
        branch: String,
        added: List<String>,
        modified: List<String>,
        removed: List<String>
    ): CodeAnalysisRequest {
        return CodeAnalysisRequest(
            commit = CommitInfo(
                id = commitId,
                message = commitMessage,
                author = AuthorInfo(
                    name = authorName,
                    email = authorEmail,
                    username = authorUsername
                ),
                timestamp = Instant.now(),
                url = commitUrl
            ),
            repository = RepositoryInfo(
                name = repository.name,
                owner = repository.ownerName,
                fullName = repository.githubRepoId,
                url = "https://github.com/${repository.githubRepoId}"
            ),
            changes = FileChanges(
                added = added.map { file ->
                    FileChange(
                        file = file,
                        language = detectLanguage(file)
                    )
                },
                modified = modified.map { file ->
                    FileModification(
                        file = file,
                        language = detectLanguage(file)
                    )
                },
                removed = removed.map { file ->
                    FileRemoval(
                        file = file,
                        language = detectLanguage(file)
                    )
                }
            ),
            metadata = AnalysisMetadata(
                branch = branch,
                totalFilesChanged = added.size + modified.size + removed.size,
                totalAdditions = 0,
                totalDeletions = 0,
                hasTests = added.any { it.contains("test") || it.contains("spec") },
                hasDocumentation = added.any { it.endsWith(".md") || it.endsWith(".rst") }
            )
        )
    }

    private fun detectLanguage(file: String): String? {
        return when {
            file.endsWith(".py") -> "python"
            file.endsWith(".java") -> "java"
            file.endsWith(".kt") -> "kotlin"
            file.endsWith(".js") || file.endsWith(".ts") -> "javascript"
            file.endsWith(".go") -> "go"
            file.endsWith(".rs") -> "rust"
            file.endsWith(".cpp") || file.endsWith(".h") || file.endsWith(".hpp") -> "cpp"
            file.endsWith(".c") -> "c"
            file.endsWith(".cs") -> "csharp"
            file.endsWith(".php") -> "php"
            file.endsWith(".rb") -> "ruby"
            file.endsWith(".swift") -> "swift"
            file.endsWith(".md") -> "markdown"
            file.endsWith(".yml") || file.endsWith(".yaml") -> "yaml"
            file.endsWith(".json") -> "json"
            file.endsWith(".xml") -> "xml"
            file.endsWith(".sql") -> "sql"
            file.endsWith(".sh") || file.endsWith(".bash") -> "shell"
            file.endsWith(".html") || file.endsWith(".htm") -> "html"
            file.endsWith(".css") -> "css"
            file.endsWith(".dockerfile") || file.contains("Dockerfile") -> "docker"
            file.endsWith(".tf") -> "terraform"
            else -> null
        }
    }

    private fun saveCommitToDatabase(
        repository: Repository,
        commitSha: String,
        message: String,
        authorName: String,
        authorEmail: String,
        branchName: String,
        analysisResult: String
    ) {
        val lock = processingLocks.computeIfAbsent(commitSha) { Any() }

        synchronized(lock) {
            val existingCommit = gitCommitRepository.findByCommitSha(commitSha).orElse(null)

            if (existingCommit != null) {
                logger.info("Commit $commitSha already exists in database")
                return
            }

            val analysisReportJson = objectMapper.writeValueAsString(analysisResult)

            val gitCommit = GitCommit(
                commitSha = commitSha,
                message = message,
                authorName = authorName,
                authorEmail = authorEmail,
                commitDate = Instant.now(),
                repository = repository,
                branchName = branchName,
                analysisReport = analysisReportJson,
                analysisStatus = if (analysisResult.contains("✅") || analysisResult.contains("No issues found"))
                    AnalysisStatus.COMPLETED else AnalysisStatus.PROCESSING
            )

            gitCommitRepository.save(gitCommit)
            logger.info("Saved commit $commitSha to database for repository: ${repository.name}")
        }
    }

    private fun sendNotifications(
        repository: Repository,
        branch: String,
        authorName: String,
        message: String,
        url: String,
        analysisResult: String
    ) {
        val subscriptions = telegramSubscriptionService.getSubscriptionsForRepository(repository.id)

        if (subscriptions.isEmpty()) {
            logger.info("No active subscriptions for repository: ${repository.name}")
            return
        }

        logger.info("Sending notifications to ${subscriptions.size} subscribers")

        subscriptions.forEach { subscription ->
            if (subscription.isActive) {
                notificationDispatcher.sendCommitNotification(
                    chatId = subscription.chatId,
                    repositoryName = repository.name,
                    ownerName = repository.ownerName,
                    branch = branch,
                    authorName = authorName,
                    message = message,
                    url = url,
                    analysisResult = analysisResult
                )
            }
        }
    }
}