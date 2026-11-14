package io.github.ntvien.liquidglass.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Ready-made background for [io.github.ntvien.liquidglass.LiquidGlassScene]: a base color and soft color blobs, drawn
 * ONCE into a quarter-resolution bitmap (smooth gradients upscale invisibly). Redrawing a full-screen gradient every
 * frame would repaint it behind every glass element. Being smooth, it also means the glass needs no blur.
 *
 * ```
 * LiquidGlassScene(background = { LiquidGradientBackground() }) { ... }
 * ```
 *
 * @param dark dark base and blob colors; follows the system like [io.github.ntvien.liquidglass.LiquidGlassTheme].
 * @param baseColor color under the blobs.
 * @param blobColors up to 4 colors, one blob each: top left, right, left, bottom right. Extra colors are ignored. Keep
 *   them strong (35–60 % alpha): the lens needs something to bend, a pale background makes glass look like plastic.
 */
@Composable
public fun LiquidGradientBackground(
    modifier: Modifier = Modifier,
    dark: Boolean = isSystemInDarkTheme(),
    baseColor: Color = if (dark) DarkBase else LightBase,
    blobColors: List<Color> = if (dark) DarkBlobColors else LightBlobColors,
) {
    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val w = (size.width / DOWNSCALE).roundToInt().coerceAtLeast(1)
                val h = (size.height / DOWNSCALE).roundToInt().coerceAtLeast(1)
                val bitmap = ImageBitmap(w, h)
                CanvasDrawScope().draw(this, layoutDirection, Canvas(bitmap), Size(w.toFloat(), h.toFloat())) {
                    drawRect(baseColor)
                    BlobPlaces.zip(blobColors).forEach { (b, color) ->
                        val center = Offset(b.x * w, b.y * h)
                        val radius = b.radius * w
                        val brush = Brush.radialGradient(
                            0f to color,
                            0.55f to color.copy(alpha = color.alpha * 0.6f),
                            1f to color.copy(alpha = 0f),
                            center = center,
                            radius = radius,
                        )
                        drawCircle(brush, radius, center)
                    }
                }
                val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
                onDrawBehind { drawImage(bitmap, IntOffset.Zero, IntSize(w, h), dstSize = dst, filterQuality = FilterQuality.Low) }
            },
    )
}

/** Blob center ([x], [y]) as a fraction of the size, [radius] as a fraction of the width. */
private class BlobPlace(val x: Float, val y: Float, val radius: Float)

private val BlobPlaces = listOf(
    BlobPlace(0.08f, 0.1f, 0.85f),
    BlobPlace(0.95f, 0.32f, 0.7f),
    BlobPlace(0.2f, 0.62f, 0.75f),
    BlobPlace(0.85f, 0.92f, 0.8f),
)

private val LightBase = Color(0xFFF4F1EB)
private val DarkBase = Color(0xFF121119)
private val LightBlobColors = listOf(Color(0x8C3B57D6), Color(0x99FFC24D), Color(0x73FF7A6B), Color(0x5940C29B))
private val DarkBlobColors = listOf(Color(0x738EA2FF), Color(0x66FFC24D), Color(0x59FF7A6B), Color(0x4D40C29B))

private const val DOWNSCALE = 4f
