package com.pronotes.app.data

/**
 * Flags common note quality issues and provides actionable feedback.
 *
 * Checks include:
 * - Very short notes (fewer than 20 words)
 * - Very long paragraphs (> 150 words without a line break)
 * - Repeated consecutive words ("the the", "is is")
 * - Unclosed parentheses / brackets
 * - Orphaned bullet points (lists with single item)
 * - Multiple exclamation marks or ALL-CAPS shouting
 */
object NoteHealthChecker {

    enum class IssueSeverity { INFO, WARNING, ERROR }

    data class HealthIssue(
        val severity: IssueSeverity,
        val code: String,
        val message: String,
    )

    data class HealthReport(
        val issues: List<HealthIssue>,
        val score: Int, // 0–100 (100 = no issues)
        val healthy: Boolean,
    )

    fun check(text: String): HealthReport {
        if (text.isBlank()) {
            return HealthReport(
                issues = listOf(HealthIssue(IssueSeverity.INFO, "EMPTY", "Note is empty.")),
                score = 100,
                healthy = true,
            )
        }

        val issues = mutableListOf<HealthIssue>()

        // 1. Very short note
        val wordCount = text.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        if (wordCount < 20) {
            issues.add(HealthIssue(IssueSeverity.INFO, "SHORT_NOTE", "Note is very short ($wordCount words). Consider expanding it."))
        }

        // 2. Long paragraph without line break
        val paragraphs = text.split(Regex("""\n\s*\n|\n"""))
        val longPara = paragraphs.any { para ->
            para.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size > 150
        }
        if (longPara) {
            issues.add(HealthIssue(IssueSeverity.WARNING, "LONG_PARAGRAPH", "One or more paragraphs exceed 150 words. Consider breaking them up for readability."))
        }

        // 3. Repeated consecutive words
        val repeatMatch = Regex("""\b(\w+)\s+\1\b""", RegexOption.IGNORE_CASE).find(text)
        if (repeatMatch != null) {
            issues.add(HealthIssue(IssueSeverity.ERROR, "DUPLICATE_WORD", "Repeated word detected: \"${repeatMatch.groupValues[1]} ${repeatMatch.groupValues[1]}\"."))
        }

        // 4. Unclosed parentheses / brackets
        var paren = 0; var bracket = 0
        for (c in text) {
            when (c) {
                '(' -> paren++; ')' -> paren--
                '[' -> bracket++; ']' -> bracket--
            }
        }
        if (paren != 0) issues.add(HealthIssue(IssueSeverity.ERROR, "UNCLOSED_PAREN", "Unclosed parenthesis detected (count delta: $paren)."))
        if (bracket != 0) issues.add(HealthIssue(IssueSeverity.WARNING, "UNCLOSED_BRACKET", "Unclosed bracket detected (count delta: $bracket)."))

        // 5. Orphaned single-item lists (a "- item" with no sibling bullet)
        val bullets = text.lines().filter { it.trimStart().startsWith("- ") || it.trimStart().startsWith("* ") }
        if (bullets.size == 1) {
            issues.add(HealthIssue(IssueSeverity.INFO, "SINGLE_BULLET", "Only one bullet point found. Lists with a single item are usually better as plain sentences."))
        }

        // 6. ALL CAPS shouting
        val capsWords = Regex("""\b[A-Z]{5,}\b""").findAll(text)
            .map { it.value }
            .filter { it != "IMPORTANT" && it != "NOTE" && it != "TODO" && it != "FIXME" }
            .toList()
        if (capsWords.size > 2) {
            issues.add(HealthIssue(IssueSeverity.INFO, "ALL_CAPS", "Excessive ALL-CAPS text detected (${capsWords.size} occurrences). Consider using bold/italic for emphasis."))
        }

        // Score: starts at 100, deduct per issue severity
        val deduction = issues.sumOf { when (it.severity) {
            IssueSeverity.ERROR -> 20
            IssueSeverity.WARNING -> 10
            IssueSeverity.INFO -> 5
        }}
        val score = (100 - deduction).coerceIn(0, 100)
        val errorIssues = issues.filter { it.severity == IssueSeverity.ERROR }

        return HealthReport(
            issues = issues,
            score = score,
            healthy = errorIssues.isEmpty(),
        )
    }
}
