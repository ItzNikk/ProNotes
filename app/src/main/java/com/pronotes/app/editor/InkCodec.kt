package com.pronotes.app.editor

import android.annotation.SuppressLint
import android.util.Base64
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushBehavior
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

/** Pen bodies offered in the pen card. Each has a distinct nib, width physics, opacity, and dry rendering signature. */
enum class PenType(val label: String, val family: String, val sizeFactor: Float) {
    FOUNTAIN("Fountain pen", Brushes.FOUNTAIN, 1.0f),
    FINELINER("Fineliner", Brushes.FINELINER, 0.46f),
    LETTERING("Lettering", Brushes.LETTERING, 1.55f),
    BALL("Ball pen", Brushes.BALL, 0.78f),
    BRUSH("Brush pen", Brushes.BRUSH, 2.3f),
    CALLIGRAPHY("Calligraphy", Brushes.CALLIGRAPHY, 1.55f),
    PENCIL("Pencil", Brushes.PENCIL, 0.96f),
    DASHED("Dashed pen", Brushes.DASHED, 1.0f),
}

/** Brush families come from Jetpack Ink's stock brushes, plus custom calligraphy, pencil, brush & highlighter. */
@OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)
object Brushes {
    const val PEN = "pen"
    const val FOUNTAIN = "fountain"
    const val FINELINER = "fineliner"
    const val BALL = "ball"
    const val LETTERING = "lettering"
    const val BRUSH = "brush"
    const val MARKER = "marker"
    const val HIGHLIGHTER = "highlighter"
    const val DASHED = "dashed"
    const val CALLIGRAPHY = "calligraphy"
    const val PENCIL = "pencil"
    const val CRAYON = "crayon"

    /** Highlighter ink opacity (set from settings); swatches stay opaque. */
    @Volatile var highlightAlpha = 0x73

    private val familyNames = java.util.Collections.synchronizedMap(java.util.WeakHashMap<BrushFamily, String>())

    private val pen by lazy { buildPressurePen(0.28f, 1.65f, FOUNTAIN) }
    private val ballPen by lazy { buildPressurePen(0.62f, 1.24f, BALL) }
    private val brushPen by lazy { buildPressurePen(0.16f, 1.95f, BRUSH) }
    private val letteringPen by lazy { buildPressurePen(0.18f, 1.80f, LETTERING) }
    private val marker by lazy { StockBrushes.marker().also { familyNames[it] = FINELINER } }
    private val highlighter by lazy { buildHighlighter().also { familyNames[it] = HIGHLIGHTER } }
    private val dashed by lazy { StockBrushes.dashedLine().also { familyNames[it] = DASHED } }
    private val calligraphy by lazy { buildCalligraphy(0.20f, 42f, pressureEnabled = true) }
    private val pencil by lazy { buildPencil().also { familyNames[it] = PENCIL } }
    private val crayon by lazy { buildCrayon().also { familyNames[it] = CRAYON } }

    @SuppressLint("RestrictedApi")
    private fun pressureSizeBehavior(minScale: Float, maxScale: Float): BrushBehavior =
        BrushBehavior(
            terminalNodes = listOf(
                BrushBehavior.TargetNode(
                    target = BrushBehavior.Target.SIZE_MULTIPLIER,
                    targetModifierRangeStart = minScale,
                    targetModifierRangeEnd = maxScale,
                    input = BrushBehavior.SourceNode(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        sourceValueRangeStart = 0.04f,
                        sourceValueRangeEnd = 0.96f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    ),
                ),
            ),
        )

    @SuppressLint("RestrictedApi")
    private fun pressureOpacityBehavior(minAlpha: Float, maxAlpha: Float): BrushBehavior =
        BrushBehavior(
            terminalNodes = listOf(
                BrushBehavior.TargetNode(
                    target = BrushBehavior.Target.OPACITY_MULTIPLIER,
                    targetModifierRangeStart = minAlpha,
                    targetModifierRangeEnd = maxAlpha,
                    input = BrushBehavior.SourceNode(
                        source = BrushBehavior.Source.NORMALIZED_PRESSURE,
                        sourceValueRangeStart = 0.05f,
                        sourceValueRangeEnd = 0.95f,
                        sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.CLAMP,
                    ),
                ),
            ),
        )

    @SuppressLint("RestrictedApi")
    private fun buildPressurePen(minScale: Float, maxScale: Float, tag: String): BrushFamily =
        BrushFamily(
            tip = BrushTip(
                scaleX = 1f,
                scaleY = 1f,
                cornerRounding = 1f,
                behaviors = listOf(pressureSizeBehavior(minScale, maxScale)),
            ),
            paint = BrushPaint(),
        ).also { familyNames[it] = tag }

    @SuppressLint("RestrictedApi")
    private fun buildHighlighter(): BrushFamily =
        BrushFamily(
            tip = BrushTip(
                scaleX = 0.36f,
                scaleY = 1.0f,
                cornerRounding = 0.28f,
                rotationDegrees = 10f,
                behaviors = emptyList(),
            ),
            paint = BrushPaint(),
        )

    @SuppressLint("RestrictedApi")
    private fun buildCalligraphy(roundness: Float, angleDeg: Float, pressureEnabled: Boolean = true): BrushFamily =
        BrushFamily(
            tip = BrushTip(
                scaleX = 1.42f,
                scaleY = roundness.coerceIn(0.18f, 0.85f),
                cornerRounding = 0.08f,
                rotationDegrees = angleDeg.coerceIn(0f, 180f),
                behaviors = if (pressureEnabled) listOf(pressureSizeBehavior(0.58f, 1.45f)) else emptyList(),
            ),
            paint = BrushPaint(),
        ).also { familyNames[it] = CALLIGRAPHY }

    @SuppressLint("RestrictedApi")
    private fun buildPencil(): BrushFamily =
        BrushFamily(
            tip = BrushTip(
                scaleX = 0.96f,
                scaleY = 0.88f,
                cornerRounding = 0.82f,
                behaviors = listOf(
                    pressureSizeBehavior(0.42f, 1.48f),
                    pressureOpacityBehavior(0.45f, 1.0f),
                ),
            ),
            paint = BrushPaint(),
        )

    @SuppressLint("RestrictedApi")
    private fun buildCrayon(): BrushFamily =
        BrushFamily(
            tip = BrushTip(
                scaleX = 1.18f,
                scaleY = 0.92f,
                cornerRounding = 0.65f,
                behaviors = listOf(
                    pressureSizeBehavior(0.46f, 1.52f),
                    pressureOpacityBehavior(0.62f, 1.0f),
                ),
            ),
            paint = BrushPaint(),
        )

    fun family(name: String): BrushFamily = when (name) {
        HIGHLIGHTER -> highlighter
        MARKER, FINELINER -> marker
        DASHED -> dashed
        CALLIGRAPHY -> calligraphy
        PENCIL -> pencil
        CRAYON -> crayon
        BALL -> ballPen
        BRUSH -> brushPen
        LETTERING -> letteringPen
        FOUNTAIN, PEN -> pen
        else -> pen
    }

    fun nameOf(brush: Brush): String {
        familyNames[brush.family]?.let { return it }
        return when (brush.family) {
            highlighter -> HIGHLIGHTER
            marker -> FINELINER
            dashed -> DASHED
            calligraphy -> CALLIGRAPHY
            pencil -> PENCIL
            crayon -> CRAYON
            else -> FOUNTAIN
        }
    }

    fun opaque(color: Int) = color or (0xFF shl 24)

    fun withAlpha(color: Int, opacity: Float): Int {
        val a = (opacity.coerceIn(0.15f, 1f) * 255f).toInt().coerceIn(38, 255)
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    fun create(family: String, color: Int, size: Float, opacity: Float = 1f): Brush {
        val effOpacity = when (family) {
            PENCIL -> (opacity * 0.64f).coerceIn(0.22f, 0.85f)
            CRAYON -> (opacity * 0.86f).coerceIn(0.35f, 0.96f)
            BALL -> (opacity * 0.91f).coerceIn(0.35f, 0.96f)
            BRUSH -> (opacity * 0.90f).coerceIn(0.35f, 0.96f)
            else -> opacity
        }
        val c = if (family == HIGHLIGHTER) {
            (color and 0x00FFFFFF) or (highlightAlpha shl 24)
        } else if (effOpacity >= 0.995f) {
            opaque(color)
        } else {
            withAlpha(color, effOpacity)
        }
        return Brush.createWithColorIntArgb(family(family), c, size, 0.1f)
    }

    /** Creates a customized pen brush preserving each PenType's unmistakable nib, opacity, and pressure signature. */
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
        val sens = pressureSensitivity.coerceIn(0.3f, 2.0f)
        val effOpacity = when (baseFamily) {
            PENCIL -> (opacity * 0.64f).coerceIn(0.22f, 0.85f)
            CRAYON -> (opacity * 0.86f).coerceIn(0.35f, 0.96f)
            BALL -> (opacity * 0.91f).coerceIn(0.35f, 0.96f)
            BRUSH -> (opacity * 0.90f).coerceIn(0.35f, 0.96f)
            else -> opacity
        }
        val c = if (effOpacity >= 0.995f) opaque(color) else withAlpha(color, effOpacity)

        if (baseFamily == CRAYON) {
            return Brush.createWithColorIntArgb(crayon, c, size.coerceIn(0.8f, 120f), 0.1f)
        }
        if (strokeStyle == "DASHED" || strokeStyle == "DOTTED" || baseFamily == DASHED) {
            return Brush.createWithColorIntArgb(dashed, c, size.coerceIn(0.6f, 120f), 0.1f)
        }
        // Fineliner is strictly a crisp constant-width monoline technical pen.
        if (baseFamily == FINELINER || baseFamily == MARKER) {
            return Brush.createWithColorIntArgb(marker, c, size.coerceIn(0.5f, 80f), 0.1f)
        }
        // Calligraphy uses a sharp flat-chisel angled ribbon nib (default 42 deg, thin roundness).
        if (baseFamily == CALLIGRAPHY) {
            val chiselRoundness = if (nibRoundness >= 0.90f) 0.20f else nibRoundness.coerceIn(0.18f, 0.55f)
            val chiselAngle = if (nibAngleDeg <= 1f) 42f else nibAngleDeg
            val customFamily = buildCalligraphy(chiselRoundness, chiselAngle, pressureEnabled)
            return Brush.createWithColorIntArgb(customFamily, c, size.coerceIn(1.2f, 120f), 0.1f)
        }
        // Pencil keeps its dedicated pencil family so dry strokes render with real fine graphite grain.
        if (baseFamily == PENCIL) {
            return Brush.createWithColorIntArgb(pencil, c, size.coerceIn(0.8f, 100f), 0.1f)
        }
        // User-customized flat nib override for other pens if they dialed down nib roundness explicitly.
        if (nibRoundness < 0.88f) {
            val customFamily = buildCalligraphy(nibRoundness, nibAngleDeg, pressureEnabled)
            return Brush.createWithColorIntArgb(customFamily, c, size.coerceIn(0.6f, 120f), 0.1f)
        }
        val effectiveSize = when (baseFamily) {
            BALL -> size.coerceIn(0.6f, 90f)
            LETTERING -> (size * (0.94f + 0.16f * sens)).coerceIn(1.0f, 120f)
            BRUSH -> (size * (0.96f + 0.20f * sens)).coerceIn(1.4f, 140f)
            else -> size.coerceIn(0.6f, 120f)
        }
        val resolvedFamily = when {
            !pressureEnabled && baseFamily != LETTERING && baseFamily != BRUSH -> marker
            baseFamily == BALL -> ballPen
            baseFamily == BRUSH -> brushPen
            baseFamily == LETTERING -> letteringPen
            else -> pen
        }
        return Brush.createWithColorIntArgb(resolvedFamily, c, effectiveSize, 0.1f)
    }

    fun recolor(brush: Brush, color: Int, familyHint: String? = null): Brush {
        val familyName = familyHint?.takeIf { it.isNotBlank() } ?: nameOf(brush)
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
                        family = item.brushFamily,
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
                    customFontPath = item.style.customFontPath,
                )
                is FillItem -> ItemDoc.Fill(item.points.toList(), item.color, item.dx, item.dy)
                is ImageItem -> ItemDoc.Image(item.file, item.x, item.y, item.w, item.h, item.rot)
                is TapeItem -> ItemDoc.Tape(item.x, item.y, item.w, item.h, item.color, item.rot, item.pts?.toList() ?: emptyList(), item.thick, item.pattern)
                is StickyItem -> ItemDoc.Sticky(
                    item.x, item.y, item.w, item.h, item.color, item.text, item.rot,
                    item.fontSize, item.font, item.collapsed, item.pinned, item.title, item.style,
                )
                is MindNodeItem -> ItemDoc.MindNode(item.id, item.x, item.y, item.w, item.h, item.label, item.parentId, item.color, item.shape, item.collapsed, item.notes, item.tag)
                is WebLinkItem -> ItemDoc.WebLink(item.url, item.title, item.note, item.x, item.y, item.w, item.h, item.color, item.rot, item.embedded)
                is WriteBoxItem -> ItemDoc.WriteBox(
                    item.x, item.y, item.w, item.h, item.lineSpacing,
                    item.strokeColor, item.fillColor, item.inkItemIds,
                    item.convertedText, item.isConverted,
                    item.font.name, item.fontSize, item.rot,
                )
                is TableItem -> ItemDoc.Table(
                    item.x, item.y, item.w, item.h,
                    item.rows, item.cols, item.cells,
                    item.color, item.headerRow, item.fontSize, item.rot,
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
                            InkItem(
                                id = ItemIds.next(),
                                stroke = Stroke(brush, decodeInputs(item.inputs)),
                                dx = item.dx,
                                dy = item.dy,
                                familyHint = item.brush.family,
                            )
                        }
                        is ItemDoc.Text -> TextItem(
                            ItemIds.next(), item.text, item.x, item.y, item.size, item.color,
                            TextStyleSpec(
                                font = runCatching { TextFont.valueOf(item.font) }.getOrDefault(TextFont.SANS),
                                bold = item.bold, italic = item.italic, underline = item.underline, gradient = item.gradient,
                                align = runCatching { TextAlign.valueOf(item.align) }.getOrDefault(TextAlign.LEFT),
                                customFontPath = item.customFontPath,
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
                            embedded = item.embedded,
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
                        is ItemDoc.Table -> {
                            val r = item.rows.coerceIn(1, 20)
                            val c = item.cols.coerceIn(1, 12)
                            val need = r * c
                            val normalizedCells = List(need) { idx -> item.cells.getOrNull(idx).orEmpty() }
                            TableItem(
                                id = ItemIds.next(),
                                x = item.x,
                                y = item.y,
                                w = item.w.coerceIn(200f, 1800f),
                                h = item.h.coerceIn(100f, 1800f),
                                rows = r,
                                cols = c,
                                cells = normalizedCells,
                                color = item.color,
                                headerRow = item.headerRow,
                                fontSize = item.fontSize.coerceIn(12f, 64f),
                                rot = item.rot,
                            )
                        }
                    }
                }.getOrNull()
            },
        )
    }
}
