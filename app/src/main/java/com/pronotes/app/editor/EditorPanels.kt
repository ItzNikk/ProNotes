package com.pronotes.app.editor

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.UiColorScheme
import com.pronotes.app.library.PdfSkeletonPreviewBox
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppMenuButton
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.appCardShape
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun TabStrip(
    tabs: List<Pair<String, String>>,
    current: String,
    darkUi: Boolean,
    tabStripBg: Color,
    activeTabBg: Color,
    fgColor: Color,
    onHome: () -> Unit,
    onRenameCurrent: () -> Unit,
    onNewPageTab: () -> Unit,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
) {
    val mutedFg = if (darkUi) Color(0xFF8E9BBE) else Color(0xFF475569)
    val closeFg = if (darkUi) Color(0xFF8FA3C4) else Color(0xFF64748B)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(tabStripBg)
            .padding(start = 10.dp, end = 10.dp, top = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                .clip(CircleShape)
                .focusable()
                .semantics { contentDescription = "Library" }
                .clickable(role = Role.Button, onClickLabel = "Library", onClick = onHome),
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.HOME, mutedFg, Modifier.size(18.dp))
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEach { (id, tabTitle) ->
                val sel = id == current
                val shape = if (sel) {
                    RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                } else {
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                }
                val bg = if (sel) activeTabBg else if (darkUi) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                val labelColor = if (sel) fgColor else mutedFg
                Row(
                    modifier = Modifier
                        .height(if (sel) 38.dp else 34.dp)
                        .widthIn(min = 126.dp, max = 240.dp)
                        .clip(shape)
                        .background(bg)
                        .focusable()
                        .clickable {
                            if (sel) onRenameCurrent() else onSelect(id)
                        }
                        .padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tabTitle,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = labelColor,
                    )
                    if (sel) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Rename",
                            tint = fgColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .semantics { contentDescription = "Close tab $tabTitle" }
                            .clickable(role = Role.Button, onClickLabel = "Close tab") { onClose(id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        ProIconView(ProIcon.CLOSE, if (sel) closeFg else mutedFg, Modifier.size(10.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 38.dp, minHeight = 38.dp)
                .clip(CircleShape)
                .focusable()
                .semantics { contentDescription = "Add page" }
                .clickable(role = Role.Button, onClickLabel = "Add page", onClick = onNewPageTab),
            contentAlignment = Alignment.Center,
        ) {
            ProIconView(ProIcon.ADD, mutedFg, Modifier.size(17.dp))
        }
    }
}

@Composable
internal fun BarIcon(
    icon: ProIcon,
    label: String,
    enabled: Boolean = true,
    active: Boolean = false,
    fgColor: Color = LocalContentColor.current,
    activeBg: Color = ProButtonPalette.ElectricBlue,
    activeFg: Color = Color.White,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> fgColor.copy(alpha = 0.32f)
        active -> activeFg
        else -> fgColor
    }
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) activeBg else Color.Transparent)
            .focusable()
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ProIconView(icon, tint, Modifier.size(20.dp))
    }
}

@Composable
internal fun TopBar(
    vm: EditorViewModel,
    settings: AppSettings,
    showTabs: Boolean,
    showSearch: Boolean,
    showPages: Boolean,
    darkUi: Boolean,
    onHome: () -> Unit,
    onPages: () -> Unit,
    onSearch: () -> Unit,
    onRename: () -> Unit,
    onFocus: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onSharePage: () -> Unit,
    onSettings: () -> Unit,
    onGoTo: () -> Unit,
    onScrollToggle: () -> Unit,
    onPaste: () -> Unit,
    onPageColor: () -> Unit,
    onPageImage: () -> Unit,
    onCards: () -> Unit,
    onStickyBoard: () -> Unit,
    onReviewHandwriting: () -> Unit,
    onVideo: () -> Unit,
    onCollabRoom: () -> Unit,
    onAddWebLink: () -> Unit,
    onTemplates: () -> Unit,
    onImportFiles: () -> Unit,
    onSplit: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { SettingsStore.get(context) }
    var pendingPageDelete by remember(vm) { mutableStateOf<String?>(null) }
    pendingPageDelete?.let { pageId ->
        ConfirmEditorDeletion(
            title = "Delete this page?",
            message = "The page and its contents will be removed. This action cannot be undone.",
            onDismiss = { pendingPageDelete = null },
            onDelete = { vm.deletePage(pageId) },
        )
    }
    val fgColor = LocalContentColor.current
    val dividerColor = fgColor.copy(alpha = 0.20f)
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 540
    val automaticHandwriting = vm.settingsFlow.collectAsState().value.toolDefaults.autoHandwriting

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (!showTabs) {
            BarIcon(ProIcon.HOME, "Library", fgColor = fgColor, onClick = onHome)
        }
        BarIcon(ProIcon.SIDEBAR, "Pages", active = showPages, fgColor = fgColor, onClick = onPages)
        if (!compact) BarIcon(ProIcon.SEARCH, "Search", active = showSearch, fgColor = fgColor, onClick = onSearch)
        if (!compact) BarIcon(ProIcon.PAGES, "Jump to page", fgColor = fgColor, onClick = onGoTo)
        BarIcon(ProIcon.KEYBOARD, "Keyboard Mode", active = vm.keyboardMode, fgColor = fgColor, onClick = { vm.toggleKeyboardMode() })

        Text(
            vm.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = fgColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .focusable()
                .clickable(role = Role.Button, onClickLabel = "Rename notebook", onClick = onRename)
                .padding(horizontal = 10.dp, vertical = 12.dp),
        )

        Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(22.dp).background(dividerColor))

        AppMenuButton(
            contentDescription = "Insert into notebook",
            containerColor = ProButtonPalette.Emerald.copy(alpha = if (darkUi) 0.24f else 0.16f),
            contentPadding = PaddingValues(7.dp),
            buttonContent = {
                ProIconView(ProIcon.ADD, if (darkUi) Color(0xFF6EE7B7) else Color(0xFF047857), Modifier.size(20.dp))
            },
            items = listOf(
                AppMenuItem("New page after this") { vm.addPage() },
                AppMenuItem("New page at the end") { vm.addPage(atEnd = true) },
                AppMenuItem("Add write box") { vm.addWriteBox() },
                AppMenuItem("Import file") { onImportFiles() },
                AppMenuItem("Add web link") { onAddWebLink() },
                AppMenuItem("Floating video") { onVideo() },
                AppMenuItem("Infinite whiteboard page") { vm.addPage(paper = PaperStyle.DOTTED, isWhiteboard = true) },
                AppMenuItem("Choose a template…") { onTemplates() },
            ),
        )
        Spacer(Modifier.width(3.dp))
        if (!compact) AppMenuButton(
            contentDescription = "Share and export",
            containerColor = ProButtonPalette.SkyCyan.copy(alpha = if (darkUi) 0.24f else 0.16f),
            contentPadding = PaddingValues(7.dp),
            buttonContent = {
                ProIconView(ProIcon.SHARE, if (darkUi) Color(0xFF7DD3FC) else Color(0xFF0369A1), Modifier.size(20.dp))
            },
            items = listOf(
                AppMenuItem("Export as PDF…", icon = { Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(18.dp)) }) { onExport() },
                AppMenuItem("Share PDF", icon = { Icon(Icons.Outlined.IosShare, null, Modifier.size(18.dp)) }) { onShare() },
                AppMenuItem("Share this page as image", icon = { Icon(Icons.Outlined.Image, null, Modifier.size(18.dp)) }) { onSharePage() },
            ),
        )
        Spacer(Modifier.width(3.dp))
        val moreEntries = buildList {
            if (!vm.readOnly) add(AppMenuItem("Straighten original handwriting", selected = vm.straightenInk) { vm.toggleInkStraightening() })
            val widgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            if (widgetManager.isRequestPinAppWidgetSupported) add(AppMenuItem("Pin notebook widget") {
                com.pronotes.app.NotebookWidget.pin(context, vm.noteId)
            })
            if (!vm.readOnly) add(AppMenuItem("Draw with finger", selected = !vm.settingsFlow.value.stylusOnly) {
                store.update { it.copy(stylusOnly = !it.stylusOnly) }
            })
            if (!vm.readOnly) add(AppMenuItem(
                "Automatic handwriting", selected = automaticHandwriting,
            ) { vm.setAutoHandwriting(!automaticHandwriting) })
            if (compact) {
                add(AppMenuItem("Search notebook", selected = showSearch) { onSearch() })
                add(AppMenuItem("Jump to page") { onGoTo() })
                add(AppMenuItem("Export as PDF") { onExport() })
                add(AppMenuItem("Share PDF") { onShare() })
                add(AppMenuItem("Share page as image") { onSharePage() })
            }
            add(
                AppMenuItem(
                    if (vm.keyboardMode) "Exit Keyboard Mode" else "Keyboard Mode (Full Page)",
                    icon = { ProIconView(ProIcon.KEYBOARD, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp), ink = if (vm.keyboardMode) ProButtonPalette.ElectricBlue else null) },
                ) { vm.toggleKeyboardMode() },
            )
            add(
                AppMenuItem(
                    "Place Write Box",
                    icon = { ProIconView(ProIcon.WRITE_BOX, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp), ink = ProButtonPalette.ElectricBlue) },
                ) {
                    vm.tool = Tool.TEXT
                    vm.addWriteBox(vm.pageIndex)
                },
            )
            if (!vm.readOnly && vm.handwritingReview != null) {
                add(AppMenuItem("Review last handwriting conversion") { onReviewHandwriting() })
            }
            vm.pages.forEachIndexed { index, page ->
                if (page.bookmarked) add(AppMenuItem("Bookmark · Page " + (index + 1)) { vm.goToPage(index) })
            }
            add(AppMenuItem("Sticky board") { onStickyBoard() })
            add(
                AppMenuItem(
                    "Flashcards",
                    icon = { ProIconView(ProIcon.FLASHCARDS, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp), ink = ProButtonPalette.RoyalBlue.copy(alpha = 0.22f)) },
                ) { onCards() },
            )
            add(
                AppMenuItem(
                    "Auto-generate flashcards from notes",
                    icon = { ProIconView(ProIcon.FLASHCARDS, ProButtonPalette.WarmAmber, Modifier.size(18.dp)) },
                ) {
                    vm.autoGenerateFlashcards()
                    onCards()
                },
            )
            add(
                AppMenuItem(
                    "Flashcard from area",
                    icon = { Icon(Icons.Outlined.Crop, null, Modifier.size(18.dp)) },
                ) { vm.capturingCard = true },
            )
            add(
                AppMenuItem(
                    "Live Study Room",
                    icon = { Icon(Icons.Outlined.Groups, null, Modifier.size(18.dp), tint = ProButtonPalette.ElectricBlue) },
                ) { onCollabRoom() },
            )
            add(
                AppMenuItem(
                    "Link item to page/URL…",
                    icon = { Icon(Icons.Outlined.Link, null, Modifier.size(18.dp)) },
                ) { vm.beginLinkFlow() },
            )
            add(
                AppMenuItem(
                    "Split view",
                    icon = { ProIconView(ProIcon.SPLIT, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onSplit() },
            )
            add(
                AppMenuItem(
                    "Page template…",
                    icon = { ProIconView(ProIcon.TEMPLATE, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onTemplates() },
            )
            add(
                AppMenuItem(
                    "Cycle UI Colour Scheme (${settings.uiColorScheme.label})",
                    icon = { ProIconView(ProIcon.PALETTE, ProButtonPalette.MagentaPink, Modifier.size(18.dp)) },
                ) {
                    val schemes = UiColorScheme.entries
                    val nextScheme = schemes[(settings.uiColorScheme.ordinal + 1) % schemes.size]
                    store.update { it.copy(uiColorScheme = nextScheme) }
                    vm.showHint("Theme: ${nextScheme.label}")
                },
            )
            add(
                AppMenuItem(
                    if (vm.currentPage?.isWhiteboard == true) "Switch to standard page" else "Infinite whiteboard",
                    icon = { Icon(Icons.Outlined.AllInclusive, null, Modifier.size(18.dp)) },
                ) { vm.toggleCurrentWhiteboard() },
            )
            add(
                AppMenuItem(
                    "Page colour…",
                    icon = { ProIconView(ProIcon.PALETTE, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onPageColor() },
            )
            add(
                AppMenuItem(
                    if (vm.currentPage?.bookmarked == true) "Remove bookmark" else "Bookmark page",
                    icon = {
                        ProIconView(
                            ProIcon.BOOKMARK,
                            if (vm.currentPage?.bookmarked == true) Color(0xFFE34948) else MaterialTheme.colorScheme.onSurface,
                            Modifier.size(18.dp),
                            ink = if (vm.currentPage?.bookmarked == true) Color(0xFFE34948) else null,
                        )
                    },
                ) { vm.toggleBookmark() },
            )
            if (vm.hasClipboard) {
                add(
                    AppMenuItem(
                        "Paste",
                        icon = { Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp)) },
                    ) { onPaste() },
                )
            }
            add(
                AppMenuItem(
                    "Duplicate page",
                    icon = { ProIconView(ProIcon.COPY, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { vm.duplicatePage() },
            )
            add(
                AppMenuItem(
                    "Focus mode",
                    icon = { ProIconView(ProIcon.FOCUS, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onFocus() },
            )
            add(
                AppMenuItem(
                    if (vm.readOnly) "Turn off read only" else "Read only",
                    icon = {
                        ProIconView(
                            ProIcon.LOCK,
                            if (vm.readOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            Modifier.size(18.dp),
                            ink = if (vm.readOnly) MaterialTheme.colorScheme.primary else null,
                        )
                    },
                ) { vm.toggleReadOnly() },
            )
            add(
                AppMenuItem(
                    "Rename notebook",
                    icon = { ProIconView(ProIcon.RENAME, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onRename() },
            )
            add(
                AppMenuItem(
                    "Settings",
                    icon = { ProIconView(ProIcon.SETTINGS, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)) },
                ) { onSettings() },
            )
            if (vm.pages.size > 1 && !vm.readOnly) {
                add(
                    AppMenuItem(
                        "Delete page",
                        destructive = true,
                        icon = { ProIconView(ProIcon.TRASH, MaterialTheme.colorScheme.error, Modifier.size(18.dp)) },
                    ) { pendingPageDelete = vm.currentPage?.id },
                )
            }
        }
        AppMenuButton(
            contentDescription = "More actions",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (darkUi) 0.85f else 0.92f),
            contentPadding = PaddingValues(7.dp),
            buttonContent = {
                ProIconView(ProIcon.MORE, MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp))
            },
            items = moreEntries,
        )
    }
}

@Composable
internal fun SearchBar(vm: EditorViewModel) {
    AppCard(
        hero = false,
        shape = appCardShape(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vm.searchQuery,
                    onValueChange = vm::search,
                    singleLine = true,
                    placeholder = {
                        com.pronotes.app.ui.AnimatedGradientSearchPlaceholder("What are you looking for?")
                    },
                    leadingIcon = { ProIconView(ProIcon.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp)) },
                    shape = CircleShape,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        vm.searching -> "Scanning ink & text…"
                        vm.searchQuery.isBlank() -> "Text · PDF · Handwriting"
                        vm.searchHits.isEmpty() -> "No results"
                        else -> "${vm.activeHit + 1} of ${vm.searchHits.size}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = { vm.stepHit(-1) }, enabled = vm.searchHits.isNotEmpty()) {
                    Icon(Icons.Filled.KeyboardArrowUp, "Previous match")
                }
                IconButton(onClick = { vm.stepHit(1) }, enabled = vm.searchHits.isNotEmpty()) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Next match")
                }
                IconButton(onClick = vm::clearSearch) {
                    Icon(Icons.Filled.Close, "Clear search")
                }
            }
            if (vm.searchHits.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                    itemsIndexed(vm.searchHits, key = { idx, hit -> "${hit.page}:$idx:${hit.snippet.hashCode()}" }) { i, hit ->
                        AppPill(
                            selected = i == vm.activeHit,
                            tint = ProButtonPalette.ElectricBlue,
                            onClick = { vm.jumpToHit(i) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                val fg = if (i == vm.activeHit) Color.White else MaterialTheme.colorScheme.onSurface
                                Text(
                                    "p.${hit.page + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (i == vm.activeHit) fg else ProButtonPalette.ElectricBlue,
                                    modifier = Modifier.width(44.dp),
                                )
                                Text(
                                    hit.snippet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = fg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Left panel with page thumbnails (drag to reorder) and a bookmark outline. */
@Composable
internal fun PagesPanel(vm: EditorViewModel, onJump: (Int) -> Unit, onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var selecting by remember(vm) { mutableStateOf(false) }
    var selected by remember(vm) { mutableStateOf(emptySet<String>()) }
    val reduceMotion = LocalReduceMotion.current
    AppSurface(
        role = SurfaceRole.Frost,
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.width(204.dp).fillMaxHeight(),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScaleInAnimatedText(
                    text = "Pages",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { vm.addPage(atEnd = true) }) { Icon(Icons.Outlined.Add, "Add page") }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close pages panel") }
            }
            AppSegmentedControl(
                options = listOf("All", "Bookmarks"),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            if (tab == 0 && !vm.readOnly) {
                androidx.compose.material3.TextButton(onClick = {
                    selecting = !selecting
                    selected = emptySet()
                }) { Text(if (selecting) "Cancel selection" else "Select pages") }
                if (selecting && selected.isNotEmpty()) {
                    for (marked in listOf(true, false)) {
                        androidx.compose.material3.TextButton(onClick = {
                            vm.bookmarkPages(selected, marked)
                            selecting = false
                            selected = emptySet()
                        }) { Text((if (marked) "Bookmark" else "Unbookmark") + " (" + selected.size + ")") }
                    }
                }
            }
            if (tab == 0) {
                val listState = rememberLazyListState()
                val reorder = rememberReorderableLazyListState(listState) { from, to -> vm.movePage(from.index, to.index) }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(vm.pages, key = { _, p -> p.id }) { i, page ->
                        ReorderableItem(reorder, key = page.id) { dragging ->
                            val lift by animateFloatAsState(
                                targetValue = if (dragging) 1.06f else 1f,
                                animationSpec = if (reduceMotion) snap() else tween(150),
                                label = "lift",
                            )
                            if (selecting) {
                                val checked = page.id in selected
                                androidx.compose.material3.TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { selected = if (checked) selected - page.id else selected + page.id },
                                ) { Text((if (checked) "✓  " else "○  ") + "Page " + (i + 1)) }
                            } else PageThumb(
                                vm = vm,
                                i = i,
                                page = page,
                                modifier = Modifier
                                    .graphicsLayer { scaleX = lift; scaleY = lift }
                                    .longPressDraggableHandle(),
                                onJump = onJump,
                            )
                        }
                    }
                }
            } else {
                val marks = vm.pages.indices.filter { vm.pages[it].bookmarked }
                if (marks.isEmpty()) {
                    Text(
                        "Bookmark pages from the More menu to list them here.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(marks.size, key = { vm.pages[marks[it]].id }) { k ->
                        PageThumb(vm, marks[k], vm.pages[marks[k]], Modifier, onJump)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageThumb(vm: EditorViewModel, i: Int, page: Page, modifier: Modifier, onJump: (Int) -> Unit) {
    val thumb by produceState<Bitmap?>(null, page) { value = vm.thumbnail(i, 220) }
    val reduceMotion = LocalReduceMotion.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(page.width / page.height)
                .shadow(3.dp, RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(
                    if (i == vm.pageIndex) 2.dp else 0.dp,
                    ProButtonPalette.ElectricBlue,
                    RoundedCornerShape(8.dp),
                )
                .clickable(role = Role.Button, onClickLabel = "Go to page ${i + 1}") { onJump(i) },
        ) {
            thumb?.let { Image(it.asImageBitmap(), "Page ${i + 1} thumbnail", Modifier.fillMaxSize()) } ?: run {
                val shimmer = if (reduceMotion) 0f else {
                    rememberInfiniteTransition(label = "thumbShimmer").animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart),
                        label = "sweep",
                    ).value
                }
                PdfSkeletonPreviewBox(shimmer = shimmer, modifier = Modifier.fillMaxSize())
            }
            if (page.bookmarked) {
                ProIconView(
                    ProIcon.BOOKMARK,
                    Color(0xFFE34948),
                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp),
                    ink = Color(0xFFE34948),
                )
            }
        }
        Text("${i + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun WriteBoxStudioBar(
    vm: EditorViewModel,
    box: WriteBoxItem,
    pageIndex: Int,
    darkUi: Boolean,
) {
    val barBg = if (darkUi) Color(0xF5161B26) else Color(0xFAFFFFFF)
    val barFg = if (darkUi) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    var textValue by remember(box.id, box.convertedText) { mutableStateOf(box.convertedText) }

    AppSurface(
        role = SurfaceRole.Chrome,
        containerColor = barBg,
        contentColor = barFg,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 12.dp,
        borderColor = ProButtonPalette.ElectricBlue.copy(alpha = 0.55f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier.widthIn(max = 680.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProIconView(ProIcon.WRITE_BOX, ProButtonPalette.ElectricBlue, Modifier.size(18.dp), ink = ProButtonPalette.ElectricBlue)
                Text(
                    "Write Box",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = barFg,
                )
                Text(
                    when {
                        vm.handwritingBusy -> "• Recognising handwriting…"
                        box.isConverted -> "• Converted"
                        else -> "• Writing ink (auto-converts)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ProButtonPalette.ElectricBlue,
                )
                Spacer(Modifier.weight(1f))
                AppPill(
                    selected = box.isConverted,
                    tint = ProButtonPalette.ElectricBlue,
                    onClick = { vm.toggleWriteBox(pageIndex, box) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        if (box.isConverted) "Show ink" else "Convert now",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (box.isConverted) Color.White else barFg,
                    )
                }
                AppPill(
                    selected = true,
                    tint = ProButtonPalette.Emerald,
                    onClick = { vm.flattenWriteBoxToText(pageIndex, box.id) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "Place text",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                IconButton(
                    onClick = { vm.deleteWriteBox(pageIndex, box.id) },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    ProIconView(ProIcon.TRASH, MaterialTheme.colorScheme.error, Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { vm.selectWriteBox(pageIndex, null) },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Write Box bar", tint = barFg.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }

            // Ranked handwriting candidate suggestions live preview bar
            val alts = vm.handwritingCandidates
            if (alts.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    alts.forEach { candidate ->
                        val picked = candidate == textValue.trim()
                        AppPill(
                            selected = picked,
                            tint = ProButtonPalette.ElectricBlue,
                            onClick = {
                                textValue = candidate
                                vm.updateWriteBoxText(pageIndex, box.id, candidate)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                candidate,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (picked) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (picked) Color.White else barFg,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        vm.updateWriteBoxText(pageIndex, box.id, it)
                    },
                    placeholder = { Text("Write inside the box on page or type here…", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
                AppPill(
                    onClick = {
                        val nextSize = (box.fontSize - 3f).coerceAtLeast(16f)
                        vm.updateWriteBoxText(pageIndex, box.id, textValue, fontSize = nextSize)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text("A-", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                AppPill(
                    onClick = {
                        val nextSize = (box.fontSize + 3f).coerceAtMost(64f)
                        vm.updateWriteBoxText(pageIndex, box.id, textValue, fontSize = nextSize)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text("A+", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                val nextFont = TextFont.entries[(TextFont.entries.indexOf(box.font) + 1) % TextFont.entries.size]
                AppPill(
                    onClick = {
                        vm.updateWriteBoxText(pageIndex, box.id, textValue, font = nextFont)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text(box.font.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
