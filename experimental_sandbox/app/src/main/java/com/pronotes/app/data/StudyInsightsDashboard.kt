package com.pronotes.app.data

/**
 * Aggregates all intelligent study signals into a single snapshot for dashboard UI binding.
 *
 * Call [StudyInsightsDashboard.compute] with raw data sources to get a fully populated snapshot.
 */
data class StudyDashboardSnapshot(
    val streak: StreakTracker.StreakReport,
    val mastery: StudyAnalytics.MasterySummary,
    val tagSuggestions: List<TagSuggestionEngine.TagSuggestion>,
    val readability: ReadabilityAnalytics?,
    val healthScore: Int,
    val healthIssues: List<NoteHealthChecker.HealthIssue>,
    val sessionPlan: StudySessionPlanner.Plan,
    val autoSummary: List<String>,
) {
    data class ReadabilityAnalytics(
        val wordCount: Int,
        val readingLevel: String,
        val gradeLevel: Double,
    )
}

object StudyInsightsDashboard {

    fun compute(
        noteText: String,
        otherNotesCorpus: List<String> = emptyList(),
        flashcardSnapshots: List<StudyAnalytics.CardSnapshot> = emptyList(),
        sessionTimestampsMs: List<Long> = emptyList(),
        nowMs: Long = System.currentTimeMillis(),
        targetSummaryCount: Int = 3,
        pomodoroFocusCycles: Int = 4,
    ): StudyDashboardSnapshot {
        // Streak
        val streak = StreakTracker.report(sessionTimestampsMs, nowMs)

        // Mastery
        val mastery = StudyAnalytics.summarize(flashcardSnapshots, nowMs)

        // Tag suggestions
        val tags = TagSuggestionEngine.suggest(noteText, otherNotesCorpus, maxTags = 8)

        // Readability
        val readability = if (noteText.isNotBlank()) {
            val r = ReadabilityAnalyzer.analyze(noteText)
            StudyDashboardSnapshot.ReadabilityAnalytics(
                wordCount = r.wordCount,
                readingLevel = r.readingLevel,
                gradeLevel = r.fleschKincaidGrade,
            )
        } else null

        // Note health
        val health = NoteHealthChecker.check(noteText)

        // Session plan
        val plan = StudySessionPlanner.buildPlan(focusCycles = pomodoroFocusCycles)

        // Auto summary
        val summary = AutoSummaryEngine.summarize(noteText, targetSentences = targetSummaryCount)

        return StudyDashboardSnapshot(
            streak = streak,
            mastery = mastery,
            tagSuggestions = tags,
            readability = readability,
            healthScore = health.score,
            healthIssues = health.issues,
            sessionPlan = plan,
            autoSummary = summary.sentences,
        )
    }
}
