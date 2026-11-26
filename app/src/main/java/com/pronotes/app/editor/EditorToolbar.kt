package com.pronotes.app.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.pronotes.app.data.HighlighterMode
import com.pronotes.app.data.LassoMode
import com.pronotes.app.data.MotionStyle
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.data.ToolbarStyle
import com.pronotes.app.ui.AppContextMenu
import com.pronotes.app.ui.AppMenuItem
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
import com.pronotes.app.ui.tactilePressScale
import kotlin.math.abs

private val WorkbenchDarkBg = Color(0xFF131720)
private val WorkbenchDarkElevated = Color(0xFF1E2534)
private val WorkbenchLightBg = Color(0xFFFAF9F6)
private val WorkbenchLightElevated = Color(0xFFFFFFFF)

@Composable
fun toolDisplayName(t: Tool): String = when (t) {
    Tool.PEN -> stringResource(R.string.tool_pen)
    Tool.CRAYON -> "Crayon"
    Tool.HIGHLIGHTER -> stringResource(R.string.tool_highlighter)
    Tool.ERASER -> stringResource(R.string.tool_eraser)
    Tool.SHAPE -> stringResource(R.string.tool_shapes)
    Tool.LASSO -> stringResource(R.string.tool_selection)
    Tool.MINDMAP -> stringResource(R.string.tool_concept_maps)
    Tool.IMAGE -> stringResource(R.string.tool_image)
    Tool.TEXT -> stringResource(R.string.tool_text)
    Tool.AUTO_TEXT -> "Auto-Text"
    Tool.STICKY -> stringResource(R.string.tool_sticky)
    Tool.WEBLINK -> stringResource(R.string.tool_weblink)
    Tool.TAPE -> stringResource(R.string.tool_tape)
    Tool.STICKER -> stringResource(R.string.tool_sticker)
    Tool.LASER -> stringResource(R.string.tool_laser)
}

fun shortToolLabel(t: Tool): String = when (t) {
    Tool.PEN -> "Pen"
    Tool.CRAYON -> "Crayon"
    Tool.HIGHLIGHTER -> "Highlight"
    Tool.ERASER -> "Eraser"
    Tool.SHAPE -> "Shapes"
    Tool.LASSO -> "Selection"
    Tool.MINDMAP -> "Concept"
    Tool.IMAGE -> "Image"
    Tool.TEXT -> "Text"
    Tool.AUTO_TEXT -> "Auto-Text"
    Tool.STICKY -> "Sticky"
    Tool.WEBLINK -> "Link"
    Tool.TAPE -> "Tape"
    Tool.STICKER -> "Sticker"
    Tool.LASER -> "Laser"
}

/**
 * Full-width or full-height studio workbench toolbar. Uses an open, hairline-separated drafting
 * rail where the active instrument rises on an elevated tonal pedestal paired with fast inline
 * stroke, mode, and colour controls.
 */
@Composable
fun AdaptiveFixedToolbar(
    vm: EditorViewModel,
    settings: AppSettings,
    darkUi: Boolean,
    activePopoverTool: Tool?,
    onTogglePopover: (Tool) -> Unit,
    onVideo: () -> Unit = {},
    onSelectDock: (ToolbarDock) -> Unit,
    onSelectStyle: (ToolbarStyle) -> Unit,
    onResetPosition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVertical = settings.toolbarDock == ToolbarDock.LEFT || settings.toolbarDock == ToolbarDock.RIGHT
    val barBg = Color.Black
    val barFg = Color(0xFFF1F5F9)
    val rimColor = Color.White.copy(alpha = 0.12f)
    val hairlineColor = Color.White.copy(alpha = 0.15f)
    val reduceEffects = LocalReduceEffects.current
    val context = LocalContext.current
    val store = remember(context) { SettingsStore.get(context) }
    val density = LocalDensity.current.density
    val visibleTools = remember(settings.toolbarTools) { ToolbarOrder.visible(settings.toolbarTools) }
    androidx.compose.runtime.LaunchedEffect(visibleTools) {
        if (visibleTools.isNotEmpty() && vm.tool !in visibleTools) {
            vm.tool = visibleTools.first()
        }
    }

    AppSurface(
        role = SurfaceRole.Chrome,
        containerColor = barBg,
        contentColor = barFg,
        shape = RoundedCornerShape(0.dp),
        shadowElevation = if (reduceEffects) 0.dp else 5.dp,
        borderColor = rimColor,
        contentPadding = PaddingValues(
            horizontal = if (isVertical) 6.dp else 12.dp,
            vertical = if (isVertical) 8.dp else 5.dp,
        ),
        modifier = modifier
            .then(if (isVertical) Modifier.fillMaxHeight().width(68.dp) else Modifier.fillMaxWidth()),
    ) {
        BoxWithConstraints(
            modifier = if (isVertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth(),
        ) {
            val availablePx = with(LocalDensity.current) {
                if (isVertical) maxHeight.toPx().toInt() else maxWidth.toPx().toInt()
            }
            val slotPx = (48f * density).toInt()
            val reservedPx = if (isVertical) (192f * density).toInt() else (420f * density).toInt()
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
                    vm.notebookSettings.update {
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ToolbarLayoutMenuButton(
                        settings = settings,
                        fgColor = barFg,
                        onSelectDock = onSelectDock,
                        onSelectStyle = onSelectStyle,
                        onResetPosition = onResetPosition,
                    )
                    ToolbarUndoRedoCluster(vm = vm, vertical = true, fgColor = barFg)
                    StudioHairline(vertical = false, color = hairlineColor)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        if (settings.videoInToolbar) {
                            IconButton(onClick = onVideo) {
                                Icon(Icons.Outlined.PlayCircleOutline, "Floating video", tint = Color.White)
                            }
                        }
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

                    if (toolHasInlineOptions(vm.tool)) {
                        StudioHairline(vertical = false, color = hairlineColor)
                        ToolOptions(
                            vm = vm,
                            vertical = true,
                            settings = settings,
                            dockFg = barFg,
                            darkDock = true,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Left: clean history cluster
                    ToolbarUndoRedoCluster(vm = vm, vertical = false, fgColor = barFg)
                    StudioHairline(vertical = true, color = hairlineColor)

                    // Center: open instrument rail + live tool inspector strip
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (settings.videoInToolbar) {
                                IconButton(onClick = onVideo) {
                                    Icon(Icons.Outlined.PlayCircleOutline, "Floating video", tint = Color.White)
                                }
                            }
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
                            StudioHairline(vertical = true, color = hairlineColor)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ToolOptions(
                                    vm = vm,
                                    vertical = false,
                                    settings = settings,
                                    dockFg = barFg,
                                    darkDock = true,
                                )
                            }
                        }
                    }

                    // Right: dock & toolbar style button
                    StudioHairline(vertical = true, color = hairlineColor)
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

@Composable
private fun StudioHairline(vertical: Boolean, color: Color) {
    if (vertical) {
        Box(Modifier.padding(horizontal = 2.dp).width(1.dp).height(24.dp).background(color))
    } else {
        Box(Modifier.padding(vertical = 2.dp).height(1.dp).width(28.dp).background(color))
    }
}

private fun toolHasInlineOptions(t: Tool): Boolean = when (t) {
    Tool.PEN, Tool.CRAYON, Tool.AUTO_TEXT, Tool.HIGHLIGHTER, Tool.SHAPE, Tool.ERASER, Tool.LASSO, Tool.TEXT,
    Tool.TAPE, Tool.STICKY, Tool.WEBLINK, Tool.MINDMAP,
    -> true
    else -> false
}

/**
 * Draggable floating studio dock rendered inside the canvas viewport with elevated tool pedestals,
 * hairline-separated contextual controls, and a tactile grip handle.
 */
@Composable
fun FloatingToolbarPill(
    vm: EditorViewModel,
    settings: AppSettings,
    darkUi: Boolean,
    area: IntSize,
    overlayFade: () -> Float,
    activePopoverTool: Tool?,
    onTogglePopover: (Tool) -> Unit,
    onVideo: () -> Unit = {},
    onDragStart: () -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onSelectDock: (ToolbarDock) -> Unit,
    onSelectStyle: (ToolbarStyle) -> Unit,
    onResetPosition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dockBg = Color.Black
    val dockFg = Color(0xFFF1F5F9)
    val hairlineColor = Color.White.copy(alpha = 0.15f)
    val reduceEffects = LocalReduceEffects.current
    val density = LocalDensity.current.density
    val visibleTools = remember(settings.toolbarTools) { ToolbarOrder.visible(settings.toolbarTools) }
    LaunchedEffect(visibleTools) {
        if (visibleTools.isNotEmpty() && vm.tool !in visibleTools) {
            vm.tool = visibleTools.first()
        }
    }
    val context = LocalContext.current
    val store = remember(context) { SettingsStore.get(context) }

    val layout = remember(settings.toolbarDock, settings.toolbarStyle, visibleTools, vm.tool, area, density) {
        val isVert = settings.toolbarDock == ToolbarDock.LEFT || settings.toolbarDock == ToolbarDock.RIGHT
        val availablePx = if (isVert) (area.height - (100f * density).toInt()) else (area.width - (140f * density).toInt())
        ToolbarGeometry.computeAdaptiveLayout(
            dockEdge = settings.toolbarDock,
            style = if (settings.toolbarStyle == ToolbarStyle.FIXED_BAR) ToolbarStyle.FLOATING_PILL else settings.toolbarStyle,
            visibleTools = visibleTools,
            activeTool = vm.tool,
            availablePx = availablePx.coerceAtLeast((220f * density).toInt()),
            slotExtentPx = (48f * density).toInt(),
            reservedPx = if (isVert) (108f * density).toInt() else (196f * density).toInt(),
        )
    }

    val isVertical = layout.orientation == ToolbarOrientation.VERTICAL
    val dens = LocalDensity.current
    val areaW = with(dens) { area.width.toDp() }
    val areaH = with(dens) { area.height.toDp() }
    val maxDockW = if (isVertical) 66.dp else (areaW - 24.dp).coerceAtLeast(220.dp)
    val maxDockH = if (isVertical) (areaH - 24.dp).coerceAtLeast(220.dp) else 84.dp

    val dragHandleDesc = stringResource(R.string.toolbar_drag_handle)
    val latestDragStart by androidx.compose.runtime.rememberUpdatedState(onDragStart)
    val latestDragDelta by androidx.compose.runtime.rememberUpdatedState(onDragDelta)
    val latestDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)

    val dragHandleModifier = Modifier
        .defaultMinSize(minWidth = if (isVertical) 44.dp else 30.dp, minHeight = if (isVertical) 30.dp else 44.dp)
        .clip(RoundedCornerShape(14.dp))
        .semantics { contentDescription = dragHandleDesc }
        .focusable()
        .pointerInput(Unit) {
            var accumDelta = Offset.Zero
            detectDragGestures(
                onDragStart = {
                    accumDelta = Offset.Zero
                    latestDragStart()
                },
                onDragCancel = { latestDragEnd() },
                onDragEnd = { latestDragEnd() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    accumDelta += dragAmount
                    latestDragDelta(accumDelta)
                },
            )
        }

    AppSurface(
        role = SurfaceRole.Chrome,
        containerColor = dockBg,
        contentColor = dockFg,
        shape = if (isVertical) RoundedCornerShape(24.dp) else RoundedCornerShape(22.dp),
        shadowElevation = if (reduceEffects) 0.dp else 14.dp,
        borderColor = Color.White.copy(alpha = 0.16f),
        contentPadding = PaddingValues(
            horizontal = if (isVertical) 5.dp else 8.dp,
            vertical = if (isVertical) 6.dp else 5.dp,
        ),
        modifier = modifier
            .then(if (isVertical) Modifier.width(66.dp) else Modifier.widthIn(max = maxDockW))
            .height(if (isVertical) maxDockH else 62.dp)
            .graphicsLayer { alpha = overlayFade() },
    ) {
        val reorderTool = { toolToMove: Tool, step: Int ->
            val idx = visibleTools.indexOf(toolToMove)
            val targetIdx = (idx + step).coerceIn(0, visibleTools.lastIndex)
            if (idx >= 0 && targetIdx != idx) {
                vm.notebookSettings.update {
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
                            .background(dockFg.copy(alpha = 0.32f)),
                    )
                }
                ToolbarLayoutMenuButton(
                    settings = settings,
                    fgColor = dockFg,
                    onSelectDock = onSelectDock,
                    onSelectStyle = onSelectStyle,
                    onResetPosition = onResetPosition,
                )
                StudioHairline(vertical = false, color = hairlineColor)
                if (settings.videoInToolbar) {
                    IconButton(onClick = onVideo) {
                        Icon(Icons.Outlined.PlayCircleOutline, "Floating video", tint = Color.White)
                    }
                }
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
                if (layout.showInlineOptions && toolHasInlineOptions(vm.tool)) {
                    StudioHairline(vertical = false, color = hairlineColor)
                    ToolOptions(vm = vm, vertical = true, settings = settings, dockFg = dockFg, darkDock = true)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(dragHandleModifier, contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(22.dp)
                            .clip(CircleShape)
                            .background(dockFg.copy(alpha = 0.34f)),
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (settings.videoInToolbar) {
                        IconButton(onClick = onVideo) {
                            Icon(Icons.Outlined.PlayCircleOutline, "Floating video", tint = Color.White)
                        }
                    }
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
                    StudioHairline(vertical = true, color = hairlineColor)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ToolOptions(vm = vm, vertical = false, settings = settings, dockFg = dockFg, darkDock = true)
                    }
                }
                StudioHairline(vertical = true, color = hairlineColor)
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
        val undoInteraction = remember { MutableInteractionSource() }
        val redoInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
                .tactilePressScale(undoInteraction, enabled = vm.canUndo, pressedScale = 0.88f)
                .clip(CircleShape)
                .focusable()
                .clickable(
                    interactionSource = undoInteraction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    enabled = vm.canUndo,
                    role = Role.Button,
                    onClickLabel = undoLabel,
                    onClick = vm::undo,
                ),
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.UNDO, if (vm.canUndo) fgColor else fgColor.copy(alpha = 0.25f), Modifier.size(18.dp))
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
                .tactilePressScale(redoInteraction, enabled = vm.canRedo, pressedScale = 0.88f)
                .clip(CircleShape)
                .focusable()
                .clickable(
                    interactionSource = redoInteraction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    enabled = vm.canRedo,
                    role = Role.Button,
                    onClickLabel = redoLabel,
                    onClick = vm::redo,
                ),
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.REDO, if (vm.canRedo) fgColor else fgColor.copy(alpha = 0.25f), Modifier.size(18.dp))
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
    var lastDismissMs by remember { mutableStateOf(0L) }
    val desc = stringResource(R.string.toolbar_more_tools)
    val hasActiveOverflow = tools.any { it == vm.tool }
    val interaction = remember { MutableInteractionSource() }
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
                .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
                .tactilePressScale(interaction, pressedScale = 0.90f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (hasActiveOverflow) ProButtonPalette.ElectricBlue.copy(alpha = 0.14f) else Color.Transparent)
                .semantics { contentDescription = desc }
                .focusable()
                .clickable(
                    interactionSource = interaction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    role = Role.Button,
                    onClickLabel = desc,
                ) {
                    val now = android.os.SystemClock.uptimeMillis()
                    if (open) {
                        open = false
                        lastDismissMs = now
                    } else if (now - lastDismissMs > 180L) {
                        open = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.MoreHoriz,
                contentDescription = desc,
                tint = if (hasActiveOverflow) ProButtonPalette.ElectricBlue else fgColor.copy(alpha = 0.80f),
                modifier = Modifier.size(20.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 9.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(ProButtonPalette.ElectricBlue),
            )
        }
        AppContextMenu(
            expanded = open,
            onDismissRequest = {
                open = false
                lastDismissMs = android.os.SystemClock.uptimeMillis()
            },
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
    var lastDismissMs by remember { mutableStateOf(0L) }
    var showCustomize by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val store = remember(context) { SettingsStore.get(context) }
    val desc = stringResource(R.string.toolbar_layout_menu)
    val interaction = remember { MutableInteractionSource() }
    Box {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 46.dp, minHeight = 46.dp)
                .tactilePressScale(interaction, pressedScale = 0.90f)
                .clip(CircleShape)
                .semantics { contentDescription = desc }
                .focusable()
                .clickable(
                    interactionSource = interaction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    role = Role.Button,
                    onClickLabel = desc,
                ) {
                    val now = android.os.SystemClock.uptimeMillis()
                    if (open) {
                        open = false
                        lastDismissMs = now
                    } else if (now - lastDismissMs > 180L) {
                        open = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.TOOLBAR, fgColor.copy(alpha = 0.82f), Modifier.size(18.dp))
        }
        AppContextMenu(
            expanded = open,
            onDismissRequest = {
                open = false
                lastDismissMs = android.os.SystemClock.uptimeMillis()
            },
            items = listOf(
                AppMenuItem("Customize tools…") { showCustomize = true },
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
    if (showCustomize) {
        CustomizeToolbarDialog(
            spec = settings.toolbarTools,
            onSpecChange = { newSpec -> store.update { it.copy(toolbarTools = newSpec) } },
            onDismiss = { showCustomize = false },
        )
    }
}

@Composable
private fun CustomizeToolbarDialog(
    spec: String,
    onSpecChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val entries = remember(spec) { ToolbarOrder.parse(spec) }
    com.pronotes.app.ui.AppDialog(
        onDismissRequest = onDismiss,
        title = {
            com.pronotes.app.ui.GradientTitle("Customize Toolbar Tools", MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Enable or disable specific tools, or toggle optional Auto-Text.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                entries.forEachIndexed { idx, (tool, visible) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSpecChange(ToolbarOrder.setVisible(spec, tool, !visible))
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProIconView(com.pronotes.app.ui.iconFor(tool), MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = toolDisplayName(tool),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            if (tool == Tool.AUTO_TEXT) {
                                Text(
                                    "Optional · Writes & auto-converts ink to text",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(
                            enabled = idx > 0,
                            onClick = {
                                val next = entries.toMutableList().apply {
                                    val moved = removeAt(idx)
                                    add(idx - 1, moved)
                                }
                                onSpecChange(ToolbarOrder.encode(next))
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            enabled = idx < entries.lastIndex,
                            onClick = {
                                val next = entries.toMutableList().apply {
                                    val moved = removeAt(idx)
                                    add(idx + 1, moved)
                                }
                                onSpecChange(ToolbarOrder.encode(next))
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                        }
                        com.pronotes.app.ui.AppToggle(
                            checked = visible,
                            onCheckedChange = { checked ->
                                onSpecChange(ToolbarOrder.setVisible(spec, tool, checked))
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = { onSpecChange("") }) { Text("Reset defaults") }
        },
    )
}

/**
 * Tactile studio instrument pedestal. Inactive tools rest flush on the rail; the selected tool
 * lifts slightly onto an elevated ink-tinted pedestal with a bold base indicator bar and chevron.
 */
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
            Tool.TEXT -> vm.textColor ?: vm.penStyle.color
            else -> vm.penStyle.color
        },
    )
    val hasColorBar = t in setOf(Tool.PEN, Tool.CRAYON, Tool.AUTO_TEXT, Tool.HIGHLIGHTER, Tool.SHAPE, Tool.TEXT, Tool.TAPE, Tool.STICKY, Tool.WEBLINK, Tool.MINDMAP)
    val accent = if (hasColorBar) {
        // Ensure near-black ink reads brightly on the pure black toolbar in both themes
        if (ink.luminance() < 0.08f) Color(0xFF93C5FD) else ink
    } else {
        ProButtonPalette.ElectricBlue
    }

    val tileW = (42 * settings.toolbarSize.scale).coerceIn(38f, 48f).dp
    val tileH = (42 * settings.toolbarSize.scale).coerceIn(38f, 48f).dp
    val density = LocalDensity.current.density
    var hovered by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var stretchDx by remember { mutableFloatStateOf(0f) }
    var stretchDy by remember { mutableFloatStateOf(0f) }

    val springDamping = when (motion) {
        MotionStyle.SPRING -> 0.58f
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
    val liftDp by animateDpAsState(
        targetValue = when {
            dragging -> (-4).dp
            selected && !verticalBar -> (-2).dp
            hovered && !verticalBar -> (-1).dp
            else -> 0.dp
        },
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "lift",
    )

    val dragReorderMod = Modifier

    val pedestalBg = WorkbenchDarkElevated
    val targetSlotBg = when {
        dragging -> ProButtonPalette.ElectricBlue.copy(alpha = 0.22f)
        selected -> androidx.compose.ui.graphics.lerp(pedestalBg, accent, 0.15f)
        hovered -> barFg.copy(alpha = 0.06f)
        else -> Color.Transparent
    }
    val animatedSlotBg by animateColorAsState(
        targetValue = targetSlotBg,
        animationSpec = if (reduceMotion) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "slotBg",
    )
    val pedestalShape = RoundedCornerShape(14.dp)
    val buttonInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .tactilePressScale(buttonInteraction, pressedScale = 0.91f)
            .clip(pedestalShape)
            .semantics { contentDescription = name }
            .focusable()
            .clickable(
                interactionSource = buttonInteraction,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button,
                onClickLabel = name,
            ) {
                if (!selected) vm.tool = t else onOpenPopover()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = tileW, height = tileH)
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY + liftDp.toPx()
                    if (dragging && !reduceMotion) {
                        scaleX = 1.07f
                        scaleY = 1.07f
                    }
                }
                .then(
                    if ((selected || dragging) && !reduceEffects) {
                        Modifier.shadow(
                            elevation = if (darkUi) 2.dp else 1.dp,
                            shape = pedestalShape,
                            ambientColor = accent.copy(alpha = 0.35f),
                            spotColor = accent.copy(alpha = 0.35f),
                        )
                    } else Modifier,
                )
                .clip(pedestalShape)
                .background(animatedSlotBg)
                .then(
                    if (selected || dragging) {
                        Modifier.border(
                            width = 1.4.dp,
                            color = accent.copy(alpha = if (darkUi) 0.72f else 0.58f),
                            shape = pedestalShape,
                        )
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
            val glyphTint = when {
                selected && !hasColorBar -> ProButtonPalette.ElectricBlue
                selected -> Color.White
                else -> barFg.copy(alpha = 0.80f)
            }
            ToolGlyph(
                tool = t,
                tint = glyphTint,
                inkColor = ink,
                modifier = Modifier
                    .padding(bottom = if (hasColorBar || selected) 4.dp else 0.dp)
                    .size((20 * settings.toolbarSize.scale).dp),
                active = selected || dragging,
            )

            // Signature active nib indicator bar along the bottom edge
            if (hasColorBar) {
                val barW = if (selected) 20.dp else 12.dp
                val barH = if (selected) 3.5.dp else 2.5.dp
                val rim = if (ink.luminance() > 0.75f) Color(0xFF0F172A).copy(alpha = 0.35f)
                else if (ink.luminance() < 0.16f) Color.White.copy(alpha = 0.45f)
                else Color.Transparent
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .size(width = barW, height = barH)
                        .clip(CircleShape)
                        .background(ink)
                        .then(if (rim != Color.Transparent) Modifier.border(0.6.dp, rim, CircleShape) else Modifier),
                )
            } else if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .size(width = 18.dp, height = 3.dp)
                        .clip(CircleShape)
                        .background(ProButtonPalette.ElectricBlue),
                )
            }

            if (selected) {
                Icon(
                    if (popoverOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.9f),
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
                t == Tool.PEN && style != null -> {
                    if (!vertical) {
                        val nibShort = when (vm.penType) {
                            PenType.FOUNTAIN -> "Fountain"
                            PenType.BALL -> "Ball"
                            PenType.FINELINER -> "Fine"
                            PenType.CALLIGRAPHY -> "Calligraphy"
                            PenType.PENCIL -> "Pencil"
                            PenType.LETTERING -> "Lettering"
                            PenType.BRUSH -> "Brush"
                            PenType.DASHED -> "Dashed"
                        }
                        StudioQuickChip(
                            label = nibShort,
                            active = true,
                            accent = Color(style.color),
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {
                            val list = PenType.entries
                            vm.penType = list[(list.indexOf(vm.penType) + 1) % list.size]
                        }
                    }
                    StyleOptions(vm, t, style, dockFg, darkDock, vertical)
                }
                t == Tool.CRAYON && style != null -> {
                    if (!vertical) {
                        val grainPct = (settings.toolDefaults.crayonGrain * 100).toInt()
                        StudioQuickChip(
                            label = "Wax $grainPct%",
                            active = true,
                            accent = Color(style.color),
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {
                            val nextGrain = when {
                                settings.toolDefaults.crayonGrain < 0.45f -> 0.65f
                                settings.toolDefaults.crayonGrain < 0.85f -> 0.95f
                                else -> 0.30f
                            }
                            vm.notebookSettings.update { it.copy(toolDefaults = it.toolDefaults.copy(crayonGrain = nextGrain)) }
                        }
                    }
                    StyleOptions(vm, t, style, dockFg, darkDock, vertical)
                }
                t == Tool.AUTO_TEXT -> {
                    if (!vertical) {
                        StudioQuickChip(
                            label = "Auto → Text",
                            active = true,
                            accent = ProButtonPalette.ElectricBlue,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {}
                    }
                    StyleOptions(vm, Tool.PEN, vm.penStyle, dockFg, darkDock, vertical)
                }
                t == Tool.HIGHLIGHTER && style != null -> {
                    if (!vertical) {
                        val modeLabel = when (settings.highlighterMode) {
                            HighlighterMode.AUTO -> "Auto"
                            HighlighterMode.STRAIGHT -> "Straight"
                            HighlighterMode.FREEHAND -> "Free"
                        }
                        StudioQuickChip(
                            label = modeLabel,
                            active = settings.highlighterMode != HighlighterMode.FREEHAND,
                            accent = ProButtonPalette.ElectricBlue,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {
                            val list = HighlighterMode.entries
                            val next = list[(list.indexOf(settings.highlighterMode) + 1) % list.size]
                            vm.notebookSettings.update { it.copy(highlighterMode = next) }
                        }
                    }
                    StyleOptions(vm, t, style, dockFg, darkDock, vertical)
                }
                t == Tool.SHAPE && style != null -> {
                    if (!vertical) {
                        StudioQuickChip(
                            label = vm.shapeKind.label,
                            active = vm.shapeKind != ShapeKind.AUTO,
                            accent = ProButtonPalette.ElectricBlue,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {
                            val quickKinds = listOf(
                                ShapeKind.AUTO,
                                ShapeKind.LINE,
                                ShapeKind.ARROW,
                                ShapeKind.RECT,
                                ShapeKind.ELLIPSE,
                                ShapeKind.TRIANGLE,
                            )
                            val idx = quickKinds.indexOf(vm.shapeKind)
                            vm.shapeKind = quickKinds[(idx + 1) % quickKinds.size]
                        }
                        if (vm.shapeKind.closed || vm.shapeKind == ShapeKind.AUTO) {
                            StudioQuickChip(
                                label = if (vm.shapeFillOn) "Fill: On" else "Fill",
                                active = vm.shapeFillOn,
                                accent = ProButtonPalette.ElectricBlue,
                                fg = dockFg,
                                darkDock = darkDock,
                            ) {
                                vm.shapeFillOn = !vm.shapeFillOn
                            }
                        }
                    }
                    StyleOptions(vm, t, style, dockFg, darkDock, vertical)
                }
                style != null -> StyleOptions(vm, t, style, dockFg, darkDock, vertical)
                t == Tool.ERASER -> {
                    SizeDots(listOf(8f, 16f, 30f), listOf(8f, 16f, 30f)[vm.eraserSize], ring = true, fg = dockFg, darkDock = darkDock) { s ->
                        vm.eraserCustomDp = null
                        vm.eraserSize = listOf(8f, 16f, 30f).indexOf(s).coerceIn(0, 2)
                    }
                    if (!vertical) {
                        StudioQuickChip(
                            label = if (settings.eraserMode == EraserMode.PIXEL) "Pixel" else "Stroke",
                            active = settings.eraserMode == EraserMode.PIXEL,
                            accent = ProButtonPalette.ElectricBlue,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {
                            vm.notebookSettings.update {
                                it.copy(eraserMode = if (it.eraserMode == EraserMode.PIXEL) EraserMode.STROKE else EraserMode.PIXEL)
                            }
                        }
                        StudioQuickChip(
                            label = "HL only",
                            active = settings.eraseHighlighterOnly,
                            accent = ProButtonPalette.VibrantOrange,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {
                            vm.notebookSettings.update { it.copy(eraseHighlighterOnly = !it.eraseHighlighterOnly) }
                        }
                    }
                }
                t == Tool.LASSO -> {
                    if (!vertical) {
                        StudioQuickChip(
                            label = if (settings.lassoMode == LassoMode.RECT) "Box" else "Freeform",
                            active = true,
                            accent = ProButtonPalette.ElectricBlue,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {
                            vm.notebookSettings.update {
                                it.copy(lassoMode = if (it.lassoMode == LassoMode.RECT) LassoMode.FREEFORM else LassoMode.RECT)
                            }
                        }
                        StudioQuickChip(
                            label = if (settings.lassoTouching) "Touch" else "Inside",
                            active = settings.lassoTouching,
                            accent = ProButtonPalette.Emerald,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {
                            vm.notebookSettings.update { it.copy(lassoTouching = !it.lassoTouching) }
                        }
                    } else {
                        StudioQuickChip(
                            label = if (settings.lassoMode == LassoMode.RECT) "Box" else "Loop",
                            active = true,
                            accent = ProButtonPalette.ElectricBlue,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) {
                            vm.notebookSettings.update {
                                it.copy(lassoMode = if (it.lassoMode == LassoMode.RECT) LassoMode.FREEFORM else LassoMode.RECT)
                            }
                        }
                    }
                }
                t == Tool.TEXT -> {
                    SizeDots(listOf(28f, 40f, 64f), vm.textSize, fg = dockFg, darkDock = darkDock) { vm.textSize = it }
                    if (!vertical) {
                        Box(Modifier.padding(horizontal = 3.dp).width(1.dp).height(18.dp).background(dockFg.copy(alpha = 0.14f)))
                        CustomSwatches(PenPalette.take(4), vm.textColor ?: PenPalette.first(), vm) { vm.textColor = it; vm.pushRecent(it) }
                    }
                }
                t == Tool.TAPE -> {
                    CustomSwatches(TapeColors, vm.tapeColor, vm) { vm.tapeColor = it }
                    if (!vertical) {
                        StudioQuickChip(
                            label = if (vm.tapeFreehand) "Freehand" else "Straight",
                            active = vm.tapeFreehand,
                            accent = ProButtonPalette.ElectricBlue,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) { vm.tapeFreehand = !vm.tapeFreehand }
                    }
                }
                t == Tool.STICKY -> CustomSwatches(StickyColors, vm.stickyColor, vm) { vm.stickyColor = it }
                t == Tool.WEBLINK -> CustomSwatches(WebLinkColors, vm.webLinkColor, vm) { vm.webLinkColor = it }
                t == Tool.MINDMAP -> {
                    MindPalette.take(5).forEach { c ->
                        Swatch(c, c == vm.mindColor, 18) { vm.recolorMindNode(color = c) }
                    }
                    if (!vertical) {
                        EditorViewModel.MindLayout.entries.forEach { layout ->
                            StudioQuickChip(layout.label, active = false, accent = ProButtonPalette.ElectricBlue, fg = dockFg, darkDock = darkDock) {
                                vm.autoLayoutMindMap(layout)
                            }
                        }
                        StudioQuickChip("Fit Map", active = false, accent = ProButtonPalette.ElectricBlue, fg = dockFg, darkDock = darkDock) {
                            vm.fitMindMapToView()
                        }
                        StudioQuickChip(
                            if (vm.moveMindSubtree) "Subtree" else "Single",
                            active = vm.moveMindSubtree,
                            accent = ProButtonPalette.Emerald,
                            fg = dockFg,
                            darkDock = darkDock,
                        ) { vm.moveMindSubtree = !vm.moveMindSubtree }
                        StudioQuickChip("Cards", active = false, accent = ProButtonPalette.Violet, fg = dockFg, darkDock = darkDock) {
                            vm.flashcardsFromMindMap()
                        }
                    }
                }
                else -> Unit
            }
        }
        if (vertical) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) { content() }
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { content() }
    }
}

@Composable
private fun StudioQuickChip(
    label: String,
    active: Boolean,
    accent: Color,
    fg: Color,
    darkDock: Boolean,
    onClick: () -> Unit,
) {
    val safeAccent = when {
        darkDock && accent.luminance() < 0.08f -> Color(0xFF93C5FD)
        !darkDock && accent.luminance() > 0.85f -> ProButtonPalette.ElectricBlue
        else -> accent
    }
    val bg = when {
        active && darkDock -> safeAccent.copy(alpha = 0.20f)
        active -> safeAccent.copy(alpha = 0.12f)
        darkDock -> Color.White.copy(alpha = 0.06f)
        else -> Color(0xFF0F172A).copy(alpha = 0.045f)
    }
    val border = when {
        active -> safeAccent.copy(alpha = if (darkDock) 0.55f else 0.42f)
        darkDock -> Color.White.copy(alpha = 0.10f)
        else -> Color(0xFF0F172A).copy(alpha = 0.09f)
    }
    val textColor = if (active) {
        if (darkDock) Color.White else safeAccent
    } else {
        fg.copy(alpha = 0.85f)
    }
    val chipInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 36.dp)
            .tactilePressScale(chipInteraction, pressedScale = 0.93f)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = chipInteraction,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
        )
    }
}

@Composable
fun CustomSwatches(base: List<Int>, current: Int, vm: EditorViewModel, onPick: (Int) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    base.distinct().forEach { c -> Swatch(c, c == current, 18) { onPick(c); vm.pushRecent(c) } }
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
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
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
        Box(Modifier.padding(vertical = 3.dp).height(1.dp).width(22.dp).background(dockFg.copy(alpha = 0.14f)))
    } else {
        Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(18.dp).background(dockFg.copy(alpha = 0.14f)))
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

    val presets = when (t) {
        Tool.HIGHLIGHTER -> HighlighterPalette + PenPalette
        Tool.CRAYON -> CrayonPalette + PenPalette
        else -> PenPalette
    }
    editing?.let { slot ->
        ColorEditorDialog(style.colors.getOrNull(slot) ?: style.color, presets.distinct(), vm.recentColors(), onDismiss = { editing = null }) { picked ->
            val latest = vm.styleFor(t) ?: return@ColorEditorDialog
            if (slot !in latest.colors.indices) { editing = null; return@ColorEditorDialog }
            val colors = latest.colors.toMutableList().also { it[slot] = picked }
            vm.updateStyle(t, latest.copy(colors = colors, color = picked))
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

/**
 * Graduated studio weight selectors: shows proportional angled ink strokes for drawing tools
 * and concentric diameter rings for the eraser.
 */
@Composable
fun SizeDots(
    sizes: List<Float>,
    current: Float,
    ring: Boolean = false,
    fg: Color = MaterialTheme.colorScheme.onSurface,
    darkDock: Boolean = false,
    onPick: (Float) -> Unit,
) {
    val activeBg = WorkbenchDarkElevated
    val activeAccent = Color(0xFF93C5FD)
    val activeBorder = activeAccent.copy(alpha = 0.65f)
    sizes.forEachIndexed { i, s ->
        val selected = s == current
        val dotInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .padding(horizontal = 1.dp)
                .size(38.dp)
                .tactilePressScale(dotInteraction, pressedScale = 0.90f)
                .clip(RoundedCornerShape(11.dp))
                .background(if (selected) activeBg else Color.Transparent)
                .then(
                    if (selected) Modifier.border(1.2.dp, activeBorder, RoundedCornerShape(11.dp))
                    else Modifier,
                )
                .clickable(
                    interactionSource = dotInteraction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    role = Role.Button,
                    onClickLabel = "Size ${s.toInt()}",
                ) { onPick(s) },
            contentAlignment = Alignment.Center,
        ) {
            if (ring) {
                Box(Modifier.size((8 + 5 * i).dp).border(1.6.dp, if (selected) activeAccent else fg.copy(alpha = 0.78f), CircleShape))
            } else {
                val strokeColor = if (selected) activeAccent else fg.copy(alpha = 0.80f)
                val strokeDp = (1.8f + 1.8f * i).dp
                Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                    drawLine(
                        color = strokeColor,
                        start = Offset(size.width * 0.12f, size.height * 0.84f),
                        end = Offset(size.width * 0.88f, size.height * 0.16f),
                        strokeWidth = strokeDp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
