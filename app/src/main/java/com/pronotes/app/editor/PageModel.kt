package com.pronotes.app.editor

import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.ink.strokes.Stroke
import com.pronotes.app.data.PaperStyle
import java.util.concurrent.atomic.AtomicLong

object ItemIds {
    private val counter = AtomicLong(System.nanoTime())
    fun next(): Long = counter.incrementAndGet()
}

/** Everything that can live on a page. Coordinates are in page ("world") units. */
@Immutable
sealed interface PageItem {
    val id: Long
    val bounds: RectF
    fun translated(dx: Float, dy: Float): PageItem
    fun withId(newId: Long): PageItem
}

@Stable
class InkItem(
    override val id: Long,
    val stroke: Stroke,
    val dx: Float = 0f,
    val dy: Float = 0f,
    private val cachedLocal: RectF? = null,
    val familyHint: String? = null,
) : PageItem {
    /** Bounds in the stroke's own coordinates, before the dx/dy offset. */
    val localBounds: RectF = cachedLocal ?: strokeBounds(stroke)

    val brushFamily: String get() = familyHint?.takeIf { it.isNotBlank() } ?: Brushes.nameOf(stroke.brush)
    val isHighlighter: Boolean get() = brushFamily == Brushes.HIGHLIGHTER
    val isCrayon: Boolean get() = brushFamily == Brushes.CRAYON
    val isPencil: Boolean get() = brushFamily == Brushes.PENCIL
    val isBrushPen: Boolean get() = brushFamily == Brushes.BRUSH
    val isCalligraphy: Boolean get() = brushFamily == Brushes.CALLIGRAPHY
    val isLettering: Boolean get() = brushFamily == Brushes.LETTERING
    val isBallPen: Boolean get() = brushFamily == Brushes.BALL
    val isFineliner: Boolean get() = brushFamily == Brushes.FINELINER || brushFamily == Brushes.MARKER

    override val bounds: RectF
        get() = RectF(localBounds).apply { offset(dx, dy) }

    override fun translated(dx: Float, dy: Float): PageItem =
        InkItem(id, stroke, this.dx + dx, this.dy + dy, localBounds, familyHint)

    override fun withId(newId: Long): PageItem = InkItem(newId, stroke, dx, dy, localBounds, familyHint)

    fun withStroke(newStroke: Stroke): InkItem = InkItem(id, newStroke, dx, dy, localBounds, familyHint)
}

enum class TextFont(val label: String, val typeface: Typeface) {
    SANS("Sans", Typeface.SANS_SERIF),
    SERIF("Serif", Typeface.SERIF),
    MONO("Mono", Typeface.MONOSPACE),
    ROUNDED("Rounded", Typeface.create("sans-serif-medium", Typeface.NORMAL)),
    CONDENSED("Condensed", Typeface.create("sans-serif-condensed", Typeface.NORMAL)),
    LIGHT("Light", Typeface.create("sans-serif-light", Typeface.NORMAL)),
    FINE("Fine italic", Typeface.create("sans-serif-light", Typeface.ITALIC)),
    SERIF_BOLD("Book bold", Typeface.create("serif", Typeface.BOLD)),
    HUMANIST("Humanist", Typeface.create("sans-serif", Typeface.ITALIC)),
    MEDIUM_SCRIPT("Medium script", Typeface.create("cursive", Typeface.BOLD)),
    TYPEWRITER("Serif mono", Typeface.create("serif-monospace", Typeface.NORMAL)),
    NARROW_ITALIC("Narrow italic", Typeface.create("sans-serif-condensed", Typeface.ITALIC)),
    SCRIPT("Script italic", Typeface.create("cursive", Typeface.ITALIC)),
    BOOK("Book italic", Typeface.create("serif", Typeface.ITALIC)),
    HAND("Casual", Typeface.create("casual", Typeface.NORMAL)),
    CURSIVE("Cursive", Typeface.create("cursive", Typeface.NORMAL)),
}

enum class TextAlign { LEFT, CENTER, RIGHT }

/** Formatting for typed text. [gradient] is an optional second colour. */
@Immutable
data class TextStyleSpec(
    val font: TextFont = TextFont.SANS,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val gradient: Int? = null,
    val align: TextAlign = TextAlign.LEFT,
    val customFontPath: String = "",
)

@Immutable
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

@Immutable
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

/** Masking tape that hides what is under it until tapped. [revealed] is not saved. */
@Stable
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

@Immutable
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
    val paperBounds: RectF get() = RectF(x, y, x + w, y + h)
    val raw: RectF get() = if (collapsed) RectF(x, y, x + 76f, y + 76f) else paperBounds
    override val bounds: RectF get() = rotatedBounds(raw, rot)
    override fun translated(dx: Float, dy: Float): PageItem = if (pinned) this else copy(x = x + dx, y = y + dy)
    override fun withId(newId: Long): PageItem = copy(id = newId)
}

/** Solid fill inside a closed shape. Drawn under the shape's outline. */
@Stable
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

@Immutable
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

@Immutable
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
    val embedded: Boolean = false,
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

@Immutable
data class WriteBoxItem(
    override val id: Long,
    val x: Float,
    val y: Float,
    val w: Float = 640f,
    val h: Float = 320f,
    val lineSpacing: Float = 48f,
    val strokeColor: Int = 0x884A70E2.toInt(),
    val fillColor: Int = 0x144A70E2.toInt(),
    val inkItemIds: List<Long> = emptyList(),
    val convertedText: String = "",
    val isConverted: Boolean = false,
    val font: TextFont = TextFont.SANS,
    val fontSize: Float = 24f,
    val rot: Float = 0f,
) : PageItem {
    val raw: RectF get() = RectF(x, y, x + w, y + h)
    override val bounds: RectF get() = rotatedBounds(raw, rot)
    override fun translated(dx: Float, dy: Float): PageItem = copy(x = x + dx, y = y + dy)
    override fun withId(newId: Long): PageItem = copy(id = newId)
}

/** Editable table grid that works on both notebook and PDF pages: tap any cell to type or write/highlight directly on top. */
@Immutable
data class TableItem(
    override val id: Long,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val rows: Int = 3,
    val cols: Int = 3,
    val cells: List<String> = List(9) { "" },
    val color: Int = 0xFF2563EB.toInt(),
    val headerRow: Boolean = true,
    val fontSize: Float = 22f,
    val rot: Float = 0f,
) : PageItem {
    val raw: RectF get() = RectF(x, y, x + w, y + h)
    override val bounds: RectF get() = rotatedBounds(raw, rot)
    override fun translated(dx: Float, dy: Float): PageItem = copy(x = x + dx, y = y + dy)
    override fun withId(newId: Long): PageItem = copy(id = newId)
    fun cellText(r: Int, c: Int): String = cells.getOrNull(r * cols + c).orEmpty()
    fun cellAt(px: Float, py: Float): Pair<Int, Int>? {
        if (!raw.contains(px, py) || rows <= 0 || cols <= 0) return null
        val col = (((px - x) / w.coerceAtLeast(1f)) * cols).toInt().coerceIn(0, cols - 1)
        val row = (((py - y) / h.coerceAtLeast(1f)) * rows).toInt().coerceIn(0, rows - 1)
        return row to col
    }
}

@Immutable
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
            InkItem(id, InkCodec.transformStroke(stroke, full, s), familyHint = familyHint)
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
        is StickyItem -> {
            if (pinned) this
            else if (collapsed) {
                val (cx, cy) = movedCenter(raw)
                copy(x = cx - 38f, y = cy - 38f, rot = (rot + deg) % 360f)
            } else {
                val (cx, cy) = movedCenter(raw)
                copy(x = cx - w * s / 2f, y = cy - h * s / 2f, w = (w * s).coerceIn(140f, 1400f), h = (h * s).coerceIn(120f, 1400f), rot = (rot + deg) % 360f)
            }
        }
        is MindNodeItem -> { val (cx, cy) = movedCenter(bounds); val nw = (w * s).coerceIn(110f, 520f); val nh = (h * s).coerceIn(48f, 220f); copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh) }
        is WebLinkItem -> { val (cx, cy) = movedCenter(raw); val nw = (w * s).coerceIn(220f, 1400f); val nh = (h * s).coerceIn(if (embedded) 180f else 96f, if (embedded) 1400f else 480f); copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh, rot = (rot + deg) % 360f) }
        is WriteBoxItem -> { val (cx, cy) = movedCenter(raw); val nw = (w * s).coerceIn(220f, 1200f); val nh = (h * s).coerceIn(120f, 960f); copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh, rot = (rot + deg) % 360f) }
        is TableItem -> { val (cx, cy) = movedCenter(raw); val nw = (w * s).coerceIn(200f, 1600f); val nh = (h * s).coerceIn(100f, 1600f); copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh, rot = (rot + deg) % 360f) }
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
            val b = Brushes.create(brushFamily, stroke.brush.colorIntArgb, (stroke.brush.size * avgS).coerceIn(1f, 80f))
            InkItem(id, InkCodec.strokeFromPoints(newPts, b), familyHint = familyHint)
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
            if (pinned) this
            else if (collapsed) {
                val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
                copy(x = cx - 38f, y = cy - 38f)
            } else {
                val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
                val nw = (w * sx).coerceIn(140f, 1400f)
                val nh = (h * sy).coerceIn(120f, 1400f)
                copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh)
            }
        }
        is MindNodeItem -> {
            val (cx, cy) = mapPt(bounds.centerX(), bounds.centerY())
            val nw = (w * sx).coerceIn(110f, 900f)
            val nh = (h * sy).coerceIn(48f, 520f)
            copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh)
        }
        is WebLinkItem -> {
            val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
            val nw = (w * sx).coerceIn(200f, 1400f)
            val nh = (h * sy).coerceIn(if (embedded) 160f else 90f, if (embedded) 1400f else 600f)
            copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh)
        }
        is WriteBoxItem -> {
            val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
            val nw = (w * sx).coerceIn(220f, 1400f)
            val nh = (h * sy).coerceIn(120f, 1000f)
            copy(x = cx - nw / 2f, y = cy - nh / 2f, w = nw, h = nh)
        }
        is TableItem -> {
            val (cx, cy) = mapPt(raw.centerX(), raw.centerY())
            val nw = (w * sx).coerceIn(200f, 1800f)
            val nh = (h * sy).coerceIn(100f, 1800f)
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
private val measureTextPaintTL = ThreadLocal.withInitial { TextPaint(TextPaint.ANTI_ALIAS_FLAG) }
private val resolvedTypefaceCache = java.util.concurrent.ConcurrentHashMap<String, Typeface>()

internal fun TextStyleSpec.resolvedTypeface(): Typeface {
    val key = "${customFontPath}|${font.name}|${if (bold) 1 else 0}|${if (italic) 1 else 0}"
    resolvedTypefaceCache[key]?.let { return it }
    val base = if (customFontPath.isNotBlank()) {
        CustomFontRegistry.loadTypeface(customFontPath) ?: font.typeface
    } else {
        font.typeface
    }
    val wantBold = bold || base.isBold
    val wantItalic = italic || base.isItalic
    val created = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        val weight = if (wantBold) 700 else 400
        runCatching { Typeface.create(base, weight, wantItalic) }.getOrElse {
            val mask = (if (wantBold) Typeface.BOLD else 0) or (if (wantItalic) Typeface.ITALIC else 0)
            Typeface.create(base, mask)
        }
    } else {
        val mask = (if (wantBold) Typeface.BOLD else 0) or (if (wantItalic) Typeface.ITALIC else 0)
        Typeface.create(base, mask)
    }
    if (resolvedTypefaceCache.size > 192) resolvedTypefaceCache.clear()
    resolvedTypefaceCache[key] = created
    return created
}

fun applyTextStyle(paint: TextPaint, size: Float, style: TextStyleSpec) {
    val tf = style.resolvedTypeface()
    paint.textSize = size
    paint.typeface = tf
    // Guarantee unmistakable bold & italic even on single-weight custom .ttf/.otf and cursive fonts
    val customOrNonBold = style.bold && (style.customFontPath.isNotBlank() || !tf.isBold || style.font == TextFont.CURSIVE || style.font == TextFont.HAND || style.font == TextFont.SCRIPT)
    paint.isFakeBoldText = customOrNonBold
    paint.textSkewX = if (style.italic && !tf.isItalic) -0.20f else 0f
    paint.isUnderlineText = style.underline
    paint.textAlign = android.graphics.Paint.Align.LEFT
}

internal data class StyledTextSpan(val text: String, val bold: Boolean)

private val INLINE_BOLD_SPAN_REGEX = Regex("\\*\\*(.+?)\\*\\*")

internal fun parseInlineBoldLine(line: String, baseBold: Boolean): List<StyledTextSpan> {
    if (!line.contains("**")) return listOf(StyledTextSpan(line, baseBold))
    val out = ArrayList<StyledTextSpan>(4)
    var cursor = 0
    for (m in INLINE_BOLD_SPAN_REGEX.findAll(line)) {
        if (m.range.first > cursor) {
            out += StyledTextSpan(line.substring(cursor, m.range.first), baseBold)
        }
        val inner = m.groupValues[1]
        if (inner.isNotEmpty()) {
            out += StyledTextSpan(inner, true)
        }
        cursor = m.range.last + 1
    }
    if (cursor < line.length) {
        out += StyledTextSpan(line.substring(cursor), baseBold)
    }
    return if (out.isEmpty()) listOf(StyledTextSpan(line, baseBold)) else out
}

internal fun formatInlineBoldTokens(text: String): String {
    if (text.isBlank()) return text
    return text.split('\n').joinToString("\n") { line ->
        line.split(Regex("(?<=\\s)|(?=\\s)")).joinToString("") { token ->
            if (token.isBlank()) token
            else if (token.startsWith("**") && token.endsWith("**") && token.length > 4) token
            else "**$token**"
        }
    }
}

internal fun stripInlineBoldMarkers(text: String): String {
    if (!text.contains("**")) return text
    return INLINE_BOLD_SPAN_REGEX.replace(text) { it.groupValues[1] }
}

fun measureText(text: String, size: Float, x: Float, y: Float, style: TextStyleSpec = TextStyleSpec()): RectF {
    val p = measureTextPaintTL.get() ?: TextPaint(TextPaint.ANTI_ALIAS_FLAG)
    val lines = text.split('\n')
    val width = lines.maxOf { line ->
        val spans = parseInlineBoldLine(line, style.bold)
        if (spans.size == 1 && spans[0].bold == style.bold && spans[0].text == line) {
            applyTextStyle(p, size, style)
            p.measureText(line)
        } else {
            var sum = 0f
            for (span in spans) {
                if (span.text.isEmpty()) continue
                applyTextStyle(p, size, if (span.bold == style.bold) style else style.copy(bold = span.bold))
                sum += p.measureText(span.text)
            }
            sum
        }
    }.coerceAtLeast(size)
    val lineHeight = size * 1.3f
    return RectF(x, y, x + width, y + lineHeight * lines.size)
}

