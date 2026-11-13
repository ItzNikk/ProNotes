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

    val isHighlighter: Boolean get() = Brushes.nameOf(stroke.brush) == Brushes.HIGHLIGHTER

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
    val rot: Float = 0f,
) : PageItem {
    /** Unrotated box. */
    val raw: RectF by lazy { measureText(text, size, x, y, style) }
    override val bounds: RectF by lazy { rotatedBounds(raw, rot) }
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
    val rot: Float = 0f,
) : PageItem {
    val raw: RectF get() = RectF(x, y, x + w, y + h)
    override val bounds: RectF get() = rotatedBounds(raw, rot)
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
    val rot: Float = 0f,
    /** Freehand tape: points relative to (x, y). Null for a straight strip. */
    val pts: FloatArray? = null,
    val thick: Float = 46f,
    /** See ItemDoc.Tape.pattern. */
    val pattern: String = "stripes",
) : PageItem {
    val path: android.graphics.Path? by lazy {
        pts?.let { p ->
            android.graphics.Path().apply {
                var i = 0
                while (i + 1 < p.size) { if (i == 0) moveTo(p[0], p[1]) else lineTo(p[i], p[i + 1]); i += 2 }
            }
        }
    }
    val raw: RectF get() = RectF(x, y, x + w, y + h)
    override val bounds: RectF get() = rotatedBounds(raw, rot)
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
    val rot: Float = 0f,
    val fontSize: Float = 28f,
    val font: String = "SANS",
    val collapsed: Boolean = false,
    val pinned: Boolean = false,
    val title: String = "",
    val style: String = "classic",
) : PageItem {
    val raw: RectF get() = RectF(x, y, x + w, y + (if (collapsed) 58f else h))
    override val bounds: RectF get() = rotatedBounds(raw, rot)
    override fun translated(dx: Float, dy: Float): PageItem = if (pinned) this else copy(x = x + dx, y = y + dy)
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

data class MindNodeItem(
    override val id: Long,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val label: String,
    val parentId: Long? = null,
    val color: Int = 0xFF3D6CF4.toInt(),
    val shape: String = "pill",
    val collapsed: Boolean = false,
    val notes: String = "",
    val tag: String = "",
) : PageItem {
    override val bounds: RectF get() = RectF(x, y, x + w, y + h)
    override fun translated(dx: Float, dy: Float): PageItem = copy(x = x + dx, y = y + dy)
    override fun withId(newId: Long): PageItem = copy(id = newId)
}

data class WebLinkItem(
    override val id: Long,
    val url: String,
    val title: String,
    val note: String = "",
    val x: Float,
    val y: Float,
    val w: Float = 380f,
    val h: Float = 136f,
    val color: Int = 0xFF2A78D6.toInt(),
    val rot: Float = 0f,
) : PageItem {
    val domain: String
        get() = runCatching {
            val normalized = if (url.startsWith("http://", true) || url.startsWith("https://", true)) url else "https://$url"
            android.net.Uri.parse(normalized).host?.removePrefix("www.") ?: url
        }.getOrElse { url }

    val raw: RectF get() = RectF(x, y, x + w, y + h)
    override val bounds: RectF get() = rotatedBounds(raw, rot)
    override fun translated(dx: Float, dy: Float): PageItem = copy(x = x + dx, y = y + dy)
    override fun withId(newId: Long): PageItem = copy(id = newId)
}

data class Page(
    val id: String,
    val width: Float,
    val height: Float,
    val paper: PaperStyle,
    val pdfPage: Int = -1,
    val items: List<PageItem> = emptyList(),
    val bookmarked: Boolean = false,
    val paperColor: Int? = null,
    val cover: String? = null,
    val bgImage: String? = null,
    val lineScale: Float = 1f,
    val lineColor: Int? = null,
    val isWhiteboard: Boolean = false,
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

/** Builds a freehand tape from absolute page points. */
fun freehandTape(id: Long, abs: FloatArray, color: Int, thick: Float, pattern: String = "stripes"): TapeItem {
    var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
    for (i in abs.indices step 2) { l = minOf(l, abs[i]); r = maxOf(r, abs[i]); t = minOf(t, abs[i + 1]); b = maxOf(b, abs[i + 1]) }
    val x = l - thick / 2f; val y = t - thick / 2f
    val rel = FloatArray(abs.size) { i -> abs[i] - if (i % 2 == 0) x else y }
    return TapeItem(id, x, y, (r - l) + thick, (b - t) + thick, color, pts = rel, thick = thick, pattern = pattern)
}

/** Axis-aligned bounds of [r] rotated by [deg] about its centre. */
fun rotatedBounds(r: RectF, deg: Float): RectF {
    if (deg == 0f) return RectF(r)
    val m = android.graphics.Matrix().apply { setRotate(deg, r.centerX(), r.centerY()) }
    return RectF(r).also { m.mapRect(it) }
}

/**
 * Applies a resize/rotate permanently. [m] maps page coordinates to their new place,
 * [s] is its uniform scale and [deg] its rotation.
 */
fun PageItem.transformed(m: android.graphics.Matrix, s: Float, deg: Float): PageItem {
    fun movedCenter(r: RectF): Pair<Float, Float> {
        val pt = floatArrayOf(r.centerX(), r.centerY()); m.mapPoints(pt); return pt[0] to pt[1]
    }
    return when (this) {
        is InkItem -> {
            val full = android.graphics.Matrix(m).apply { preTranslate(dx, dy) }
            InkItem(id, InkCodec.transformStroke(stroke, full, s))
        }
        is FillItem -> {
            val pts = FloatArray(points.size)
            for (i in points.indices step 2) { pts[i] = points[i] + dx; pts[i + 1] = points[i + 1] + dy }
            m.mapPoints(pts)
            FillItem(id, pts, color)
        }
        is TextItem -> {
            val (cx, cy) = movedCenter(raw)
            val nw = raw.width() * s; val nh = raw.height() * s
            copy(x = cx - nw / 2f, y = cy - nh / 2f, size = size * s, rot = (rot + deg) % 360f)
        }
        is ImageItem -> { val (cx, cy) = movedCenter(raw); copy(x = cx - w * s / 2f, y = cy - h * s / 2f, w = w * s, h = h * s, rot = (rot + deg) % 360f) }
        is StickyItem -> { val (cx, cy) = movedCenter(raw); copy(x = cx - w * s / 2f, y = cy - h * s / 2f, w = w * s, h = h * s, rot = (rot + deg) % 360f) }
        is MindNodeItem -> { val (cx, cy) = movedCenter(bounds); val nw = (w * s).coerceIn(110f, 520f); val nh = (h * s).coerceIn(48f, 220f); copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh) }
        is WebLinkItem -> { val (cx, cy) = movedCenter(raw); val nw = (w * s).coerceIn(220f, 760f); val nh = (h * s).coerceIn(96f, 340f); copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh, rot = (rot + deg) % 360f) }
        is TapeItem -> if (pts != null) {
            val abs = FloatArray(pts.size) { i -> pts[i] + if (i % 2 == 0) x else y }
            m.mapPoints(abs)
            freehandTape(id, abs, color, thick * s)
        } else { val (cx, cy) = movedCenter(raw); copy(x = cx - w * s / 2f, y = cy - h * s / 2f, w = w * s, h = h * s, rot = (rot + deg) % 360f) }
    }
}

/**
 * Applies free horizontal/vertical stretching from [oldBox] to [newBox] for EVERY PageItem subtype.
 */
fun PageItem.stretched(oldBox: RectF, newBox: RectF): PageItem {
    val sx = (newBox.width() / oldBox.width().coerceAtLeast(1f)).coerceIn(0.15f, 12f)
    val sy = (newBox.height() / oldBox.height().coerceAtLeast(1f)).coerceIn(0.15f, 12f)
    val m = android.graphics.Matrix().apply { setRectToRect(oldBox, newBox, android.graphics.Matrix.ScaleToFit.FILL) }
    fun mapPt(px: Float, py: Float): Pair<Float, Float> {
        val a = floatArrayOf(px, py)
        m.mapPoints(a)
        return a[0] to a[1]
    }
    return when (this) {
        is InkItem -> {
            val avgS = ((sx + sy) / 2f).coerceIn(0.25f, 8f)
            val pts = stroke.inputs
            val newPts = List(pts.size) { i ->
                val p = pts.get(i)
                mapPt(p.x + dx, p.y + dy)
            }
            val b = Brushes.create(Brushes.nameOf(stroke.brush), stroke.brush.colorIntArgb, (stroke.brush.size * avgS).coerceIn(1f, 80f))
            InkItem(id, InkCodec.strokeFromPoints(newPts, b))
        }
        is FillItem -> {
            val pts = FloatArray(points.size)
            for (i in points.indices step 2) {
                pts[i] = points[i] + dx
                pts[i + 1] = points[i + 1] + dy
            }
            m.mapPoints(pts)
            FillItem(id, pts, color)
        }
        is TextItem -> {
            val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
            val nw = raw.width() * sx
            val nh = raw.height() * sy
            val avgS = ((sx + sy) / 2f).coerceIn(0.25f, 6f)
            copy(x = cx - nw / 2f, y = cy - nh / 2f, size = (size * avgS).coerceIn(12f, 220f))
        }
        is ImageItem -> {
            val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
            val nw = (w * sx).coerceIn(32f, 4000f)
            val nh = (h * sy).coerceIn(32f, 4000f)
            copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh)
        }
        is StickyItem -> {
            val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
            val nw = (w * sx).coerceIn(140f, 1400f)
            val nh = (h * sy).coerceIn(120f, 1400f)
            copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh)
        }
        is MindNodeItem -> {
            val (cx, cy) = mapPt(bounds.centerX(), bounds.centerY())
            val nw = (w * sx).coerceIn(110f, 900f)
            val nh = (h * sy).coerceIn(48f, 520f)
            copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh)
        }
        is WebLinkItem -> {
            val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
            val nw = (w * sx).coerceIn(200f, 1200f)
            val nh = (h * sy).coerceIn(90f, 600f)
            copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh)
        }
        is TapeItem -> if (pts != null) {
            val abs = FloatArray(pts.size) { i -> pts[i] + if (i % 2 == 0) x else y }
            m.mapPoints(abs)
            val avgS = ((sx + sy) / 2f).coerceIn(0.3f, 4f)
            freehandTape(id, abs, color, thick * avgS, pattern)
        } else {
            val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
            val nw = (w * sx).coerceIn(40f, 2000f)
            val nh = (h * sy).coerceIn(18f, 800f)
            copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh)
        }
    }
}

val textPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

fun applyTextStyle(paint: TextPaint, size: Float, style: TextStyleSpec) {
    paint.textSize = size
    val styleFlags = when {
        style.bold && style.italic -> Typeface.BOLD_ITALIC
        style.bold -> Typeface.BOLD
        style.italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }
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
