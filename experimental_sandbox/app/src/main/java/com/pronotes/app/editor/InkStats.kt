package com.pronotes.app.editor

import kotlin.math.hypot

/**
 * Computes stroke-level ink statistics for a page (total strokes, path length,
 * pen vs. highlighter split, and bounding-box coverage).
 */
object InkStats {

    data class StrokeSample(
        val points: List<Pair<Float, Float>>,
        val isHighlighter: Boolean = false,
    )

    data class Summary(
        val totalStrokes: Int,
        val penStrokes: Int,
        val highlighterStrokes: Int,
        val totalPathLengthPx: Double,
        val boundingWidthPx: Float,
        val boundingHeightPx: Float,
    )

    fun analyze(strokes: List<StrokeSample>): Summary {
        if (strokes.isEmpty()) {
            return Summary(
                totalStrokes = 0,
                penStrokes = 0,
                highlighterStrokes = 0,
                totalPathLengthPx = 0.0,
                boundingWidthPx = 0f,
                boundingHeightPx = 0f,
            )
        }

        var penCount = 0
        var hlCount = 0
        var totalLen = 0.0
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var hasPoint = false

        for (stroke in strokes) {
            if (stroke.isHighlighter) hlCount++ else penCount++
            val pts = stroke.points
            for (i in pts.indices) {
                val (x, y) = pts[i]
                hasPoint = true
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
                if (i > 0) {
                    val (px, py) = pts[i - 1]
                    totalLen += hypot((x - px).toDouble(), (y - py).toDouble())
                }
            }
        }

        return Summary(
            totalStrokes = strokes.size,
            penStrokes = penCount,
            highlighterStrokes = hlCount,
            totalPathLengthPx = totalLen,
            boundingWidthPx = if (hasPoint) (maxX - minX).coerceAtLeast(0f) else 0f,
            boundingHeightPx = if (hasPoint) (maxY - minY).coerceAtLeast(0f) else 0f,
        )
    }
}
