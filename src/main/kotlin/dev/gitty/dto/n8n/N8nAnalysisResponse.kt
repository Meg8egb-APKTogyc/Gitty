package dev.gitty.dto.n8n

import com.fasterxml.jackson.annotation.JsonProperty

data class N8nAnalysisResponse(
    val summary: N8nAnalysisResponse.Summary? = null,
    val files: List<N8nAnalysisResponse.FileAnalysis> = emptyList(),
    val generalIssues: List<N8nAnalysisResponse.Issue> = emptyList(),
    val securityChecks: List<N8nAnalysisResponse.SecurityCheck> = emptyList(),
    val performanceMetrics: N8nAnalysisResponse.PerformanceMetrics? = null,
    @JsonProperty("ai_review") val aiReview: String? = null,
    @JsonProperty("text") val text: String? = null,
    @JsonProperty("analysis") val analysis: String? = null,
    @JsonProperty("issues") val issues: List<N8nAnalysisResponse.Issue> = emptyList(),
    @JsonProperty("score") val score: Int? = null
) {
    data class Summary(
        @JsonProperty("overall_score") val overallScore: Int? = null,
        val complexity: String? = null,
        @JsonProperty("risk_level") val riskLevel: String? = null,
        @JsonProperty("estimated_review_time") val estimatedReviewTime: String? = null
    )

    data class FileAnalysis(
        val file: String? = null,
        val score: Int? = null,
        val issues: List<Issue> = emptyList(),
        val strengths: List<String> = emptyList(),
        val improvements: List<String> = emptyList()
    )

    data class Issue(
        val type: String? = null,
        val severity: String? = null,
        val description: String? = null,
        val suggestion: String? = null,
        val line: Int? = null,
        val rule: String? = null,
        val category: String? = null
    )

    data class SecurityCheck(
        val check: String? = null,
        val description: String? = null,
        val status: String? = null
    )

    data class PerformanceMetrics(
        @JsonProperty("cyclomatic_complexity") val cyclomaticComplexity: Int? = null,
        @JsonProperty("cognitive_complexity") val cognitiveComplexity: Int? = null,
        @JsonProperty("maintainability_index") val maintainabilityIndex: Int? = null
    )

    fun toReadableString(): String {
        return buildString {
            append("📊 **Анализ кода**\n\n")

            summary?.let {
                it.overallScore?.let { score ->
                    val stars = "⭐".repeat(score / 20) + "☆".repeat(5 - score / 20)
                    append("**Общая оценка:** $score/100 $stars\n")
                }
                it.complexity?.let { complexity ->
                    append("**Сложность:** $complexity\n")
                }
                it.riskLevel?.let { risk ->
                    val riskEmoji = when(risk.uppercase()) {
                        "LOW" -> "✅"
                        "MEDIUM" -> "⚠️"
                        "HIGH" -> "🔴"
                        else -> "⚪"
                    }
                    append("**Уровень риска:** $riskEmoji $risk\n")
                }
                it.estimatedReviewTime?.let { time ->
                    append("**Время проверки:** $time\n")
                }
            }

            val allIssues = (generalIssues + issues).distinctBy { it.description }

            if (allIssues.isNotEmpty()) {
                append("\n🔍 **Найдены проблемы:**\n")
                allIssues.take(3).forEachIndexed { index, issue ->
                    val typeEmoji = when(issue.type?.uppercase()) {
                        "ERROR", "BUG" -> "❌"
                        "WARNING" -> "⚠️"
                        "STYLE" -> "🎨"
                        "SECURITY" -> "🔒"
                        else -> "💡"
                    }

                    append("\n$typeEmoji ")
                    issue.description?.let { desc ->
                        append("*${desc}*")
                    }

                    issue.suggestion?.let { sugg ->
                        append("\n   💡 *Рекомендация:* $sugg")
                    }
                }

                if (allIssues.size > 3) {
                    append("\n\n📊 *И еще ${allIssues.size - 3} проблем...*")
                }
            } else {
                append("\n✅ *Отлично! Проблем не обнаружено.*")
            }

            if (files.isNotEmpty()) {
                append("\n\n📁 **Проанализировано файлов:** ${files.size}")
                files.take(2).forEach { fileAnalysis ->
                    fileAnalysis.file?.let { fileName ->
                        append("\n  • $fileName")
                        fileAnalysis.score?.let { fileScore ->
                            append(" ($fileScore/100)")
                        }
                    }
                }
                if (files.size > 2) {
                    append("\n  • ... и еще ${files.size - 2}")
                }
            }

            val aiText = aiReview ?: text ?: analysis
            aiText?.takeIf { it.isNotBlank() }?.let { txt ->
                append("\n\n🤖 **AI обзор:**\n")
                append(txt.take(300))
                if (txt.length > 300) append("...")
            }

            score?.takeIf { it > 0 }?.let { numericScore ->
                append("\n\n🎯 **Итоговая оценка:** $numericScore/100")
            }

            append("\n\n---\n")
            append("📊 _Полный отчет доступен в приложении_")
        }
    }
}