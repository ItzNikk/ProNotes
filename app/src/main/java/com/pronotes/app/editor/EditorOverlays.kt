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
import androidx.compose.material.icons.outlined.TextFields
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
import com.pronotes.app.ui.ToolGlyph
import com.pronotes.app.ui.appPillShape
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
 * Radial Quick Actions popup triggered by pen button click, clamped inside window bounds
 * with 48 dp+ targets and reduced-motion support.
 */
@Composable
fun QuickActionsOverlay(
    vm: EditorViewModel,
    at: Offset,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current.density
    val reduceMotion = LocalReduceMotion.current
    val reduceEffects = LocalReduceEffects.current
    val tools = remember {
        listOf(Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO, Tool.SHAPE, Tool.MINDMAP, Tool.TEXT, Tool.LASER, Tool.TAPE)
    }
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            appear.snapTo(1f)
        } else {
            appear.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = 420f))
        }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val closeDesc = stringResource(R.string.quick_actions_close)
    val undoDesc = stringResource(R.string.toolbar_undo)
    val redoDesc = stringResource(R.string.toolbar_redo)

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .background(Color.Black.copy(alpha = 0.16f * appear.value))
            .onPreviewKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
                    onDismiss()
                    true
                } else false
            }
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        val radius = 104f * density
        val safePad = 48f * density
        val cx = at.x.coerceIn(radius + safePad, (size.width - radius - safePad).coerceAtLeast(radius + safePad))
        val cy = at.y.coerceIn(radius + safePad, (size.height - radius - safePad).coerceAtLeast(radius + safePad))

        // Center hub: Undo, Close, Redo cluster
        AppSurface(
            role = SurfaceRole.Popover,
            shape = CircleShape,
            shadowElevation = if (reduceEffects) 0.dp else 10.dp,
            modifier = Modifier
                .offset { IntOffset((cx - 34f * density).roundToInt(), (cy - 34f * density).roundToInt()) }
                .size(68.dp)
                .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
                .semantics { contentDescription = closeDesc }
                .focusable()
                .clickable(role = Role.Button, onClickLabel = closeDesc, onClick = onDismiss),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ProIconView(ProIcon.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
            }
        }

        // Quick Undo/Redo flank below center
        Row(
            modifier = Modifier
                .offset { IntOffset((cx - 56f * density).roundToInt(), (cy + radius + 34f * density).roundToInt()) }
                .graphicsLayer { alpha = appear.value },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppSurface(
                role = SurfaceRole.Chrome,
                shape = CircleShape,
                shadowElevation = if (reduceEffects) 0.dp else 6.dp,
                onClick = { if (vm.canUndo) vm.undo(); onDismiss() },
                onClickLabel = undoDesc,
                enabled = vm.canUndo,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                contentPadding = PaddingValues(12.dp),
            ) {
                ProIconView(
                    ProIcon.UNDO,
                    if (vm.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    Modifier.size(20.dp),
                )
            }
            AppSurface(
                role = SurfaceRole.Chrome,
                shape = CircleShape,
                shadowElevation = if (reduceEffects) 0.dp else 6.dp,
                onClick = { if (vm.canRedo) vm.redo(); onDismiss() },
                onClickLabel = redoDesc,
                enabled = vm.canRedo,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                contentPadding = PaddingValues(12.dp),
            ) {
                ProIconView(
                    ProIcon.REDO,
                    if (vm.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    Modifier.size(20.dp),
                )
            }
        }

        tools.forEachIndexed { i, t ->
            val a = -Math.PI / 2 + i * 2 * Math.PI / tools.size
            val r = radius * appear.value
            val x = cx + (r * cos(a)).toFloat()
            val y = cy + (r * sin(a)).toFloat()
            val sel = vm.tool == t
            val label = toolDisplayName(t)
            Box(
                Modifier
                    .offset { IntOffset((x - 26f * density).roundToInt(), (y - 26f * density).roundToInt()) }
                    .size(52.dp)
                    .then(if (reduceEffects) Modifier else Modifier.shadow(6.dp, CircleShape))
                    .clip(CircleShape)
                    .background(if (sel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.surface)
                    .border(
                        width = if (sel) 1.5.dp else 1.dp,
                        color = if (sel) Color.White else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .semantics { contentDescription = label }
                    .focusable()
                    .clickable(role = Role.Button, onClickLabel = label) { vm.tool = t; onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                ToolGlyph(
                    t,
                    if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                    Color(vm.styleFor(t)?.color ?: vm.penStyle.color),
                    Modifier.size(24.dp),
                    active = sel,
                )
            }
        }
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
