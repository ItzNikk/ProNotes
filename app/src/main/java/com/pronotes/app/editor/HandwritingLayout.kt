package com.pronotes.app.editor

import android.graphics.Paint
import android.graphics.RectF
import com.pronotes.app.data.PaperStyle
import kotlin.math.abs
import kotlin.math.round

/** Group nearby strokes by writing band, keeping the recognizer's original stroke order. */
internal fun handwritingLines(ink: List<InkItem>): List<List<InkItem>> {
    val groups = mutableListOf<MutableList<InkItem>>()
    for (stroke in ink) {
        val box = stroke.bounds
        val group = groups.lastOrNull()
        val anchor = group?.first()?.bounds
        val last = group?.last()?.bounds
        if (anchor != null && last != null &&
            abs(anchor.centerY() - box.centerY()) <= maxOf(20f, anchor.height() * .60f) &&
            box.left - last.right <= maxOf(64f, anchor.height() * 2f) &&
            last.left - box.right <= maxOf(48f, anchor.height() * 1.5f)
        ) {
            group.add(stroke)
        } else {
            groups.add(mutableListOf(stroke))
        }
    }
    return groups.filter { it.size >= 2 }
}

/** Keep the result on one physical line. Leave the ink intact if it cannot fit legibly. */
internal fun singleLineLayout(
    page: Page,
    text: String,
    font: TextFont,
    x: Float,
    ink: RectF,
    estimatedSize: Float,
    align: Boolean,
): Pair<Float, Float>? {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = font.typeface
        textSize = estimatedSize.coerceIn(14f, 72f)
    }
    val width = paint.measureText(text)
    if (width <= 0f) return null
    val size = paint.textSize * ((page.width - x - 8f) / width).coerceAtMost(1f)
    if (size < 14f || size < paint.textSize * .65f) return null
    val ruled = when (page.paper) {
        PaperStyle.RULED -> 180f to 52f
        PaperStyle.NARROW -> 160f to 38f
        PaperStyle.COLLEGE -> 170f to 42f
        PaperStyle.WIDE -> 190f to 64f
        PaperStyle.LEGAL -> 220f to 50f
        else -> null
    }
    val adjacent = if (align) page.items.asSequence().filterIsInstance<TextItem>()
        .filter { it.style.font == font && it.rot == 0f && !it.text.contains('\n') && x - it.bounds.right in 0f..(size * 2f) }
        .map { it.y + it.size }.filter { abs(it - ink.bottom) < size * .6f }
        .minByOrNull { abs(it - ink.bottom) } else null
    val baseline = adjacent ?: if (align && ruled != null && page.pdfPage < 0) {
        val (start, spacing) = ruled
        val gap = spacing * page.lineScale.coerceIn(.5f, 2.5f)
        val nearest = start + round((ink.bottom - start) / gap).coerceAtLeast(0f) * gap
        if (abs(nearest - ink.bottom) <= gap * .36f && nearest < page.height - 40f) nearest else ink.bottom
    } else ink.bottom
    val top = (baseline - size).coerceAtLeast(0f)
    return if (top + size * 1.3f <= page.height) size to top else null
}
