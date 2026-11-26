package com.pronotes.app.data

import java.util.concurrent.TimeUnit

/**
 * Tracks daily study streaks and consistency habits.
 *
 * A session on a given calendar day (UTC day index) counts as a study day.
 * A streak is a consecutive run of study days up to and including today.
 */
object StreakTracker {

    private val DAY_MS = TimeUnit.DAYS.toMillis(1)

    data class StreakReport(
        val currentStreak: Int,
        val longestStreak: Int,
        val totalStudyDays: Int,
        val studiedToday: Boolean,
        val consistency7Days: Double,
        val consistency30Days: Double,
    )

    /**
     * @param sessionTimestampsMs list of epoch-millisecond timestamps, one per session (may have duplicates for the same day)
     * @param nowMs current epoch-milliseconds (defaults to [System.currentTimeMillis])
     */
    fun report(
        sessionTimestampsMs: List<Long>,
        nowMs: Long = System.currentTimeMillis(),
    ): StreakReport {
        val todayDay = nowMs / DAY_MS

        val studyDays = sessionTimestampsMs
            .map { it / DAY_MS }
            .toSortedSet()

        val studiedToday = todayDay in studyDays

        // Current streak: consecutive days ending at today (or yesterday if not studied today)
        val currentStreak = computeCurrentStreak(studyDays, todayDay)
        val longestStreak = computeLongestStreak(studyDays)
        val totalStudyDays = studyDays.size

        val consistency7 = studyDays.count { it in (todayDay - 6)..todayDay }.toDouble() / 7.0
        val consistency30 = studyDays.count { it in (todayDay - 29)..todayDay }.toDouble() / 30.0

        return StreakReport(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalStudyDays = totalStudyDays,
            studiedToday = studiedToday,
            consistency7Days = kotlin.math.round(consistency7 * 1000.0) / 1000.0,
            consistency30Days = kotlin.math.round(consistency30 * 1000.0) / 1000.0,
        )
    }

    private fun computeCurrentStreak(days: Set<Long>, todayDay: Long): Int {
        var streak = 0
        var check = if (todayDay in days) todayDay else todayDay - 1
        while (check in days) {
            streak++
            check--
        }
        return streak
    }

    private fun computeLongestStreak(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        val sorted = days.toLongArray().also { it.sort() }
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
        }
        return best
    }
}
