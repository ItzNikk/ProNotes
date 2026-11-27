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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ControlPointDuplicate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.FlipToFront
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
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
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pronotes.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TextButton
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppPill
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
import com.pronotes.app.ui.tactilePressScale
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
    transformEnabled: Boolean = false,
    onToggleTransform: () -> Unit = {},
) {
    var colors by remember { mutableStateOf(false) }
    var fontPickerOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember(vm) { mutableStateOf<Pair<String, Set<Long>>?>(null) }
    pendingDelete?.let { (pageId, ids) ->
        ConfirmEditorDeletion(
            title = "Delete selection?",
            message = "The selected items will be removed. You can bring them back with Undo.",
            onDismiss = { pendingDelete = null },
            onDelete = { vm.deletePageItems(pageId, ids) },
        )
    }
    val fg = MaterialTheme.colorScheme.onSurface
    val mutedFg = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current.density
    val reduceEffects = LocalReduceEffects.current
    val maxMenuW = if (areaSize.width > 0) (areaSize.width / density - 24f).coerceAtLeast(220f).dp else 560.dp

    val cutLabel = stringResource(R.string.selection_cut)
    val copyLabel = stringResource(R.string.selection_copy)
    val dupLabel = stringResource(R.string.selection_duplicate)
    val colourLabel = stringResource(R.string.selection_colour)
    val moreLabel = stringResource(R.string.selection_more)
    val deleteLabel = stringResource(R.string.selection_delete)
    val editableText = vm.selectedTextItem()
    val hasInkSelected = remember(vm.selection, vm.selectionPage, vm.pages) {
        vm.pages.getOrNull(vm.selectionPage)?.items?.any { it.id in vm.selection && it is InkItem } == true
    }
    val hasClipboard = vm.hasAnyClipboardContent()

    AppSurface(
        role = SurfaceRole.Chrome,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        contentColor = fg,
        shape = appPillShape(),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        shadowElevation = if (reduceEffects) 0.dp else 10.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        modifier = Modifier.widthIn(max = maxMenuW),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (editableText != null) {
                    SelectionActionIcon(
                        label = "Edit text",
                        onClick = { vm.openInlineTextBox(vm.selectionPage, editableText.x, editableText.y, editableText) },
                    ) {
                        Icon(Icons.Outlined.Keyboard, contentDescription = null, Modifier.size(20.dp), tint = ProButtonPalette.Emerald)
                    }
                }
                SelectionActionIcon(
                    label = if (transformEnabled) "Done resizing" else "Resize & rotate",
                    onClick = onToggleTransform,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (transformEnabled) ProButtonPalette.ElectricBlue else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        com.pronotes.app.ui.TransformSelectionIcon(
                            tint = if (transformEnabled) Color.White else fg,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                SelectionActionIcon(label = cutLabel, onClick = vm::cutSelection) {
                    Icon(Icons.Outlined.ContentCut, contentDescription = null, Modifier.size(19.dp), tint = fg)
                }
                SelectionActionIcon(label = copyLabel, onClick = vm::copySelection) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, Modifier.size(19.dp), tint = fg)
                }
                if (hasClipboard) {
                    SelectionActionIcon(
                        label = "Paste",
                        onClick = {
                            val r = vm.selectionUnion()
                            val px = (r?.right ?: 160f) + 24f
                            val py = r?.top ?: 160f
                            vm.pasteAt(vm.selectionPage, px, py) { _, _ -> }
                        },
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null, Modifier.size(19.dp), tint = fg)
                    }
                }
                SelectionActionIcon(label = dupLabel, onClick = onDuplicate) {
                    Icon(Icons.Outlined.ControlPointDuplicate, contentDescription = null, Modifier.size(19.dp), tint = fg)
                }
                Box(Modifier.padding(horizontal = 2.dp).width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                SelectionActionIcon(label = colourLabel, onClick = { colors = true }) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, Modifier.size(19.dp), tint = fg)
                }
                SelectionActionIcon(label = "Font", onClick = { fontPickerOpen = true }) {
                    Icon(Icons.Outlined.FontDownload, contentDescription = null, Modifier.size(19.dp), tint = ProButtonPalette.ElectricBlue)
                }
                if (hasInkSelected) {
                    SelectionActionIcon(label = "Convert to text", onClick = { vm.convertSelectionToText(onConvertToText) }) {
                        Icon(Icons.Outlined.TextFields, contentDescription = null, Modifier.size(19.dp), tint = ProButtonPalette.ElectricBlue)
                    }
                }
                SelectionActionIcon(label = "Save as sticker", onClick = { vm.saveSelectionAsSticker() }) {
                    ProIconView(ProIcon.STICKER, ProButtonPalette.MagentaPink, Modifier.size(19.dp), ink = ProButtonPalette.MagentaPink)
                }
                SelectionActionIcon(label = "Share", onClick = onShareImage) {
                    Icon(Icons.Outlined.IosShare, contentDescription = null, Modifier.size(19.dp), tint = fg)
                }
                SelectionActionIcon(label = stringResource(R.string.selection_link), onClick = { vm.startLinkFromSelection() }) {
                    Icon(Icons.Outlined.Link, contentDescription = null, Modifier.size(19.dp), tint = fg)
                }
                Box {
                    SelectionActionIcon(label = moreLabel, onClick = { moreOpen = true }) {
                        ProIconView(ProIcon.MORE, mutedFg, Modifier.size(19.dp))
                    }
                    AppContextMenu(
                        expanded = moreOpen,
                        onDismissRequest = { moreOpen = false },
                        items = buildList {
                            if (vm.selectionLinks().isNotEmpty()) {
                                add(
                                    AppMenuItem(
                                        stringResource(R.string.selection_unlink),
                                        destructive = true,
                                    ) {
                                        vm.unlinkSelection()
                                    },
                                )
                            }
                            add(
                                AppMenuItem(
                                    "Save as sticker",
                                    icon = { ProIconView(ProIcon.STICKER, ProButtonPalette.MagentaPink, Modifier.size(18.dp), ink = ProButtonPalette.MagentaPink) },
                                ) {
                                    vm.saveSelectionAsSticker()
                                },
                            )
                            add(
                                AppMenuItem(
                                    "Copy handwriting as text",
                                    icon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, Modifier.size(18.dp)) },
                                ) {
                                    vm.copySelectionAsText()
                                },
                            )
                            add(
                                AppMenuItem(
                                    stringResource(R.string.sheet_lasso_convert),
                                    icon = { Icon(Icons.Outlined.TextFields, contentDescription = null, Modifier.size(18.dp)) },
                                ) {
                                    vm.convertSelectionToText(onConvertToText)
                                },
                            )
                            add(AppMenuItem("Add attached comment") { vm.addSelectionComment() })
                            add(
                                AppMenuItem("Flip horizontally", icon = { Icon(Icons.Outlined.Flip, contentDescription = null, Modifier.size(18.dp)) }) {
                                    keep { vm.flipSelection() }
                                },
                            )
                            add(
                                AppMenuItem("Bring to front", icon = { Icon(Icons.Outlined.FlipToFront, contentDescription = null, Modifier.size(18.dp)) }) {
                                    keep { vm.arrange(true) }
                                },
                            )
                            add(
                                AppMenuItem("Send to back", icon = { Icon(Icons.Outlined.FlipToBack, contentDescription = null, Modifier.size(18.dp)) }) {
                                    keep { vm.arrange(false) }
                                },
                            )
                            add(
                                AppMenuItem("Save as screenshot", icon = { Icon(Icons.Outlined.Image, contentDescription = null, Modifier.size(18.dp)) }) {
                                    vm.saveSelectionScreenshot()
                                },
                            )
                        },
                    )
                }
                Box(Modifier.padding(horizontal = 2.dp).width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                SelectionActionIcon(
                    label = deleteLabel,
                    onClick = {
                        vm.pages.getOrNull(vm.selectionPage)?.let { page ->
                            if (vm.selection.isNotEmpty()) pendingDelete = page.id to vm.selection.toSet()
                        }
                    },
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.error)
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
    if (fontPickerOpen) {
        LassoFontPickerDialog(
            vm = vm,
            keep = keep,
            onConvertToText = onConvertToText,
            onDismiss = { fontPickerOpen = false },
        )
    }
}

@Composable
internal fun ConfirmEditorDeletion(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onDelete()
            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LassoFontPickerDialog(
    vm: EditorViewModel,
    keep: (() -> Unit) -> Unit,
    onConvertToText: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val hasStylableText = vm.selectionHasStylableText()
    val (activeFont, activeCustomPath) = vm.selectionActiveFont()
    var customFonts by remember { mutableStateOf(CustomFontRegistry.listFonts(context)) }
    var showLinkImport by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf(FontGalleryCategory.ALL) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val imported = CustomFontRegistry.importFont(context, uri)
            if (imported != null) {
                customFonts = CustomFontRegistry.listFonts(context)
                if (hasStylableText) {
                    keep { vm.setSelectionFont(TextFont.SANS, imported.path, onConvertToText) }
                } else {
                    vm.setSelectionFont(TextFont.SANS, imported.path, onConvertToText)
                    onDismiss()
                }
            } else {
                vm.say("Could not import font file")
            }
        }
    }

    val activeCustom = remember(activeCustomPath, customFonts) {
        if (activeCustomPath.isBlank()) null else customFonts.firstOrNull { it.path == activeCustomPath }
    }
    val previewFamily = remember(activeFont, activeCustomPath) {
        val tf = if (activeCustomPath.isNotBlank()) CustomFontRegistry.loadTypeface(activeCustomPath) else null
        FontFamily(tf ?: activeFont.typeface)
    }
    val filteredFonts = remember(activeCategory) {
        if (activeCategory == FontGalleryCategory.ALL) TextFont.entries
        else TextFont.entries.filter { it.galleryCategory() == activeCategory }
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Handwriting & Font Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = if (hasStylableText) "Update typeface, weight, and scale on selected notes"
                    else "Choose a typeface to convert selected handwriting cleanly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Live Specimen Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ProButtonPalette.ElectricBlue.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ProButtonPalette.ElectricBlue.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            activeCustom?.displayName ?: activeFont.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ProButtonPalette.ElectricBlue,
                        )
                        Text(
                            "The quick brown fox jumps · 123",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = previewFamily),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (hasStylableText) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppPill(
                            onClick = { keep { vm.toggleSelectionBold() } },
                            tint = ProButtonPalette.ElectricBlue,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Outlined.FormatBold, contentDescription = "Bold", Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Bold", style = MaterialTheme.typography.labelLarge)
                        }
                        AppPill(
                            onClick = { keep { vm.toggleSelectionItalic() } },
                            tint = ProButtonPalette.Violet,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Outlined.FormatItalic, contentDescription = "Italic", Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Italic", style = MaterialTheme.typography.labelLarge)
                        }
                        AppPill(
                            onClick = { keep { vm.scaleSelectionFontSize(-4f) } },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("A−", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        AppPill(
                            onClick = { keep { vm.scaleSelectionFontSize(4f) } },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("A+", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }

                // Custom imported fonts section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Custom Fonts",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    AppPill(
                        onClick = { showLinkImport = true },
                        tint = ProButtonPalette.Violet,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("From link", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                    AppPill(
                        onClick = { importLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream", "*/*")) },
                        tint = ProButtonPalette.Emerald,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("File", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (showLinkImport) {
                    ImportFontFromLinkDialog(
                        onDismiss = { showLinkImport = false },
                        onImported = { imported ->
                            showLinkImport = false
                            customFonts = CustomFontRegistry.listFonts(context)
                            if (hasStylableText) {
                                keep { vm.setSelectionFont(TextFont.SANS, imported.path, onConvertToText) }
                            } else {
                                vm.setSelectionFont(TextFont.SANS, imported.path, onConvertToText)
                                onDismiss()
                            }
                        },
                    )
                }
                if (customFonts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        customFonts.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pair.forEach { cf ->
                                    val selected = activeCustomPath == cf.path
                                    val cfFamily = remember(cf.path) {
                                        CustomFontRegistry.loadTypeface(cf.path)?.let { FontFamily(it) } ?: FontFamily.SansSerif
                                    }
                                    Surface(
                                        onClick = {
                                            if (hasStylableText) {
                                                keep { vm.setSelectionFont(TextFont.SANS, cf.path, onConvertToText) }
                                            } else {
                                                vm.setSelectionFont(TextFont.SANS, cf.path, onConvertToText)
                                                onDismiss()
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) ProButtonPalette.Violet.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (selected) 1.5.dp else 0.8.dp,
                                            if (selected) ProButtonPalette.Violet else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                        ),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                            Text(
                                                text = cf.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = cfFamily),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) ProButtonPalette.Violet else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "Aa Bb 123",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = cfFamily),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Category tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FontGalleryCategory.entries.forEach { cat ->
                        com.pronotes.app.ui.AppChip(
                            selected = activeCategory == cat,
                            onClick = { activeCategory = cat },
                            label = cat.label,
                            tint = ProButtonPalette.ElectricBlue,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredFonts.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { font ->
                                val selected = activeCustomPath.isBlank() && activeFont == font
                                val family = remember(font) { FontFamily(font.typeface) }
                                Surface(
                                    onClick = {
                                        if (hasStylableText) {
                                            keep { vm.setSelectionFont(font, "", onConvertToText) }
                                        } else {
                                            vm.setSelectionFont(font, "", onConvertToText)
                                            onDismiss()
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) ProButtonPalette.ElectricBlue.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (selected) 1.5.dp else 0.8.dp,
                                        if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                        Text(
                                            text = font.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = family),
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "Aa Bb 123",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = family),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun SelectionActionIcon(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .tactilePressScale(interaction, pressedScale = 0.90f)
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .focusable()
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private enum class QuickArcMode { ACTIONS, COLORS, SIZES, NIBS, MORE_TOOLS }

/**
 * Curved dual-arc Quick Actions palette anchored right beside the stylus tip.
 * Uses a transparent native Compose [Popup] with zero screen dim or canvas blur:
 * - Center hub pill: shows active tool + nib/colour at a glance (tap to close).
 * - Inner primary arc: Undo, Redo (stays open for rapid multi-step undo/redo), Pen, Eraser,
 *   Highlighter, Selection, separator, Thickness, and Active Colour dot.
 * - Outer concentric arc: More tools (Shapes, Text, Sticky, Tape, Table, Live Web Page),
 *   Pen Nib switcher, Tool settings, Auto-Text, and Page Bookmark toggle — dynamically expanding
 *   into circular Ink Colour swatches, Stroke Thickness dots, Pen Nibs, or extra tools.
 */
@Composable
fun QuickActionsOverlay(
    vm: EditorViewModel,
    at: Offset,
    areaSize: IntSize,
    onOpenToolSettings: (Tool) -> Unit,
    onAddTable: () -> Unit = {},
    onAddWebPage: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current.density
    val reduceMotion = LocalReduceMotion.current
    val reduceEffects = LocalReduceEffects.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var outerMode by remember { mutableStateOf(QuickArcMode.ACTIONS) }
    var openCustomColor by remember { mutableStateOf(false) }

    val appear = remember { Animatable(if (reduceMotion) 1f else 0.78f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            appear.snapTo(1f)
        } else {
            appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 560f))
        }
    }

    // Mirror across the vertical axis when the stylus is close to the left screen edge so the arcs stay clear.
    val mirrorToRight = remember(at, areaSize, density) {
        areaSize.width > 0 && at.x < 236f * density
    }

    val canvasW = 368.dp
    val canvasH = 332.dp
    val canvasWPx = (368f * density).roundToInt()
    val canvasHPx = (332f * density).roundToInt()

    // Pivot inside the popup box where the stylus tip rests inside the open curve of the arcs.
    val pivotX = if (mirrorToRight) 114f * density else 254f * density
    val pivotY = 230f * density

    val marginPx = (8f * density).roundToInt()
    val popupOffset = remember(at, areaSize, pivotX, pivotY, canvasWPx, canvasHPx, marginPx) {
        ToolbarGeometry.clampFloatingOffset(
            desiredX = (at.x - pivotX).roundToInt(),
            desiredY = (at.y - pivotY).roundToInt(),
            pillSize = IntSize(canvasWPx, canvasHPx),
            areaSize = areaSize,
            marginPx = marginPx,
        )
    }

    val innerRadiusPx = 138f * density
    val innerBandPx = 50f * density
    val outerRadiusPx = 200f * density
    val outerBandPx = 46f * density

    // Map canonical left-curving angle (158°..302°) to mirrored right-curving angle when near left edge.
    fun mapAngle(deg: Float): Float = if (mirrorToRight) 540f - deg else deg

    fun arcRange(startDeg: Float, endDeg: Float): Pair<Float, Float> {
        val a = mapAngle(startDeg)
        val b = mapAngle(endDeg)
        return if (a <= b) a to b else b to a
    }

    fun polarPos(radiusPx: Float, deg: Float): Offset {
        val rad = Math.toRadians(mapAngle(deg).toDouble())
        return Offset(
            x = pivotX + (radiusPx * kotlin.math.cos(rad)).toFloat(),
            y = pivotY + (radiusPx * kotlin.math.sin(rad)).toFloat(),
        )
    }

    val innerStart = 158f
    val innerEnd = 302f
    val (innerDrawStart, innerDrawEnd) = arcRange(innerStart, innerEnd)

    val activeStyle = vm.styleFor(vm.tool) ?: vm.penStyle
    val activeColor = Color(activeStyle.color)
    val paletteColors = remember(activeStyle.colors) { activeStyle.colors.take(7) }
    val sizePresets = remember(activeStyle.sizes) {
        val base = activeStyle.sizes.ifEmpty { listOf(2f, 4f, 8f, 14f) }
        base.take(4)
    }
    val quickNibs = remember {
        listOf(
            PenType.FOUNTAIN to "Fnt",
            PenType.BALL to "Ball",
            PenType.FINELINER to "Fine",
            PenType.BRUSH to "Brsh",
            PenType.PENCIL to "Pcl",
            PenType.CALLIGRAPHY to "Call",
        )
    }
    val extraTools = remember {
        listOf(Tool.SHAPE, Tool.TEXT, Tool.STICKY, Tool.TAPE)
    }
    val isBookmarked = vm.currentPage?.bookmarked == true

    val outerAngles: List<Float> = when (outerMode) {
        QuickArcMode.ACTIONS -> listOf(158f, 179f, 200f, 221f, 242f)
        QuickArcMode.COLORS -> {
            val count = paletteColors.size + 1 // circular swatches + custom colour button
            val span = (count - 1).coerceAtLeast(1)
            val step = (108f / span).coerceIn(12.5f, 16f)
            val start = 148f
            List(count) { i -> start + i * step }
        }
        QuickArcMode.SIZES -> {
            val count = sizePresets.size
            val start = 166f
            val step = 17.5f
            List(count) { i -> start + i * step }
        }
        QuickArcMode.NIBS -> {
            val start = 152f
            val step = 17.5f
            List(quickNibs.size) { i -> start + i * step }
        }
        QuickArcMode.MORE_TOOLS -> {
            // 4 extra tools + Table + Web Page + Keyboard = 7 crisp slots
            val count = extraTools.size + 3
            val start = 148f
            val step = 16.5f
            List(count) { i -> start + i * step }
        }
    }
    val (outerDrawStart, outerDrawEnd) = arcRange(outerAngles.first(), outerAngles.last())

    val scheme = MaterialTheme.colorScheme
    val arcSurface = scheme.surface
    val arcBorder = scheme.outlineVariant.copy(alpha = 0.65f)
    val arcFg = scheme.onSurface
    val arcMuted = arcFg.copy(alpha = 0.30f)
    val activeHalo = scheme.primary.copy(alpha = if (darkTheme) 0.28f else 0.16f)

    val undoDesc = stringResource(R.string.toolbar_undo)
    val redoDesc = stringResource(R.string.toolbar_redo)
    val settingsDesc = stringResource(R.string.quick_actions_tool_settings)
    val paletteDesc = stringResource(R.string.quick_actions_palette)
    val thicknessDesc = stringResource(R.string.quick_actions_thickness)
    val moreToolsDesc = stringResource(R.string.quick_actions_more_tools)
    val colourDesc = stringResource(R.string.quick_actions_colour)

    val nibLabel = when (vm.penType) {
        PenType.FOUNTAIN -> "Fountain"
        PenType.BALL -> "Ball"
        PenType.FINELINER -> "Fine"
        PenType.CALLIGRAPHY -> "Callig"
        PenType.PENCIL -> "Pencil"
        PenType.LETTERING -> "Letter"
        PenType.BRUSH -> "Brush"
        PenType.DASHED -> "Dashed"
    }
    val hubText = when (outerMode) {
        QuickArcMode.COLORS -> "Ink Colour"
        QuickArcMode.SIZES -> "Stroke Size"
        QuickArcMode.NIBS -> "Pen Nib"
        QuickArcMode.MORE_TOOLS -> "Insert & Tools"
        QuickArcMode.ACTIONS -> if (vm.tool == Tool.PEN) "Pen · $nibLabel" else toolDisplayName(vm.tool)
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = popupOffset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(canvasW, canvasH)
                .graphicsLayer {
                    val s = appear.value
                    scaleX = s
                    scaleY = s
                    alpha = ((s - 0.72f) / 0.28f).coerceIn(0f, 1f)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                        pivotFractionX = (pivotX / canvasWPx).coerceIn(0f, 1f),
                        pivotFractionY = (pivotY / canvasHPx).coerceIn(0f, 1f),
                    )
                }
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                }
                .pointerInput(onDismiss) {
                    detectTapGestures { onDismiss() }
                },
        ) {
            // Draw the two concentric curved stadium ribbons with soft layered shadows and crisp borders.
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val innerPath = buildCurvedArcPath(
                    cx = pivotX,
                    cy = pivotY,
                    radius = innerRadiusPx,
                    halfThickness = innerBandPx / 2f,
                    startDeg = innerDrawStart,
                    endDeg = innerDrawEnd,
                )
                val outerPath = buildCurvedArcPath(
                    cx = pivotX,
                    cy = pivotY,
                    radius = outerRadiusPx,
                    halfThickness = outerBandPx / 2f,
                    startDeg = outerDrawStart,
                    endDeg = outerDrawEnd,
                )

                if (!reduceEffects) {
                    val shadowColor = if (darkTheme) Color.Black else Color(0xFF0F172A)
                    for ((dyDp, alpha) in listOf(7f to 0.06f, 4f to 0.08f, 2f to 0.09f)) {
                        val dy = dyDp * density
                        drawContext.canvas.save()
                        drawContext.canvas.translate(0f, dy)
                        drawPath(outerPath, color = shadowColor.copy(alpha = alpha))
                        drawPath(innerPath, color = shadowColor.copy(alpha = alpha))
                        drawContext.canvas.restore()
                    }
                }

                // Fill curved ribbons
                drawPath(outerPath, color = arcSurface)
                drawPath(innerPath, color = arcSurface)

                // Crisp hairline contour borders
                val strokeStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f * density)
                drawPath(outerPath, color = arcBorder, style = strokeStyle)
                drawPath(innerPath, color = arcBorder, style = strokeStyle)

                // Radial divider separating Tools from Thickness & Colour on the inner arc
                val divRad = Math.toRadians(mapAngle(270.5f).toDouble())
                val cosD = kotlin.math.cos(divRad).toFloat()
                val sinD = kotlin.math.sin(divRad).toFloat()
                val divInner = innerRadiusPx - innerBandPx * 0.30f
                val divOuter = innerRadiusPx + innerBandPx * 0.30f
                drawLine(
                    color = arcFg.copy(alpha = 0.14f),
                    start = Offset(pivotX + divInner * cosD, pivotY + divInner * sinD),
                    end = Offset(pivotX + divOuter * cosD, pivotY + divOuter * sinD),
                    strokeWidth = 1.2f * density,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }

            // -------- Center Hub Pill at stylus pivot --------
            Box(
                modifier = Modifier
                    .offset {
                        val wPx = (116f * density).roundToInt()
                        val hPx = (32f * density).roundToInt()
                        IntOffset(
                            x = (pivotX - wPx / 2f).roundToInt().coerceIn(4, (canvasWPx - wPx - 4).coerceAtLeast(4)),
                            y = (pivotY - hPx / 2f).roundToInt().coerceIn(4, (canvasHPx - hPx - 4).coerceAtLeast(4)),
                        )
                    }
                    .width(116.dp)
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(arcSurface.copy(alpha = 0.96f))
                    .border(1.dp, arcBorder, CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Close quick actions") {
                        if (outerMode != QuickArcMode.ACTIONS) outerMode = QuickArcMode.ACTIONS else onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp),
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(activeColor),
                    )
                    Text(
                        text = hubText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = arcFg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // -------- Inner Primary Arc Controls --------
            // 1. Undo (stays open so S Pen users can undo multiple strokes smoothly)
            ArcSlotButton(
                center = polarPos(innerRadiusPx, 158f),
                label = undoDesc,
                enabled = vm.canUndo,
                selected = false,
                selectedBg = activeHalo,
                onClick = {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    vm.undo()
                },
            ) {
                ProIconView(ProIcon.UNDO, if (vm.canUndo) arcFg else arcMuted, Modifier.size(20.dp))
            }

            // 2. Redo (stays open for multi-step redo)
            ArcSlotButton(
                center = polarPos(innerRadiusPx, 178f),
                label = redoDesc,
                enabled = vm.canRedo,
                selected = false,
                selectedBg = activeHalo,
                onClick = {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    vm.redo()
                },
            ) {
                ProIconView(ProIcon.REDO, if (vm.canRedo) arcFg else arcMuted, Modifier.size(20.dp))
            }

            // 3-6. Primary Pro Notes tools: Pen, Eraser, Highlighter, Selection (Lasso)
            val coreTools = remember {
                listOf(
                    Tool.PEN to 198.5f,
                    Tool.ERASER to 218.5f,
                    Tool.HIGHLIGHTER to 238.5f,
                    Tool.LASSO to 258.5f,
                )
            }
            coreTools.forEach { (toolItem, deg) ->
                val isSelected = vm.tool == toolItem
                val toolLabel = toolDisplayName(toolItem)
                val ink = Color(vm.styleFor(toolItem)?.color ?: vm.penStyle.color)
                ArcSlotButton(
                    center = polarPos(innerRadiusPx, deg),
                    label = toolLabel,
                    enabled = true,
                    selected = isSelected,
                    selectedBg = activeHalo,
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        vm.tool = toolItem
                        onDismiss()
                    },
                ) {
                    ToolGlyph(
                        tool = toolItem,
                        tint = if (isSelected) ProButtonPalette.ElectricBlue else arcFg,
                        inkColor = ink,
                        modifier = Modifier.size(22.dp),
                        active = isSelected,
                    )
                }
            }

            // 7. Stroke Thickness -> toggles SIZES outer arc
            ArcSlotButton(
                center = polarPos(innerRadiusPx, 282f),
                label = thicknessDesc,
                enabled = true,
                selected = outerMode == QuickArcMode.SIZES,
                selectedBg = activeHalo,
                onClick = {
                    outerMode = if (outerMode == QuickArcMode.SIZES) QuickArcMode.ACTIONS else QuickArcMode.SIZES
                },
            ) {
                val lineTint = if (outerMode == QuickArcMode.SIZES) ProButtonPalette.ElectricBlue else arcFg
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(18.dp).height(1.8.dp).clip(CircleShape).background(lineTint))
                    Box(Modifier.width(18.dp).height(3.2.dp).clip(CircleShape).background(lineTint.copy(alpha = 0.65f)))
                    Box(Modifier.width(18.dp).height(4.8.dp).clip(CircleShape).background(lineTint.copy(alpha = 0.35f)))
                }
            }

            // 8. Active Ink Colour Dot -> toggles COLORS outer arc
            ArcSlotButton(
                center = polarPos(innerRadiusPx, 302f),
                label = colourDesc,
                enabled = true,
                selected = outerMode == QuickArcMode.COLORS,
                selectedBg = activeHalo,
                onClick = {
                    outerMode = if (outerMode == QuickArcMode.COLORS) QuickArcMode.ACTIONS else QuickArcMode.COLORS
                },
            ) {
                val ringColor = if (outerMode == QuickArcMode.COLORS) ProButtonPalette.ElectricBlue else arcBorder
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .border(
                            width = if (outerMode == QuickArcMode.COLORS) 2.dp else 1.2.dp,
                            color = ringColor,
                            shape = CircleShape,
                        ),
                )
            }

            // -------- Outer Concentric Arc Controls --------
            when (outerMode) {
                QuickArcMode.ACTIONS -> {
                    // 1. More Pro Notes tools & Insert (Shapes, Text, Sticky, Tape, Table, Live Web)
                    ArcSlotButton(
                        center = polarPos(outerRadiusPx, outerAngles[0]),
                        label = moreToolsDesc,
                        enabled = true,
                        selected = false,
                        selectedBg = activeHalo,
                        onClick = { outerMode = QuickArcMode.MORE_TOOLS },
                    ) {
                        Icon(
                            Icons.Outlined.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = arcFg,
                        )
                    }
                    // 2. Pen Nib Switcher (Fountain, Ball, Fine, Brush, Pencil, Calligraphy)
                    ArcSlotButton(
                        center = polarPos(outerRadiusPx, outerAngles[1]),
                        label = "Pen nib",
                        enabled = true,
                        selected = false,
                        selectedBg = activeHalo,
                        onClick = { outerMode = QuickArcMode.NIBS },
                    ) {
                        Icon(
                            Icons.Outlined.Create,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ProButtonPalette.ElectricBlue,
                        )
                    }
                    // 3. Tool settings sheet
                    ArcSlotButton(
                        center = polarPos(outerRadiusPx, outerAngles[2]),
                        label = settingsDesc,
                        enabled = true,
                        selected = false,
                        selectedBg = activeHalo,
                        onClick = {
                            val current = vm.tool
                            onDismiss()
                            onOpenToolSettings(current)
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = arcFg,
                        )
                    }
                    // 4. Auto-Text / Handwriting-to-Text
                    ArcSlotButton(
                        center = polarPos(outerRadiusPx, outerAngles[3]),
                        label = "Auto-Text",
                        enabled = true,
                        selected = vm.tool == Tool.AUTO_TEXT,
                        selectedBg = activeHalo,
                        onClick = {
                            if (vm.selection.isNotEmpty()) {
                                vm.convertSelectionToText()
                            } else {
                                vm.tool = Tool.AUTO_TEXT
                            }
                            onDismiss()
                        },
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (vm.tool == Tool.AUTO_TEXT) scheme.primary else arcFg,
                        )
                    }
                    // 5. Bookmark current page (stays open with haptic feedback)
                    ArcSlotButton(
                        center = polarPos(outerRadiusPx, outerAngles[4]),
                        label = if (isBookmarked) "Remove bookmark" else "Bookmark page",
                        enabled = true,
                        selected = isBookmarked,
                        selectedBg = Color(0xFFE34948).copy(alpha = 0.22f),
                        onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            vm.toggleBookmark()
                        },
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isBookmarked) Color(0xFFE34948) else arcFg,
                        )
                    }
                }

                QuickArcMode.NIBS -> {
                    quickNibs.forEachIndexed { idx, (nib, shortName) ->
                        val isPicked = vm.tool == Tool.PEN && vm.penType == nib
                        ArcSlotButton(
                            center = polarPos(outerRadiusPx, outerAngles[idx]),
                            label = nib.name,
                            enabled = true,
                            selected = isPicked,
                            selectedBg = activeHalo,
                            onClick = {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                vm.tool = Tool.PEN
                                vm.penType = nib
                                onDismiss()
                            },
                        ) {
                            Text(
                                text = shortName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPicked) ProButtonPalette.ElectricBlue else arcFg,
                                maxLines = 1,
                            )
                        }
                    }
                }

                QuickArcMode.COLORS -> {
                    paletteColors.forEachIndexed { idx, argb ->
                        val isPicked = argb == activeStyle.color
                        ArcSlotButton(
                            center = polarPos(outerRadiusPx, outerAngles[idx]),
                            label = colourDesc,
                            enabled = true,
                            selected = isPicked,
                            selectedBg = activeHalo,
                            onClick = {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                val targetTool = if (vm.styleFor(vm.tool) != null) vm.tool else Tool.PEN
                                vm.updateStyle(targetTool, activeStyle.copy(color = argb))
                                onDismiss()
                            },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isPicked) 24.dp else 21.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (isPicked) 2.2.dp else 1.dp,
                                        color = if (isPicked) ProButtonPalette.ElectricBlue else arcBorder,
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                    // Custom colour wheel trigger
                    ArcSlotButton(
                        center = polarPos(outerRadiusPx, outerAngles.last()),
                        label = paletteDesc,
                        enabled = true,
                        selected = false,
                        selectedBg = activeHalo,
                        onClick = { openCustomColor = true },
                    ) {
                        Icon(
                            Icons.Outlined.ColorLens,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ProButtonPalette.MagentaPink,
                        )
                    }
                }

                QuickArcMode.SIZES -> {
                    sizePresets.forEachIndexed { idx, sizeVal ->
                        val isPicked = kotlin.math.abs(activeStyle.size - sizeVal) < 0.25f
                        val dotDp = (6f + idx * 4.2f).coerceIn(6f, 20f).dp
                        ArcSlotButton(
                            center = polarPos(outerRadiusPx, outerAngles[idx]),
                            label = thicknessDesc,
                            enabled = true,
                            selected = isPicked,
                            selectedBg = activeHalo,
                            onClick = {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                val targetTool = if (vm.styleFor(vm.tool) != null) vm.tool else Tool.PEN
                                vm.updateStyle(targetTool, activeStyle.copy(size = sizeVal))
                                onDismiss()
                            },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dotDp)
                                    .clip(CircleShape)
                                    .background(if (isPicked) ProButtonPalette.ElectricBlue else arcFg),
                            )
                        }
                    }
                }

                QuickArcMode.MORE_TOOLS -> {
                    extraTools.forEachIndexed { idx, toolItem ->
                        val isSelected = vm.tool == toolItem
                        val toolLabel = toolDisplayName(toolItem)
                        val ink = Color(vm.styleFor(toolItem)?.color ?: vm.penStyle.color)
                        ArcSlotButton(
                            center = polarPos(outerRadiusPx, outerAngles[idx]),
                            label = toolLabel,
                            enabled = true,
                            selected = isSelected,
                            selectedBg = activeHalo,
                            onClick = {
                                vm.tool = toolItem
                                onDismiss()
                            },
                        ) {
                            ToolGlyph(
                                tool = toolItem,
                                tint = if (isSelected) ProButtonPalette.ElectricBlue else arcFg,
                                inkColor = ink,
                                modifier = Modifier.size(20.dp),
                                active = isSelected,
                            )
                        }
                    }
                    // Table insert shortcut
                    ArcSlotButton(
                        center = polarPos(outerRadiusPx, outerAngles[extraTools.size]),
                        label = "Insert table",
                        enabled = true,
                        selected = false,
                        selectedBg = activeHalo,
                        onClick = {
                            onDismiss()
                            onAddTable()
                        },
                    ) {
                        Icon(
                            Icons.Outlined.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ProButtonPalette.Emerald,
                        )
                    }
                    // Live Web Page shortcut
                    ArcSlotButton(
                        center = polarPos(outerRadiusPx, outerAngles[extraTools.size + 1]),
                        label = "Add web page",
                        enabled = true,
                        selected = false,
                        selectedBg = activeHalo,
                        onClick = {
                            onDismiss()
                            onAddWebPage()
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Public,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ProButtonPalette.ElectricBlue,
                        )
                    }
                    // Keyboard Mode shortcut
                    ArcSlotButton(
                        center = polarPos(outerRadiusPx, outerAngles[extraTools.size + 2]),
                        label = "Keyboard mode",
                        enabled = true,
                        selected = vm.keyboardMode,
                        selectedBg = activeHalo,
                        onClick = {
                            vm.toggleKeyboardMode()
                            onDismiss()
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (vm.keyboardMode) scheme.primary else arcFg,
                        )
                    }
                }
            }
        }
    }

    if (openCustomColor) {
        ColorEditorDialog(
            initial = activeStyle.color,
            presets = PenPalette,
            recent = vm.recentColors(),
            onDismiss = { openCustomColor = false },
        ) { picked ->
            val targetTool = if (vm.styleFor(vm.tool) != null) vm.tool else Tool.PEN
            vm.updateStyle(targetTool, activeStyle.copy(color = picked))
            openCustomColor = false
            onDismiss()
        }
    }
}

/** Builds a smooth G1-continuous annular stadium path (curved rounded pill) from [startDeg] to [endDeg]. */
private fun buildCurvedArcPath(
    cx: Float,
    cy: Float,
    radius: Float,
    halfThickness: Float,
    startDeg: Float,
    endDeg: Float,
): androidx.compose.ui.graphics.Path {
    val sweep = endDeg - startDeg
    val outerR = radius + halfThickness
    val innerR = (radius - halfThickness).coerceAtLeast(1f)
    val startRad = Math.toRadians(startDeg.toDouble())
    val endRad = Math.toRadians(endDeg.toDouble())
    val endCapCx = cx + (radius * kotlin.math.cos(endRad)).toFloat()
    val endCapCy = cy + (radius * kotlin.math.sin(endRad)).toFloat()
    val startCapCx = cx + (radius * kotlin.math.cos(startRad)).toFloat()
    val startCapCy = cy + (radius * kotlin.math.sin(startRad)).toFloat()

    return androidx.compose.ui.graphics.Path().apply {
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR),
            startAngleDegrees = startDeg,
            sweepAngleDegrees = sweep,
            forceMoveTo = true,
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                endCapCx - halfThickness,
                endCapCy - halfThickness,
                endCapCx + halfThickness,
                endCapCy + halfThickness,
            ),
            startAngleDegrees = endDeg,
            sweepAngleDegrees = 180f,
            forceMoveTo = false,
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR),
            startAngleDegrees = endDeg,
            sweepAngleDegrees = -sweep,
            forceMoveTo = false,
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                startCapCx - halfThickness,
                startCapCy - halfThickness,
                startCapCx + halfThickness,
                startCapCy + halfThickness,
            ),
            startAngleDegrees = startDeg + 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false,
        )
        close()
    }
}

@Composable
private fun ArcSlotButton(
    center: Offset,
    label: String,
    enabled: Boolean,
    selected: Boolean,
    selectedBg: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current.density
    val slotPx = (44f * density).roundToInt()
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (center.x - slotPx / 2f).roundToInt(),
                    y = (center.y - slotPx / 2f).roundToInt(),
                )
            }
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) selectedBg else Color.Transparent)
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
    onBulletList: () -> Unit = {},
    onTable: () -> Unit = {},
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
    val estH = if (menuSize.height > 0) menuSize.height else (420f * density).roundToInt()
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
                QuickInsertRow("Bullet list", {
                    Icon(Icons.Outlined.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp), tint = ProButtonPalette.ElectricBlue)
                }, onBulletList)
                QuickInsertRow("Table", {
                    Icon(Icons.Outlined.TableChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = ProButtonPalette.Emerald)
                }, onTable)
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                QuickInsertRow(stringResource(R.string.quick_insert_paste), {
                    Icon(
                        Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (hasClipboard) ProButtonPalette.Emerald else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }, onPaste)
            }
        }
    }
}

@Composable
private fun AndroidBarAction(
    label: String,
    color: Color = Color(0xFFF8FAFC),
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(appPillShape())
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun AndroidBarDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(16.dp)
            .background(Color.White.copy(alpha = 0.14f)),
    )
}

@Composable
private fun FloatingSelectionToolPill(
    label: String,
    accent: Color = Color(0xFF1A1C21),
    filled: Boolean = false,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = appPillShape(),
        color = if (filled) accent else Color(0xEE181A1F),
        contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (filled) accent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.16f),
        ),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
            )
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
