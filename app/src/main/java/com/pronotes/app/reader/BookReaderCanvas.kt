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
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
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
 * 3D cylindrical page-curl geometry starting strictly from the bottom corner
 * (`(w, h)` for forward turns, `(0, h)` for backward turns).
 *
 * The sheet wraps smoothly around a virtual 3D cylinder of radius [radius] whose tangent axis
 * sweeps diagonally upward and across the turning leaf. Every vertex `(px, py)` with signed
 * distance `d = (P · n) - lineOffset` maps smoothly over the cylinder:
 * - `d <= 0`: flat front page
 * - `0 < d <= πR/2`: front curving crest up to silhouette `f = +R`
 * - `πR/2 < d <= πR`: back curving cylinder tube (`f = R * sin(d / R)`)
 * - `d > πR`: flipped flat underside (`f = πR - d`)
 */
private data class CylindricalCurlGeometry(
    val sheetLeft: Float,
    val sheetRight: Float,
    val sheetHeight: Float,
    val normal: Offset,          // Unit normal pointing from flat remaining page toward lifted corner
    val tangent: Offset,         // Unit tangent along the cylinder axis (-ny, nx)
    val lineOffset: Float,       // Signed coordinate of cylinder tangent line L along normal
    val radius: Float,           // 3D cylinder radius R
    val halfPiR: Float,          // π * R / 2 (silhouette crest arc distance)
    val piR: Float,              // π * R (full half-turn around cylinder)
    val maxD: Float,             // Maximum signed distance d reached on the turning leaf
    val arcSin: Float,
    val keptFrontPoly: List<Offset>,   // Flat + crest-clipped region of current sheet (`P · n <= lineOffset + R`)
    val revealedPoly: List<Offset>,    // Exposed region of next sheet (`P · n >= lineOffset + R`)
    val backSilhouettePts: List<Offset>, // Exact 3D curved outer boundary of the rolled back leaf
    val crestMid: Offset,        // Midpoint along the cylinder crest line `P · n = lineOffset + R`
    val crestStart: Offset,
    val crestEnd: Offset,
) {
    /** Monotone projection for back-side `drawBitmapMesh` (zero self-folding). */
    fun projectBack(px: Float, py: Float): Offset {
        val d = px * normal.x + py * normal.y - lineOffset
        val f = when {
            d < halfPiR -> radius + (halfPiR - d)
            d <= piR -> radius * sin((d / radius).toDouble()).toFloat()
            else -> piR - d
        }
        val delta = f - d
        return Offset(px + delta * normal.x, py + delta * normal.y)
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

/**
 * Intersects the infinite line `P · n = targetProj` with rectangle `[left..right, 0..height]`.
 * Returns the two extreme endpoints sorted along tangent `t = (-ny, nx)`, or `null` if disjoint.
 */
private fun intersectLineWithRect(
    left: Float,
    right: Float,
    height: Float,
    nx: Float,
    ny: Float,
    tx: Float,
    ty: Float,
    targetProj: Float,
): Pair<Offset, Offset>? {
    var minS = Float.POSITIVE_INFINITY
    var maxS = Float.NEGATIVE_INFINITY
    var minPt: Offset? = null
    var maxPt: Offset? = null

    fun consider(x: Float, y: Float) {
        val s = x * tx + y * ty
        if (s < minS) {
            minS = s
            minPt = Offset(x, y)
        }
        if (s > maxS) {
            maxS = s
            maxPt = Offset(x, y)
        }
    }

    val eps = 1e-4f
    // Top edge y = 0, Bottom edge y = height
    if (abs(nx) > eps) {
        val xTop = targetProj / nx
        if (xTop in (left - 0.5f)..(right + 0.5f)) consider(xTop.coerceIn(left, right), 0f)
        val xBot = (targetProj - height * ny) / nx
        if (xBot in (left - 0.5f)..(right + 0.5f)) consider(xBot.coerceIn(left, right), height)
    }
    // Left edge x = left, Right edge x = right
    if (abs(ny) > eps) {
        val yLeft = (targetProj - left * nx) / ny
        if (yLeft in -0.5f..(height + 0.5f)) consider(left, yLeft.coerceIn(0f, height))
        val yRight = (targetProj - right * nx) / ny
        if (yRight in -0.5f..(height + 0.5f)) consider(right, yRight.coerceIn(0f, height))
    }
    val a = minPt ?: return null
    val b = maxPt ?: return null
    return a to b
}

private fun computeCylindricalCurl(
    w: Float,
    h: Float,
    spread: Boolean,
    turnDir: Int,
    progress: Float,
    touchY: Float,
): CylindricalCurlGeometry {
    val p = progress.coerceIn(0.001f, 1f)
    val arcSin = sin(p * PI).toFloat().coerceAtLeast(0f)
    val clampedTouch = touchY.coerceIn(0.24f, 0.96f)

    // In 2-page spread mode, only the active right (forward) or left (backward) leaf turns across the spine.
    val sheetL = if (spread && turnDir > 0) w * 0.5f else 0f
    val sheetR = if (spread && turnDir < 0) w * 0.5f else w
    val sheetW = (sheetR - sheetL).coerceAtLeast(1f)

    // Cylinder radius swells smoothly into a rich 3D tube through mid-turn, then relaxes flat at settlement.
    val swell = kotlin.math.sqrt(arcSin.coerceIn(0f, 1f))
    val radius = (sheetW * (if (spread) (0.014f + 0.155f * swell) else (0.012f + 0.118f * swell))).coerceAtLeast(4f)
    val halfPiR = (PI * 0.5).toFloat() * radius
    val piR = PI.toFloat() * radius

    // Diagonal angle starts tilted from the bottom corner and straightens smoothly toward vertical as the page settles.
    val baseTilt = (clampedTouch - 0.5f) * 1.1f
    val tilt = baseTilt * (1f - p * p)
    val cosA = kotlin.math.cos(tilt.toDouble()).toFloat()
    val sinA = sin(tilt.toDouble()).toFloat()
    val nx = if (turnDir > 0) cosA else -cosA
    val ny = sinA
    val tx = -ny
    val ty = nx

    val sheetCorners = listOf(
        Offset(sheetL, 0f),
        Offset(sheetR, 0f),
        Offset(sheetR, h),
        Offset(sheetL, h),
    )
    val projs = sheetCorners.map { it.x * nx + it.y * ny }
    val maxProj = projs.maxOrNull() ?: 0f // Always the bottom outer corner (sheetR, h) or (sheetL, h)
    val minProj = projs.minOrNull() ?: 0f // Opposite top corner

    // Start with cylinder crest right at the bottom corner (maxProj - halfPiR), sweep cleanly to settlement.
    val startOffset = maxProj - halfPiR * 0.92f
    val endOffset = if (spread) {
        // Settle the spine edge cleanly at x = w * 0.5f so back page lays flat over the opposite half.
        val spineProj = (w * 0.5f) * nx + (h * 0.5f) * ny
        spineProj - halfPiR - 2f
    } else {
        // Clear the entire single-page sheet smoothly off the opposite edge.
        minProj - piR - sheetW * 0.06f
    }
    val lineOffset = startOffset + (endOffset - startOffset) * p

    val crestOffset = lineOffset + radius
    val crestOrigin = Offset(nx * crestOffset, ny * crestOffset)

    // Clip full book stage: keep flat current pages on `P · n <= crestOffset`, reveal next pages on `P · n >= crestOffset`.
    val fullRect = listOf(Offset(0f, 0f), Offset(w, 0f), Offset(w, h), Offset(0f, h))
    val keptFrontPoly = if (spread) {
        clipPolygonHalfPlane(sheetCorners, crestOrigin, Offset(nx, ny), keepNonPositive = true)
    } else {
        clipPolygonHalfPlane(fullRect, crestOrigin, Offset(nx, ny), keepNonPositive = true)
    }

    val revealedPoly = clipPolygonHalfPlane(sheetCorners, crestOrigin, Offset(nx, ny), keepNonPositive = false)

    // Find crest segment on turning sheet
    val crestSeg = intersectLineWithRect(sheetL, sheetR, h, nx, ny, tx, ty, lineOffset + halfPiR)
        ?: intersectLineWithRect(sheetL, sheetR, h, nx, ny, tx, ty, crestOffset)
    val crestStart = crestSeg?.first?.let {
        val d = it.x * nx + it.y * ny - lineOffset
        Offset(it.x + (radius - d) * nx, it.y + (radius - d) * ny)
    } ?: crestOrigin
    val crestEnd = crestSeg?.second?.let {
        val d = it.x * nx + it.y * ny - lineOffset
        Offset(it.x + (radius - d) * nx, it.y + (radius - d) * ny)
    } ?: crestOrigin
    val crestMid = Offset((crestStart.x + crestEnd.x) * 0.5f, (crestStart.y + crestEnd.y) * 0.5f)

    val maxD = (maxProj - lineOffset).coerceAtLeast(halfPiR)

    // Trace exact 3D curved outer silhouette of the rolled back leaf (`d` in `[halfPiR .. maxD]`).
    val backSilhouettePts = ArrayList<Offset>(68)
    if (maxD > halfPiR + 1.5f) {
        val samples = ArrayList<Float>(32)
        val arcEnd = minOf(maxD, piR)
        val arcSteps = 18
        for (i in 0..arcSteps) {
            samples += halfPiR + (arcEnd - halfPiR) * (i.toFloat() / arcSteps)
        }
        if (maxD > piR + 1f) {
            val flatSteps = 10
            for (i in 1..flatSteps) {
                samples += piR + (maxD - piR) * (i.toFloat() / flatSteps)
            }
        }
        val sideA = ArrayList<Offset>(samples.size)
        val sideB = ArrayList<Offset>(samples.size)
        for (d in samples) {
            val seg = intersectLineWithRect(sheetL, sheetR, h, nx, ny, tx, ty, lineOffset + d) ?: continue
            val f = if (d <= piR) radius * sin((d / radius).toDouble()).toFloat() else piR - d
            val delta = f - d
            sideA += Offset(seg.first.x + delta * nx, seg.first.y + delta * ny)
            sideB += Offset(seg.second.x + delta * nx, seg.second.y + delta * ny)
        }
        backSilhouettePts.addAll(sideA)
        for (i in sideB.lastIndex downTo 0) {
            backSilhouettePts.add(sideB[i])
        }
    }

    return CylindricalCurlGeometry(
        sheetLeft = sheetL,
        sheetRight = sheetR,
        sheetHeight = h,
        normal = Offset(nx, ny),
        tangent = Offset(tx, ty),
        lineOffset = lineOffset,
        radius = radius,
        halfPiR = halfPiR,
        piR = piR,
        maxD = maxD,
        arcSin = arcSin,
        keptFrontPoly = keptFrontPoly,
        revealedPoly = revealedPoly,
        backSilhouettePts = backSilhouettePts,
        crestMid = crestMid,
        crestStart = crestStart,
        crestEnd = crestEnd,
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

private fun drawCylindricalBackBitmapMesh(
    nativeCanvas: android.graphics.Canvas,
    bmp: Bitmap,
    curl: CylindricalCurlGeometry,
    mirrorHorizontal: Boolean,
    alpha: Int,
    colorFilter: ColorFilter?,
) {
    val cols = 26
    val rows = 26
    val verts = FloatArray((cols + 1) * (rows + 1) * 2)
    val sheetW = curl.sheetRight - curl.sheetLeft
    val sheetH = curl.sheetHeight
    val scale = minOf(sheetW / bmp.width, sheetH / bmp.height)
    val contentWidth = bmp.width * scale
    val contentHeight = bmp.height * scale
    val insetX = (sheetW - contentWidth) / 2f
    val insetY = (sheetH - contentHeight) / 2f
    var idx = 0
    for (r in 0..rows) {
        val v = r.toFloat() / rows
        val py = insetY + v * contentHeight
        for (c in 0..cols) {
            val u = c.toFloat() / cols
            val px = if (mirrorHorizontal) {
                curl.sheetRight - insetX - u * contentWidth
            } else {
                curl.sheetLeft + insetX + u * contentWidth
            }
            val projected = curl.projectBack(px, py)
            verts[idx++] = projected.x
            verts[idx++] = projected.y
        }
    }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        this.alpha = alpha
        this.colorFilter = colorFilter?.asAndroidColorFilter()
    }
    nativeCanvas.drawBitmapMesh(bmp, cols, rows, verts, 0, null, 0, paint)
}

/**
 * Interactive 3D cylindrical bottom-corner page-curl stage:
 * 1) Target next sheet underneath with a wide directional cast shadow inside the peeled region
 * 2) Current sheet clipped cleanly along the 3D cylinder crest silhouette with subtle roll shading
 * 3) True 3D cylindrical curved backside leaf rendered via `Canvas.drawBitmapMesh` (26×26 mesh)
 *    with real back-side page content (two-page spread) or faint mirrored bleed-through (single page),
 *    lush glossy cylindrical specular lighting, deep valley crease shadow, and soft floating cast shadow
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
                                spread = spread,
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
                // On 2-page spreads, the underside of the turning leaf displays the real next facing page!
                // On single-page sheets, the underside displays warm paper with subtle mirrored bleed-through.
                val backBmp = when {
                    spread && turnDir > 0 -> nextLeftBmp
                    spread && turnDir < 0 -> nextRightBmp
                    else -> curLeftBmp
                }
                // Flipping an opaque page around a vertical spine reverses UVs so text reads left-to-right upright.
                val mirrorBackUv = spread
                val backAlpha = when {
                    spread && backBmp != null -> 248
                    theme.isDark -> 34
                    else -> 46
                }

                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (w < 4f || h < 4f) return@Canvas
                    val curl = computeCylindricalCurl(w, h, spread, turnDir, curlProgress, curlTouchY)
                    val nx = curl.normal.x
                    val ny = curl.normal.y
                    val arcSin = curl.arcSin
                    val r = curl.radius

                    // 1) Cast shadow onto the REVEALED next page underneath the lifted cylinder crest
                    if (curl.revealedPoly.size >= 3) {
                        val revealedPath = polygonToPath(curl.revealedPoly)
                        val shadowReach = (r * 3.1f + 26f).coerceAtMost(w * 0.36f)
                        val s0 = curl.crestMid
                        val s1 = Offset(s0.x + nx * shadowReach, s0.y + ny * shadowReach)
                        clipPath(revealedPath) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Black.copy(alpha = 0.48f * arcSin.coerceAtLeast(0.28f)),
                                        0.28f to Color.Black.copy(alpha = 0.22f * arcSin.coerceAtLeast(0.22f)),
                                        0.68f to Color.Black.copy(alpha = 0.06f * arcSin.coerceAtLeast(0.15f)),
                                        1.00f to Color.Transparent,
                                    ),
                                    start = s0,
                                    end = s1,
                                ),
                            )
                        }
                    }

                    // 2) Subtle curved roll-off shading on the front cylinder lip (`lineOffset .. lineOffset + R`)
                    if (curl.keptFrontPoly.size >= 3) {
                        val keptPath = polygonToPath(curl.keptFrontPoly)
                        val lipStart = Offset(curl.crestMid.x - nx * r, curl.crestMid.y - ny * r)
                        val lipEnd = curl.crestMid
                        clipPath(keptPath) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Transparent,
                                        0.55f to Color.Black.copy(alpha = 0.06f * arcSin.coerceAtLeast(0.20f)),
                                        1.00f to Color.Black.copy(alpha = 0.22f * arcSin.coerceAtLeast(0.28f)),
                                    ),
                                    start = lipStart,
                                    end = lipEnd,
                                ),
                            )
                        }
                    }

                    // 3) True 3D cylindrical rolled back-side paper sheet with drawBitmapMesh & specular tube lighting
                    if (curl.backSilhouettePts.size >= 3) {
                        val backPath = polygonToPath(curl.backSilhouettePts)

                        // Soft wide cast shadow under the overhanging rolled back leaf onto the underlying flat page
                        if (curl.keptFrontPoly.size >= 3) {
                            val keptPath = polygonToPath(curl.keptFrontPoly)
                            val backReach = ((curl.maxD - curl.halfPiR) + r * 1.4f).coerceAtLeast(r * 2f)
                            val k0 = curl.crestMid
                            val k1 = Offset(k0.x - nx * backReach, k0.y - ny * backReach)
                            clipPath(keptPath) {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colorStops = arrayOf(
                                            0.00f to Color.Black.copy(alpha = 0.28f * arcSin.coerceAtLeast(0.24f)),
                                            0.35f to Color.Black.copy(alpha = 0.14f * arcSin.coerceAtLeast(0.18f)),
                                            0.75f to Color.Black.copy(alpha = 0.04f * arcSin.coerceAtLeast(0.12f)),
                                            1.00f to Color.Transparent,
                                        ),
                                        start = k0,
                                        end = k1,
                                    ),
                                )
                            }
                        }

                        // Soft feathered drop-shadow rim framing the curved 3D outer leaf edge
                        drawPath(
                            path = backPath,
                            color = Color.Black.copy(alpha = 0.17f * arcSin.coerceAtLeast(0.20f)),
                            style = Stroke(width = 18f),
                        )
                        drawPath(
                            path = backPath,
                            color = Color.Black.copy(alpha = 0.07f * arcSin.coerceAtLeast(0.18f)),
                            style = Stroke(width = 34f),
                        )

                        clipPath(backPath) {
                            // Base backside paper colour
                            drawRect(theme.pageBg)

                            // Warped 26x26 3D cylindrical bitmap mesh of back-side page content
                            if (backBmp != null && !backBmp.isRecycled) {
                                drawIntoCanvas { canvas ->
                                    drawCylindricalBackBitmapMesh(
                                        nativeCanvas = canvas.nativeCanvas,
                                        bmp = backBmp,
                                        curl = curl,
                                        mirrorHorizontal = mirrorBackUv,
                                        alpha = backAlpha,
                                        colorFilter = colorFilter,
                                    )
                                }
                            }

                            // Cylindrical 3D specular tube highlight + deep valley crease shadow across `[crest .. valley]`
                            val tubeStart = curl.crestMid
                            val tubeSpan = (r * 1.65f).coerceAtLeast(24f)
                            val tubeEnd = Offset(tubeStart.x - nx * tubeSpan, tubeStart.y - ny * tubeSpan)
                            val hiAlpha = if (theme.isDark) 0.22f else 0.72f
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Black.copy(alpha = 0.34f * arcSin.coerceAtLeast(0.32f)),
                                        0.16f to Color.Black.copy(alpha = 0.10f * arcSin.coerceAtLeast(0.25f)),
                                        0.38f to Color.White.copy(alpha = hiAlpha * arcSin.coerceAtLeast(0.45f)),
                                        0.56f to Color.White.copy(alpha = hiAlpha * 0.40f),
                                        0.76f to Color.Black.copy(alpha = 0.22f * arcSin.coerceAtLeast(0.30f)),
                                        0.92f to Color.Black.copy(alpha = 0.07f * arcSin.coerceAtLeast(0.20f)),
                                        1.00f to Color.Transparent,
                                    ),
                                    start = tubeStart,
                                    end = tubeEnd,
                                ),
                            )
                        }

                        // Crisp subtle paper rim along the curved 3D silhouette & soft highlight along crest
                        drawPath(
                            path = backPath,
                            color = theme.mutedText.copy(alpha = 0.26f),
                            style = Stroke(width = 1.4f),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = if (theme.isDark) 0.14f else 0.42f),
                            start = curl.crestStart,
                            end = curl.crestEnd,
                            strokeWidth = 1.8f,
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.drawCurvedPageClip(
    spread: Boolean,
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
                val curl = computeCylindricalCurl(w, h, spread, turnDir, progress, touchY)
                val keptPath = polygonToPath(curl.keptFrontPoly)
                if (!spread) return Outline.Generic(keptPath)
                val stationaryPts = if (turnDir > 0) {
                    listOf(Offset(0f, 0f), Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), Offset(0f, h))
                } else {
                    listOf(Offset(w * 0.5f, 0f), Offset(w, 0f), Offset(w, h), Offset(w * 0.5f, h))
                }
                val stationaryPath = polygonToPath(stationaryPts)
                val combined = if (curl.keptFrontPoly.size >= 3) {
                    Path.combine(PathOperation.Union, stationaryPath, keptPath)
                } else {
                    stationaryPath
                }
                return Outline.Generic(combined)
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
    Box(modifier.background(theme.pageBg)) {
        Column(Modifier.fillMaxSize()) {
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
