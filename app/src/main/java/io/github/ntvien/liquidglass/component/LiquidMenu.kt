package io.github.ntvien.liquidglass.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.GlassStyle
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.LocalGlassBackdrops
import io.github.ntvien.liquidglass.LocalGlassSceneTop
import io.github.ntvien.liquidglass.liquidGlass
import io.github.ntvien.liquidglass.rememberGlassInteraction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/** One entry of [LiquidMenuButton]: a [LiquidMenuItem] or a [LiquidMenuDivider]. */
public sealed interface LiquidMenuEntry

/**
 * A menu action.
 *
 * @property label visible text, also read by accessibility services.
 * @property icon optional 24 dp outline icon, drawn at 20 dp before the label.
 * @property destructive draws the item in the badge color (delete, remove).
 * @property enabled disabled items are dimmed and cannot be chosen.
 * @property onClick called after the menu starts closing.
 */
@Stable
public class LiquidMenuItem(
    public val label: String,
    public val icon: ImageVector? = null,
    public val destructive: Boolean = false,
    public val enabled: Boolean = true,
    public val leadingContent: (@Composable () -> Unit)? = null,
    public val onClick: () -> Unit,
) : LiquidMenuEntry {
    public constructor(
        label: String,
        icon: @Composable () -> Unit,
        isDestructive: Boolean = false,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) : this(
        label = label,
        icon = null,
        destructive = isDestructive,
        enabled = enabled,
        leadingContent = icon,
        onClick = onClick,
    )

    public constructor(
        label: String,
        isDestructive: Boolean,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) : this(
        label = label,
        icon = null,
        destructive = isDestructive,
        enabled = enabled,
        leadingContent = null,
        onClick = onClick,
    )
}

/** A thin line between groups of [LiquidMenuItem]s. */
public object LiquidMenuDivider : LiquidMenuEntry

@Composable
public fun LiquidMenuButton(
    items: List<LiquidMenuEntry>,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassDefaults.control(),
    shape: Shape = GlassDefaults.PillShape,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    menuWidth: Dp = 250.dp,
    contentDescription: String = "Menu",
    buttonContent: @Composable RowScope.() -> Unit,
) {
    LiquidMenuButton(
        items = items,
        contentDescription = contentDescription,
        modifier = modifier,
        style = style,
        shape = shape,
        menuWidth = menuWidth,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = buttonContent,
        )
    }
}

@Composable
public fun LiquidMenuButton(
    items: List<LiquidMenuEntry>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassDefaults.control(),
    shape: Shape = GlassDefaults.PillShape,
    menuWidth: Dp = 250.dp,
    textStyle: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 21.sp),
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val state = remember(scope) { LiquidMenuState(scope) }
    SideEffect {
        state.items = items
        state.haptic = { haptic.performHapticFeedback(it) }
    }
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val interaction = rememberGlassInteraction()

    Box(
        modifier
            .onGloballyPositioned { state.anchorInWindow = it.boundsInWindow() }
            .liquidGlass(style, shape, interaction)
            // Press opens the menu at once; dragging onto an item and releasing chooses it.
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (state.isOpen && !state.closing) return@awaitEachGesture
                    state.open()
                    var dragged = false
                    var last = down.position
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        last = change.position
                        if (!change.pressed) break
                        if ((change.position - down.position).getDistance() > touchSlop) dragged = true
                        if (dragged) state.hover(state.anchorInWindow.topLeft + change.position)
                    }
                    if (dragged) state.release(state.anchorInWindow.topLeft + last)
                }
            }
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                onClick {
                    state.open()
                    true
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )

    if (state.isOpen) {
        val top = LocalGlassSceneTop.current
        val currentWidth by rememberUpdatedState(menuWidth)
        val currentTextStyle by rememberUpdatedState(textStyle)
        val currentTitle by rememberUpdatedState(contentDescription)
        if (top != null) {
            val key = remember { Any() }
            DisposableEffect(top, key) {
                top.show(key) { MenuOverlay(state, currentWidth, currentTextStyle, currentTitle) }
                onDispose { top.remove(key) }
            }
        } else {
            Popup(
                popupPositionProvider = WindowOrigin,
                onDismissRequest = { state.close() },
                properties = PopupProperties(focusable = true),
            ) {
                // A popup is a separate window: it cannot sample the scene, so draw the frosted look.
                CompositionLocalProvider(LocalGlassBackdrops provides null) {
                    MenuOverlay(state, menuWidth, textStyle, contentDescription)
                }
            }
        }
    }
}

/** Full-screen layer that catches touches, places the menu and runs the morph. Coordinates are window pixels. */
@Composable
private fun MenuOverlay(state: LiquidMenuState, menuWidth: Dp, textStyle: TextStyle, title: String) {
    val colors = LiquidGlassTheme.colors
    val density = LocalDensity.current
    val insets = WindowInsets.safeDrawing
    val insetTop = insets.getTop(density).toFloat()
    val insetBottom = insets.getBottom(density).toFloat()
    val menuStyle = GlassDefaults.chrome().copy(
        chromaticAberration = false,
        surface = colors.glassFillOpaque.copy(alpha = colors.glassFillOpaque.alpha * MENU_TINT),
        shadowRadius = 24.dp,
    )
    BackHandler(enabled = !state.closing) { state.close() }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { state.overlayOrigin = it.positionInWindow() }
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val start = state.overlayOrigin + down.position
                    if (!state.menuContains(start)) {
                        state.close()
                        return@awaitEachGesture
                    }
                    state.hover(start)
                    var last = start
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        last = state.overlayOrigin + change.position
                        if (!change.pressed) break
                        state.hover(last)
                    }
                    state.release(last)
                }
            }
            .semantics { paneTitle = title },
    ) {
        Layout(
            content = {
                Box(
                    Modifier
                        .graphicsLayer { alpha = (state.progress.value * FADE_IN_SPEED).coerceIn(0f, 1f) }
                        .liquidGlass(menuStyle, MENU_SHAPE)
                        .clip(MENU_SHAPE),
                ) {
                    // Items are laid out at the final menu size and pinned to the corner nearest the button, so the
                    // growing glass reveals them from there.
                    Layout(content = { MenuItems(state, textStyle) }) { measurables, constraints ->
                        val g = state.geometry
                        val fw = g?.menu?.width?.roundToInt()?.coerceAtLeast(1) ?: constraints.maxWidth
                        val fh = g?.menu?.height?.roundToInt()?.coerceAtLeast(1) ?: constraints.maxHeight
                        val placeable = measurables[0].measure(Constraints.fixed(fw, fh))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            val x = if (g?.alignRight == true) constraints.maxWidth - fw else 0
                            val y = if (g?.below == false) constraints.maxHeight - fh else 0
                            placeable.place(x, y)
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val container = IntSize(constraints.maxWidth, constraints.maxHeight)
            val anchor = state.anchorInWindow.translate(-state.overlayOrigin)
            val g = menuGeometry(
                anchor = anchor,
                container = container,
                menuWidth = menuWidth.toPx(),
                menuHeight = menuHeight(state.items).toPx(),
                gap = MENU_GAP.toPx(),
                margin = MENU_MARGIN.toPx(),
                insetTop = insetTop,
                insetBottom = insetBottom,
                itemsTop = MENU_PADDING.toPx(),
                itemHeight = ITEM_HEIGHT.toPx(),
                dividerHeight = DIVIDER_HEIGHT.toPx(),
                entries = state.items,
            )
            state.geometry = g
            // The morph: from the button's frame (p = 0) to the menu's frame (p = 1); the spring overshoots past 1.
            val r = lerpRect(g.anchor, g.menu, state.progress.value)
            val placeable = measurables[0].measure(
                Constraints.fixed(max(1, r.width.roundToInt()), max(1, r.height.roundToInt())),
            )
            layout(container.width, container.height) { placeable.place(r.left.roundToInt(), r.top.roundToInt()) }
        }
    }
}

@Composable
private fun MenuItems(state: LiquidMenuState, textStyle: TextStyle) {
    val colors = LiquidGlassTheme.colors
    val highlightColor = colors.content.copy(alpha = HIGHLIGHT_ALPHA)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = MENU_PADDING)) {
        state.items.forEachIndexed { index, entry ->
            // Items appear one after another while the menu opens (and leave in reverse while it closes).
            val stagger = Modifier.graphicsLayer {
                val t = ((state.progress.value - STAGGER_START - index * STAGGER_STEP) / STAGGER_LENGTH).coerceIn(0f, 1f)
                alpha = t
                translationY = (1f - t) * -ITEM_SLIDE.toPx()
            }
            when (entry) {
                is LiquidMenuItem -> {
                    val color = when {
                        !entry.enabled -> colors.contentMuted.copy(alpha = DISABLED_ALPHA)
                        entry.destructive -> colors.badge
                        else -> colors.content
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(ITEM_HEIGHT)
                            .then(stagger)
                            .drawBehind { if (state.highlighted == index) drawRect(highlightColor) }
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                if (entry.enabled) {
                                    onClick(label = entry.label) {
                                        state.select(index)
                                        true
                                    }
                                } else {
                                    disabled()
                                }
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (entry.icon != null) {
                            GlassIcon(entry.icon, color, size = 20.dp)
                        } else if (entry.leadingContent != null) {
                            entry.leadingContent.invoke()
                        }
                        BasicText(
                            entry.label,
                            style = textStyle.copy(color = color),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                LiquidMenuDivider -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(DIVIDER_HEIGHT)
                        .then(stagger)
                        .padding(vertical = (DIVIDER_HEIGHT - 0.5.dp) / 2)
                        .background(colors.content.copy(alpha = DIVIDER_ALPHA)),
                )
            }
        }
    }
}

/** Open/close progress, positions and the finger-tracking logic shared by the button and the overlay. */
@Stable
internal class LiquidMenuState(private val scope: CoroutineScope) {
    var isOpen by mutableStateOf(false)
        private set
    var closing by mutableStateOf(false)
        private set
    val progress = Animatable(0f)
    var anchorInWindow by mutableStateOf(Rect.Zero)
    var overlayOrigin by mutableStateOf(Offset.Zero)
    var highlighted by mutableIntStateOf(-1)
        private set
    var items: List<LiquidMenuEntry> = emptyList()
    var haptic: (HapticFeedbackType) -> Unit = {}

    /** Written during layout of the overlay; read by touch handling. */
    var geometry: MenuGeometry? = null

    fun open() {
        if (isOpen && !closing) return
        isOpen = true
        closing = false
        highlighted = -1
        haptic(HapticFeedbackType.ContextClick)
        scope.launch { progress.animateTo(1f, spring(dampingRatio = OPEN_DAMPING, stiffness = OPEN_STIFFNESS)) }
    }

    fun close() {
        if (!isOpen || closing) return
        closing = true
        highlighted = -1
        scope.launch {
            progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = CLOSE_STIFFNESS))
            if (closing) {
                isOpen = false
                closing = false
            }
        }
    }

    fun menuContains(window: Offset): Boolean = geometry?.menu?.contains(window - overlayOrigin) == true

    fun hover(window: Offset) {
        if (closing) return
        val index = indexAt(window)
        if (index != highlighted) {
            highlighted = index
            if (index >= 0) haptic(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    fun release(window: Offset) {
        if (closing) return
        val index = indexAt(window)
        when {
            index >= 0 -> select(index)
            anchorInWindow.contains(window) || menuContains(window) -> highlighted = -1
            else -> close()
        }
    }

    fun select(index: Int) {
        val item = items.getOrNull(index) as? LiquidMenuItem ?: return
        if (!item.enabled || closing) return
        haptic(HapticFeedbackType.Confirm)
        close()
        item.onClick()
    }

    private fun indexAt(window: Offset): Int {
        val g = geometry ?: return -1
        val index = g.indexAt(window - overlayOrigin)
        return if ((items.getOrNull(index) as? LiquidMenuItem)?.enabled == true) index else -1
    }
}

/** Where the menu goes, in overlay pixels, plus the item rows for hit testing. */
internal class MenuGeometry(
    val anchor: Rect,
    val menu: Rect,
    val alignRight: Boolean,
    val below: Boolean,
    private val rowTops: FloatArray,
    private val rowBottoms: FloatArray,
) {
    fun indexAt(p: Offset): Int {
        if (!menu.contains(p)) return -1
        val y = p.y - menu.top
        for (i in rowTops.indices) if (y >= rowTops[i] && y < rowBottoms[i]) return i
        return -1
    }
}

private fun menuGeometry(
    anchor: Rect,
    container: IntSize,
    menuWidth: Float,
    menuHeight: Float,
    gap: Float,
    margin: Float,
    insetTop: Float,
    insetBottom: Float,
    itemsTop: Float,
    itemHeight: Float,
    dividerHeight: Float,
    entries: List<LiquidMenuEntry>,
): MenuGeometry {
    val width = menuWidth.coerceAtMost(container.width - 2 * margin).coerceAtLeast(1f)
    val alignRight = anchor.center.x > container.width / 2f
    val left = (if (alignRight) anchor.right - width else anchor.left)
        .coerceAtMost(container.width - margin - width)
        .coerceAtLeast(margin)
    val minTop = insetTop + margin
    val maxBottom = container.height - insetBottom - margin
    val cappedHeight = menuHeight.coerceAtMost((maxBottom - minTop).coerceAtLeast(1f))
    val spaceBelow = maxBottom - (anchor.bottom + gap)
    val spaceAbove = (anchor.top - gap) - minTop
    val below = spaceBelow >= cappedHeight || spaceBelow >= spaceAbove
    val top = (if (below) anchor.bottom + gap else anchor.top - gap - cappedHeight)
        .coerceAtMost(maxBottom - cappedHeight)
        .coerceAtLeast(minTop)
    val rowTops = FloatArray(entries.size)
    val rowBottoms = FloatArray(entries.size)
    var y = itemsTop
    entries.forEachIndexed { i, entry ->
        val h = if (entry is LiquidMenuItem) itemHeight else dividerHeight
        rowTops[i] = if (entry is LiquidMenuItem) y else -1f
        rowBottoms[i] = if (entry is LiquidMenuItem) y + h else -1f
        y += h
    }
    return MenuGeometry(anchor, Rect(left, top, left + width, top + cappedHeight), alignRight, below, rowTops, rowBottoms)
}

private fun menuHeight(entries: List<LiquidMenuEntry>): Dp =
    MENU_PADDING * 2 + entries.fold(0.dp) { acc, e -> acc + if (e is LiquidMenuItem) ITEM_HEIGHT else DIVIDER_HEIGHT }

private fun lerpRect(a: Rect, b: Rect, t: Float): Rect =
    Rect(lerp(a.left, b.left, t), lerp(a.top, b.top, t), lerp(a.right, b.right, t), lerp(a.bottom, b.bottom, t))

/** Places a popup at the window origin so the overlay and window coordinates match. */
private object WindowOrigin : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

private val MENU_SHAPE = RoundedCornerShape(22.dp)
private val MENU_GAP = 8.dp
private val MENU_MARGIN = 12.dp
private val MENU_PADDING = 6.dp
private val ITEM_HEIGHT = 44.dp
private val DIVIDER_HEIGHT = 9.dp
private val ITEM_SLIDE = 6.dp

/** Tint of the menu glass: readable over busy content, still glass. */
private const val MENU_TINT = 0.82f
private const val HIGHLIGHT_ALPHA = 0.08f
private const val DIVIDER_ALPHA = 0.12f
private const val DISABLED_ALPHA = 0.5f

/** Bouncy open (≈ 8 % overshoot), calm close. */
private const val OPEN_DAMPING = 0.62f
private const val OPEN_STIFFNESS = 360f
private const val CLOSE_STIFFNESS = 520f

/** The glass fades in over the first quarter of the morph. */
private const val FADE_IN_SPEED = 4f
private const val STAGGER_START = 0.2f
private const val STAGGER_STEP = 0.06f
private const val STAGGER_LENGTH = 0.45f
