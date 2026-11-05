package com.pronotes.app.editor

import android.util.Base64
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
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

/** Pen bodies offered in the pen card. All map onto Jetpack Ink stock brushes. */
enum class PenType(val label: String, val family: String, val sizeFactor: Float) {
    FOUNTAIN("Fountain pen", Brushes.PEN, 1f),
    BALL("Ball pen", Brushes.MARKER, 0.8f),
    BRUSH("Brush pen", Brushes.PEN, 2.2f),
}

/** Brush families come straight from Jetpack Ink's stock brushes. */
object Brushes {
    const val PEN = "pen"
    const val MARKER = "marker"
    const val HIGHLIGHTER = "highlighter"
    const val DASHED = "dashed"

    /** Highlighter ink is always drawn at this opacity; swatches stay opaque. */
    const val HIGHLIGHT_ALPHA = 0x70

    private val pen by lazy { StockBrushes.pressurePen() }
    private val marker by lazy { StockBrushes.marker() }
    private val highlighter by lazy { StockBrushes.highlighter() }
    private val dashed by lazy { StockBrushes.dashedLine() }

    fun family(name: String): BrushFamily = when (name) {
        HIGHLIGHTER -> highlighter
        MARKER -> marker
        DASHED -> dashed
        else -> pen
    }

    fun nameOf(family: BrushFamily): String = when (family) {
        highlighter -> HIGHLIGHTER
        marker -> MARKER
        dashed -> DASHED
        else -> PEN
    }

    fun opaque(color: Int) = color or (0xFF shl 24)

    fun create(family: String, color: Int, size: Float): Brush {
        val c = if (family == HIGHLIGHTER) (color and 0x00FFFFFF) or (HIGHLIGHT_ALPHA shl 24) else opaque(color)
        return Brush.createWithColorIntArgb(family(family), c, size, 0.1f)
    }

    fun recolor(brush: Brush, color: Int): Brush = create(nameOf(brush.family), color, brush.size)
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
        for ((x, y) in points) {
            batch.add(InputToolType.STYLUS, x, y, t)
            t += 8
        }
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
                cur.add(p)
                curCount++
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
        items = page.items.map { item ->
            when (item) {
                is InkItem -> ItemDoc.Ink(
                    inputs = encodeInputs(item.stroke.inputs),
                    brush = BrushDoc(
                        family = Brushes.nameOf(item.stroke.brush.family),
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
                )
                is FillItem -> ItemDoc.Fill(item.points.toList(), item.color, item.dx, item.dy)
                is ImageItem -> ItemDoc.Image(item.file, item.x, item.y, item.w, item.h)
                is TapeItem -> ItemDoc.Tape(item.x, item.y, item.w, item.h, item.color)
                is StickyItem -> ItemDoc.Sticky(item.x, item.y, item.w, item.h, item.color, item.text)
            }
        },
    )

    fun fromDoc(doc: PageDoc): Page = Page(
        id = doc.id,
        width = doc.width,
        height = doc.height,
        paper = doc.paper,
        pdfPage = doc.pdfPage,
        bookmarked = doc.bookmarked,
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
                    )
                    is ItemDoc.Fill -> FillItem(ItemIds.next(), item.points.toFloatArray(), item.color, item.dx, item.dy)
                    is ItemDoc.Image -> ImageItem(ItemIds.next(), item.file, item.x, item.y, item.w, item.h)
                    is ItemDoc.Tape -> TapeItem(ItemIds.next(), item.x, item.y, item.w, item.h, item.color)
                    is ItemDoc.Sticky -> StickyItem(ItemIds.next(), item.x, item.y, item.w, item.h, item.color, item.text)
                }
            }.getOrNull()
        },
    )
}
