package com.pronotes.app.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.FlipToFront
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pronotes.app.R
import com.pronotes.app.ui.AppContextMenu
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.LocalReduceEffects
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.ToolGlyph
import com.pronotes.app.ui.appPillShape
import kotlin.math.roundToInt

/** Target page/screen location for the empty-page Quick Insert popover. */
@Immutable
data class QuickInsertRequest(
    val page: Int,
    val x: Float,
    val y: Float,
    val screenX: Float,
    val screenY: Float,
)

/**
 * Floating contextual action bar anchored to the current Selection (Lasso) bounds.
 * Keeps every touch target at least 48 dp and supports keyboard navigation and ink-to-text conversion.
 */
@Composable
fun SelectionMenu(
    vm: EditorViewModel,
    areaSize: IntSize,
    onDuplicate: () -> Unit,
    onColor: (Int) -> Unit,
    keep: (() -> Unit) -> Unit,
    onShareImage: () -> Unit,
    onConvertToText: (Long) -> Unit = {},
) {
    var colors by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val fg = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current.density
    val reduceEffects = LocalReduceEffects.current
    val maxMenuW = if (areaSize.width > 0) (areaSize.width / density - 24f).coerceAtLeast(220f).dp else 560.dp

    val cutLabel = stringResource(R.string.selection_cut)
    val copyLabel = stringResource(R.string.selection_copy)
    val dupLabel = stringResource(R.string.selection_duplicate)
    val tintLabel = stringResource(R.string.selection_palette_tint)
    val colourLabel = stringResource(R.string.selection_colour)
    val rotateLabel = stringResource(R.string.selection_rotate)
    val moreLabel = stringResource(R.string.selection_more)
    val deleteLabel = stringResource(R.string.selection_delete)

    AppSurface(
        role = SurfaceRole.Popover,
        shape = appPillShape(),
        shadowElevation = if (reduceEffects) 0.dp else 10.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        modifier = Modifier.widthIn(max = maxMenuW),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Row(
                Modifier
                    .padding(horizontal = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = { vm.startLinkFromSelection() },
                    shape = appPillShape(),
                    color = ProButtonPalette.ElectricBlue,
                    contentColor = Color.White,
                    modifier = Modifier.padding(2.dp).defaultMinSize(minHeight = 48.dp),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Link, contentDescription = null, Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.selection_link), style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
                if (vm.selectionLinks().isNotEmpty()) {
                    Surface(
                        onClick = { vm.unlinkSelection() },
                        shape = appPillShape(),
                        color = Color(0xFFE5484D),
                        contentColor = Color.White,
                        modifier = Modifier.padding(2.dp).defaultMinSize(minHeight = 48.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.selection_unlink),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                }
                SelectionActionIcon(cutLabel, vm::cutSelection) {
                    Icon(Icons.Outlined.ContentCut, contentDescription = cutLabel)
                }
                SelectionActionIcon(copyLabel, vm::copySelection) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = copyLabel)
                }
                SelectionActionIcon(dupLabel, onDuplicate) {
                    ProIconView(ProIcon.ADD, fg, Modifier.size(22.dp))
                }
                SelectionActionIcon(tintLabel, { vm.rainbowSelection() }) {
                    Icon(Icons.Outlined.ColorLens, contentDescription = tintLabel, tint = ProButtonPalette.MagentaPink)
                }
                SelectionActionIcon(colourLabel, { colors = true }) {
                    Icon(Icons.Outlined.Palette, contentDescription = colourLabel)
                }
                SelectionActionIcon(rotateLabel, { keep { vm.rotateSelection(90f) } }) {
                    Icon(Icons.Outlined.RotateRight, contentDescription = rotateLabel)
                }
                Box {
                    SelectionActionIcon(moreLabel, { moreOpen = true }) {
                        ProIconView(ProIcon.MORE, fg, Modifier.size(22.dp))
                    }
                    AppContextMenu(
                        expanded = moreOpen,
                        onDismissRequest = { moreOpen = false },
                        items = listOf(
                            AppMenuItem(
                                stringResource(R.string.sheet_lasso_convert),
                                icon = { Icon(Icons.Outlined.TextFields, contentDescription = null, Modifier.size(18.dp)) },
                            ) {
                                vm.convertSelectionToText()?.let(onConvertToText)
                            },
                            AppMenuItem("Flip horizontally", icon = { Icon(Icons.Outlined.Flip, contentDescription = null, Modifier.size(18.dp)) }) {
                                keep { vm.flipSelection() }
                            },
                            AppMenuItem("Bring to front", icon = { Icon(Icons.Outlined.FlipToFront, contentDescription = null, Modifier.size(18.dp)) }) {
                                keep { vm.arrange(true) }
                            },
                            AppMenuItem("Send to back", icon = { Icon(Icons.Outlined.FlipToBack, contentDescription = null, Modifier.size(18.dp)) }) {
                                keep { vm.arrange(false) }
                            },
                            AppMenuItem("Share as image", icon = { Icon(Icons.Outlined.IosShare, contentDescription = null, Modifier.size(18.dp)) }) {
                                onShareImage()
                            },
                            AppMenuItem("Make flashcard", icon = { Icon(Icons.Outlined.Style, contentDescription = null, Modifier.size(18.dp)) }) {
                                vm.cardFromSelection()
                            },
                            AppMenuItem("Add link…", icon = { Icon(Icons.Outlined.Link, contentDescription = null, Modifier.size(18.dp)) }) {
                                vm.startLinkFromSelection()
                            },
                            AppMenuItem("Save as screenshot", icon = { Icon(Icons.Outlined.Image, contentDescription = null, Modifier.size(18.dp)) }) {
                                vm.saveSelectionScreenshot()
                            },
                        ),
                    )
                }
                SelectionActionIcon(deleteLabel, vm::deleteSelection) {
                    Icon(Icons.Outlined.Delete, contentDescription = deleteLabel, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (colors) {
        ColorEditorDialog(0xFF1B1F2A.toInt(), PenPalette, vm.recentColors(), onDismiss = { colors = false }) { picked ->
            onColor(picked)
            colors = false
        }
    }
}

@Composable
private fun SelectionActionIcon(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .focusable()
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Compact anchored Quick Actions popup triggered by a stylus side/hover button click.
 * Uses a native Compose [Popup] anchored to the pointer coordinates with no full-screen dim or blur.
 */
@Composable
fun QuickActionsOverlay(
    vm: EditorViewModel,
    at: Offset,
    areaSize: IntSize,
    onOpenToolSettings: (Tool) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current.density
    val reduceMotion = LocalReduceMotion.current
    val reduceEffects = LocalReduceEffects.current
    val tools = remember {
        listOf(
            Tool.PEN,
            Tool.HIGHLIGHTER,
            Tool.ERASER,
            Tool.LASSO,
            Tool.SHAPE,
            Tool.MINDMAP,
            Tool.TEXT,
            Tool.LASER,
            Tool.TAPE,
        )
    }
    val appear = remember { Animatable(if (reduceMotion) 1f else 0.86f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            appear.snapTo(1f)
        } else {
            appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 480f))
        }
    }

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val estW = if (cardSize.width > 0) cardSize.width else (268f * density).roundToInt()
    val estH = if (cardSize.height > 0) cardSize.height else (296f * density).roundToInt()
    val marginPx = (12f * density).roundToInt()
    val clamped = remember(at, areaSize, estW, estH, marginPx) {
        ToolbarGeometry.clampFloatingOffset(
            desiredX = (at.x - estW / 2f).roundToInt(),
            desiredY = (at.y - estH / 3f).roundToInt(),
            pillSize = IntSize(estW, estH),
            areaSize = areaSize,
            marginPx = marginPx,
        )
    }

    val undoDesc = stringResource(R.string.toolbar_undo)
    val redoDesc = stringResource(R.string.toolbar_redo)
    val prevDesc = stringResource(R.string.quick_actions_previous_tool)
    val settingsLabel = stringResource(R.string.quick_actions_tool_settings)

    Popup(
        alignment = Alignment.TopStart,
        offset = clamped,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        AppSurface(
            role = SurfaceRole.Popover,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = if (reduceEffects) 0.dp else 14.dp,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier
                .width(268.dp)
                .onSizeChanged { cardSize = it }
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                    alpha = appear.value.coerceIn(0f, 1f)
                }
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header: Title + History & Swap actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        stringResource(R.string.quick_actions_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    QuickHeaderIcon(
                        label = undoDesc,
                        enabled = vm.canUndo,
                        onClick = {
                            vm.undo()
                            onDismiss()
                        },
                    ) {
                        ProIconView(
                            ProIcon.UNDO,
                            if (vm.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                            Modifier.size(18.dp),
                        )
                    }
                    QuickHeaderIcon(
                        label = redoDesc,
                        enabled = vm.canRedo,
                        onClick = {
                            vm.redo()
                            onDismiss()
                        },
                    ) {
                        ProIconView(
                            ProIcon.REDO,
                            if (vm.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                            Modifier.size(18.dp),
                        )
                    }
                    QuickHeaderIcon(
                        label = prevDesc,
                        enabled = true,
                        onClick = {
                            vm.swapToPrevious()
                            onDismiss()
                        },
                    ) {
                        Icon(Icons.Outlined.SwapHoriz, contentDescription = prevDesc, modifier = Modifier.size(18.dp))
                    }
                }

                // 3x3 Tool Matrix
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tools.chunked(3).forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            rowTools.forEach { t ->
                                val sel = vm.tool == t
                                val fullLabel = toolDisplayName(t)
                                val shortName = shortToolLabel(t)
                                val ink = Color(vm.styleFor(t)?.color ?: vm.penStyle.color)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (sel) ProButtonPalette.ElectricBlue
                                            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                                        )
                                        .border(
                                            width = if (sel) 1.2.dp else 0.75.dp,
                                            color = if (sel) Color.White.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                            shape = RoundedCornerShape(14.dp),
                                        )
                                        .semantics { contentDescription = fullLabel }
                                        .focusable()
                                        .clickable(role = Role.Button, onClickLabel = fullLabel) {
                                            vm.tool = t
                                            onDismiss()
                                        }
                                        .padding(vertical = 7.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                                ) {
                                    ToolGlyph(
                                        tool = t,
                                        tint = if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                                        inkColor = ink,
                                        modifier = Modifier.size(20.dp),
                                        active = sel,
                                    )
                                    Text(
                                        text = shortName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                // Active tool quick ink colour row + Tool Settings trigger
                val activeStyle = vm.styleFor(vm.tool)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (activeStyle != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            activeStyle.colors.take(4).forEach { c ->
                                Swatch(c, c == activeStyle.color, 18) {
                                    vm.updateStyle(vm.tool, activeStyle.copy(color = c))
                                    onDismiss()
                                }
                            }
                        }
                    } else {
                        Text(
                            toolDisplayName(vm.tool),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button, onClickLabel = settingsLabel) {
                                val current = vm.tool
                                onDismiss()
                                onOpenToolSettings(current)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(15.dp), tint = ProButtonPalette.ElectricBlue)
                        Text(settingsLabel, style = MaterialTheme.typography.labelSmall, color = ProButtonPalette.ElectricBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickHeaderIcon(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .focusable()
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Long-press Quick Insert popover anchored near the long-pressed page coordinates
 * and clamped strictly within screen bounds.
 */
@Composable
fun QuickInsertOverlay(
    request: QuickInsertRequest,
    areaSize: IntSize,
    hasClipboard: Boolean,
    onDismiss: () -> Unit,
    onWriteBox: () -> Unit,
    onTextBox: () -> Unit,
    onSticky: () -> Unit,
    onWebLink: () -> Unit,
    onImage: () -> Unit,
    onSticker: () -> Unit,
    onPaste: () -> Unit,
) {
    val density = LocalDensity.current.density
    val reduceEffects = LocalReduceEffects.current
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    val estW = if (menuSize.width > 0) menuSize.width else (232f * density).roundToInt()
    val estH = if (menuSize.height > 0) menuSize.height else (340f * density).roundToInt()
    val marginPx = (12f * density).roundToInt()
    val clamped = remember(request, areaSize, estW, estH, marginPx) {
        ToolbarGeometry.clampFloatingOffset(
            desiredX = (request.screenX - estW / 2f).roundToInt(),
            desiredY = (request.screenY - estH / 3f).roundToInt(),
            pillSize = IntSize(estW, estH),
            areaSize = areaSize,
            marginPx = marginPx,
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
                    onDismiss()
                    true
                } else false
            }
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        AppSurface(
            role = SurfaceRole.Popover,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = if (reduceEffects) 0.dp else 14.dp,
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .offset { clamped }
                .width(232.dp)
                .onSizeChanged { menuSize = it },
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.quick_insert_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                QuickInsertRow(stringResource(R.string.quick_insert_write_box), {
                    ProIconView(ProIcon.WRITE_BOX, ProButtonPalette.ElectricBlue, Modifier.size(18.dp), ink = ProButtonPalette.ElectricBlue)
                }, onWriteBox)
                QuickInsertRow(stringResource(R.string.quick_insert_text_box), {
                    Icon(Icons.Outlined.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                }, onTextBox)
                QuickInsertRow(stringResource(R.string.quick_insert_sticky), {
                    Icon(Icons.Outlined.StickyNote2, contentDescription = null, modifier = Modifier.size(18.dp), tint = ProButtonPalette.WarmAmber)
                }, onSticky)
                QuickInsertRow(stringResource(R.string.quick_insert_link_preview), {
                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp), tint = ProButtonPalette.ElectricBlue)
                }, onWebLink)
                QuickInsertRow(stringResource(R.string.quick_insert_image), {
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                }, onImage)
                QuickInsertRow(stringResource(R.string.quick_insert_sticker), {
                    ProIconView(ProIcon.STICKER, ProButtonPalette.MagentaPink, Modifier.size(18.dp), ink = ProButtonPalette.MagentaPink)
                }, onSticker)
                if (hasClipboard) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    QuickInsertRow(stringResource(R.string.quick_insert_paste), {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp), tint = ProButtonPalette.Emerald)
                    }, onPaste)
                }
            }
        }
    }
}

@Composable
private fun QuickInsertRow(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .focusable()
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
