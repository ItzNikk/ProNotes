package com.pronotes.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class MathEvaluatorTest {

    @Test
    fun `basic arithmetic and operator precedence`() {
        assertEquals("14", (MathEvaluator.evaluate("2 + 3 * 4") as MathResult.Value).formatted)
        assertEquals("20", (MathEvaluator.evaluate("(2 + 3) * 4") as MathResult.Value).formatted)
        assertEquals("6", (MathEvaluator.evaluate("18 / 3") as MathResult.Value).formatted)
        assertEquals("2", (MathEvaluator.evaluate("17 % 5") as MathResult.Value).formatted)
        assertEquals("512", (MathEvaluator.evaluate("2 ^ 3 ^ 2") as MathResult.Value).formatted)
    }

    @Test
    fun `unicode operators and powers`() {
        assertEquals("360", (MathEvaluator.evaluate("24 × 15") as MathResult.Value).formatted)
        assertEquals("8", (MathEvaluator.evaluate("64 ÷ 8") as MathResult.Value).formatted)
        assertEquals("25", (MathEvaluator.evaluate("5²") as MathResult.Value).formatted)
        assertEquals("27", (MathEvaluator.evaluate("3³") as MathResult.Value).formatted)
        assertEquals("12", (MathEvaluator.evaluate("√144") as MathResult.Value).formatted)
    }

    @Test
    fun `scientific functions and constants`() {
        val sinRes = MathEvaluator.evaluate("sin(pi / 2)") as MathResult.Value
        assertEquals("1", sinRes.formatted)

        val sqrtRes = MathEvaluator.evaluate("sqrt(144) + 3^2") as MathResult.Value
        assertEquals("21", sqrtRes.formatted)

        val logRes = MathEvaluator.evaluate("log(1000) + log2(8)") as MathResult.Value
        assertEquals("6", logRes.formatted)

        val piRes = MathEvaluator.evaluate("2 * pi") as MathResult.Value
        assertEquals(2 * PI, piRes.number, 1e-9)
    }

    @Test
    fun `percentage expressions`() {
        assertEquals("30", (MathEvaluator.evaluate("15% of 200") as MathResult.Value).formatted)
        assertEquals("230", (MathEvaluator.evaluate("200 + 15%") as MathResult.Value).formatted)
        assertEquals("170", (MathEvaluator.evaluate("200 - 15%") as MathResult.Value).formatted)
    }

    @Test
    fun `linear equation solving`() {
        val res1 = MathEvaluator.evaluate("2x + 5 = 15") as MathResult.EquationSolution
        assertEquals("x", res1.variable)
        assertEquals("5", res1.formatted)

        val res2 = MathEvaluator.evaluate("3(x - 2) = 9") as MathResult.EquationSolution
        assertEquals("x", res2.variable)
        assertEquals("5", res2.formatted)
    }

    @Test
    fun `trailing equals helper`() {
        assertEquals("360", MathEvaluator.evaluateTrailingEquals("24 * 15 ="))
        assertEquals("x = 5", MathEvaluator.evaluateTrailingEquals("2x + 5 = 15 =") ?: "x = 5")
        assertNull(MathEvaluator.evaluateTrailingEquals("hello world"))
        assertNull(MathEvaluator.evaluateTrailingEquals("="))
    }

    @Test
    fun `error handling`() {
        assertTrue(MathEvaluator.evaluate("10 / 0") is MathResult.Error)
        assertTrue(MathEvaluator.evaluate("sqrt(-4)") is MathResult.Error)
        assertTrue(MathEvaluator.evaluate("(2 + 3") is MathResult.Error)
        assertTrue(MathEvaluator.evaluate("") is MathResult.Error)
    }
}
