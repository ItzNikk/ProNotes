package com.pronotes.app.reader

import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.appPillShape
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthesizes realistic physical paper-flip swish & page-settle acoustic waves in real time.
 * Zero external audio files required; works offline with ultra-low latency.
 */
internal object BookFlipSoundEngine {
    private const val SAMPLE_RATE = 22050
    private val audioExecutor = Executors.newSingleThreadExecutor()
    @Volatile
    private var lastRustleAt = 0L

    fun playPageTurnSwish(volume: Float = 0.72f, forward: Boolean = true) {
        val vol = volume.coerceIn(0.05f, 1f)
        audioExecutor.execute {
            runCatching {
                val durationSec = 0.25f
                val totalSamples = (SAMPLE_RATE * durationSec).toInt()
                val pcm = ShortArray(totalSamples)
                val rng = Random(System.nanoTime())
                var lp1 = 0f
                var lp2 = 0f
                var prev = 0f

                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / totalSamples
                    val liftEnv = exp(-((t - 0.14f) * (t - 0.14f)) / 0.008f) * 0.45f
                    val swishEnv = sin(t * PI).toFloat().let { it * it } * 0.85f
                    val settleEnv = exp(-((t - 0.86f) * (t - 0.86f)) / 0.0018f) * 0.52f

                    val white = rng.nextFloat() * 2f - 1f
                    val cutoff = if (forward) (0.18f + 0.34f * sin(t * PI).toFloat()) else (0.15f + 0.28f * sin(t * PI).toFloat())
                    lp1 += cutoff * (white - lp1)
                    lp2 += cutoff * (lp1 - lp2)
                    val bandNoise = (lp1 - lp2 * 0.55f) + (white - prev) * 0.22f
                    prev = white

                    val settleThud = sin(2.0 * PI * (if (forward) 105.0 else 92.0) * (i.toDouble() / SAMPLE_RATE)).toFloat() * settleEnv
                    val sample = ((bandNoise * (liftEnv + swishEnv) + settleThud) * vol * 0.82f).coerceIn(-1f, 1f)
                    pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                playPcm(pcm)
            }
        }
    }

    fun playDragRustleTick(speedPx: Float, volume: Float = 0.45f) {
        val now = System.currentTimeMillis()
        if (now - lastRustleAt < 65L) return
        lastRustleAt = now
        val vol = (volume * (abs(speedPx) / 48f).coerceIn(0.2f, 0.85f)).coerceIn(0.04f, 0.65f)
        audioExecutor.execute {
            runCatching {
                val totalSamples = (SAMPLE_RATE * 0.045f).toInt()
                val pcm = ShortArray(totalSamples)
                val rng = Random(now)
                var lp = 0f
                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / totalSamples
                    val env = sin(t * PI).toFloat()
                    val w = rng.nextFloat() * 2f - 1f
                    lp += 0.28f * (w - lp)
                    val s = (lp * env * vol * 0.55f).coerceIn(-1f, 1f)
                    pcm[i] = (s * Short.MAX_VALUE).toInt().toShort()
                }
                playPcm(pcm)
            }
        }
    }

    private fun playPcm(pcm: ShortArray) {
        val byteCount = pcm.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(byteCount, AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        Thread.sleep((pcm.size * 1000L / SAMPLE_RATE) + 35L)
        runCatching {
            track.stop()
            track.release()
        }
    }
}

/**
 * Interactive 3-layer curved page-peel stage:
 * 1) Target next sheet underneath with crease drop shadow
 * 2) Current sheet clipped dynamically along a curved Bezier diagonal crease
 * 3) Curled backside paper flap with faint bleed-through reflection & cylindrical shading gradient
 */
@Composable
internal fun BookCurlStage(
    theme: BookTheme,
    flipStyle: BookFlipStyle,
    spread: Boolean,
    title: String,
    totalPages: Int,
    curPageNum: Int,
    nextPageNum: Int,
    curLeftBmp: Bitmap?,
    curRightBmp: Bitmap?,
    nextLeftBmp: Bitmap?,
    nextRightBmp: Bitmap?,
    turnDir: Int,
    curlProgress: Float,
    curlTouchY: Float,
    colorFilter: ColorFilter?,
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    val activeCurl = turnDir != 0 && curlProgress > 0.005f
    Box(modifier) {
        if (activeCurl) {
            BookSheetFrame(
                theme = theme,
                spread = spread,
                leftBmp = nextLeftBmp,
                rightBmp = nextRightBmp,
                leftPageNum = nextPageNum,
                rightPageNum = if (spread && nextPageNum < totalPages) nextPageNum + 1 else null,
                totalPages = totalPages,
                title = title,
                colorFilter = colorFilter,
                shimmer = shimmer,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val cornerRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { clip = true; shape = RoundedCornerShape(16.dp) },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .let { base ->
                        when {
                            !activeCurl -> base
                            flipStyle == BookFlipStyle.REALISTIC_CURL -> base.drawCurvedPageClip(
                                turnDir = turnDir,
                                progress = curlProgress,
                                touchY = curlTouchY,
                            )
                            flipStyle == BookFlipStyle.SPINE_FLIP -> base.graphicsLayer {
                                val p = curlProgress.coerceIn(0f, 1f)
                                cameraDistance = 28f * density
                                transformOrigin = TransformOrigin(if (turnDir > 0) 0f else 1f, 0.5f)
                                rotationY = if (turnDir > 0) -105f * p else 105f * p
                                alpha = (1f - (p - 0.55f).coerceAtLeast(0f) * 2.2f).coerceIn(0f, 1f)
                            }
                            flipStyle == BookFlipStyle.CRISP_SLIDE -> base.graphicsLayer {
                                val p = curlProgress.coerceIn(0f, 1f)
                                translationX = (if (turnDir > 0) -1f else 1f) * size.width * p
                            }
                            else -> base
                        }
                    },
            ) {
                BookSheetFrame(
                    theme = theme,
                    spread = spread,
                    leftBmp = curLeftBmp,
                    rightBmp = curRightBmp,
                    leftPageNum = curPageNum,
                    rightPageNum = if (spread && curPageNum < totalPages) curPageNum + 1 else null,
                    totalPages = totalPages,
                    title = title,
                    colorFilter = colorFilter,
                    shimmer = shimmer,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (activeCurl && flipStyle == BookFlipStyle.REALISTIC_CURL) {
                val turningBmp = if (spread && turnDir > 0) (curRightBmp ?: curLeftBmp) else curLeftBmp
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val p = curlProgress.coerceIn(0f, 1f)
                    val arcSin = sin(p * PI).toFloat()
                    val bias = (curlTouchY - 0.5f) * 2f

                    if (turnDir > 0) {
                        val foldX = w * (1f - 1.18f * p)
                        val skew = bias * h * 0.17f * arcSin
                        val topX = foldX + skew
                        val botX = foldX - skew
                        val midX = (topX + botX) * 0.5f + minOf(w * 0.075f, (w - foldX).coerceAtLeast(0f) * 0.22f) * arcSin
                        val flapW = ((w - foldX).coerceAtLeast(0f) * 0.48f).coerceIn(16f, w * 0.48f)

                        val exposedShadow = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.36f * arcSin),
                                Color.Black.copy(alpha = 0.14f * arcSin),
                                Color.Transparent,
                            ),
                            startX = minOf(topX, botX) - 4f,
                            endX = minOf(maxOf(topX, botX) + flapW * 1.15f + 56f, w),
                        )
                        val shadowPath = Path().apply {
                            moveTo(topX, 0f)
                            quadraticTo(midX, h * 0.5f, botX, h)
                            lineTo(w, h)
                            lineTo(w, 0f)
                            close()
                        }
                        clipPath(shadowPath) {
                            drawRect(exposedShadow)
                        }

                        val flapTopX = (topX - flapW * (0.95f - 0.14f * bias)).coerceAtLeast(-w * 0.25f)
                        val flapBotX = (botX - flapW * (0.95f + 0.14f * bias)).coerceAtLeast(-w * 0.25f)
                        val flapOuterMidX = (flapTopX + flapBotX) * 0.5f - flapW * 0.15f * arcSin
                        val topArchY = (h * 0.025f * arcSin * (1f - bias * 0.4f)).coerceAtLeast(0f)
                        val botArchY = h - (h * 0.025f * arcSin * (1f + bias * 0.4f)).coerceAtLeast(0f)

                        val flapPath = Path().apply {
                            moveTo(topX, 0f)
                            quadraticTo(midX, h * 0.5f, botX, h)
                            lineTo(flapBotX, botArchY)
                            quadraticTo(flapOuterMidX, h * 0.5f, flapTopX, topArchY)
                            close()
                        }

                        drawPath(path = flapPath, color = Color.Black.copy(alpha = 0.18f * arcSin), style = Stroke(width = 22f))
                        drawPath(path = flapPath, color = Color.Black.copy(alpha = 0.09f * arcSin), style = Stroke(width = 38f))

                        clipPath(flapPath) {
                            drawRect(theme.pageBg)
                            if (turningBmp != null && !turningBmp.isRecycled) {
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    nc.save()
                                    val rectLeft = minOf(flapTopX, flapBotX)
                                    val rectRight = maxOf(topX, botX)
                                    val rectW = (rectRight - rectLeft).coerceAtLeast(1f)
                                    nc.translate(rectRight, 0f)
                                    nc.scale(-1f, 1f)
                                    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                                        alpha = if (theme.isDark) 28 else 38
                                    }
                                    nc.drawBitmap(turningBmp, null, android.graphics.RectF(0f, 0f, rectW * 1.45f, h), paint)
                                    nc.restore()
                                }
                            }
                            val gradStart = minOf(flapTopX, flapBotX)
                            val gradEnd = maxOf(topX, botX)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.22f * arcSin),
                                        Color.White.copy(alpha = if (theme.isDark) 0.14f else 0.52f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.12f * arcSin),
                                        Color.White.copy(alpha = (if (theme.isDark) 0.18f else 0.62f) * arcSin),
                                        Color.Black.copy(alpha = 0.28f * arcSin),
                                    ),
                                    startX = gradStart,
                                    endX = gradEnd.coerceAtLeast(gradStart + 4f),
                                ),
                            )
                        }

                        drawPath(path = flapPath, color = theme.mutedText.copy(alpha = 0.30f), style = Stroke(width = 1.6f))
                    } else {
                        val foldX = w * 1.18f * p
                        val skew = bias * h * 0.17f * arcSin
                        val topX = foldX - skew
                        val botX = foldX + skew
                        val midX = (topX + botX) * 0.5f - minOf(w * 0.075f, foldX.coerceAtLeast(0f) * 0.22f) * arcSin
                        val flapW = (foldX.coerceAtLeast(0f) * 0.48f).coerceIn(16f, w * 0.48f)

                        val shadowPath = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(topX, 0f)
                            quadraticTo(midX, h * 0.5f, botX, h)
                            lineTo(0f, h)
                            close()
                        }
                        clipPath(shadowPath) {
                            val endX = maxOf(topX, botX)
                            val startX = (endX - flapW * 1.15f - 56f).coerceAtLeast(0f)
                            drawRect(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.14f * arcSin),
                                        Color.Black.copy(alpha = 0.36f * arcSin),
                                    ),
                                    startX = startX,
                                    endX = endX.coerceAtLeast(startX + 4f),
                                ),
                            )
                        }

                        val flapTopX = (topX + flapW * (0.95f - 0.14f * bias)).coerceAtMost(w * 1.25f)
                        val flapBotX = (botX + flapW * (0.95f + 0.14f * bias)).coerceAtMost(w * 1.25f)
                        val flapOuterMidX = (flapTopX + flapBotX) * 0.5f + flapW * 0.15f * arcSin
                        val topArchY = (h * 0.025f * arcSin * (1f - bias * 0.4f)).coerceAtLeast(0f)
                        val botArchY = h - (h * 0.025f * arcSin * (1f + bias * 0.4f)).coerceAtLeast(0f)

                        val flapPath = Path().apply {
                            moveTo(topX, 0f)
                            quadraticTo(midX, h * 0.5f, botX, h)
                            lineTo(flapBotX, botArchY)
                            quadraticTo(flapOuterMidX, h * 0.5f, flapTopX, topArchY)
                            close()
                        }

                        drawPath(path = flapPath, color = Color.Black.copy(alpha = 0.18f * arcSin), style = Stroke(width = 22f))
                        drawPath(path = flapPath, color = Color.Black.copy(alpha = 0.09f * arcSin), style = Stroke(width = 38f))

                        clipPath(flapPath) {
                            drawRect(theme.pageBg)
                            if (turningBmp != null && !turningBmp.isRecycled) {
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    nc.save()
                                    val rectLeft = minOf(topX, botX)
                                    val rectRight = maxOf(flapTopX, flapBotX)
                                    val rectW = (rectRight - rectLeft).coerceAtLeast(1f)
                                    nc.translate(rectLeft + rectW, 0f)
                                    nc.scale(-1f, 1f)
                                    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                                        alpha = if (theme.isDark) 28 else 38
                                    }
                                    nc.drawBitmap(turningBmp, null, android.graphics.RectF(0f, 0f, rectW * 1.45f, h), paint)
                                    nc.restore()
                                }
                            }
                            val gradStart = minOf(topX, botX)
                            val gradEnd = maxOf(flapTopX, flapBotX)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.28f * arcSin),
                                        Color.White.copy(alpha = (if (theme.isDark) 0.18f else 0.62f) * arcSin),
                                        Color.Black.copy(alpha = 0.12f * arcSin),
                                        Color.Transparent,
                                        Color.White.copy(alpha = if (theme.isDark) 0.14f else 0.52f),
                                        Color.Black.copy(alpha = 0.22f * arcSin),
                                    ),
                                    startX = gradStart,
                                    endX = gradEnd.coerceAtLeast(gradStart + 4f),
                                ),
                            )
                        }

                        drawPath(path = flapPath, color = theme.mutedText.copy(alpha = 0.30f), style = Stroke(width = 1.6f))
                    }
                    @Suppress("UNUSED_VARIABLE")
                    val unusedCorner = cornerRadiusPx
                }
            }
        }
    }
}

private fun Modifier.drawCurvedPageClip(
    turnDir: Int,
    progress: Float,
    touchY: Float,
): Modifier = this.then(
    Modifier.graphicsLayer {
        clip = true
        shape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Outline {
                val w = size.width
                val h = size.height
                val p = progress.coerceIn(0f, 1f)
                val arcSin = sin(p * PI).toFloat()
                val bias = (touchY - 0.5f) * 2f
                val path = Path()
                if (turnDir > 0) {
                    val foldX = w * (1f - 1.18f * p)
                    val skew = bias * h * 0.17f * arcSin
                    val topX = foldX + skew
                    val botX = foldX - skew
                    val midX = (topX + botX) * 0.5f + minOf(w * 0.075f, (w - foldX).coerceAtLeast(0f) * 0.22f) * arcSin
                    path.moveTo(0f, 0f)
                    path.lineTo(topX, 0f)
                    path.quadraticTo(midX, h * 0.5f, botX, h)
                    path.lineTo(0f, h)
                    path.close()
                } else {
                    val foldX = w * 1.18f * p
                    val skew = bias * h * 0.17f * arcSin
                    val topX = foldX - skew
                    val botX = foldX + skew
                    val midX = (topX + botX) * 0.5f - minOf(w * 0.075f, foldX.coerceAtLeast(0f) * 0.22f) * arcSin
                    path.moveTo(topX, 0f)
                    path.lineTo(w, 0f)
                    path.lineTo(w, h)
                    path.lineTo(botX, h)
                    path.quadraticTo(midX, h * 0.5f, topX, 0f)
                    path.close()
                }
                return Outline.Generic(path)
            }
        }
    },
)

@Composable
internal fun BookSheetFrame(
    theme: BookTheme,
    spread: Boolean,
    leftBmp: Bitmap?,
    rightBmp: Bitmap?,
    leftPageNum: Int,
    rightPageNum: Int?,
    totalPages: Int,
    title: String,
    colorFilter: ColorFilter?,
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .shadow(10.dp, shape, clip = false)
            .clip(shape)
            .background(theme.pageBg)
            .border(1.dp, theme.mutedText.copy(alpha = 0.14f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif),
                    color = theme.mutedText.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (spread && rightPageNum != null) "pp. $leftPageNum–$rightPageNum / $totalPages"
                    else "p. $leftPageNum / $totalPages",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif),
                    color = theme.mutedText.copy(alpha = 0.78f),
                )
            }

            if (spread) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    BookPageSurface(
                        bmp = leftBmp,
                        pageNum = leftPageNum,
                        colorFilter = colorFilter,
                        theme = theme,
                        shimmer = shimmer,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    Box(
                        Modifier
                            .width(14.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = if (theme.isDark) 0.42f else 0.16f),
                                        Color.Black.copy(alpha = if (theme.isDark) 0.25f else 0.08f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    if (rightPageNum != null) {
                        BookPageSurface(
                            bmp = rightBmp,
                            pageNum = rightPageNum,
                            colorFilter = colorFilter,
                            theme = theme,
                            shimmer = shimmer,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    } else {
                        Box(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else {
                BookPageSurface(
                    bmp = leftBmp,
                    pageNum = leftPageNum,
                    colorFilter = colorFilter,
                    theme = theme,
                    shimmer = shimmer,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BookPageSurface(
    bmp: Bitmap?,
    pageNum: Int,
    colorFilter: ColorFilter?,
    theme: BookTheme,
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.pageBg),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Book page $pageNum",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            BookPageLoaderPreview(
                theme = theme,
                pageNum = pageNum,
                shimmer = shimmer,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Animated book page skeleton wireframe displayed while high-res PDF/note pages render. */
@Composable
private fun BookPageLoaderPreview(
    theme: BookTheme,
    pageNum: Int,
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(theme.pageBg), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padX = w * 0.11f
            val padY = h * 0.10f
            val lineCol = theme.mutedText.copy(alpha = 0.14f)
            val accentCol = theme.accent.copy(alpha = 0.20f)
            val cardCol = theme.mutedText.copy(alpha = 0.08f)

            drawRoundRect(accentCol, Offset(padX, padY), Size((w - padX * 2f) * 0.45f, h * 0.032f), CornerRadius(8f))
            val ratios = listOf(0.92f, 0.96f, 0.85f, 0.94f, 0.68f, 0.90f, 0.93f, 0.78f, 0.91f, 0.56f)
            var y = padY + h * 0.08f
            val step = h * 0.055f
            for (r in ratios) {
                if (y > h - padY * 2f) break
                drawRoundRect(lineCol, Offset(padX, y), Size((w - padX * 2f) * r, h * 0.018f), CornerRadius(6f))
                y += step
            }
            drawRoundRect(cardCol, Offset(padX, h * 0.68f), Size(w - padX * 2f, h * 0.18f), CornerRadius(12f))

            val sweepX = -w * 0.3f + w * 1.6f * shimmer
            drawRect(
                color = if (theme.isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.42f),
                topLeft = Offset(sweepX, 0f),
                size = Size(w * 0.25f, h),
            )
        }
        AppSurface(
            role = SurfaceRole.Popover,
            containerColor = theme.chromeBg,
            contentColor = theme.inkText,
            shape = appPillShape(),
            borderColor = theme.mutedText.copy(alpha = 0.22f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                "Loading Page $pageNum…",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Serif),
                color = theme.inkText,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
