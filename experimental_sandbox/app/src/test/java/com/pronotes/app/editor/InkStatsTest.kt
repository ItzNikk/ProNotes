package com.pronotes.app.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class InkStatsTest {

    @Test
    fun `empty strokes returns zero summary`() {
        val summary = InkStats.analyze(emptyList())
        assertEquals(0, summary.totalStrokes)
        assertEquals(0.0, summary.totalPathLengthPx, 1e-6)
    }

    @Test
    fun `computes stroke counts path length and bounding box`() {
        val strokes = listOf(
            InkStats.StrokeSample(points = listOf(0f to 0f, 3f to 4f), isHighlighter = false),
            InkStats.StrokeSample(points = listOf(10f to 0f, 10f to 10f), isHighlighter = true),
        )
        val summary = InkStats.analyze(strokes)
        assertEquals(2, summary.totalStrokes)
        assertEquals(1, summary.penStrokes)
        assertEquals(1, summary.highlighterStrokes)
        assertEquals(15.0, summary.totalPathLengthPx, 1e-6)
        assertEquals(10f, summary.boundingWidthPx, 1e-6f)
        assertEquals(10f, summary.boundingHeightPx, 1e-6f)
    }
}
