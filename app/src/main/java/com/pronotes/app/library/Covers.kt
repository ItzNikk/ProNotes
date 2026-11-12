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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.pronotes.app.data.PageSize
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.editor.Page
import com.pronotes.app.editor.PageRenderer
import java.io.File
import kotlin.math.sin

val NotebookShape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp)

/**
 * Draws a notebook cover from its spec ("solid:", "pattern:", "image:", "thumb").
 * [noteDir] holds cover photos and thumbnails.
 */
@Composable
fun CoverArt(spec: String, title: String, noteDir: File?, cacheKey: Any, modifier: Modifier = Modifier, showTitle: Boolean = true) {
    val context = LocalContext.current
    Box(modifier.shadow(6.dp, NotebookShape).clip(NotebookShape).background(Color.White)) {
        when {
            spec.startsWith("image:") || spec == "thumb" -> {
                val file = if (spec == "thumb") noteDir?.let { File(it, "thumb.png") } else noteDir?.let { File(it, spec.removePrefix("image:")) }
                if (file != null && file.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(file).memoryCacheKey(file.path + cacheKey)
                            .diskCacheKey(file.path + cacheKey).crossfade(true).build(),
                        contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            else -> {
                val parts = spec.split(':')
                val color = Color((parts.lastOrNull()?.toLongOrNull() ?: 0xFF3D6CF4).toInt())
                val pattern = if (parts.firstOrNull() == "pattern") parts.getOrNull(1) else null
                // Same renderer as the cover page inside the notebook, so both always match.
                // Drawn once into a cached bitmap: galleries of covers stay smooth while you pick.
                Canvas(Modifier.fillMaxSize()) {
                    val bmp = CoverCache.get(spec, size.width.toInt(), size.height.toInt())
                    if (bmp != null) drawIntoCanvas { c -> c.nativeCanvas.drawBitmap(bmp, 0f, 0f, null) }
                }
                @Suppress("UNUSED_VARIABLE") val unused = color
                if (showTitle && pattern == null) {
                    Box(
                        Modifier.align(Alignment.Center).fillMaxWidth(0.66f).padding(start = 8.dp)
                            .clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.92f)).padding(6.dp),
                    ) {
                        Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B1F2A), maxLines = 2,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        // Spine shading.
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.08f)
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent))),
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

/** Cover and template previews: one shared renderer, results cached as small bitmaps. */
object CoverCache {
    private val renderer by lazy { PageRenderer(null) }
    private val cache = object : android.util.LruCache<String, android.graphics.Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: android.graphics.Bitmap) = value.byteCount
    }

    fun get(spec: String, w: Int, h: Int): android.graphics.Bitmap? {
        if (w <= 0 || h <= 0) return null
        val key = "c:$spec:$w:$h"
        cache.get(key)?.let { return it }
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        renderer.drawCover(android.graphics.Canvas(bmp), android.graphics.RectF(0f, 0f, w.toFloat(), h.toFloat()), spec)
        cache.put(key, bmp)
        return bmp
    }

    fun paper(style: PaperStyle, tint: Int, w: Int, h: Int, pageW: Float = PageSize.WIDTH, pageH: Float = PageSize.HEIGHT): android.graphics.Bitmap? {
        if (w <= 0 || h <= 0) return null
        val key = "p:${style.name}:$tint:$w:$h:${pageW.toInt()}:${pageH.toInt()}"
        cache.get(key)?.let { return it }
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.eraseColor(tint)
        val c = android.graphics.Canvas(bmp)
        renderer.paperColor = tint
        val sx = w / pageW
        val sy = h / pageH
        c.scale(sx, sy)
        renderer.drawPaper(c, Page("preview", pageW, pageH, style), null, shadow = false)
        cache.put(key, bmp)
        return bmp
    }
}

/** Paper template preview, drawn with the same renderer the editor uses (cached). */
@Composable
fun PaperPreview(
    style: PaperStyle,
    modifier: Modifier = Modifier,
    tint: Int = 0xFFFFFFFF.toInt(),
    landscape: Boolean = false,
) {
    val pw = if (landscape) PageSize.HEIGHT else PageSize.WIDTH
    val ph = if (landscape) PageSize.WIDTH else PageSize.HEIGHT
    Canvas(modifier.background(Color(tint))) {
        val bmp = CoverCache.paper(style, tint, size.width.toInt(), size.height.toInt(), pw, ph)
        if (bmp != null) drawIntoCanvas { c -> c.nativeCanvas.drawBitmap(bmp, 0f, 0f, null) }
    }
}
