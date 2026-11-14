package io.github.ntvien.liquidglass.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ntvien.liquidglass.DropletState
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.droplet
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * One tab of [LiquidTabBar].
 *
 * @property key stable id passed back in `onSelect`.
 * @property label visible label (depending on [LiquidTabLabels]) and always the accessibility label.
 * @property icon 24 dp outline icon; it is tinted by the bar.
 * @property badge shows a small dot on the icon.
 */
@Immutable
public class LiquidTab(
    public val key: String,
    public val label: String,
    public val icon: ImageVector,
    public val badge: Boolean = false,
) {
    override fun equals(other: Any?): Boolean =
        other is LiquidTab && key == other.key && label == other.label && icon == other.icon && badge == other.badge

    override fun hashCode(): Int = listOf(key, label, icon, badge).hashCode()
}

/** How [LiquidTabBar] shows labels. */
public enum class LiquidTabLabels {
    /** Every tab shows icon above label; a soft accent droplet sits behind the selected tab. Good for 4–5 tabs. */
    Stacked,

    /**
     * The selected tab grows into a strong pill with icon and label side by side; other tabs show only the icon.
     * Good for 3–5 tabs with very clear icons and short labels.
     */
    SelectedOnly,
}

/**
 * Floating liquid-glass tab bar.
 *
 * - A pill of chrome glass floating above the system navigation bar.
 * - The selected tab is marked by a WATER DROPLET: tap a tab and the droplet rises into glass with a lens, slides over
 *   (stretching with velocity) and settles; drag sideways on the bar and it follows the finger, snapping to the
 *   nearest tab on release.
 * - Icon and label colors cross-fade with the droplet position: animating never recomposes.
 * - A light haptic tick on change; tapping the selected tab again calls [onReselect] (scroll to top).
 * - With system font scale ≥ 130 % only icons are shown (accessibility labels stay).
 * - Slides away while the keyboard is open ([hideWhenImeVisible]).
 *
 * Place it in the `overlay` of [io.github.ntvien.liquidglass.LiquidGlassScene], aligned to the bottom, and give scrolling
 * content [LiquidTabBarDefaults.contentBottomPadding] so the last item is not covered.
 *
 * @param tabs 2–5 tabs.
 * @param selectedKey key of the selected tab.
 * @param onSelect called with the key of a newly selected tab.
 * @param labels label layout, see [LiquidTabLabels].
 * @param onReselect called when the selected tab is tapped again.
 * @param hideWhenImeVisible slide the bar away while the soft keyboard is visible.
 * @param labelStyle label style for [LiquidTabLabels.Stacked].
 * @param selectedLabelStyle label style inside the selected pill for [LiquidTabLabels.SelectedOnly].
 * @param action optional round button to the right of the bar, e.g. a [LiquidSurface] with
 *   [GlassDefaults.chrome], [GlassDefaults.ContinuousPillShape] and size [LiquidTabBarDefaults.Height].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun LiquidTabBar(
    tabs: List<LiquidTab>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    labels: LiquidTabLabels = LiquidTabLabels.Stacked,
    onReselect: (String) -> Unit = {},
    hideWhenImeVisible: Boolean = true,
    labelStyle: TextStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold),
    selectedLabelStyle: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
    action: (@Composable () -> Unit)? = null,
) {
    val selectedIndex = tabs.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val currentTabs by rememberUpdatedState(tabs)
    val currentSelectedKey by rememberUpdatedState(selectedKey)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnReselect by rememberUpdatedState(onReselect)
    val haptic = LocalHapticFeedback.current
    val showLabels = LocalDensity.current.fontScale < LARGE_TEXT_SCALE

    val scope = rememberCoroutineScope()
    val droplet = remember(scope, tabs.size) {
        DropletState(scope, selectedIndex.toFloat(), 0f..(tabs.size - 1).coerceAtLeast(0).toFloat(), pressedScale = PRESS_SCALE)
    }
    // Selection changed from outside (tap, navigation, deep link): the droplet slides over.
    LaunchedEffect(selectedIndex) {
        if (droplet.targetValue.roundToInt() != selectedIndex) droplet.animateTo(selectedIndex.toFloat())
    }
    // Only reads state through rememberUpdatedState: the droplet's pointerInput keeps this lambda.
    val choose: (Int) -> Unit = { i ->
        currentTabs.getOrNull(i)?.let { tab ->
            if (tab.key == currentSelectedKey) {
                currentOnReselect(tab.key)
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                currentOnSelect(tab.key)
            }
        }
    }

    AnimatedVisibility(
        visible = !(hideWhenImeVisible && WindowInsets.isImeVisible),
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = BAR_INSET)
                .padding(bottom = BAR_BOTTOM),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).height(LiquidTabBarDefaults.Height)) {
                // The glass is its own layer: droplet and icon changes do not rebuild it.
                LiquidSurface(
                    modifier = Modifier.matchParentSize(),
                    style = GlassDefaults.chrome(),
                    shape = GlassDefaults.ContinuousPillShape,
                    contentPadding = PaddingValues(),
                ) {}
                when (labels) {
                    LiquidTabLabels.Stacked -> StackedTabs(tabs, selectedIndex, droplet, showLabels, labelStyle, choose)
                    LiquidTabLabels.SelectedOnly -> PillTabs(tabs, selectedIndex, droplet, showLabels, selectedLabelStyle, choose)
                }
            }
            action?.invoke()
        }
    }
}

/** Sizes of [LiquidTabBar], for laying out content around it. */
public object LiquidTabBarDefaults {
    /** Height of the bar. Use it for a round action button next to the bar too. */
    public val Height: Dp = 64.dp

    /**
     * Bottom padding for scrolling content behind the bar so the last item ends above it:
     * system navigation bar + bar + gaps.
     */
    @Composable
    public fun contentBottomPadding(): Dp =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Height + BAR_BOTTOM + 16.dp
}

// ---------------------------------------------------------------- Stacked

@Composable
private fun BoxScope.StackedTabs(
    tabs: List<LiquidTab>,
    selectedIndex: Int,
    droplet: DropletState,
    showLabels: Boolean,
    labelStyle: TextStyle,
    choose: (Int) -> Unit,
) {
    val density = LocalDensity.current
    // Width of one tab slot in px; an array so the droplet's drag lambda reads the latest value.
    val slot = remember { FloatArray(1) }
    var slotPx by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .matchParentSize()
            .padding(horizontal = 6.dp)
            .onSizeChanged {
                slot[0] = it.width.toFloat() / tabs.size.coerceAtLeast(1)
                slotPx = slot[0]
            }
            .then(droplet.dragModifier(slotWidthPx = { slot[0] }, onSettle = choose)),
        contentAlignment = Alignment.CenterStart,
    ) {
        // The droplet sits under the icons so icons stay crisp.
        Box(
            Modifier
                .offset { IntOffset((droplet.value * slot[0]).roundToInt(), 0) }
                .width(with(density) { slotPx.toDp() })
                .height(ITEM_HEIGHT)
                .droplet(droplet, GlassDefaults.ContinuousPillShape, idleColor = LiquidGlassTheme.colors.tabSelectedFill, blur = GlassDefaults.chrome().blur),
        )
        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                val colors = LiquidGlassTheme.colors
                val highlight = { (1f - abs(droplet.value - index)).coerceIn(0f, 1f) }
                Box(
                    Modifier
                        .weight(1f)
                        .height(ITEM_HEIGHT)
                        .clickable(interactionSource = null, indication = null, role = Role.Tab) { choose(index) }
                        .clearAndSetSemantics {
                            contentDescription = tab.label
                            role = Role.Tab
                            selected = index == selectedIndex
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    StackedFace(tab, colors.contentMuted, colors.badge, showLabels, labelStyle, Modifier.graphicsLayer { alpha = 1f - highlight(); compositingStrategy = CompositingStrategy.ModulateAlpha })
                    StackedFace(tab, colors.tabSelectedContent, colors.badge, showLabels, labelStyle, Modifier.graphicsLayer { alpha = highlight(); compositingStrategy = CompositingStrategy.ModulateAlpha })
                }
            }
        }
    }
}

@Composable
private fun StackedFace(tab: LiquidTab, tint: Color, badge: Color, showLabel: Boolean, labelStyle: TextStyle, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box {
            GlassIcon(tab.icon, tint)
            if (tab.badge) Badge(badge, Modifier.align(Alignment.TopEnd))
        }
        if (showLabel) BasicText(tab.label, style = labelStyle.copy(color = tint), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- SelectedOnly

@Composable
private fun BoxScope.PillTabs(
    tabs: List<LiquidTab>,
    selectedIndex: Int,
    droplet: DropletState,
    showLabels: Boolean,
    selectedLabelStyle: TextStyle,
    choose: (Int) -> Unit,
) {
    // How "open" tab i is from the droplet position: 1 on the tab, 0 one tab away or more.
    val highlight: (Int) -> Float = { i -> (1f - abs(droplet.value - i)).coerceIn(0f, 1f) }
    val expansion: (Int) -> Float = { i -> if (showLabels) highlight(i) else 0f }
    val slot = remember { FloatArray(1) }
    // Read in layout: relayout only when the glass appears or disappears, not on every frame of the press spring.
    val lifted by remember(droplet) { derivedStateOf { droplet.lift > 0f } }

    Layout(
        modifier = Modifier
            .matchParentSize()
            .padding(horizontal = 8.dp)
            .then(droplet.dragModifier(slotWidthPx = { slot[0] }, onSettle = choose)),
        content = {
            Box(Modifier.droplet(droplet, GlassDefaults.ContinuousPillShape, idleColor = LiquidGlassTheme.colors.selectedFill))
            tabs.forEachIndexed { index, tab ->
                PillTab(
                    tab = tab,
                    selected = index == selectedIndex,
                    expansion = { expansion(index) },
                    highlight = { highlight(index) },
                    selectedLabelStyle = selectedLabelStyle,
                    onClick = { choose(index) },
                )
            }
        },
    ) { measurables, constraints ->
        // Tab widths follow the droplet position and are computed during layout: no recomposition.
        val itemH = ITEM_HEIGHT.roundToPx()
        val n = tabs.size
        val tabMs = measurables.subList(1, 1 + n)
        val avail = constraints.maxWidth
        val collapsed = itemH
        val endPad = 16.dp.roundToPx()
        val extras = List(n) { i ->
            val full = (tabMs[i].maxIntrinsicWidth(itemH) + endPad).coerceAtLeast(collapsed)
            (full - collapsed) * expansion(i)
        }
        // Narrow screens: shrink the expansion proportionally (the label is clipped by the tab).
        val room = (avail - n * collapsed).coerceAtLeast(0)
        val shrink = extras.sum().let { if (it > room && it > 0f) room / it else 1f }
        val widths = List(n) { i -> collapsed + (extras[i] * shrink).roundToInt() }
        slot[0] = avail.toFloat() / n.coerceAtLeast(1)

        val gap = if (n > 1) (avail - widths.sum()).coerceAtLeast(0).toFloat() / (n - 1) else 0f
        val xs = FloatArray(n)
        var x = 0f
        for (i in 0 until n) {
            xs[i] = x
            x += widths[i] + gap
        }
        // Droplet: interpolate position and width between neighbouring tabs by the fraction of droplet.value.
        val last = (n - 1).coerceAtLeast(0)
        val v = droplet.value.coerceIn(0f, last.toFloat())
        val i0 = floor(v).toInt().coerceIn(0, last)
        val i1 = (i0 + 1).coerceAtMost(last)
        val f = v - i0
        val dropX = xs[i0] + (xs[i1] - xs[i0]) * f
        val dropW = (widths[i0] + (widths[i1] - widths[i0]) * f).roundToInt().coerceAtLeast(1)
        // A new droplet size rebuilds its glass (lens, blur, shadow, rim). While the glass moves between tabs, measure
        // it at the next width of a ladder and squeeze the rest with scaleX: a few rebuilds per tab change instead of
        // one every frame. On a tab, and at rest (an opaque pill, no glass), it gets its exact width.
        val measuredW = if (lifted && f > 0f) ladderWidth(dropW, collapsed) else dropW
        val squeeze = dropW.toFloat() / measuredW
        val dropletP = measurables[0].measure(Constraints.fixed(measuredW, itemH))
        val tabPs = tabMs.mapIndexed { i, m -> m.measure(Constraints.fixed(widths[i], itemH)) }

        layout(avail, constraints.maxHeight) {
            val y = (constraints.maxHeight - itemH) / 2
            dropletP.placeWithLayer(dropX.roundToInt(), y) {
                scaleX = squeeze
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            tabPs.forEachIndexed { i, p -> p.place(xs[i].roundToInt(), y) }
        }
    }
}

/** Smallest width of the ladder [min] × [DROPLET_WIDTH_STEP]^k that is at least [width]. */
private fun ladderWidth(width: Int, min: Int): Int {
    if (width <= min || min <= 0) return width
    val k = ceil(ln(width.toFloat() / min) / ln(DROPLET_WIDTH_STEP))
    return ceil(min * DROPLET_WIDTH_STEP.pow(k)).toInt().coerceAtLeast(width)
}

@Composable
private fun PillTab(
    tab: LiquidTab,
    selected: Boolean,
    expansion: () -> Float,
    highlight: () -> Float,
    selectedLabelStyle: TextStyle,
    onClick: () -> Unit,
) {
    val colors = LiquidGlassTheme.colors
    Box(
        modifier = Modifier
            .clipToBounds()
            .clickable(interactionSource = null, indication = null, role = Role.Tab, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = tab.label
                role = Role.Tab
                this.selected = selected
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .padding(start = (ITEM_HEIGHT - ICON_SIZE) / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                GlassIcon(tab.icon, colors.contentMuted, Modifier.graphicsLayer { alpha = 1f - highlight(); compositingStrategy = CompositingStrategy.ModulateAlpha })
                GlassIcon(tab.icon, colors.selectedContent, Modifier.graphicsLayer { alpha = highlight(); compositingStrategy = CompositingStrategy.ModulateAlpha })
                if (tab.badge) Badge(colors.badge, Modifier.align(Alignment.TopEnd))
            }
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = tab.label,
                style = selectedLabelStyle.copy(color = colors.selectedContent),
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = expansion(); compositingStrategy = CompositingStrategy.ModulateAlpha },
            )
        }
    }
}

@Composable
private fun Badge(color: Color, modifier: Modifier) {
    Box(modifier.size(8.dp).background(color, GlassDefaults.PillShape))
}

private val BAR_INSET = 16.dp
private val BAR_BOTTOM = 12.dp
private val ITEM_HEIGHT = 52.dp
private val ICON_SIZE = 24.dp

/** System font scale from which the bar shows icons only. */
private const val LARGE_TEXT_SCALE = 1.3f

/** Droplet swell on press (Kyant's icon tab uses ≈ 1.39; tabs with labels are wider, so less). */
private const val PRESS_SCALE = 1.12f

/** Width ratio between sizes the lifted SelectedOnly droplet is measured at: it is squeezed by at most 6 %. */
private const val DROPLET_WIDTH_STEP = 1.06f
