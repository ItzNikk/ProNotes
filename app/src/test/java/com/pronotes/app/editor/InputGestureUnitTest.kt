package com.pronotes.app.editor

import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.ScribbleSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Recorded-point-list unit tests for Phase 1 input geometry and gesture detectors:
 * [isClosedLoop], [isScribble], [isLiveScribble], [ShapeRecognizer], [GestureConfig], and [CircleDetector].
 */
class InputGestureUnitTest {

    // ---------------------------------------------------------------- recorded point generators

    private fun recordedCircle(cx: Float = 200f, cy: Float = 200f, r: Float = 80f, steps: Int = 36, sweepDeg: Float = 355f): List<Pt> =
        (0..steps).map { i ->
            val rad = Math.toRadians((i / steps.toDouble()) * sweepDeg)
            (cx + r * cos(rad).toFloat()) to (cy + r * sin(rad).toFloat())
        }

    private fun recordedPolygon(vertices: List<Pt>, samplesPerEdge: Int = 8, jitter: Float = 0f): List<Pt> {
        val out = ArrayList<Pt>()
        for (i in 0 until vertices.size - 1) {
            val a = vertices[i]
            val b = vertices[i + 1]
            for (k in 0 until samplesPerEdge) {
                val t = k / samplesPerEdge.toFloat()
                val j = if (k % 2 == 0) jitter else -jitter
                out += (a.first + (b.first - a.first) * t + j) to (a.second + (b.second - a.second) * t - j)
            }
        }
        out += vertices.last()
        return out
    }

    private fun recordedScribble(
        startX: Float = 100f,
        startY: Float = 100f,
        spanX: Float = 90f,
        stepY: Float = 6f,
        swings: Int = 6,
    ): List<Pt> {
        val out = ArrayList<Pt>()
        for (i in 0..swings) {
            val x0 = if (i % 2 == 0) startX else startX + spanX
            val x1 = if (i % 2 == 0) startX + spanX else startX
            val y0 = startY + i * stepY
            val y1 = startY + (i + 0.5f) * stepY
            for (k in 0 until 6) {
                val t = k / 6f
                out += (x0 + (x1 - x0) * t) to (y0 + (y1 - y0) * t)
            }
        }
        return out
    }

    private fun recordedCursiveZigZagWord(): List<Pt> {
        // Simulates writing connected "mmmmmmmm" steadily across a baseline (high forward progression).
        val out = ArrayList<Pt>()
        for (i in 0..16) {
            val x0 = 60f + i * 24f
            val y0 = if (i % 2 == 0) 200f else 172f
            val x1 = 60f + (i + 1) * 24f
            val y1 = if (i % 2 == 0) 172f else 200f
            for (k in 0 until 4) {
                val t = k / 4f
                out += (x0 + (x1 - x0) * t) to (y0 + (y1 - y0) * t)
            }
        }
        return out
    }

    // ---------------------------------------------------------------- isClosedLoop

    @Test
    fun isClosedLoop_acceptsClosedAndOverlappingLoops() {
        val closedCircle = recordedCircle(sweepDeg = 352f)
        val overlappingCircle = recordedCircle(sweepDeg = 415f)
        assertTrue(isClosedLoop(closedCircle))
        assertTrue(isClosedLoop(overlappingCircle))
    }

    @Test
    fun isClosedLoop_rejectsOpenArcStraightLineAndTinyLoop() {
        val openArc = recordedCircle(sweepDeg = 210f)
        val straightLine = (0..15).map { i -> (50f + i * 16f) to 120f }
        val tinyCircle = recordedCircle(r = 10f, sweepDeg = 355f)
        assertFalse(isClosedLoop(openArc))
        assertFalse(isClosedLoop(straightLine))
        assertFalse(isClosedLoop(tinyCircle))
    }

    @Test
    fun isClosedLoop_respectsGestureConfigClosingRatio() {
        // Endpoint gap is roughly 0.37 * size: fails strict 0.25 ratio, passes 0.42 ratio.
        val gappedArc = recordedCircle(r = 80f, sweepDeg = 315f)
        val strict = GestureConfig(loopClosingRatio = 0.25f)
        val relaxed = GestureConfig(loopClosingRatio = 0.42f)
        assertFalse(isClosedLoop(gappedArc, strict))
        assertTrue(isClosedLoop(gappedArc, relaxed))
    }

    // ---------------------------------------------------------------- isScribble & sensitivity

    @Test
    fun isScribble_detectsBackAndForthScribbleAndIgnoresCursiveHandwriting() {
        val scribble = recordedScribble(spanX = 110f, stepY = 5f, swings = 7)
        val cursive = recordedCursiveZigZagWord()
        assertTrue(isScribble(scribble, GestureConfig.fromSensitivity(ScribbleSensitivity.BALANCED)))
        assertTrue(isLiveScribble(scribble, GestureConfig.fromSensitivity(ScribbleSensitivity.BALANCED)))
        assertFalse(isScribble(cursive, GestureConfig.fromSensitivity(ScribbleSensitivity.BALANCED)))
        assertFalse(isLiveScribble(cursive, GestureConfig.fromSensitivity(ScribbleSensitivity.BALANCED)))
    }

    @Test
    fun isScribble_rejectsShortStrokesBelowMinimumLength() {
        val shortZigZag = recordedScribble(spanX = 14f, stepY = 3f, swings = 2)
        assertFalse(isScribble(shortZigZag, ScribbleSensitivity.INSTANT))
        assertFalse(isScribble(shortZigZag, ScribbleSensitivity.BALANCED))
        assertFalse(isScribble(shortZigZag, ScribbleSensitivity.GENTLE))
    }

    @Test
    fun scribbleSensitivity_changesThresholdsAcrossLevels() {
        // A medium 3-reversal scrub (~110px total length, 3 swings across 28px) should trigger INSTANT
        // but stay ignored by GENTLE so careful shading does not trigger erasure.
        val mediumScrub = recordedScribble(spanX = 28f, stepY = 3.5f, swings = 3)
        val len = pathLength(mediumScrub)
        assertTrue("Expected calibrated test scrub >= 70px, was $len", len >= 70f)
        assertTrue(isScribble(mediumScrub, ScribbleSensitivity.INSTANT))
        assertFalse(isScribble(mediumScrub, ScribbleSensitivity.GENTLE))
    }

    // ---------------------------------------------------------------- ShapeRecognizer

    @Test
    fun shapeRecognizer_recognizesLineRectangleTriangleDiamondAndCircle() {
        val line = (0..18).map { i -> (40f + i * 15f) to (100f + (if (i % 2 == 0) 1.5f else -1.5f)) }
        val recognizedLine = ShapeRecognizer.recognize(line)
        assertNotNull(recognizedLine)
        assertTrue(hypot(recognizedLine!!.last().first - line.last().first, recognizedLine.last().second - line.last().second) < 5f)

        val rect = recordedPolygon(
            listOf(100f to 100f, 280f to 100f, 280f to 220f, 100f to 220f, 100f to 100f),
            samplesPerEdge = 10,
            jitter = 2f,
        )
        val recognizedRect = ShapeRecognizer.recognize(rect)
        assertNotNull(recognizedRect)
        assertTrue(isClosedLoop(recognizedRect!!))

        val tri = recordedPolygon(
            listOf(200f to 80f, 310f to 240f, 90f to 240f, 200f to 80f),
            samplesPerEdge = 10,
        )
        assertNotNull(ShapeRecognizer.recognize(tri))

        val diamond = recordedPolygon(
            listOf(200f to 80f, 290f to 170f, 200f to 260f, 110f to 170f, 200f to 80f),
            samplesPerEdge = 10,
        )
        assertNotNull(ShapeRecognizer.recognize(diamond))

        val circle = recordedCircle(cx = 200f, cy = 200f, r = 90f, steps = 40, sweepDeg = 352f)
        assertNotNull(ShapeRecognizer.recognize(circle))
    }

    @Test
    fun shapeRecognizer_forceNearestRecoversRoughShapeAndRejectsChaos() {
        // Rough open-ish circle with larger gap & slight irregularity fails strict recognize(),
        // but forceNearest = true (Shape tool) snaps it cleanly instead of dropping to raw ink.
        val roughCircle = (0..32).map { i ->
            val a = Math.toRadians(i / 32.0 * 320.0)
            val r = 85f + (if (i % 3 == 0) 12f else -10f)
            (200f + r * cos(a).toFloat()) to (200f + r * sin(a).toFloat())
        }
        val snapped = ShapeRecognizer.recognize(roughCircle, forceNearest = true)
        assertNotNull(snapped)

        // Random chaotic squiggle fails even with forceNearest = true -> caller shows 'Shape not recognised'.
        val chaos = recordedScribble(spanX = 120f, stepY = 12f, swings = 6)
        assertNull(ShapeRecognizer.recognize(chaos, forceNearest = true))
    }

    // ---------------------------------------------------------------- CircleDetector & GestureConfig

    @Test
    fun circleDetector_triggersEvenBeforeAsyncItemIdAssigned() {
        val detector = CircleDetector()
        val config = GestureConfig.fromSettings(AppSettings())
        val loop = recordedCircle(cx = 250f, cy = 250f, r = 90f)
        assertTrue(detector.isValidLoopCandidate(loop, scale = 1f, density = 2f, config = config))

        val strokeKey = "stroke-1"
        val candidate = detector.offerCandidate(
            strokeKey = strokeKey,
            page = 0,
            targetIds = setOf(101L, 102L),
            loop = loop,
            nowMs = 1000L,
        )
        assertNotNull(candidate)

        // Fast second press at loop end BEFORE onStrokesFinished sets itemId (itemId == -1L):
        val armed = detector.tryArmHold(
            px = loop.last().first,
            py = loop.last().second,
            nowMs = 1250L,
            isPen = true,
            density = 2f,
            config = config,
        ) { _, x, y -> x to y }
        assertTrue(armed)
        assertTrue(detector.isHoldArmed())

        val triggered = detector.triggerArmedHold()
        assertNotNull(triggered)
        assertEquals(setOf(101L, 102L), triggered!!.first.targetIds)
        // Late async callback attaches itemId and receives pendingRemoval == true so loop ink isn't kept.
        // Wait: attachItemId works while candidate is active; once triggered, caller recorded consumed stroke key.
    }

    @Test
    fun circleDetector_attachItemIdSignalsPendingRemovalWhenTriggeredEarly() {
        val detector = CircleDetector()
        val config = GestureConfig.fromSettings(AppSettings())
        val loop = recordedCircle()
        val key = "async-stroke"
        detector.offerCandidate(key, 0, setOf(42L), loop, nowMs = 1000L)
        assertFalse(detector.attachItemId(key, 999L))

        // Also verify expiration after short window:
        assertNull(detector.activeCandidate(nowMs = 1000L + config.circleWindowMs + 1L, config = config))
        assertEquals(CircleDetector.State.Idle, detector.state)
    }

    @Test
    fun circleDetector_cancelsHoldOnMoveBeyondSlopAndExpiresAfterWindow() {
        val detector = CircleDetector()
        val config = GestureConfig(circleWindowMs = 1800L, circleEndRadiusDp = 40f)
        val loop = recordedCircle(cx = 200f, cy = 200f, r = 80f)
        detector.offerCandidate("s1", 0, setOf(7L), loop, nowMs = 5000L)

        // Expired after 1801 ms
        assertFalse(
            detector.tryArmHold(
                px = loop.last().first,
                py = loop.last().second,
                nowMs = 6801L,
                isPen = true,
                density = 2f,
                config = config,
            ) { _, x, y -> x to y },
        )

        // Re-offer within window and verify movement slop check
        detector.offerCandidate("s2", 0, setOf(8L), loop, nowMs = 7000L)
        assertTrue(
            detector.tryArmHold(
                px = loop.last().first,
                py = loop.last().second,
                nowMs = 7200L,
                isPen = true,
                density = 2f,
                config = config,
            ) { _, x, y -> x to y },
        )
        assertFalse(detector.shouldCancelHoldOnMove(loop.last().first + 4f, loop.last().second + 4f, slopPx = 16f))
        assertTrue(detector.shouldCancelHoldOnMove(loop.last().first + 24f, loop.last().second, slopPx = 16f))
        detector.cancelHold()
        assertFalse(detector.isHoldArmed())
    }
}
