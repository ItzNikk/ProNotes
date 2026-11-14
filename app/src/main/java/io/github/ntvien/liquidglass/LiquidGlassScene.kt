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
import io.github.ntvien.liquidglass.internal.backdrop.backdrops.LayerBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.backdrops.layerBackdrop
import io.github.ntvien.liquidglass.internal.backdrop.backdrops.rememberLayerBackdrop

/**
 * Where a glass element samples from:
 * - Content: glass inside the content (cards, buttons in a list) samples the BACKGROUND. Cheap, stable while scrolling.
 * - Overlay: glass floating over the content (tab bar, header) samples background + CONTENT when the device allows it.
 */
internal enum class GlassLayer { Content, Overlay }

@Stable
internal class GlassBackdrops(val background: LayerBackdrop, val overlay: LayerBackdrop)

internal val LocalGlassBackdrops = staticCompositionLocalOf<GlassBackdrops?> { null }
internal val LocalGlassLayer = staticCompositionLocalOf { GlassLayer.Content }

/**
 * Top layer of the scene, above the overlay (tab bar). Menus are called from anywhere in the content but draw here, so
 * they cover everything and still sample the scene like other floating glass.
 */
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
 * Frame of every screen that has glass. Use exactly ONE per screen.
 *
 * ```
 * LiquidGlassScene(
 *     background = { LiquidGradientBackground() }, // or your own: draw it once (e.g. into a bitmap)
 *     overlay = { LiquidTabBar(..., Modifier.align(Alignment.BottomCenter)) },
 * ) {
 *     LazyColumn { item { LiquidSurface { ... } } }
 * }
 * ```
 *
 * @param background what the glass refracts, e.g. [io.github.ntvien.liquidglass.component.LiquidGradientBackground].
 *   Keep it smooth (a gradient, a blurred photo): then the glass needs no blur.
 * @param overlay floating glass (tab bar, header). On capable devices it also refracts the scrolling [content].
 * @param content the screen content. Glass inside it samples only [background].
 */
@Composable
public fun LiquidGlassScene(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val overlaySamplesContent = LocalGlassOverlaySamplesContent.current
    val backgroundBackdrop = rememberLayerBackdrop()
    val overlayBackdrop = rememberLayerBackdrop()
    val backdrops = remember(backgroundBackdrop, overlayBackdrop, overlaySamplesContent) {
        GlassBackdrops(backgroundBackdrop, if (overlaySamplesContent) overlayBackdrop else backgroundBackdrop)
    }
    val top = remember { GlassSceneTop() }
    CompositionLocalProvider(LocalGlassBackdrops provides backdrops, LocalGlassSceneTop provides top) {
        Box(modifier.fillMaxSize()) {
            // Recorded as the overlay backdrop: background + content (capable devices only).
            Box(if (overlaySamplesContent) Modifier.fillMaxSize().layerBackdrop(overlayBackdrop) else Modifier.fillMaxSize()) {
                // Background and content get their own layers: the overlay backdrop only records two references
                // instead of re-recording the whole screen whenever something in the content changes.
                Box(Modifier.fillMaxSize().graphicsLayer().layerBackdrop(backgroundBackdrop), content = background)
                CompositionLocalProvider(LocalGlassLayer provides GlassLayer.Content) {
                    Box(Modifier.fillMaxSize().graphicsLayer(), content = content)
                }
            }
            CompositionLocalProvider(LocalGlassLayer provides GlassLayer.Overlay) {
                Box(Modifier.fillMaxSize(), content = overlay)
                Box(Modifier.fillMaxSize()) { top.entries.forEach { (k, c) -> key(k) { c() } } }
            }
        }
    }
}
