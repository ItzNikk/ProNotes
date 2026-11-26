package com.pronotes.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StudySessionPlannerTest {

    @Test
    fun `builds standard 4-cycle pomodoro plan`() {
        val plan = StudySessionPlanner.buildPlan(
            focusCycles = 4,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            cyclesBeforeLongBreak = 4,
        )
        assertEquals(7, plan.phases.size)
        assertEquals(100, plan.totalFocusMinutes)
        assertEquals(15, plan.totalBreakMinutes)
        assertEquals(115, plan.totalMinutes)
    }

    @Test
    fun `inserts long break after configured cycle count`() {
        val plan = StudySessionPlanner.buildPlan(
            focusCycles = 3,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            cyclesBeforeLongBreak = 2,
        )
        assertEquals(StudySessionPlanner.PhaseType.LONG_BREAK, plan.phases[3].type)
        assertEquals(15, plan.phases[3].durationMinutes)
    }
}
