package com.pronotes.app.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.pronotes.app.data.PageSize
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.editor.Page
import com.pronotes.app.editor.PageRenderer
import com.pronotes.app.ui.LocalPerformanceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt
import kotlin.math.sin

val NotebookShape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp)

private val coverDrawPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG)
private val coverDestRect = android.graphics.RectF()

private fun quantizeThumbSize(rawW: Int, rawH: Int, maxW: Int): Pair<Int, Int> {
    if (rawW <= 0 || rawH <= 0) return 0 to 0
    val cappedW = rawW.coerceAtMost(maxW).coerceAtLeast(1)
    // Quantize width to 32px buckets so slightly different card widths share the same cached bitmap.
    val bucketW = (((cappedW + 31) / 32) * 32).coerceAtMost(maxW).coerceAtLeast(64)
    val bucketH = (rawH * (bucketW.toFloat() / rawW.coerceAtLeast(1))).roundToInt().coerceAtLeast(64)
    return bucketW to bucketH
}

/**
 * Draws a notebook cover from its spec ("solid:", "pattern:", "image:", "thumb").
 * [noteDir] holds cover photos and thumbnails.
 * Avoids SubcomposeLayout (BoxWithConstraints) so grids and creator dialogs scroll at 120fps.
 */
@Composable
fun CoverArt(
    spec: String,
    title: String,
    noteDir: File?,
    cacheKey: Any,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    drawShadow: Boolean = true,
    explicitWidth: Dp? = null,
    explicitHeight: Dp? = null,
) {
    val context = LocalContext.current
    val perfMode = LocalPerformanceMode.current
    val density = LocalDensity.current
    val outerMod = if (drawShadow) {
        modifier.shadow(6.dp, NotebookShape).clip(NotebookShape).background(Color.White)
    } else {
        modifier.clip(NotebookShape).background(Color.White)
    }
    Box(outerMod) {
        when {
            spec.startsWith("image:") || spec == "thumb" -> {
                val file = remember(spec, noteDir) {
                    if (spec == "thumb") noteDir?.let { File(it, "thumb.png") } else noteDir?.let { File(it, spec.removePrefix("image:")) }
                }
                if (file != null && file.exists()) {
                    val request = remember(context, file.path, cacheKey) {
                        ImageRequest.Builder(context)
                            .data(file)
                            .memoryCacheKey(file.path + cacheKey)
                            .diskCacheKey(file.path + cacheKey)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            else -> {
                val isSolidOnly = remember(spec) { !spec.startsWith("pattern:") }
                val baseColor = remember(spec) {
                    val parts = spec.split(':')
                    Color((parts.lastOrNull()?.toLongOrNull() ?: 0xFF3D6CF4).toInt())
                }
                if (isSolidOnly) {
                    // Pure GPU vector draw for solid covers: zero Bitmap allocation, zero coroutine launch, instant 120fps scroll!
                    Canvas(Modifier.fillMaxSize().background(baseColor)) {
                        drawRect(
                            color = Color(0x29000000),
                            topLeft = Offset(size.width * 0.82f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.05f, size.height),
                        )
                    }
                } else {
                    val explicitSize = remember(explicitWidth, explicitHeight, density) {
                        if (explicitWidth != null && explicitHeight != null) {
                            IntSize(
                                with(density) { explicitWidth.toPx() }.roundToInt().coerceAtLeast(1),
                                with(density) { explicitHeight.toPx() }.roundToInt().coerceAtLeast(1),
                            )
                        } else null
                    }
                    var measuredSize by remember { mutableStateOf(explicitSize ?: IntSize.Zero) }
                    val effectiveSize = explicitSize ?: measuredSize
                    val maxW = perfMode.thumbWidthPx.coerceAtLeast(180)
                    val (targetW, targetH) = remember(effectiveSize, maxW) {
                        quantizeThumbSize(effectiveSize.width, effectiveSize.height, maxW)
                    }

                    var bmp by remember(spec, targetW, targetH) {
                        mutableStateOf(if (targetW > 0) CoverCache.peek(spec, targetW, targetH) else null)
                    }
                    LaunchedEffect(spec, targetW, targetH) {
                        if (targetW > 0 && bmp == null) {
                            bmp = CoverCache.getAsync(spec, targetW, targetH)
                        }
                    }
                    val canvasMod = if (explicitSize == null) {
                        Modifier
                            .fillMaxSize()
                            .background(baseColor)
                            .onSizeChanged { if (it != measuredSize) measuredSize = it }
                    } else {
                        Modifier
                            .fillMaxSize()
                            .background(baseColor)
                    }
                    Canvas(canvasMod) {
                        val ready = bmp
                        if (ready != null) {
                            drawIntoCanvas { c ->
                                coverDestRect.set(0f, 0f, size.width, size.height)
                                c.nativeCanvas.drawBitmap(ready, null, coverDestRect, coverDrawPaint)
                            }
                        }
                    }
                }
                if (showTitle && title.isNotBlank()) {
                    // OpenLeaf CoverView cream label plate (#F4EEDF) with subtle border and serif title
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 18.dp, start = 14.dp, end = 10.dp)
                            .fillMaxWidth(0.76f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF4EEDF))
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            ),
                            color = Color(0xFF2A2824),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        // OpenLeaf spine crease & shading.
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.09f)
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.24f), Color.White.copy(alpha = 0.08f), Color.Transparent))),
        )
    }
}

private fun DrawScope.drawPattern(name: String, base: Color) {
    val light = Color.White.copy(alpha = 0.28f)
    val w = size.width; val h = size.height
    clipRect {
        when (name) {
            "dots" -> {
                val step = w / 8
                var y = step / 2
                var row = 0
                while (y < h) {
                    var x = if (row % 2 == 0) step / 2 else step
                    while (x < w) { drawCircle(light, step * 0.18f, Offset(x, y)); x += step }
                    y += step; row++
                }
            }
            "waves" -> {
                val step = h / 9
                var y = 0f
                while (y < h + step) {
                    val path = Path()
                    var x = 0f
                    path.moveTo(0f, y)
                    while (x <= w) { path.lineTo(x, y + sin(x / w * 12.56f) * step * 0.3f); x += 4f }
                    drawPath(path, light, style = Stroke(step * 0.28f))
                    y += step
                }
            }
            "grid" -> {
                val step = w / 7
                var x = 0f
                while (x < w) { drawLine(light, Offset(x, 0f), Offset(x, h), 2f); x += step }
                var y = 0f
                while (y < h) { drawLine(light, Offset(0f, y), Offset(w, y), 2f); y += step }
            }
            "stripes" -> {
                val step = w / 5
                var x = -h
                while (x < w) { drawLine(light, Offset(x, h), Offset(x + h, 0f), step * 0.35f); x += step }
            }
            "linen" -> {
                var y = 0f
                while (y < h) { drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(w, y), 1.5f); y += 5f }
                var x = 0f
                while (x < w) { drawLine(Color.Black.copy(alpha = 0.06f), Offset(x, 0f), Offset(x, h), 1.5f); x += 5f }
            }
            "scales" -> {
                val r = w / 6
                var y = 0f; var row = 0
                while (y < h + r) {
                    var x = if (row % 2 == 0) 0f else r
                    while (x < w + r) { drawCircle(light, r, Offset(x, y), style = Stroke(r * 0.18f)); x += r * 2 }
                    y += r * 0.9f; row++
                }
            }
        }
    }
    if (base.alpha < 0.1f) drawRect(base)
}

/**
 * Cover and template previews: thread-local renderers allow true parallel multi-core rendering
 * with zero lock contention, cached as small quantized bitmaps with async GPU pre-upload.
 */
object CoverCache {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val renderDispatcher = Dispatchers.Default.limitedParallelism(2)
    private val allRenderers = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<PageRenderer, Boolean>()))
    private val threadRenderer = ThreadLocal.withInitial {
        PageRenderer(null).also { allRenderers.add(it) }
    }
    private val cache = object : android.util.LruCache<String, android.graphics.Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: android.graphics.Bitmap) = value.byteCount
    }

    init {
        com.pronotes.app.data.MemoryTrimRegistry.register { level ->
            if (com.pronotes.app.data.MemoryTrimRegistry.isCriticalLevel(level)) {
                cache.evictAll()
                synchronized(allRenderers) {
                    allRenderers.forEach { it.evictCaches() }
                }
            } else {
                cache.trimToSize(cache.maxSize() / 2)
            }
        }
    }

    fun peek(spec: String, w: Int, h: Int): android.graphics.Bitmap? {
        if (w <= 0 || h <= 0) return null
        return cache.get("c:$spec:$w:$h")
    }

    fun get(spec: String, w: Int, h: Int): android.graphics.Bitmap? {
        if (w <= 0 || h <= 0) return null
        val key = "c:$spec:$w:$h"
        cache.get(key)?.let { return it }
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        threadRenderer.get()?.drawCover(android.graphics.Canvas(bmp), android.graphics.RectF(0f, 0f, w.toFloat(), h.toFloat()), spec)
        bmp.prepareToDraw()
        cache.put(key, bmp)
        return bmp
    }

    suspend fun getAsync(spec: String, w: Int, h: Int): android.graphics.Bitmap? {
        if (w <= 0 || h <= 0) return null
        peek(spec, w, h)?.let { return it }
        return withContext(renderDispatcher) { get(spec, w, h) }
    }

    fun peekPaper(style: PaperStyle, tint: Int, w: Int, h: Int, pageW: Float = PageSize.WIDTH, pageH: Float = PageSize.HEIGHT): android.graphics.Bitmap? {
        if (w <= 0 || h <= 0) return null
        return cache.get("p:${style.name}:$tint:$w:$h:${pageW.toInt()}:${pageH.toInt()}")
    }

    fun paper(style: PaperStyle, tint: Int, w: Int, h: Int, pageW: Float = PageSize.WIDTH, pageH: Float = PageSize.HEIGHT): android.graphics.Bitmap? {
        if (w <= 0 || h <= 0) return null
        val key = "p:${style.name}:$tint:$w:$h:${pageW.toInt()}:${pageH.toInt()}"
        cache.get(key)?.let { return it }
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.eraseColor(tint)
        val c = android.graphics.Canvas(bmp)
        val sx = w / pageW
        val sy = h / pageH
        c.scale(sx, sy)
        val r = threadRenderer.get()
        if (r != null) {
            r.paperColor = tint
            r.drawPaper(c, Page("preview", pageW, pageH, style), null, shadow = false)
        }
        bmp.prepareToDraw()
        cache.put(key, bmp)
        return bmp
    }

    suspend fun paperAsync(style: PaperStyle, tint: Int, w: Int, h: Int, pageW: Float = PageSize.WIDTH, pageH: Float = PageSize.HEIGHT): android.graphics.Bitmap? {
        if (w <= 0 || h <= 0) return null
        peekPaper(style, tint, w, h, pageW, pageH)?.let { return it }
        return withContext(renderDispatcher) { paper(style, tint, w, h, pageW, pageH) }
    }
}

/**
 * Paper template preview drawn asynchronously off the main thread with GPU pre-upload.
 * Eliminates SubcomposeLayout (BoxWithConstraints) so template grids scroll smoothly at 120fps.
 */
@Composable
fun PaperPreview(
    style: PaperStyle,
    modifier: Modifier = Modifier,
    tint: Int = 0xFFFFFFFF.toInt(),
    landscape: Boolean = false,
    explicitWidth: Dp? = null,
    explicitHeight: Dp? = null,
) {
    if (style == PaperStyle.BLANK) {
        Box(modifier.background(Color(tint)))
        return
    }
    val pw = if (landscape) PageSize.HEIGHT else PageSize.WIDTH
    val ph = if (landscape) PageSize.WIDTH else PageSize.HEIGHT
    val perfMode = LocalPerformanceMode.current
    val density = LocalDensity.current
    val explicitSize = remember(explicitWidth, explicitHeight, density) {
        if (explicitWidth != null && explicitHeight != null) {
            IntSize(
                with(density) { explicitWidth.toPx() }.roundToInt().coerceAtLeast(1),
                with(density) { explicitHeight.toPx() }.roundToInt().coerceAtLeast(1),
            )
        } else null
    }
    var measuredSize by remember { mutableStateOf(explicitSize ?: IntSize.Zero) }
    val effectiveSize = explicitSize ?: measuredSize
    val maxW = if (explicitSize != null) 160 else perfMode.thumbWidthPx.coerceAtLeast(180)
    val (targetW, targetH) = remember(effectiveSize, maxW) {
        quantizeThumbSize(effectiveSize.width, effectiveSize.height, maxW)
    }

    var bmp by remember(style, tint, targetW, targetH, landscape) {
        mutableStateOf(if (targetW > 0) CoverCache.peekPaper(style, tint, targetW, targetH, pw, ph) else null)
    }
    LaunchedEffect(style, tint, targetW, targetH, landscape) {
        if (targetW > 0 && bmp == null) {
            bmp = CoverCache.paperAsync(style, tint, targetW, targetH, pw, ph)
        }
    }
    val canvasMod = if (explicitSize == null) {
        modifier
            .background(Color(tint))
            .onSizeChanged { if (it != measuredSize) measuredSize = it }
    } else {
        modifier.background(Color(tint))
    }
    Canvas(canvasMod) {
        val ready = bmp
        if (ready != null) {
            drawIntoCanvas { c ->
                coverDestRect.set(0f, 0f, size.width, size.height)
                c.nativeCanvas.drawBitmap(ready, null, coverDestRect, coverDrawPaint)
            }
        }
    }
}
