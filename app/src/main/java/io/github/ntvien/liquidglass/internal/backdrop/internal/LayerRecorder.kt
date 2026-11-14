/*
 * Copied from Kyant0/AndroidLiquidGlass "backdrop" 2.0.1 (Apache License 2.0, https://github.com/Kyant0/AndroidLiquidGlass).
 * Modified for liquid-glass: package renamed, declarations made internal, performance changes listed in NOTICE.
 */
package io.github.ntvien.liquidglass.internal.backdrop.internal

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize

context(node: DelegatableNode)
internal fun DrawScope.recordLayer(
    layer: GraphicsLayer,
    size: IntSize = this.size.toIntSize(),
    block: DrawScope.() -> Unit
) {
    val density = node.requireDensity()
    layer.record(size) {
        val prevDensity = drawContext.density
        drawContext.density = density
        try {
            this.block()
        } finally {
            drawContext.density = prevDensity
        }
    }
}
