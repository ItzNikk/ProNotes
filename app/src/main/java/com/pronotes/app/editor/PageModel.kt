package com.pronotes.app.editor

import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
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
    fun withId(newId: Long): PageItem
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

    val isHighlighter: Boolean get() = Brushes.nameOf(stroke.brush.family) == Brushes.HIGHLIGHTER

    override val bounds: RectF
        get() = RectF(localBounds).apply { offset(dx, dy) }

    override fun translated(dx: Float, dy: Float): PageItem =
        InkItem(id, stroke, this.dx + dx, this.dy + dy, localBounds)

    override fun withId(newId: Long): PageItem = InkItem(newId, stroke, dx, dy, localBounds)

    fun withStroke(newStroke: Stroke): InkItem = InkItem(id, newStroke, dx, dy, localBounds)
}

enum class TextFont(val label: String, val typeface: Typeface) {
    SANS("Sans", Typeface.SANS_SERIF),
    SERIF("Serif", Typeface.SERIF),
    MONO("Mono", Typeface.MONOSPACE),
    ROUNDED("Rounded", Typeface.create("sans-serif-medium", Typeface.NORMAL)),
    CONDENSED("Condensed", Typeface.create("sans-serif-condensed", Typeface.NORMAL)),
    HAND("Casual", Typeface.create("casual", Typeface.NORMAL)),
    CURSIVE("Cursive", Typeface.create("cursive", Typeface.NORMAL)),
}

enum class TextAlign { LEFT, CENTER, RIGHT }

/** Formatting for typed text. [gradient] is an optional second colour. */
data class TextStyleSpec(
    val font: TextFont = TextFont.SANS,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val gradient: Int? = null,
    val align: TextAlign = TextAlign.LEFT,
)

data class TextItem(
    override val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Int,
    val style: TextStyleSpec = TextStyleSpec(),
) : PageItem {
    override val bounds: RectF by lazy { measureText(text, size, x, y, style) }
    override fun translated(dx: Float, dy: Float): PageItem = copy(x = x + dx, y = y + dy)
    override fun withId(newId: Long): PageItem = copy(id = newId)
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
    override fun withId(newId: Long): PageItem = copy(id = newId)
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
    override fun withId(newId: Long): PageItem = copy(id = newId)
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
    override fun withId(newId: Long): PageItem = copy(id = newId)
}

/** Solid fill inside a closed shape. Drawn under the shape's outline. */
class FillItem(
    override val id: Long,
    val points: FloatArray,
    val color: Int,
    val dx: Float = 0f,
    val dy: Float = 0f,
) : PageItem {
    val path: Path by lazy {
        Path().apply {
            var i = 0
            while (i + 1 < points.size) {
                if (i == 0) moveTo(points[0], points[1]) else lineTo(points[i], points[i + 1])
                i += 2
            }
            close()
        }
    }
    private val local: RectF by lazy { RectF().also { path.computeBounds(it, true) } }
    override val bounds: RectF get() = RectF(local).apply { offset(dx, dy) }
    override fun translated(dx: Float, dy: Float): PageItem = FillItem(id, points, color, this.dx + dx, this.dy + dy)
    override fun withId(newId: Long): PageItem = FillItem(newId, points, color, dx, dy)
    fun recolored(c: Int) = FillItem(id, points, c, dx, dy)
}

data class Page(
    val id: String,
    val width: Float,
    val height: Float,
    val paper: PaperStyle,
    val pdfPage: Int = -1,
    val items: List<PageItem> = emptyList(),
    val bookmarked: Boolean = false,
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

fun applyTextStyle(paint: TextPaint, size: Float, style: TextStyleSpec) {
    paint.textSize = size
    val styleFlags = (if (style.bold) Typeface.BOLD else 0) or (if (style.italic) Typeface.ITALIC else 0)
    paint.typeface = Typeface.create(style.font.typeface, styleFlags)
    paint.isUnderlineText = style.underline
    paint.textAlign = android.graphics.Paint.Align.LEFT
}

fun measureText(text: String, size: Float, x: Float, y: Float, style: TextStyleSpec = TextStyleSpec()): RectF {
    applyTextStyle(textPaint, size, style)
    val lines = text.split('\n')
    val width = lines.maxOf { textPaint.measureText(it) }.coerceAtLeast(size)
    val lineHeight = size * 1.3f
    return RectF(x, y, x + width, y + lineHeight * lines.size)
}
