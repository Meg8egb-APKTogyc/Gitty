package dev.gitty.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "git_commits")
data class GitCommit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "commit_sha", nullable = false, unique = true)
    val commitSha: String,

    @Column(nullable = false)
    val message: String,

    @Column(name = "author_name", nullable = false)
    val authorName: String,

    @Column(name = "author_email", nullable = false)
    val authorEmail: String,

    @Column(name = "commit_date", nullable = false)
    val commitDate: Instant,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    val repository: Repository,

    @Column(name = "branch_name", nullable = false)
    val branchName: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "code_quality_score")
    val codeQualityScore: Int? = null,

    @Column(name = "test_coverage")
    val testCoverage: Double? = null,

    @Column(name = "has_style_violations")
    val hasStyleViolations: Boolean = false,

    @Column(name = "analysis_status")
    @Enumerated(EnumType.STRING)
    val analysisStatus: AnalysisStatus = AnalysisStatus.PENDING,

    @Column(name = "report_path")
    val reportPath: String? = null,

    @Column(name = "uml_diagram_path")
    val umlDiagramPath: String? = null,

    @Column(name = "analysis_report", columnDefinition = "TEXT")
    val analysisReport: String? = null
)

enum class AnalysisStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}