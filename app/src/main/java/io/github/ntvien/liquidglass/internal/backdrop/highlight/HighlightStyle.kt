/*
 * Copied from Kyant0/AndroidLiquidGlass "backdrop" 2.0.1 (Apache License 2.0, https://github.com/Kyant0/AndroidLiquidGlass).
 * Modified for liquid-glass: package renamed, declarations made internal, performance changes listed in NOTICE.
 */
package io.github.ntvien.liquidglass.internal.backdrop.highlight

import androidx.annotation.FloatRange
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.shapes.RoundedRectangularShape
import io.github.ntvien.liquidglass.internal.backdrop.RuntimeShader
import io.github.ntvien.liquidglass.internal.backdrop.RuntimeShaderCache
import io.github.ntvien.liquidglass.internal.backdrop.internal.AmbientHighlightShaderString
import io.github.ntvien.liquidglass.internal.backdrop.internal.DefaultHighlightShaderString
import io.github.ntvien.liquidglass.internal.backdrop.isRuntimeShaderSupported
import kotlin.math.PI

@Immutable
internal interface HighlightStyle {

    val color: Color

    val blendMode: BlendMode

    fun DrawScope.createShader(
        shape: Shape,
        runtimeShaderCache: RuntimeShaderCache
    ): RuntimeShader?

    @Immutable
    data class Plain(
        override val color: Color = Color.White.copy(alpha = 0.38f),
        override val blendMode: BlendMode = BlendMode.Plus
    ) : HighlightStyle {

        override fun DrawScope.createShader(
            shape: Shape,
            runtimeShaderCache: RuntimeShaderCache
        ): RuntimeShader? = null
    }

    @Immutable
    data class Default(
        override val color: Color = Color.White.copy(alpha = 0.5f),
        override val blendMode: BlendMode = BlendMode.Plus,
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f
    ) : HighlightStyle {

        override fun DrawScope.createShader(
            shape: Shape,
            runtimeShaderCache: RuntimeShaderCache
        ): RuntimeShader? {
            return if (isRuntimeShaderSupported()) {
                runtimeShaderCache.obtainRuntimeShader(
                    "Default",
                    DefaultHighlightShaderString
                ).apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadii", getCornerRadii(shape))
                    setColorUniform("color", color.copy(alpha = 1f))
                    setFloatUniform("angle", angle * (PI / 180f).toFloat())
                    setFloatUniform("falloff", falloff)
                }
            } else {
                null
            }
        }
    }

    @Immutable
    data class Ambient(
        @param:FloatRange(from = 0.0, to = 1.0) val intensity: Float = 0.38f
    ) : HighlightStyle {

        override val color: Color = Color.White.copy(alpha = intensity)

        override val blendMode: BlendMode = DrawScope.DefaultBlendMode

        override fun DrawScope.createShader(
            shape: Shape,
            runtimeShaderCache: RuntimeShaderCache
        ): RuntimeShader? {
            return if (isRuntimeShaderSupported()) {
                runtimeShaderCache.obtainRuntimeShader(
                    "Ambient",
                    AmbientHighlightShaderString
                ).apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadii", getCornerRadii(shape))
                    setFloatUniform("angle", 45f * (PI / 180f).toFloat())
                    setFloatUniform("falloff", 1f)
                }
            } else {
                null
            }
        }
    }

    companion object {

        @Stable
        val Default: Default = Default()

        @Stable
        val Ambient: Ambient = Ambient()

        @Stable
        val Plain: Plain = Plain()
    }
}

private fun DrawScope.getCornerRadii(shape: Shape): FloatArray {
    val size = size
    val maxRadius = size.minDimension / 2f
    // Continuous corners (RoundedRectangle, Capsule) are not CornerBasedShape: read their radii like effects/Lens.kt does,
    // instead of treating them as a capsule (wrong rim on tall cards).
    if (shape is RoundedRectangularShape) {
        val corners = shape.corners(size, layoutDirection, this)
        return floatArrayOf(corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft)
    }
    val shape = shape as? CornerBasedShape ?: return FloatArray(4) { maxRadius }
    val isLtr = layoutDirection == LayoutDirection.Ltr
    val topLeft =
        if (isLtr) shape.topStart.toPx(size, this)
        else shape.topEnd.toPx(size, this)
    val topRight =
        if (isLtr) shape.topEnd.toPx(size, this)
        else shape.topStart.toPx(size, this)
    val bottomRight =
        if (isLtr) shape.bottomEnd.toPx(size, this)
        else shape.bottomStart.toPx(size, this)
    val bottomLeft =
        if (isLtr) shape.bottomStart.toPx(size, this)
        else shape.bottomEnd.toPx(size, this)
    return floatArrayOf(
        topLeft.fastCoerceAtMost(maxRadius),
        topRight.fastCoerceAtMost(maxRadius),
        bottomRight.fastCoerceAtMost(maxRadius),
        bottomLeft.fastCoerceAtMost(maxRadius)
    )
}
