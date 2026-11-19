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
import kotlin.math.hypot
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
 * Exact 2D perpendicular-bisector paper fold geometry starting from the bottom corner
 * (`(w, h)` for forward turns, `(0, h)` for backward turns).
 *
 * The top corner remains completely flat until the diagonal fold axis naturally climbs the
 * side edge and reaches the top edge, producing authentic book-page curl kinematics.
 */
private data class BottomCurlGeometry(
    val keptPoly: List<Offset>,
    val peeledPoly: List<Offset>,
    val backFlapPoly: List<Offset>,
    val foldMid: Offset,
    val foldNormal: Offset, // Unit normal pointing from flat page toward lifted corner C
    val foldStart: Offset,
    val foldEnd: Offset,
    val flapReach: Float,
    val arcSin: Float,
) {
    fun reflect(p: Offset): Offset {
        val d = (p.x - foldMid.x) * foldNormal.x + (p.y - foldMid.y) * foldNormal.y
        return Offset(p.x - 2f * d * foldNormal.x, p.y - 2f * d * foldNormal.y)
    }
}

private fun clipPolygonHalfPlane(
    poly: List<Offset>,
    origin: Offset,
    normal: Offset,
    keepNonPositive: Boolean,
): List<Offset> {
    if (poly.isEmpty()) return emptyList()
    fun signedDist(p: Offset) = (p.x - origin.x) * normal.x + (p.y - origin.y) * normal.y
    fun inside(d: Float) = if (keepNonPositive) d <= 1e-3f else d >= -1e-3f
    val out = ArrayList<Offset>(poly.size + 2)
    var prev = poly.last()
    var prevD = signedDist(prev)
    var prevIn = inside(prevD)
    for (cur in poly) {
        val curD = signedDist(cur)
        val curIn = inside(curD)
        if (curIn != prevIn) {
            val denom = (prevD - curD)
            val t = if (abs(denom) > 1e-5f) (prevD / denom).coerceIn(0f, 1f) else 0.5f
            out += Offset(prev.x + (cur.x - prev.x) * t, prev.y + (cur.y - prev.y) * t)
        }
        if (curIn) out += cur
        prev = cur
        prevD = curD
        prevIn = curIn
    }
    return out
}

private fun computeBottomCurl(
    w: Float,
    h: Float,
    turnDir: Int,
    progress: Float,
    touchY: Float,
): BottomCurlGeometry {
    val p = progress.coerceIn(0.001f, 1f)
    val arcSin = sin(p * PI).toFloat().coerceAtLeast(0f)
    // Natural bottom-corner lift bias: higher drag Y keeps curl tighter near bottom;
    // mid/upper drag smoothly broadens the diagonal angle while still starting at the bottom corner.
    val clampedTouch = touchY.coerceIn(0.32f, 0.96f)
    val liftFactor = (0.34f + (1f - clampedTouch) * 0.42f).coerceIn(0.28f, 0.62f)

    // Corner origin C at bottom-right (forward) or bottom-left (backward)
    val corner = if (turnDir > 0) Offset(w, h) else Offset(0f, h)

    // Smooth curved tip trajectory from corner C across the opposite edge
    val tipX = if (turnDir > 0) {
        w * (1f - 2.18f * p)
    } else {
        w * (2.18f * p)
    }
    // Arch upward along a smooth sin curve so fold starts at a realistic diagonal angle from the bottom corner
    val tipLift = h * liftFactor * sin(p * PI * 0.88).toFloat().coerceAtLeast(0f) * (1f - 0.35f * p)
    val tip = Offset(tipX, (h - tipLift).coerceAtLeast(h * 0.14f))

    val dx = corner.x - tip.x
    val dy = corner.y - tip.y
    val dist = hypot(dx, dy).coerceAtLeast(1f)
    val nx = dx / dist
    val ny = dy / dist
    val normal = Offset(nx, ny)
    val mid = Offset((corner.x + tip.x) * 0.5f, (corner.y + tip.y) * 0.5f)

    val rect = listOf(Offset(0f, 0f), Offset(w, 0f), Offset(w, h), Offset(0f, h))
    val kept = clipPolygonHalfPlane(rect, mid, normal, keepNonPositive = true)
    val peeled = clipPolygonHalfPlane(rect, mid, normal, keepNonPositive = false)

    // Find the two intersection vertices lying directly on the fold crease line (d ≈ 0)
    val creasePts = peeled.filter { pt ->
        abs((pt.x - mid.x) * nx + (pt.y - mid.y) * ny) < 2.5f
    }
    val foldStart = creasePts.firstOrNull() ?: mid
    val foldEnd = creasePts.lastOrNull() ?: mid

    val backFlap = peeled.map { pt ->
        val d = (pt.x - mid.x) * nx + (pt.y - mid.y) * ny
        Offset(pt.x - 2f * d * nx, pt.y - 2f * d * ny)
    }

    val maxDepth = peeled.maxOfOrNull { pt ->
        ((pt.x - mid.x) * nx + (pt.y - mid.y) * ny).coerceAtLeast(0f)
    } ?: 1f

    return BottomCurlGeometry(
        keptPoly = kept,
        peeledPoly = peeled,
        backFlapPoly = backFlap,
        foldMid = mid,
        foldNormal = normal,
        foldStart = foldStart,
        foldEnd = foldEnd,
        flapReach = maxDepth.coerceAtLeast(4f),
        arcSin = arcSin,
    )
}

private fun polygonToPath(pts: List<Offset>): Path = Path().apply {
    if (pts.size < 3) return@apply
    moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        lineTo(pts[i].x, pts[i].y)
    }
    close()
}

/**
 * Interactive 3-layer bottom-corner realistic page-curl stage:
 * 1) Target next sheet underneath with a fold-normal cast shadow inside the peeled region
 * 2) Current sheet clipped cleanly along the exact diagonal fold axis (keeping top flat initially)
 * 3) Geometrically reflected curved backside paper flap with mirrored bleed-through, cylindrical
 *    paper lighting gradient along the fold normal, and soft cast shadow onto the flat page
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
                    if (w < 4f || h < 4f) return@Canvas
                    val curl = computeBottomCurl(w, h, turnDir, curlProgress, curlTouchY)
                    val nx = curl.foldNormal.x
                    val ny = curl.foldNormal.y
                    val arcSin = curl.arcSin

                    // 1) Cast shadow onto the REVEALED next page inside the peeled diagonal corner
                    if (curl.peeledPoly.size >= 3) {
                        val peeledPath = polygonToPath(curl.peeledPoly)
                        val shadowDepth = (curl.flapReach * 0.52f).coerceIn(22f, w * 0.36f)
                        val s0 = curl.foldMid
                        val s1 = Offset(s0.x + nx * shadowDepth, s0.y + ny * shadowDepth)
                        clipPath(peeledPath) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.42f * arcSin.coerceAtLeast(0.25f)),
                                        Color.Black.copy(alpha = 0.16f * arcSin.coerceAtLeast(0.20f)),
                                        Color.Transparent,
                                    ),
                                    start = s0,
                                    end = s1,
                                ),
                            )
                        }
                    }

                    // 2) Soft floating shadow cast by the lifted curl flap onto the flat current page
                    if (curl.keptPoly.size >= 3 && curl.backFlapPoly.size >= 3) {
                        val keptPath = polygonToPath(curl.keptPoly)
                        val flapShadowDepth = (curl.flapReach * 1.18f + 26f).coerceAtLeast(32f)
                        val k0 = curl.foldMid
                        val k1 = Offset(k0.x - nx * flapShadowDepth, k0.y - ny * flapShadowDepth)
                        clipPath(keptPath) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.24f * arcSin.coerceAtLeast(0.2f)),
                                        Color.Black.copy(alpha = 0.08f * arcSin.coerceAtLeast(0.15f)),
                                        Color.Transparent,
                                    ),
                                    start = k0,
                                    end = k1,
                                ),
                            )
                        }
                    }

                    // 3) Reflected backside paper flap with mirrored page bleed-through & cylindrical highlight
                    if (curl.backFlapPoly.size >= 3) {
                        val flapPath = polygonToPath(curl.backFlapPoly)

                        // Soft ambient drop rim around the lifted back flap
                        drawPath(
                            path = flapPath,
                            color = Color.Black.copy(alpha = 0.16f * arcSin.coerceAtLeast(0.2f)),
                            style = Stroke(width = 16f),
                        )
                        drawPath(
                            path = flapPath,
                            color = Color.Black.copy(alpha = 0.07f * arcSin.coerceAtLeast(0.2f)),
                            style = Stroke(width = 30f),
                        )

                        clipPath(flapPath) {
                            // Backside paper colour (slightly warmer/toned than front for realism)
                            drawRect(theme.pageBg)

                            // Geometrically exact Householder reflection of the turning page onto the paper back
                            if (turningBmp != null && !turningBmp.isRecycled) {
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    nc.save()
                                    val mx = curl.foldMid.x
                                    val my = curl.foldMid.y
                                    val refl = android.graphics.Matrix().apply {
                                        // Householder reflection matrix R = I - 2 n n^T about foldMid
                                        val r00 = 1f - 2f * nx * nx
                                        val r01 = -2f * nx * ny
                                        val r10 = -2f * nx * ny
                                        val r11 = 1f - 2f * ny * ny
                                        val tx = mx - (r00 * mx + r01 * my)
                                        val ty = my - (r10 * mx + r11 * my)
                                        setValues(
                                            floatArrayOf(
                                                r00, r01, tx,
                                                r10, r11, ty,
                                                0f, 0f, 1f,
                                            ),
                                        )
                                    }
                                    nc.concat(refl)
                                    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                                        alpha = if (theme.isDark) 26 else 36
                                    }
                                    val leftRect = if (spread && turnDir > 0) w * 0.5f else 0f
                                    val rightRect = if (spread && turnDir < 0) w * 0.5f else w
                                    nc.drawBitmap(turningBmp, null, android.graphics.RectF(leftRect, 0f, rightRect, h), paint)
                                    nc.restore()
                                }
                            }

                            // Cylindrical paper-bend shading gradient orthogonal to the diagonal fold axis
                            val gStart = curl.foldMid
                            val gEnd = Offset(
                                curl.foldMid.x - nx * curl.flapReach.coerceAtLeast(18f),
                                curl.foldMid.y - ny * curl.flapReach.coerceAtLeast(18f),
                            )
                            val highlightAlpha = if (theme.isDark) 0.17f else 0.58f
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Black.copy(alpha = 0.28f * arcSin.coerceAtLeast(0.35f)),
                                        0.10f to Color.White.copy(alpha = highlightAlpha * arcSin.coerceAtLeast(0.45f)),
                                        0.24f to Color.White.copy(alpha = highlightAlpha * 0.42f),
                                        0.48f to Color.Transparent,
                                        0.82f to Color.Black.copy(alpha = 0.09f),
                                        1.00f to Color.Black.copy(alpha = 0.20f * arcSin.coerceAtLeast(0.30f)),
                                    ),
                                    start = gStart,
                                    end = gEnd,
                                ),
                            )
                        }

                        // Subtle crisp paper border along the curled flap & fold crease highlight
                        drawPath(
                            path = flapPath,
                            color = theme.mutedText.copy(alpha = 0.28f),
                            style = Stroke(width = 1.4f),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = if (theme.isDark) 0.16f else 0.48f),
                            start = curl.foldStart,
                            end = curl.foldEnd,
                            strokeWidth = 2.2f,
                        )
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
                val curl = computeBottomCurl(size.width, size.height, turnDir, progress, touchY)
                return Outline.Generic(polygonToPath(curl.keptPoly))
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
