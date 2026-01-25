package dev.gitty.dto.analysis

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class CodeAnalysisRequest(
    val commit: CommitInfo,
    val repository: RepositoryInfo,
    val changes: FileChanges,
    val metadata: AnalysisMetadata
)

data class CommitInfo(
    val id: String,
    val message: String,
    val author: AuthorInfo,
    val timestamp: Instant,
    val url: String
)

data class AuthorInfo(
    val name: String,
    val email: String,
    val username: String? = null
)

data class RepositoryInfo(
    val name: String,
    val owner: String,
    @JsonProperty("full_name")
    val fullName: String,
    val url: String
)

data class FileChanges(
    val added: List<FileChange> = emptyList(),
    val modified: List<FileModification> = emptyList(),
    val removed: List<FileRemoval> = emptyList()
)

data class FileChange(
    val file: String,
    val language: String? = null,
    val size: Int? = null,
    val snippet: String? = null,
    val content: String? = null
)

data class FileModification(
    val file: String,
    val language: String? = null,
    val diff: String? = null,
    @JsonProperty("previous_size")
    val previousSize: Int? = null,
    @JsonProperty("new_size")
    val newSize: Int? = null,
    val changes: List<CodeChange>? = null
)

data class CodeChange(
    val type: ChangeType,
    val line: Int,
    val content: String,
    @JsonProperty("previous_content")
    val previousContent: String? = null
)

data class FileRemoval(
    val file: String,
    val language: String? = null,
    val reason: String? = null
)

data class AnalysisMetadata(
    val branch: String,
    @JsonProperty("total_files_changed")
    val totalFilesChanged: Int,
    @JsonProperty("total_additions")
    val totalAdditions: Int,
    @JsonProperty("total_deletions")
    val totalDeletions: Int,
    @JsonProperty("has_tests")
    val hasTests: Boolean,
    @JsonProperty("has_documentation")
    val hasDocumentation: Boolean,
    @JsonProperty("has_config_changes")
    val hasConfigChanges: Boolean = false,
    @JsonProperty("has_migrations")
    val hasMigrations: Boolean = false
)

enum class ChangeType {
    ADDED, MODIFIED, DELETED
}