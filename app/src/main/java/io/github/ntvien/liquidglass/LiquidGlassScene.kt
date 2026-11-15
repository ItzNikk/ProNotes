package io.github.ntvien.liquidglass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal enum class GlassLayer { Content, Overlay }

/**
 * Single shared window backdrop. Captures ONLY the background layer (never NoteCanvasView or scrolling content)
 * so ink drawing latency and recomposition remain completely unaffected.
 */
@Stable
internal class GlassBackdrops(val background: LayerBackdrop) {
    val overlay: LayerBackdrop get() = background
}

internal val LocalGlassBackdrops = staticCompositionLocalOf<GlassBackdrops?> { null }
internal val LocalGlassLayer = staticCompositionLocalOf { GlassLayer.Content }

@Stable
internal class GlassSceneTop {
    internal val entries = mutableStateListOf<Pair<Any, @Composable () -> Unit>>()

    fun show(key: Any, content: @Composable () -> Unit) {
        val i = entries.indexOfFirst { it.first === key }
        if (i >= 0) entries[i] = key to content else entries += key to content
    }

    fun remove(key: Any) {
        entries.removeAll { it.first === key }
    }
}

internal val LocalGlassSceneTop = staticCompositionLocalOf<GlassSceneTop?> { null }

/**
 * Frame of every screen that has glass. Allocates at most ONE shared [LayerBackdrop] per window,
 * and ZERO [LayerBackdrop]s when quality is [GlassQuality.Flat].
 */
@Composable
public fun LiquidGlassScene(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val quality = LocalGlassQuality.current
    val backdrops = if (quality != GlassQuality.Flat) {
        val sharedBackdrop = rememberLayerBackdrop()
        remember(sharedBackdrop) { GlassBackdrops(sharedBackdrop) }
    } else {
        null
    }
    val top = remember { GlassSceneTop() }

    CompositionLocalProvider(
        LocalGlassBackdrops provides backdrops,
        LocalGlassSceneTop provides top,
        LocalInsideGlass provides false,
    ) {
        Box(modifier.fillMaxSize()) {
            val bgModifier = if (backdrops != null) {
                Modifier.fillMaxSize().graphicsLayer().layerBackdrop(backdrops.background)
            } else {
                Modifier.fillMaxSize()
            }
            Box(bgModifier, content = background)
            CompositionLocalProvider(LocalGlassLayer provides GlassLayer.Content) {
                Box(Modifier.fillMaxSize(), content = content)
            }
            CompositionLocalProvider(LocalGlassLayer provides GlassLayer.Overlay) {
                Box(Modifier.fillMaxSize(), content = overlay)
                Box(Modifier.fillMaxSize()) { top.entries.forEach { (k, c) -> key(k) { c() } } }
            }
        }
    }
}

/**
 * Clears scene-level backdrops inside separate Windows (Dialog / Popup) so popups do not attempt
 * to sample coordinates from a parent window or stack nested glass layers.
 */
@Composable
public fun DisableGlassSceneBackdrop(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGlassBackdrops provides null,
        LocalInsideGlass provides false,
        content = content,
    )
}
