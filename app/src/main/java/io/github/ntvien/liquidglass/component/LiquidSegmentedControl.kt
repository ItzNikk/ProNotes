package io.github.ntvien.liquidglass.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
 * In-screen segmented control with Kyant0 droplet indicator and immediate BoxWithConstraints layout.
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
    val choose: (Int) -> Unit = { i ->
        if (i != currentSelected) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            currentOnSelect(i)
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(SEGMENTED_HEIGHT)
            .liquidGlass(GlassDefaults.frost().copy(fallbackSurface = colors.glassFill, fallbackSheen = 0.25f), GlassDefaults.PillShape)
            .padding(3.dp)
            .selectableGroup(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val count = options.size.coerceAtLeast(1)
        val slotPx = constraints.maxWidth.toFloat() / count
        val slotDp = with(density) { slotPx.toDp() }

        Box(
            Modifier
                .fillMaxSize()
                .then(droplet.dragModifier(slotWidthPx = { slotPx }, onSettle = choose)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .offset { IntOffset((droplet.value * slotPx).roundToInt(), 0) }
                    .width(slotDp)
                    .fillMaxHeight()
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
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
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
}

private val SEGMENTED_HEIGHT = 40.dp
private const val PRESS_SCALE = 1.06f
