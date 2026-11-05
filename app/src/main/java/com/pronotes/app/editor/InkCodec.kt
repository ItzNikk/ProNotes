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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Brush families come straight from Jetpack Ink's stock brushes. */
object Brushes {
    const val PEN = "pen"
    const val MARKER = "marker"
    const val HIGHLIGHTER = "highlighter"

    private val pen by lazy { StockBrushes.pressurePen() }
    private val marker by lazy { StockBrushes.marker() }
    private val highlighter by lazy { StockBrushes.highlighter() }

    fun family(name: String): BrushFamily = when (name) {
        HIGHLIGHTER -> highlighter
        MARKER -> marker
        else -> pen
    }

    fun nameOf(family: BrushFamily): String = when (family) {
        highlighter -> HIGHLIGHTER
        marker -> MARKER
        else -> PEN
    }

    fun create(family: String, color: Int, size: Float): Brush =
        Brush.createWithColorIntArgb(family(family), color, size, 0.1f)

    fun recolor(brush: Brush, color: Int): Brush {
        // Keep highlighter translucency when recoloring.
        val c = if (nameOf(brush.family) == HIGHLIGHTER) (color and 0x00FFFFFF) or (0x66 shl 24) else color
        return Brush.createWithColorIntArgb(brush.family, c, brush.size, brush.epsilon)
    }
}

object InkCodec {

    fun encodeInputs(batch: StrokeInputBatch): String =
        ByteArrayOutputStream().use { out ->
            batch.encode(out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }

    fun decodeInputs(data: String): StrokeInputBatch =
        ByteArrayInputStream(Base64.decode(data, Base64.NO_WRAP)).use { StrokeInputBatch.decode(it) }

    /** Builds a stroke from synthetic points (used for snapped shapes). */
    fun strokeFromPoints(points: List<Pair<Float, Float>>, brush: Brush): Stroke {
        val batch = MutableStrokeInputBatch()
        var t = 0L
        for ((x, y) in points) {
            batch.add(InputToolType.STYLUS, x, y, t)
            t += 8
        }
        return Stroke(brush, batch)
    }

    fun toDoc(page: Page): PageDoc = PageDoc(
        id = page.id,
        width = page.width,
        height = page.height,
        paper = page.paper,
        pdfPage = page.pdfPage,
        items = page.items.mapNotNull { item ->
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
                is TextItem -> ItemDoc.Text(item.text, item.x, item.y, item.size, item.color)
                is ImageItem -> ItemDoc.Image(item.file, item.x, item.y, item.w, item.h)
            }
        },
    )

    fun fromDoc(doc: PageDoc): Page = Page(
        id = doc.id,
        width = doc.width,
        height = doc.height,
        paper = doc.paper,
        pdfPage = doc.pdfPage,
        items = doc.items.mapNotNull { item ->
            runCatching<PageItem> {
                when (item) {
                    is ItemDoc.Ink -> {
                        val brush = Brush.createWithColorIntArgb(
                            Brushes.family(item.brush.family), item.brush.color, item.brush.size, item.brush.epsilon,
                        )
                        InkItem(ItemIds.next(), Stroke(brush, decodeInputs(item.inputs)), item.dx, item.dy)
                    }
                    is ItemDoc.Text -> TextItem(ItemIds.next(), item.text, item.x, item.y, item.size, item.color)
                    is ItemDoc.Image -> ImageItem(ItemIds.next(), item.file, item.x, item.y, item.w, item.h)
                }
            }.getOrNull()
        },
    )
}
