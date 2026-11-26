package com.pronotes.app.data

/**
 * Pure-Kotlin Pomodoro & focus session planner.
 */
object StudySessionPlanner {

    enum class PhaseType { FOCUS, SHORT_BREAK, LONG_BREAK }

    data class Phase(
        val index: Int,
        val type: PhaseType,
        val durationMinutes: Int,
    )

    data class Plan(
        val phases: List<Phase>,
        val totalFocusMinutes: Int,
        val totalBreakMinutes: Int,
        val totalMinutes: Int,
    )

    fun buildPlan(
        focusCycles: Int = 4,
        focusMinutes: Int = 25,
        shortBreakMinutes: Int = 5,
        longBreakMinutes: Int = 15,
        cyclesBeforeLongBreak: Int = 4,
    ): Plan {
        if (focusCycles <= 0 || focusMinutes <= 0) {
            return Plan(emptyList(), 0, 0, 0)
        }
        val phases = mutableListOf<Phase>()
        var focusTotal = 0
        var breakTotal = 0
        var idx = 0

        for (cycle in 1..focusCycles) {
            phases.add(Phase(idx++, PhaseType.FOCUS, focusMinutes))
            focusTotal += focusMinutes
            if (cycle < focusCycles) {
                val isLong = cyclesBeforeLongBreak > 0 && cycle % cyclesBeforeLongBreak == 0
                val brk = if (isLong) longBreakMinutes else shortBreakMinutes
                if (brk > 0) {
                    phases.add(Phase(idx++, if (isLong) PhaseType.LONG_BREAK else PhaseType.SHORT_BREAK, brk))
                    breakTotal += brk
                }
            }
        }

        return Plan(
            phases = phases,
            totalFocusMinutes = focusTotal,
            totalBreakMinutes = breakTotal,
            totalMinutes = focusTotal + breakTotal,
        )
    }
}
