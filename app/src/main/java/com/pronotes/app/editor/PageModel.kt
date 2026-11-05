package com.pronotes.app.editor

import android.graphics.RectF
import android.text.TextPaint
import androidx.ink.strokes.Stroke
import com.pronotes.app.data.PaperStyle
import java.util.concurrent.atomic.AtomicLong

object ItemIds {
    private val counter = AtomicLong(System.nanoTime())
    fun next(): Long = counter.incrementAndGet()
}

/** Everything that can live on a page. Coordinates are in page ("world") units. */
sealed interface PageItem {
    val id: Long
    val bounds: RectF
    fun translated(dx: Float, dy: Float): PageItem
}

class InkItem(
    override val id: Long,
    val stroke: Stroke,
    val dx: Float = 0f,
    val dy: Float = 0f,
    private val cachedLocal: RectF? = null,
) : PageItem {
    /** Bounds in the stroke's own coordinates, before the dx/dy offset. */
    val localBounds: RectF = cachedLocal ?: strokeBounds(stroke)

    override val bounds: RectF
        get() = RectF(localBounds).apply { offset(dx, dy) }

    override fun translated(dx: Float, dy: Float): PageItem =
        InkItem(id, stroke, this.dx + dx, this.dy + dy, localBounds)

    fun withStroke(newStroke: Stroke): InkItem = InkItem(id, newStroke, dx, dy, localBounds)
}

data class TextItem(
    override val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Int,
) : PageItem {
    override val bounds: RectF by lazy { measureText(text, size, x, y) }
    override fun translated(dx: Float, dy: Float): PageItem = copy(x = x + dx, y = y + dy)
}

data class ImageItem(
    override val id: Long,
    val file: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
) : PageItem {
    override val bounds: RectF get() = RectF(x, y, x + w, y + h)
    override fun translated(dx: Float, dy: Float): PageItem = copy(x = x + dx, y = y + dy)
}

/** Washi tape that hides what is under it until tapped. [revealed] is not saved. */
data class TapeItem(
    override val id: Long,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val color: Int,
    val revealed: Boolean = false,
) : PageItem {
    override val bounds: RectF get() = RectF(x, y, x + w, y + h)
    override fun translated(dx: Float, dy: Float): PageItem = copy(x = x + dx, y = y + dy)
}

data class StickyItem(
    override val id: Long,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val color: Int,
    val text: String,
) : PageItem {
    override val bounds: RectF get() = RectF(x, y, x + w, y + h)
    override fun translated(dx: Float, dy: Float): PageItem = copy(x = x + dx, y = y + dy)
}

data class Page(
    val id: String,
    val width: Float,
    val height: Float,
    val paper: PaperStyle,
    val pdfPage: Int = -1,
    val items: List<PageItem> = emptyList(),
)

fun strokeBounds(stroke: Stroke): RectF {
    val inputs = stroke.inputs
    if (inputs.size == 0) return RectF()
    var l = Float.MAX_VALUE
    var t = Float.MAX_VALUE
    var r = -Float.MAX_VALUE
    var b = -Float.MAX_VALUE
    for (i in 0 until inputs.size) {
        val p = inputs.get(i)
        if (p.x < l) l = p.x
        if (p.y < t) t = p.y
        if (p.x > r) r = p.x
        if (p.y > b) b = p.y
    }
    val pad = stroke.brush.size / 2f + 2f
    return RectF(l - pad, t - pad, r + pad, b + pad)
}

/** Points of a stroke in world coordinates (offset applied). */
fun InkItem.worldPoints(): List<Pair<Float, Float>> {
    val inputs = stroke.inputs
    return List(inputs.size) { i ->
        val p = inputs.get(i)
        (p.x + dx) to (p.y + dy)
    }
}

val textPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

fun measureText(text: String, size: Float, x: Float, y: Float): RectF {
    textPaint.textSize = size
    val lines = text.split('\n')
    val width = lines.maxOf { textPaint.measureText(it) }.coerceAtLeast(size)
    val lineHeight = size * 1.3f
    return RectF(x, y, x + width, y + lineHeight * lines.size)
}
