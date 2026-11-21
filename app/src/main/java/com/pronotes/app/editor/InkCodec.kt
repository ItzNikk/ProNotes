package com.pronotes.app.editor

import android.annotation.SuppressLint
import android.util.Base64
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushTip
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import com.pronotes.app.data.BrushDoc
import com.pronotes.app.data.ItemDoc
import com.pronotes.app.data.PageDoc

/** Pen bodies offered in the pen card. All map onto Jetpack Ink stock or custom brushes. */
enum class PenType(val label: String, val family: String, val sizeFactor: Float) {
    FOUNTAIN("Fountain pen", Brushes.PEN, 1f),
    FINELINER("Fineliner", Brushes.MARKER, 0.42f),
    LETTERING("Lettering", Brushes.PEN, 1.55f),
    BALL("Ball pen", Brushes.MARKER, 0.8f),
    BRUSH("Brush pen", Brushes.PEN, 2.2f),
    CALLIGRAPHY("Calligraphy", Brushes.CALLIGRAPHY, 1.45f),
    PENCIL("Pencil", Brushes.PENCIL, 0.9f),
    DASHED("Dashed pen", Brushes.DASHED, 1f),
}

/** Brush families come from Jetpack Ink's stock brushes, plus custom calligraphy, pencil & highlighter. */
@OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)
object Brushes {
    const val PEN = "pen"
    const val MARKER = "marker"
    const val HIGHLIGHTER = "highlighter"
    const val DASHED = "dashed"
    const val CALLIGRAPHY = "calligraphy"
    const val PENCIL = "pencil"

    /** Highlighter ink opacity (set from settings); swatches stay opaque. */
    @Volatile var highlightAlpha = 0x73

    private val pen by lazy { StockBrushes.pressurePen() }
    private val marker by lazy { StockBrushes.marker() }
    private val highlighter by lazy { buildHighlighter() }
    private val dashed by lazy { StockBrushes.dashedLine() }
    private val calligraphy by lazy { buildCalligraphy(0.32f, 35f) }
    private val pencil by lazy { buildPencil() }

    @SuppressLint("RestrictedApi")
    private fun buildHighlighter(): BrushFamily =
        BrushFamily(tip = BrushTip(scaleX = 1f, scaleY = 1f, cornerRounding = 1f, behaviors = emptyList()), paint = BrushPaint())

    @SuppressLint("RestrictedApi")
    private fun buildCalligraphy(roundness: Float, angleDeg: Float): BrushFamily =
        BrushFamily(
            tip = BrushTip(
                scaleX = 1.15f,
                scaleY = roundness.coerceIn(0.22f, 1f),
                cornerRounding = 0.25f,
                rotationDegrees = angleDeg.coerceIn(0f, 180f),
                behaviors = emptyList(),
            ),
            paint = BrushPaint(),
        )

    @SuppressLint("RestrictedApi")
    private fun buildPencil(): BrushFamily =
        BrushFamily(
            tip = BrushTip(
                scaleX = 0.95f,
                scaleY = 0.85f,
                cornerRounding = 0.8f,
                behaviors = emptyList(),
            ),
            paint = BrushPaint(),
        )

    fun family(name: String): BrushFamily = when (name) {
        HIGHLIGHTER -> highlighter
        MARKER -> marker
        DASHED -> dashed
        CALLIGRAPHY -> calligraphy
        PENCIL -> pencil
        else -> pen
    }

    fun nameOf(brush: Brush): String = when (brush.family) {
        highlighter -> HIGHLIGHTER
        marker -> MARKER
        dashed -> DASHED
        calligraphy -> CALLIGRAPHY
        pencil -> PENCIL
        else -> PEN
    }

    fun opaque(color: Int) = color or (0xFF shl 24)

    fun withAlpha(color: Int, opacity: Float): Int {
        val a = (opacity.coerceIn(0.15f, 1f) * 255f).toInt().coerceIn(38, 255)
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    fun create(family: String, color: Int, size: Float, opacity: Float = 1f): Brush {
        val effOpacity = if (family == PENCIL) (opacity * 0.72f).coerceIn(0.2f, 0.9f) else opacity
        val c = if (family == HIGHLIGHTER) {
            (color and 0x00FFFFFF) or (highlightAlpha shl 24)
        } else if (effOpacity >= 0.995f) {
            opaque(color)
        } else {
            withAlpha(color, effOpacity)
        }
        return Brush.createWithColorIntArgb(family(family), c, size, 0.1f)
    }

    /** Creates a customized pen brush applying stroke style, nib geometry, and pressure sensitivity. */
    @SuppressLint("RestrictedApi")
    fun createCustomPen(
        baseFamily: String,
        color: Int,
        size: Float,
        opacity: Float,
        pressureEnabled: Boolean,
        pressureSensitivity: Float,
        nibRoundness: Float,
        nibAngleDeg: Float,
        strokeStyle: String,
    ): Brush {
        val effectiveSize = (size * (0.75f + 0.25f * pressureSensitivity.coerceIn(0.3f, 2.0f))).coerceIn(0.4f, 120f)
        val effOpacity = if (baseFamily == PENCIL) (opacity * 0.72f).coerceIn(0.2f, 0.9f) else opacity
        val c = if (effOpacity >= 0.995f) opaque(color) else withAlpha(color, effOpacity)
        if (strokeStyle == "DASHED" || strokeStyle == "DOTTED" || baseFamily == DASHED) {
            return Brush.createWithColorIntArgb(dashed, c, effectiveSize, 0.1f)
        }
        if (baseFamily == CALLIGRAPHY || nibRoundness < 0.94f) {
            val customFamily = buildCalligraphy(nibRoundness, nibAngleDeg)
            return Brush.createWithColorIntArgb(customFamily, c, effectiveSize, 0.1f)
        }
        val resolvedFamily = when {
            baseFamily == PENCIL -> pencil
            !pressureEnabled && baseFamily == PEN -> marker
            else -> family(baseFamily)
        }
        return Brush.createWithColorIntArgb(resolvedFamily, c, effectiveSize, 0.1f)
    }

    fun recolor(brush: Brush, color: Int): Brush {
        val familyName = nameOf(brush)
        val alpha = if (familyName == HIGHLIGHTER) 1f else ((brush.colorIntArgb ushr 24) and 0xFF) / 255f
        return create(familyName, color, brush.size, alpha)
    }
}

object InkCodec {

    fun encodeInputs(batch: StrokeInputBatch): String =
        java.io.ByteArrayOutputStream().use { out ->
            batch.encode(out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }

    fun decodeInputs(data: String): StrokeInputBatch =
        java.io.ByteArrayInputStream(Base64.decode(data, Base64.NO_WRAP)).use { StrokeInputBatch.decode(it) }

    /** Builds a stroke from synthetic points (used for snapped and drawn shapes). */
    fun strokeFromPoints(points: List<Pair<Float, Float>>, brush: Brush): Stroke {
        val batch = MutableStrokeInputBatch()
        var t = 0L
        var last: Pair<Float, Float>? = null
        for ((x, y) in points) {
            if (last != null && last.first == x && last.second == y) continue
            runCatching { batch.add(InputToolType.STYLUS, x, y, t) }
            last = x to y
            t += 8
        }
        return Stroke(brush, batch)
    }

    /** Moves, scales and rotates a stroke for good; thickness follows the scale. */
    fun transformStroke(stroke: Stroke, m: android.graphics.Matrix, s: Float): Stroke {
        val inputs = stroke.inputs
        val batch = MutableStrokeInputBatch()
        val pt = FloatArray(2)
        for (i in 0 until inputs.size) {
            val p = inputs.get(i)
            pt[0] = p.x; pt[1] = p.y
            m.mapPoints(pt)
            runCatching {
                batch.add(p.toolType, pt[0], pt[1], p.elapsedTimeMillis, p.strokeUnitLengthCm, p.pressure, p.tiltRadians, p.orientationRadians)
            }
        }
        val b = stroke.brush
        val brush = Brush.createWithColorIntArgb(b.family, b.colorIntArgb, (b.size * s).coerceIn(0.3f, 500f), b.epsilon)
        return Stroke(brush, batch)
    }

    /**
     * Pixel eraser: drops the input points within [r] of (x, y) and returns the
     * surviving runs as new strokes with the same brush and pressure data.
     */
    fun eraseFrom(stroke: Stroke, x: Float, y: Float, r: Float): List<Stroke>? {
        val inputs = stroke.inputs
        val r2 = r * r
        var hit = false
        val runs = ArrayList<MutableStrokeInputBatch>()
        var cur: MutableStrokeInputBatch? = null
        var curCount = 0
        for (i in 0 until inputs.size) {
            val p = inputs.get(i)
            val dx = p.x - x; val dy = p.y - y
            if (dx * dx + dy * dy <= r2) {
                hit = true
                if (cur != null && curCount > 1) runs += cur
                cur = null; curCount = 0
            } else {
                if (cur == null) { cur = MutableStrokeInputBatch(); curCount = 0 }
                if (runCatching { cur.add(p) }.isSuccess) curCount++
            }
        }
        if (!hit) return null
        if (cur != null && curCount > 1) runs += cur
        return runs.map { Stroke(stroke.brush, it) }
    }

    fun toDoc(page: Page): PageDoc = PageDoc(
        id = page.id,
        width = page.width,
        height = page.height,
        paper = page.paper,
        pdfPage = page.pdfPage,
        bookmarked = page.bookmarked,
        paperColor = page.paperColor,
        cover = page.cover,
        bgImage = page.bgImage,
        lineScale = page.lineScale,
        lineColor = page.lineColor,
        isWhiteboard = page.isWhiteboard,
        items = page.items.map { item ->
            when (item) {
                is InkItem -> ItemDoc.Ink(
                    inputs = encodeInputs(item.stroke.inputs),
                    brush = BrushDoc(
                        family = Brushes.nameOf(item.stroke.brush),
                        color = item.stroke.brush.colorIntArgb,
                        size = item.stroke.brush.size,
                        epsilon = item.stroke.brush.epsilon,
                    ),
                    dx = item.dx,
                    dy = item.dy,
                )
                is TextItem -> ItemDoc.Text(
                    item.text, item.x, item.y, item.size, item.color,
                    font = item.style.font.name, bold = item.style.bold, italic = item.style.italic,
                    underline = item.style.underline, gradient = item.style.gradient, align = item.style.align.name,
                    rot = item.rot,
                )
                is FillItem -> ItemDoc.Fill(item.points.toList(), item.color, item.dx, item.dy)
                is ImageItem -> ItemDoc.Image(item.file, item.x, item.y, item.w, item.h, item.rot)
                is TapeItem -> ItemDoc.Tape(item.x, item.y, item.w, item.h, item.color, item.rot, item.pts?.toList() ?: emptyList(), item.thick, item.pattern)
                is StickyItem -> ItemDoc.Sticky(
                    item.x, item.y, item.w, item.h, item.color, item.text, item.rot,
                    item.fontSize, item.font, item.collapsed, item.pinned, item.title, item.style,
                )
                is MindNodeItem -> ItemDoc.MindNode(item.id, item.x, item.y, item.w, item.h, item.label, item.parentId, item.color, item.shape, item.collapsed, item.notes, item.tag)
                is WebLinkItem -> ItemDoc.WebLink(item.url, item.title, item.note, item.x, item.y, item.w, item.h, item.color, item.rot)
                is WriteBoxItem -> ItemDoc.WriteBox(
                    item.x, item.y, item.w, item.h, item.lineSpacing,
                    item.strokeColor, item.fillColor, item.inkItemIds,
                    item.convertedText, item.isConverted,
                    item.font.name, item.fontSize, item.rot,
                )
            }
        },
    )

    fun fromDoc(doc: PageDoc): Page {
        val nodeIds = HashMap<Long, Long>()
        for (item in doc.items) {
            if (item is ItemDoc.MindNode) nodeIds[item.nodeId] = ItemIds.next()
        }
        return Page(
            id = doc.id,
            width = doc.width,
            height = doc.height,
            paper = doc.paper,
            pdfPage = doc.pdfPage,
            bookmarked = doc.bookmarked,
            paperColor = doc.paperColor,
            cover = doc.cover,
            bgImage = doc.bgImage,
            lineScale = doc.lineScale,
            lineColor = doc.lineColor,
            isWhiteboard = doc.isWhiteboard,
            items = doc.items.mapNotNull { item ->
                runCatching<PageItem> {
                    when (item) {
                        is ItemDoc.Ink -> {
                            // Stored colour already carries the right alpha.
                            val brush = Brush.createWithColorIntArgb(
                                Brushes.family(item.brush.family), item.brush.color, item.brush.size, item.brush.epsilon,
                            )
                            InkItem(ItemIds.next(), Stroke(brush, decodeInputs(item.inputs)), item.dx, item.dy)
                        }
                        is ItemDoc.Text -> TextItem(
                            ItemIds.next(), item.text, item.x, item.y, item.size, item.color,
                            TextStyleSpec(
                                font = runCatching { TextFont.valueOf(item.font) }.getOrDefault(TextFont.SANS),
                                bold = item.bold, italic = item.italic, underline = item.underline, gradient = item.gradient,
                                align = runCatching { TextAlign.valueOf(item.align) }.getOrDefault(TextAlign.LEFT),
                            ),
                            item.rot,
                        )
                        is ItemDoc.Fill -> FillItem(ItemIds.next(), item.points.toFloatArray(), item.color, item.dx, item.dy)
                        is ItemDoc.Image -> ImageItem(ItemIds.next(), item.file, item.x, item.y, item.w, item.h, item.rot)
                        is ItemDoc.Tape -> TapeItem(
                            ItemIds.next(), item.x, item.y, item.w, item.h, item.color, rot = item.rot,
                            pts = item.pts.takeIf { it.size >= 4 }?.toFloatArray(), thick = item.thick, pattern = item.pattern,
                        )
                        is ItemDoc.Sticky -> StickyItem(
                            ItemIds.next(), item.x, item.y, item.w, item.h, item.color, item.text, item.rot,
                            item.fontSize, item.font, item.collapsed, item.pinned, item.title, item.style,
                        )
                        is ItemDoc.MindNode -> MindNodeItem(
                            id = nodeIds[item.nodeId] ?: ItemIds.next(),
                            x = item.x,
                            y = item.y,
                            w = item.w,
                            h = item.h,
                            label = item.label,
                            parentId = item.parentId?.let { nodeIds[it] ?: it },
                            color = item.color,
                            shape = item.shape,
                            collapsed = item.collapsed,
                            notes = item.notes,
                            tag = item.tag,
                        )
                        is ItemDoc.WebLink -> WebLinkItem(
                            id = ItemIds.next(),
                            url = item.url,
                            title = item.title,
                            note = item.note,
                            x = item.x,
                            y = item.y,
                            w = item.w,
                            h = item.h,
                            color = item.color,
                            rot = item.rot,
                        )
                        is ItemDoc.WriteBox -> WriteBoxItem(
                            id = ItemIds.next(),
                            x = item.x,
                            y = item.y,
                            w = item.w,
                            h = item.h,
                            lineSpacing = item.lineSpacing,
                            strokeColor = item.strokeColor,
                            fillColor = item.fillColor,
                            inkItemIds = item.inkItemIds,
                            convertedText = item.convertedText,
                            isConverted = item.isConverted,
                            font = runCatching { TextFont.valueOf(item.font) }.getOrDefault(TextFont.SANS),
                            fontSize = item.fontSize,
                            rot = item.rot,
                        )
                    }
                }.getOrNull()
            },
        )
    }
}
