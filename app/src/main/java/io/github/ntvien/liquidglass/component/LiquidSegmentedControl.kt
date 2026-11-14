package io.github.ntvien.liquidglass.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ntvien.liquidglass.DropletState
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.droplet
import io.github.ntvien.liquidglass.liquidGlass
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * In-screen tabs for 2–4 short options ("Day · Week · Month").
 *
 * A clear frosted track with the selected option raised as a light glass pill (iOS 26 style). The pill is the same
 * water droplet as [LiquidTabBar]: tap and it lifts into glass with a lens, slides over with a small bounce and
 * settles; drag sideways and it follows the finger. Labels cross-fade by droplet position, the selected label is
 * bolder, a light haptic tick confirms the change. Accessibility services see a group of selectable tabs.
 *
 * Other looks through the colors, e.g. a strong accent pill:
 * `indicatorColor = LiquidGlassTheme.colors.accent, selectedTextColor = LiquidGlassTheme.colors.onAccent`.
 *
 * For more than 4 options or long labels use a horizontally scrolling row of chips instead.
 *
 * @param options visible labels, also read by accessibility services.
 * @param selectedIndex index of the selected option.
 * @param onSelect called with the new index (not called when the selected option is tapped again).
 * @param indicatorColor fill of the selected pill at rest (default [io.github.ntvien.liquidglass.GlassColors.raisedFill]).
 * @param selectedTextColor label color of the selected option.
 * @param textColor label color of the other options.
 * @param textStyle label style; the selected label is drawn one weight bolder.
 */
@Composable
public fun LiquidSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    indicatorColor: Color = LiquidGlassTheme.colors.raisedFill,
    selectedTextColor: Color = LiquidGlassTheme.colors.content,
    textColor: Color = LiquidGlassTheme.colors.contentMuted,
    textStyle: TextStyle = TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
) {
    val colors = LiquidGlassTheme.colors
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val scope = rememberCoroutineScope()
    val droplet = remember(scope, options.size) {
        DropletState(scope, selectedIndex.toFloat(), 0f..(options.size - 1).coerceAtLeast(0).toFloat(), pressedScale = PRESS_SCALE)
    }
    LaunchedEffect(selectedIndex) {
        if (droplet.targetValue.roundToInt() != selectedIndex) droplet.animateTo(selectedIndex.toFloat())
    }
    // Only reads state through rememberUpdatedState: the droplet's pointerInput keeps this lambda.
    val choose: (Int) -> Unit = { i ->
        if (i != currentSelected) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            currentOnSelect(i)
        }
    }
    val slot = remember { FloatArray(1) }
    var slotPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier
            .fillMaxWidth()
            .height(SEGMENTED_HEIGHT)
            // Clear track: lighter than the frost preset so the raised pill reads as the only solid thing.
            .liquidGlass(GlassDefaults.frost().copy(fallbackSurface = colors.glassFill, fallbackSheen = 0.25f), GlassDefaults.PillShape)
            .padding(3.dp)
            .onSizeChanged {
                slot[0] = it.width.toFloat() / options.size.coerceAtLeast(1)
                slotPx = slot[0]
            }
            .then(droplet.dragModifier(slotWidthPx = { slot[0] }, onSettle = choose))
            .selectableGroup(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset { IntOffset((droplet.value * slot[0]).roundToInt(), 0) }
                .width(with(density) { slotPx.toDp() })
                .fillMaxHeight()
                // Soft lift under an opaque pill; a translucent pill (dark theme) would show its own shadow through.
                .then(
                    if (indicatorColor.alpha >= 0.9f) {
                        Modifier.shadow(2.dp, GlassDefaults.PillShape, clip = false, ambientColor = colors.glassShadow, spotColor = colors.glassShadow)
                    } else {
                        Modifier
                    }
                )
                .droplet(droplet, GlassDefaults.PillShape, idleColor = indicatorColor),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                val highlight = { (1f - abs(droplet.value - index)).coerceIn(0f, 1f) }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = index == selectedIndex,
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = { choose(index) },
                        )
                        // The name lives on the segment itself: Compose drops a text layer drawn at alpha 0 from the
                        // accessibility tree, so the selected segment would lose its name if it relied on its children.
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    // Two label layers cross-fade with the droplet. Drawing only: accessibility reads the segment.
                    BasicText(
                        label,
                        style = textStyle.copy(color = textColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer { alpha = 1f - highlight(); compositingStrategy = CompositingStrategy.ModulateAlpha }.clearAndSetSemantics {},
                    )
                    BasicText(
                        label,
                        style = textStyle.copy(color = selectedTextColor, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer { alpha = highlight(); compositingStrategy = CompositingStrategy.ModulateAlpha }.clearAndSetSemantics {},
                    )
                }
            }
        }
    }
}

private val SEGMENTED_HEIGHT = 40.dp
private const val PRESS_SCALE = 1.06f
