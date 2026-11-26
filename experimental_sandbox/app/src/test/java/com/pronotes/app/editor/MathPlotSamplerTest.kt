package com.pronotes.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathPlotSamplerTest {

    @Test
    fun `samples continuous function into single segment`() {
        val segments = MathPlotSampler.sample("x^2 - 2", xMin = -3.0, xMax = 3.0, yMin = -5.0, yMax = 10.0, steps = 60)
        assertEquals(1, segments.size)
        assertTrue(segments.first().size > 50)
    }

    @Test
    fun `splits discontinuous function at asymptote`() {
        val segments = MathPlotSampler.sample("1 / x", xMin = -5.0, xMax = 5.0, yMin = -5.0, yMax = 5.0, steps = 100)
        assertTrue("Expected at least 2 segments for 1/x", segments.size >= 2)
    }

    @Test
    fun `handles y equals prefix`() {
        val segments = MathPlotSampler.sample("y = sin(x)", xMin = -3.14, xMax = 3.14, yMin = -2.0, yMax = 2.0, steps = 40)
        assertEquals(1, segments.size)
    }
}
