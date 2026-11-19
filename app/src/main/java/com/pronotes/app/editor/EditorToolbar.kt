package com.pronotes.app.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pronotes.app.R
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.EraserMode
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.data.ToolbarStyle
import com.pronotes.app.ui.AppContextMenu
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.LocalMotionStyle
import com.pronotes.app.ui.LocalReduceEffects
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.ToolGlyph
import com.pronotes.app.ui.appPillShape
import kotlin.math.abs

private val DockDarkBg = Color(0xFF171A22)
private val DockLightBg = Color(0xFFFCFBF7)

@Composable
fun toolDisplayName(t: Tool): String = when (t) {
    Tool.PEN -> stringResource(R.string.tool_pen)
    Tool.HIGHLIGHTER -> stringResource(R.string.tool_highlighter)
    Tool.ERASER -> stringResource(R.string.tool_eraser)
    Tool.SHAPE -> stringResource(R.string.tool_shapes)
    Tool.LASSO -> stringResource(R.string.tool_selection)
    Tool.MINDMAP -> stringResource(R.string.tool_concept_maps)
    Tool.IMAGE -> stringResource(R.string.tool_image)
    Tool.TEXT -> stringResource(R.string.tool_text)
    Tool.STICKY -> stringResource(R.string.tool_sticky)
    Tool.WEBLINK -> stringResource(R.string.tool_weblink)
    Tool.TAPE -> stringResource(R.string.tool_tape)
    Tool.STICKER -> stringResource(R.string.tool_sticker)
    Tool.LASER -> stringResource(R.string.tool_laser)
}

fun shortToolLabel(t: Tool): String = when (t) {
    Tool.PEN -> "Pen"
    Tool.HIGHLIGHTER -> "Highlight"
    Tool.ERASER -> "Eraser"
    Tool.SHAPE -> "Shapes"
    Tool.LASSO -> "Selection"
    Tool.MINDMAP -> "Concept"
    Tool.IMAGE -> "Image"
    Tool.TEXT -> "Text"
    Tool.STICKY -> "Sticky"
    Tool.WEBLINK -> "Link"
    Tool.TAPE -> "Tape"
    Tool.STICKER -> "Sticker"
    Tool.LASER -> "Laser"
}

/**
 * Full-width or full-height studio toolbar rendered outside the canvas viewport so active tool
 * presets, colour swatches, and controls remain separated into crisp tonal capsules without
 * cluttering or covering the page content.
 */
@Composable
fun AdaptiveFixedToolbar(
    vm: EditorViewModel,
    settings: AppSettings,
    darkUi: Boolean,
    activePopoverTool: Tool?,
    onTogglePopover: (Tool) -> Unit,
    onSelectDock: (ToolbarDock) -> Unit,
    onSelectStyle: (ToolbarStyle) -> Unit,
    onResetPosition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVertical = settings.toolbarDock == ToolbarDock.LEFT || settings.toolbarDock == ToolbarDock.RIGHT
    val barBg = if (darkUi) DockDarkBg else DockLightBg
    val barFg = if (darkUi) Color(0xFFF3F4F6) else Color(0xFF181A1F)
    val trayBg = if (darkUi) Color.White.copy(alpha = 0.055f) else Color(0xFF0F172A).copy(alpha = 0.042f)
    val trayBorder = if (darkUi) Color.White.copy(alpha = 0.10f) else Color(0xFF0F172A).copy(alpha = 0.08f)
    val dividerColor = barFg.copy(alpha = 0.12f)
    val reduceEffects = LocalReduceEffects.current
    val context = LocalContext.current
    val store = remember(context) { SettingsStore.get(context) }
    val density = LocalDensity.current.density
    val visibleTools = remember(settings.toolbarTools) { ToolbarOrder.visible(settings.toolbarTools) }

    AppSurface(
        role = SurfaceRole.Chrome,
        containerColor = barBg,
        contentColor = barFg,
        shape = RoundedCornerShape(0.dp),
        shadowElevation = if (reduceEffects) 0.dp else 4.dp,
        borderColor = dividerColor,
        contentPadding = PaddingValues(
            horizontal = if (isVertical) 6.dp else 12.dp,
            vertical = if (isVertical) 8.dp else 5.dp,
        ),
        modifier = modifier
            .then(if (isVertical) Modifier.fillMaxHeight().width(70.dp) else Modifier.fillMaxWidth()),
    ) {
        BoxWithConstraints(
            modifier = if (isVertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth(),
        ) {
            val availablePx = with(LocalDensity.current) {
                if (isVertical) maxHeight.toPx().toInt() else maxWidth.toPx().toInt()
            }
            val slotPx = (50f * density).toInt()
            val reservedPx = if (isVertical) (196f * density).toInt() else (390f * density).toInt()
            val layout = remember(settings.toolbarDock, settings.toolbarStyle, visibleTools, vm.tool, availablePx, slotPx, reservedPx) {
                ToolbarGeometry.computeAdaptiveLayout(
                    dockEdge = settings.toolbarDock,
                    style = ToolbarStyle.FIXED_BAR,
                    visibleTools = visibleTools,
                    activeTool = vm.tool,
                    availablePx = availablePx,
                    slotExtentPx = slotPx,
                    reservedPx = reservedPx,
                )
            }

            val reorderTool = { toolToMove: Tool, step: Int ->
                val idx = visibleTools.indexOf(toolToMove)
                val targetIdx = (idx + step).coerceIn(0, visibleTools.lastIndex)
                if (idx >= 0 && targetIdx != idx) {
                    store.update {
                        it.copy(toolbarTools = ToolbarOrder.moveVisible(it.toolbarTools, toolToMove, visibleTools[targetIdx]))
                    }
                }
            }

            if (layout.orientation == ToolbarOrientation.VERTICAL) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Cluster 1: Layout switcher & Undo/Redo capsule
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(trayBg)
                            .border(1.dp, trayBorder, RoundedCornerShape(16.dp))
                            .padding(vertical = 3.dp, horizontal = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        ToolbarLayoutMenuButton(
                            settings = settings,
                            fgColor = barFg,
                            onSelectDock = onSelectDock,
                            onSelectStyle = onSelectStyle,
                            onResetPosition = onResetPosition,
                        )
                        ToolbarUndoRedoCluster(vm = vm, vertical = true, fgColor = barFg)
                    }

                    // Cluster 2: Tool deck tray
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(trayBg)
                            .border(1.dp, trayBorder, RoundedCornerShape(18.dp))
                            .padding(vertical = 4.dp, horizontal = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        layout.pinnedTools.forEach { t ->
                            ToolButton(
                                vm = vm,
                                t = t,
                                settings = settings,
                                darkUi = darkUi,
                                barFg = barFg,
                                popoverOpen = activePopoverTool == t,
                                verticalBar = true,
                                onOpenPopover = { onTogglePopover(t) },
                                onReorderStep = reorderTool,
                            )
                        }
                        if (layout.overflowTools.isNotEmpty()) {
                            OverflowToolsButton(
                                tools = layout.overflowTools,
                                vm = vm,
                                fgColor = barFg,
                                onPickTool = { picked ->
                                    if (vm.tool == picked) onTogglePopover(picked) else vm.tool = picked
                                },
                            )
                        }
                    }

                    // Cluster 3: Active tool preset capsule
                    if (toolHasInlineOptions(vm.tool)) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(trayBg)
                                .border(1.dp, trayBorder, RoundedCornerShape(16.dp))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ToolOptions(
                                vm = vm,
                                vertical = true,
                                settings = settings,
                                dockFg = barFg,
                                darkDock = darkUi,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Cluster 1: History capsule
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(trayBg)
                            .border(1.dp, trayBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolbarUndoRedoCluster(vm = vm, vertical = false, fgColor = barFg)
                    }

                    // Cluster 2: Centered tool tray + Preset capsule
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(trayBg)
                                .border(1.dp, trayBorder, RoundedCornerShape(18.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            layout.pinnedTools.forEach { t ->
                                ToolButton(
                                    vm = vm,
                                    t = t,
                                    settings = settings,
                                    darkUi = darkUi,
                                    barFg = barFg,
                                    popoverOpen = activePopoverTool == t,
                                    verticalBar = false,
                                    onOpenPopover = { onTogglePopover(t) },
                                    onReorderStep = reorderTool,
                                )
                            }
                            if (layout.overflowTools.isNotEmpty()) {
                                OverflowToolsButton(
                                    tools = layout.overflowTools,
                                    vm = vm,
                                    fgColor = barFg,
                                    onPickTool = { picked ->
                                        if (vm.tool == picked) onTogglePopover(picked) else vm.tool = picked
                                    },
                                )
                            }
                        }

                        if (toolHasInlineOptions(vm.tool)) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(trayBg)
                                    .border(1.dp, trayBorder, RoundedCornerShape(16.dp))
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ToolOptions(
                                    vm = vm,
                                    vertical = false,
                                    settings = settings,
                                    dockFg = barFg,
                                    darkDock = darkUi,
                                )
                            }
                        }
                    }

                    // Cluster 3: Dock & Style switcher capsule
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(trayBg)
                            .border(1.dp, trayBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                    ) {
                        ToolbarLayoutMenuButton(
                            settings = settings,
                            fgColor = barFg,
                            onSelectDock = onSelectDock,
                            onSelectStyle = onSelectStyle,
                            onResetPosition = onResetPosition,
                        )
                    }
                }
            }
        }
    }
}

private fun toolHasInlineOptions(t: Tool): Boolean = when (t) {
    Tool.PEN, Tool.HIGHLIGHTER, Tool.SHAPE, Tool.ERASER, Tool.TEXT, Tool.TAPE, Tool.STICKY, Tool.WEBLINK, Tool.MINDMAP -> true
    else -> false
}

/**
 * Sleek draggable floating pill or compact toolbar rendered inside the canvas viewport.
 */
@Composable
fun FloatingToolbarPill(
    vm: EditorViewModel,
    settings: AppSettings,
    darkUi: Boolean,
    area: IntSize,
    overlayFade: Float,
    activePopoverTool: Tool?,
    onTogglePopover: (Tool) -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onSelectDock: (ToolbarDock) -> Unit,
    onSelectStyle: (ToolbarStyle) -> Unit,
    onResetPosition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dockBg = if (darkUi) DockDarkBg else DockLightBg
    val dockFg = if (darkUi) Color.White else Color(0xFF1E2321)
    val trayBg = if (darkUi) Color.White.copy(alpha = 0.06f) else Color(0xFF0F172A).copy(alpha = 0.045f)
    val dividerColor = dockFg.copy(alpha = 0.14f)
    val reduceEffects = LocalReduceEffects.current
    val density = LocalDensity.current.density
    val visibleTools = remember(settings.toolbarTools) { ToolbarOrder.visible(settings.toolbarTools) }
    val context = LocalContext.current
    val store = remember(context) { SettingsStore.get(context) }

    val layout = remember(settings.toolbarDock, settings.toolbarStyle, visibleTools, vm.tool, area, density) {
        val isVert = settings.toolbarDock == ToolbarDock.LEFT || settings.toolbarDock == ToolbarDock.RIGHT
        val availablePx = if (isVert) (area.height - (100f * density).toInt()) else (area.width - (150f * density).toInt())
        ToolbarGeometry.computeAdaptiveLayout(
            dockEdge = settings.toolbarDock,
            style = if (settings.toolbarStyle == ToolbarStyle.FIXED_BAR) ToolbarStyle.FLOATING_PILL else settings.toolbarStyle,
            visibleTools = visibleTools,
            activeTool = vm.tool,
            availablePx = availablePx.coerceAtLeast((220f * density).toInt()),
            slotExtentPx = (50f * density).toInt(),
            reservedPx = if (isVert) (110f * density).toInt() else (196f * density).toInt(),
        )
    }

    val isVertical = layout.orientation == ToolbarOrientation.VERTICAL
    val dens = LocalDensity.current
    val areaW = with(dens) { area.width.toDp() }
    val areaH = with(dens) { area.height.toDp() }
    val maxDockW = if (isVertical) 68.dp else (areaW - 24.dp).coerceAtLeast(220.dp)
    val maxDockH = if (isVertical) (areaH - 24.dp).coerceAtLeast(220.dp) else 84.dp

    val dragHandleDesc = stringResource(R.string.toolbar_drag_handle)

    val dragHandleModifier = Modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .clip(RoundedCornerShape(14.dp))
        .semantics { contentDescription = dragHandleDesc }
        .focusable()
        .pointerInput(Unit) {
            var accumDelta = Offset.Zero
            detectDragGestures(
                onDragStart = {
                    accumDelta = Offset.Zero
                    onDragStart()
                },
                onDragCancel = { onDragEnd() },
                onDragEnd = { onDragEnd() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    accumDelta += dragAmount
                    onDragDelta(accumDelta)
                },
            )
        }

    AppSurface(
        role = SurfaceRole.Chrome,
        containerColor = dockBg,
        contentColor = dockFg,
        shape = if (isVertical) RoundedCornerShape(26.dp) else appPillShape(),
        shadowElevation = if (reduceEffects) 0.dp else 12.dp,
        borderColor = if (darkUi) Color.White.copy(alpha = 0.16f) else Color(0xFF0F172A).copy(alpha = 0.12f),
        contentPadding = PaddingValues(
            horizontal = if (isVertical) 6.dp else 8.dp,
            vertical = if (isVertical) 6.dp else 4.dp,
        ),
        modifier = modifier
            .then(if (isVertical) Modifier.width(68.dp) else Modifier.widthIn(max = maxDockW))
            .height(if (isVertical) maxDockH else 60.dp)
            .graphicsLayer { alpha = overlayFade },
    ) {
        val reorderTool = { toolToMove: Tool, step: Int ->
            val idx = visibleTools.indexOf(toolToMove)
            val targetIdx = (idx + step).coerceIn(0, visibleTools.lastIndex)
            if (idx >= 0 && targetIdx != idx) {
                store.update {
                    it.copy(toolbarTools = ToolbarOrder.moveVisible(it.toolbarTools, toolToMove, visibleTools[targetIdx]))
                }
            }
        }

        if (isVertical) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(dragHandleModifier, contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .width(24.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(dockFg.copy(alpha = 0.34f)),
                    )
                }
                ToolbarLayoutMenuButton(
                    settings = settings,
                    fgColor = dockFg,
                    onSelectDock = onSelectDock,
                    onSelectStyle = onSelectStyle,
                    onResetPosition = onResetPosition,
                )
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(trayBg)
                        .padding(vertical = 3.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    layout.pinnedTools.forEach { t ->
                        ToolButton(
                            vm = vm,
                            t = t,
                            settings = settings,
                            darkUi = darkUi,
                            barFg = dockFg,
                            popoverOpen = activePopoverTool == t,
                            verticalBar = true,
                            onOpenPopover = { onTogglePopover(t) },
                            onReorderStep = reorderTool,
                        )
                    }
                    if (layout.overflowTools.isNotEmpty()) {
                        OverflowToolsButton(
                            tools = layout.overflowTools,
                            vm = vm,
                            fgColor = dockFg,
                            onPickTool = { picked ->
                                if (vm.tool == picked) onTogglePopover(picked) else vm.tool = picked
                            },
                        )
                    }
                }
                if (layout.showInlineOptions && toolHasInlineOptions(vm.tool)) {
                    Box(Modifier.height(1.dp).width(26.dp).background(dividerColor))
                    ToolOptions(vm = vm, vertical = true, settings = settings, dockFg = dockFg, darkDock = darkUi)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(dragHandleModifier, contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.DragIndicator,
                        contentDescription = dragHandleDesc,
                        tint = dockFg.copy(alpha = 0.56f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(18.dp))
                        .background(trayBg)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    layout.pinnedTools.forEach { t ->
                        ToolButton(
                            vm = vm,
                            t = t,
                            settings = settings,
                            darkUi = darkUi,
                            barFg = dockFg,
                            popoverOpen = activePopoverTool == t,
                            verticalBar = false,
                            onOpenPopover = { onTogglePopover(t) },
                            onReorderStep = reorderTool,
                        )
                    }
                    if (layout.overflowTools.isNotEmpty()) {
                        OverflowToolsButton(
                            tools = layout.overflowTools,
                            vm = vm,
                            fgColor = dockFg,
                            onPickTool = { picked ->
                                if (vm.tool == picked) onTogglePopover(picked) else vm.tool = picked
                            },
                        )
                    }
                }
                if (layout.showInlineOptions && toolHasInlineOptions(vm.tool)) {
                    Box(Modifier.padding(horizontal = 2.dp).width(1.dp).height(24.dp).background(dividerColor))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolOptions(vm = vm, vertical = false, settings = settings, dockFg = dockFg, darkDock = darkUi)
                    }
                }
                ToolbarLayoutMenuButton(
                    settings = settings,
                    fgColor = dockFg,
                    onSelectDock = onSelectDock,
                    onSelectStyle = onSelectStyle,
                    onResetPosition = onResetPosition,
                )
            }
        }
    }
}

@Composable
private fun ToolbarUndoRedoCluster(
    vm: EditorViewModel,
    vertical: Boolean,
    fgColor: Color,
) {
    val undoLabel = stringResource(R.string.toolbar_undo)
    val redoLabel = stringResource(R.string.toolbar_redo)
    val items = @Composable {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(13.dp))
                .focusable()
                .clickable(enabled = vm.canUndo, role = Role.Button, onClickLabel = undoLabel, onClick = vm::undo),
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.UNDO, if (vm.canUndo) fgColor else fgColor.copy(alpha = 0.28f), Modifier.size(19.dp))
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(13.dp))
                .focusable()
                .clickable(enabled = vm.canRedo, role = Role.Button, onClickLabel = redoLabel, onClick = vm::redo),
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.REDO, if (vm.canRedo) fgColor else fgColor.copy(alpha = 0.28f), Modifier.size(19.dp))
        }
    }
    if (vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { items() }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) { items() }
    }
}

@Composable
private fun OverflowToolsButton(
    tools: List<Tool>,
    vm: EditorViewModel,
    fgColor: Color,
    onPickTool: (Tool) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val desc = stringResource(R.string.toolbar_more_tools)
    val menuItems = tools.map { tool ->
        val name = toolDisplayName(tool)
        val ink = Color(vm.styleFor(tool)?.color ?: vm.penStyle.color)
        AppMenuItem(
            label = name,
            selected = vm.tool == tool,
            icon = { ToolGlyph(tool, fgColor, ink, Modifier.size(18.dp), active = vm.tool == tool) },
            onClick = { onPickTool(tool) },
        )
    }
    Box {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(13.dp))
                .semantics { contentDescription = desc }
                .focusable()
                .clickable(role = Role.Button, onClickLabel = desc) { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.MoreHoriz, contentDescription = desc, tint = fgColor.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ProButtonPalette.ElectricBlue),
            )
        }
        AppContextMenu(
            expanded = open,
            onDismissRequest = { open = false },
            items = menuItems,
        )
    }
}

@Composable
private fun ToolbarLayoutMenuButton(
    settings: AppSettings,
    fgColor: Color,
    onSelectDock: (ToolbarDock) -> Unit,
    onSelectStyle: (ToolbarStyle) -> Unit,
    onResetPosition: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val desc = stringResource(R.string.toolbar_layout_menu)
    Box {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(13.dp))
                .semantics { contentDescription = desc }
                .focusable()
                .clickable(role = Role.Button, onClickLabel = desc) { open = true },
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.TOOLBAR, fgColor.copy(alpha = 0.82f), Modifier.size(19.dp))
        }
        AppContextMenu(
            expanded = open,
            onDismissRequest = { open = false },
            items = listOf(
                AppMenuItem(stringResource(R.string.toolbar_style_fixed), selected = settings.toolbarStyle == ToolbarStyle.FIXED_BAR) {
                    onSelectStyle(ToolbarStyle.FIXED_BAR)
                },
                AppMenuItem(stringResource(R.string.toolbar_style_floating), selected = settings.toolbarStyle == ToolbarStyle.FLOATING_PILL) {
                    onSelectStyle(ToolbarStyle.FLOATING_PILL)
                },
                AppMenuItem(stringResource(R.string.toolbar_style_compact), selected = settings.toolbarStyle == ToolbarStyle.COMPACT) {
                    onSelectStyle(ToolbarStyle.COMPACT)
                },
                AppMenuItem(stringResource(R.string.toolbar_dock_top), selected = settings.toolbarDock == ToolbarDock.TOP) {
                    onSelectDock(ToolbarDock.TOP)
                },
                AppMenuItem(stringResource(R.string.toolbar_dock_bottom), selected = settings.toolbarDock == ToolbarDock.BOTTOM) {
                    onSelectDock(ToolbarDock.BOTTOM)
                },
                AppMenuItem(stringResource(R.string.toolbar_dock_left), selected = settings.toolbarDock == ToolbarDock.LEFT) {
                    onSelectDock(ToolbarDock.LEFT)
                },
                AppMenuItem(stringResource(R.string.toolbar_dock_right), selected = settings.toolbarDock == ToolbarDock.RIGHT) {
                    onSelectDock(ToolbarDock.RIGHT)
                },
                AppMenuItem(stringResource(R.string.toolbar_dock_floating), selected = settings.toolbarDock == ToolbarDock.FREE) {
                    onSelectDock(ToolbarDock.FREE)
                },
                AppMenuItem(stringResource(R.string.toolbar_reset_position)) {
                    onResetPosition()
                },
            ),
        )
    }
}

@Composable
fun ToolButton(
    vm: EditorViewModel,
    t: Tool,
    settings: AppSettings,
    darkUi: Boolean,
    barFg: Color,
    popoverOpen: Boolean,
    verticalBar: Boolean = false,
    onOpenPopover: () -> Unit,
    onReorderStep: ((Tool, Int) -> Unit)? = null,
) {
    val selected = vm.tool == t
    val motion = LocalMotionStyle.current
    val reduceMotion = LocalReduceMotion.current
    val reduceEffects = LocalReduceEffects.current
    val name = toolDisplayName(t)
    val ink = Color(
        vm.styleFor(t)?.color ?: when (t) {
            Tool.TAPE -> vm.tapeColor
            Tool.STICKY -> vm.stickyColor
            Tool.WEBLINK -> vm.webLinkColor
            else -> vm.penStyle.color
        },
    )
    val tileW = (40 * settings.toolbarSize.scale).coerceIn(36f, 48f).dp
    val tileH = (40 * settings.toolbarSize.scale).coerceIn(36f, 48f).dp
    val density = LocalDensity.current.density
    var hovered by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var stretchDx by remember { mutableFloatStateOf(0f) }
    var stretchDy by remember { mutableFloatStateOf(0f) }

    val springDamping = when (motion) {
        MotionStyle.SPRING -> 0.56f
        MotionStyle.MINIMAL_CRISP -> Spring.DampingRatioNoBouncy
        MotionStyle.REDUCED -> 1f
    }
    val offsetX by animateFloatAsState(
        targetValue = if (dragging) stretchDx.coerceIn(-22f * density, 22f * density) else 0f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = springDamping, stiffness = Spring.StiffnessMedium),
        label = "ox",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (dragging) stretchDy.coerceIn(-22f * density, 22f * density) else 0f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = springDamping, stiffness = Spring.StiffnessMedium),
        label = "oy",
    )

    val dragReorderMod = if (onReorderStep != null) {
        Modifier.pointerInput(t, verticalBar) {
            val stepPx = 38f * density
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragging = true
                    hovered = false
                    dragAccum = 0f
                    stretchDx = 0f
                    stretchDy = 0f
                },
                onDragEnd = {
                    dragging = false
                    dragAccum = 0f
                    stretchDx = 0f
                    stretchDy = 0f
                },
                onDragCancel = {
                    dragging = false
                    dragAccum = 0f
                    stretchDx = 0f
                    stretchDy = 0f
                },
                onDrag = { change, amount ->
                    change.consume()
                    stretchDx = (stretchDx + amount.x).coerceIn(-26f * density, 26f * density)
                    stretchDy = (stretchDy + amount.y).coerceIn(-26f * density, 26f * density)
                    dragAccum += if (verticalBar) amount.y else amount.x
                    if (abs(dragAccum) >= stepPx) {
                        val dir = if (dragAccum > 0f) 1 else -1
                        onReorderStep(t, dir)
                        dragAccum -= dir * stepPx
                    }
                },
            )
        }
    } else Modifier

    val targetSlotBg = when {
        dragging -> ProButtonPalette.SkyCyan
        selected -> if (darkUi) ProButtonPalette.ElectricBlue else ProButtonPalette.RoyalBlue
        hovered -> barFg.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val animatedSlotBg by animateColorAsState(
        targetValue = targetSlotBg,
        animationSpec = if (reduceMotion) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "slotBg",
    )
    val activeSlotFg = Color.White
    val hasColorBar = t in setOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.SHAPE, Tool.TEXT, Tool.TAPE, Tool.STICKY, Tool.WEBLINK, Tool.MINDMAP)
    val inkLum = ink.luminance()
    val colorBarRim = when {
        inkLum > 0.68f -> Color(0xFF0F172A)
        inkLum < 0.24f -> Color(0xFFF8FAFC)
        else -> if (selected || darkUi) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = name }
            .focusable()
            .clickable(role = Role.Button, onClickLabel = name) {
                if (!selected) vm.tool = t else onOpenPopover()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = tileW, height = tileH)
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    if (dragging && !reduceMotion) {
                        scaleX = 1.08f
                        scaleY = 1.08f
                    }
                }
                .then(
                    if ((selected || dragging) && !reduceEffects) {
                        Modifier.shadow(4.dp, RoundedCornerShape(13.dp), ambientColor = ProButtonPalette.RoyalBlue, spotColor = ProButtonPalette.RoyalBlue)
                    } else Modifier,
                )
                .clip(RoundedCornerShape(13.dp))
                .background(animatedSlotBg)
                .then(
                    if (selected || dragging) {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(13.dp))
                    } else Modifier,
                )
                .pointerInput(t) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Enter -> hovered = true
                                PointerEventType.Exit -> hovered = false
                            }
                        }
                    }
                }
                .then(dragReorderMod),
            contentAlignment = Alignment.Center,
        ) {
            val tint = if (selected || dragging) activeSlotFg else barFg
            ToolGlyph(
                tool = t,
                tint = tint,
                inkColor = ink,
                modifier = Modifier
                    .padding(bottom = if (hasColorBar) 3.dp else 0.dp)
                    .size((20 * settings.toolbarSize.scale).dp),
                active = selected || dragging,
            )
            if (hasColorBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .size(width = 16.dp, height = 3.5.dp)
                        .clip(CircleShape)
                        .background(ink)
                        .border(0.75.dp, colorBarRim.copy(alpha = 0.8f), CircleShape),
                )
            }
            if (selected) {
                Icon(
                    if (popoverOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = activeSlotFg.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .size(11.dp),
                )
            }
        }

        if (hovered && !dragging && !popoverOpen && settings.showHoverTooltips && settings.hoverCursor) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, (44 * density).toInt()),
                properties = PopupProperties(focusable = false),
            ) {
                AppSurface(
                    role = SurfaceRole.Popover,
                    shape = appPillShape(),
                    shadowElevation = 6.dp,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun ToolOptions(
    vm: EditorViewModel,
    vertical: Boolean,
    settings: AppSettings,
    dockFg: Color = MaterialTheme.colorScheme.onSurface,
    darkDock: Boolean = false,
) {
    val context = LocalContext.current
    val store = remember(context) { SettingsStore.get(context) }
    val reduceMotion = LocalReduceMotion.current
    AnimatedContent(
        targetState = vm.tool,
        transitionSpec = {
            if (reduceMotion) fadeIn(snap()) togetherWith fadeOut(snap())
            else fadeIn() togetherWith fadeOut()
        },
        label = "options",
    ) { t ->
        val content = @Composable {
            val style = vm.styleFor(t)
            when {
                style != null -> StyleOptions(vm, t, style, dockFg, darkDock, vertical)
                t == Tool.ERASER -> {
                    SizeDots(listOf(8f, 16f, 30f), listOf(8f, 16f, 30f)[vm.eraserSize], ring = true, fg = dockFg, darkDock = darkDock) { s ->
                        vm.eraserCustomDp = null
                        vm.eraserSize = listOf(8f, 16f, 30f).indexOf(s).coerceIn(0, 2)
                    }
                    if (!vertical) {
                        MiniChip(if (settings.eraserMode == EraserMode.PIXEL) "Pixel" else "Stroke") {
                            store.update {
                                it.copy(eraserMode = if (it.eraserMode == EraserMode.PIXEL) EraserMode.STROKE else EraserMode.PIXEL)
                            }
                        }
                    }
                }
                t == Tool.TEXT -> SizeDots(listOf(28f, 40f, 64f), vm.textSize, fg = dockFg, darkDock = darkDock) { vm.textSize = it }
                t == Tool.TAPE -> {
                    CustomSwatches(TapeColors, vm.tapeColor, vm) { vm.tapeColor = it }
                    if (!vertical) MiniChip(if (vm.tapeFreehand) "Freehand" else "Straight") { vm.tapeFreehand = !vm.tapeFreehand }
                }
                t == Tool.STICKY -> CustomSwatches(StickyColors, vm.stickyColor, vm) { vm.stickyColor = it }
                t == Tool.WEBLINK -> CustomSwatches(WebLinkColors, vm.webLinkColor, vm) { vm.webLinkColor = it }
                t == Tool.MINDMAP -> {
                    MindPalette.take(5).forEach { c ->
                        Swatch(c, c == vm.mindColor, 18) { vm.recolorMindNode(color = c) }
                    }
                    if (!vertical) {
                        EditorViewModel.MindLayout.entries.forEach { layout ->
                            MiniChip(layout.label) { vm.autoLayoutMindMap(layout) }
                        }
                        MiniChip("Fit Map") { vm.fitMindMapToView() }
                        MiniChip(if (vm.moveMindSubtree) "Subtree: On" else "Subtree: Off") { vm.moveMindSubtree = !vm.moveMindSubtree }
                        MiniChip("Cards") { vm.flashcardsFromMindMap() }
                    }
                }
                else -> Unit
            }
        }
        if (vertical) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) { content() }
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) { content() }
    }
}

@Composable
private fun MiniChip(label: String, onClick: () -> Unit) {
    AppPill(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CustomSwatches(base: List<Int>, current: Int, vm: EditorViewModel, onPick: (Int) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    base.forEach { c -> Swatch(c, c == current, 18) { onPick(c) } }
    if (current !in base) Swatch(current, true, 18) { custom = true }
    AddSwatchButton { custom = true }
    if (custom) {
        ColorEditorDialog(current, PenPalette, vm.recentColors(), onDismiss = { custom = false }) {
            onPick(it)
            vm.pushRecent(it)
            custom = false
        }
    }
}

@Composable
fun AddSwatchButton(onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 38.dp, minHeight = 38.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Custom colour", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF3B30),
                            Color(0xFFFFCC00),
                            Color(0xFF34C759),
                            Color(0xFF0A84FF),
                            Color(0xFFAF52DE),
                            Color(0xFFFF3B30),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Add, "Custom colour", Modifier.size(10.dp))
            }
        }
    }
}

@Composable
fun StyleOptions(
    vm: EditorViewModel,
    t: Tool,
    style: ToolStyle,
    dockFg: Color,
    darkDock: Boolean,
    vertical: Boolean = false,
) {
    var editing by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    var removeMenu by remember { mutableStateOf<Int?>(null) }

    SizeDots(style.sizes, style.size, fg = dockFg, darkDock = darkDock) { sz ->
        vm.updateStyle(t, style.copy(size = sz))
    }
    if (vertical) {
        Box(Modifier.padding(vertical = 4.dp).height(1.dp).width(24.dp).background(dockFg.copy(alpha = 0.16f)))
    } else {
        Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(20.dp).background(dockFg.copy(alpha = 0.16f)))
    }
    style.colors.forEachIndexed { i, c ->
        val selected = c == style.color
        Box {
            Swatch(c, selected, 18, onLongClick = { removeMenu = i }) {
                if (selected) editing = i else vm.updateStyle(t, style.copy(color = c))
            }
            AppContextMenu(
                expanded = removeMenu == i,
                onDismissRequest = { removeMenu = null },
                items = buildList {
                    add(AppMenuItem("Edit colour") { editing = i })
                    if (style.colors.size > 1) {
                        add(AppMenuItem("Remove", destructive = true) { vm.removeSwatch(t, i) })
                    }
                },
            )
        }
    }
    if (style.colors.size < 8) AddSwatchButton { adding = true }

    val presets = if (t == Tool.HIGHLIGHTER) HighlighterPalette + PenPalette else PenPalette
    editing?.let { slot ->
        ColorEditorDialog(style.colors[slot], presets, vm.recentColors(), onDismiss = { editing = null }) { picked ->
            val colors = style.colors.toMutableList().also { it[slot] = picked }
            vm.updateStyle(t, style.copy(colors = colors, color = picked))
            vm.pushRecent(picked)
            editing = null
        }
    }
    if (adding) {
        ColorEditorDialog(style.color, presets, vm.recentColors(), onDismiss = { adding = false }) { picked ->
            vm.addSwatch(t, picked)
            adding = false
        }
    }
}

@Composable
fun SizeDots(
    sizes: List<Float>,
    current: Float,
    ring: Boolean = false,
    fg: Color = MaterialTheme.colorScheme.onSurface,
    darkDock: Boolean = false,
    onPick: (Float) -> Unit,
) {
    val activeBg = if (darkDock) ProButtonPalette.ElectricBlue.copy(alpha = 0.24f) else MaterialTheme.colorScheme.primaryContainer
    val activeStroke = if (darkDock) Color(0xFF93C5FD) else MaterialTheme.colorScheme.primary
    sizes.forEachIndexed { i, s ->
        val selected = s == current
        Box(
            Modifier
                .padding(horizontal = 1.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) activeBg else Color.Transparent)
                .then(
                    if (selected) Modifier.border(1.dp, activeStroke.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    else Modifier,
                )
                .clickable(role = Role.Button, onClickLabel = "Size ${s.toInt()}") { onPick(s) },
            contentAlignment = Alignment.Center,
        ) {
            if (ring) {
                Box(Modifier.size((8 + 5 * i).dp).border(1.6.dp, if (selected) activeStroke else fg, CircleShape))
            } else {
                val dotDiameter = (4 + 3.5f * i).dp
                Box(Modifier.size(dotDiameter).clip(CircleShape).background(if (selected) activeStroke else fg.copy(alpha = 0.85f)))
            }
        }
    }
}
