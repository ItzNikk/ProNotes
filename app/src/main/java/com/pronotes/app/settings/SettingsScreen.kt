package com.pronotes.app.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DragHandle
import kotlinx.coroutines.launch
import com.pronotes.app.data.Updater.Phase as UPhase
import sh.calvin.reorderable.ReorderableItem
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush as GBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pronotes.app.BuildConfigInfo
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.DeskColor
import com.pronotes.app.data.EraserMode
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PaperTint
import com.pronotes.app.data.PenClickDelay
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.SideButton
import com.pronotes.app.data.ThemeMode
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.data.ToolbarSize
import com.pronotes.app.library.PaperPreview
import com.pronotes.app.ui.GradientTitle

private enum class Category(val label: String, val sub: String, val icon: ProIcon, val tint: Long) {
    STYLUS("Stylus & input", "Pens, side button, palm rejection", ProIcon.PEN, 0xFF2A78D6),
    GESTURES("Gestures", "Scribble, circle, taps", ProIcon.LASSO, 0xFF7161E0),
    PAGES("Pages & paper", "Scroll, templates, paper colour", ProIcon.DOCUMENT, 0xFF0F7E5C),
    TOOLBAR("Toolbar & tabs", "Position, size, tool order, tabs", ProIcon.TOOLBAR, 0xFFC04873),
    LOOK("Appearance", "Theme, accent, gradients, desk", ProIcon.PALETTE, 0xFFD97706),
    LIBRARY("Library & backup", "Covers, backup and restore", ProIcon.FOLDER, 0xFF256ABF),
    ABOUT("About", "Version, updates", ProIcon.INFO, 0xFF6B7280),
}

/**
 * Two-pane settings: categories on the left, details on the right.
 * On phones the category list opens each section full screen.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onTour: () -> Unit = {}, onSupport: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val s by store.settings.collectAsState()
    val wide = LocalConfiguration.current.screenWidthDp >= 700
    var selected by rememberSaveable { mutableStateOf(if (wide) Category.STYLUS else null) }

    BackHandler(enabled = !wide && selected != null) { selected = null }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (!wide && selected != null) selected = null else onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            GradientTitle(if (!wide && selected != null) selected!!.label else "Settings", MaterialTheme.typography.headlineLarge)
        }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                CategoryList(selected, Modifier.width(300.dp).fillMaxHeight()) { selected = it }
                AnimatedContent(
                    targetState = selected ?: Category.STYLUS,
                    transitionSpec = { (fadeIn() + slideInHorizontally { it / 12 }) togetherWith fadeOut() },
                    label = "detail", modifier = Modifier.weight(1f),
                ) { c -> Detail(c, s, store, onTour, onSupport) }
            }
        } else {
            AnimatedContent(targetState = selected, label = "phone") { c ->
                if (c == null) CategoryList(null, Modifier.fillMaxSize()) { selected = it } else Detail(c, s, store, onTour, onSupport)
            }
        }
    }
}

@Composable
private fun CategoryList(selected: Category?, modifier: Modifier, onPick: (Category) -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        items(Category.entries.size) { i ->
            val c = Category.entries[i]
            val sel = c == selected
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(CircleShape)
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onPick(c) }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(GBrush.linearGradient(listOf(Color(c.tint), Color(c.tint).copy(alpha = 0.78f)))),
                    contentAlignment = Alignment.Center,
                ) { ProIconView(c.icon, Color.White, Modifier.size(22.dp)) }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(c.label, style = MaterialTheme.typography.titleMedium)
                    Text(c.sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Detail(c: Category, s: AppSettings, store: SettingsStore, onTour: () -> Unit, onSupport: () -> Unit) {
    val context = LocalContext.current
    fun up(block: (AppSettings) -> AppSettings) = store.update(block)
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(bottom = 48.dp)) {
        item {
            when (c) {
                Category.STYLUS -> {
                    Section("Drawing") {
                        Toggle("Draw with stylus only", "Fingers scroll and zoom, they never draw", s.stylusOnly) { v -> up { it.copy(stylusOnly = v) } }
                        Toggle("Palm rejection", "Ignore touches right after the pen", s.palmRejection) { v -> up { it.copy(palmRejection = v) } }
                        Toggle("Haptic feedback", "Small buzz on shapes, selection and page add", s.haptics) { v -> up { it.copy(haptics = v) } }
                    }
                    Section("Pen side button") {
                        Choice("While held", s.sideButton.label, SideButton.entries.map { it.label }) { i -> up { it.copy(sideButton = SideButton.entries[i]) } }
                        Choice("Button click delay", s.penClickDelay.label, PenClickDelay.entries.map { it.label }) { i -> up { it.copy(penClickDelay = PenClickDelay.entries[i]) } }
                        Toggle("Hover preview", "Show where the pen will touch", s.hoverCursor) { v -> up { it.copy(hoverCursor = v) } }
                    }
                    PensSection(store, onTour)
                }
                Category.GESTURES -> {
                    Section("Pen gestures") {
                        Toggle("Scribble to erase", "Zig-zag over ink to delete it", s.scribbleToErase) { v -> up { it.copy(scribbleToErase = v) } }
                        Toggle("Draw and hold for shapes", "Hold the pen still at the end of a line", s.holdToShape) { v -> up { it.copy(holdToShape = v) } }
                        Toggle("Circle and hold to select", "Loop around ink, then hold the pen still", s.circleToLasso) { v -> up { it.copy(circleToLasso = v) } }
                    }
                    Section("Finger gestures") {
                        Toggle("Tap to undo / redo", "Two-finger tap undo, three-finger tap redo", s.fingerTapUndo) { v -> up { it.copy(fingerTapUndo = v) } }
                        Toggle("Double-tap to zoom", "Double-tap with one finger", s.doubleTapZoom) { v -> up { it.copy(doubleTapZoom = v) } }
                    }
                }
                Category.PAGES -> {
                    Section("Scrolling") {
                        Segmented(ScrollDirection.entries.map { it.label }, s.scroll.ordinal) { i -> up { it.copy(scroll = ScrollDirection.entries[i]) } }
                        Toggle("Pull to add a page", "Scroll past the last page to add one", s.pullToAddPage) { v -> up { it.copy(pullToAddPage = v) } }
                        Toggle("Page numbers", "Show a number under each page", s.pageNumbers) { v -> up { it.copy(pageNumbers = v) } }
                        Toggle("Dark PDF pages", "Invert PDF pages for reading at night", s.darkPdf) { v -> up { it.copy(darkPdf = v) } }
                        Toggle("Keep screen on", "While a notebook is open", s.keepScreenOn) { v -> up { it.copy(keepScreenOn = v) } }
                    }
                    Section("Paper colour") {
                        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PaperTint.entries.forEach { t ->
                                Swatchy(Color(t.argb), t.label, s.paperTint == t) { up { it.copy(paperTint = t) } }
                            }
                        }
                    }
                    Section("Default template") {
                        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PaperStyle.entries.forEach { p ->
                                val sel = s.defaultPaper == p
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { up { it.copy(defaultPaper = p) } }) {
                                    PaperPreview(
                                        p,
                                        Modifier.size(width = 60.dp, height = 84.dp).clip(RoundedCornerShape(6.dp))
                                            .border(if (sel) 2.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                                        tint = s.paperTint.argb,
                                    )
                                    Text(p.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Category.TOOLBAR -> {
                    Section("Toolbar") {
                        Choice("Position", s.toolbarDock.label, ToolbarDock.entries.map { it.label }) { i -> up { it.copy(toolbarDock = ToolbarDock.entries[i]) } }
                        Segmented(ToolbarSize.entries.map { it.label }, s.toolbarSize.ordinal) { i -> up { it.copy(toolbarSize = ToolbarSize.entries[i]) } }
                    }
                    Section("Tools in the toolbar") {
                        ToolbarEditor(s.toolbarTools) { spec -> up { it.copy(toolbarTools = spec) } }
                        Info("Long-press to reorder. Tap a selected tool for its settings.")
                    }
                    Section("Tabs") {
                        Toggle("Notebook tabs", "Keep several notebooks open and switch between them", s.showTabs) { v -> up { it.copy(showTabs = v) } }
                    }
                }
                Category.LOOK -> {
                    Section("Theme") {
                        Segmented(ThemeMode.entries.map { it.label }, s.themeMode.ordinal) { i -> up { it.copy(themeMode = ThemeMode.entries[i]) } }
                    }
                    Section("Accent colour") {
                        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccentColor.entries.forEach { a ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { up { it.copy(accent = a) } }) {
                                    Box(
                                        Modifier.size(40.dp).clip(CircleShape)
                                            .background(GBrush.linearGradient(listOf(Color(a.argb), Color(a.argb2))))
                                            .border(if (s.accent == a) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                                    )
                                    Text(a.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Toggle("Gradient titles", "Headings use the accent gradient", s.gradientTitles) { v -> up { it.copy(gradientTitles = v) } }
                    }
                    Section("Desk") {
                        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DeskColor.entries.forEach { d -> Swatchy(Color(d.argb), d.label, s.deskColor == d) { up { it.copy(deskColor = d) } } }
                        }
                    }
                }
                Category.LIBRARY -> {
                    Section("Covers") {
                        Toggle("First page as cover", "New notebooks show page 1 on the cover", s.thumbCovers) { v -> up { it.copy(thumbCovers = v) } }
                    }
                    Section("Backup") { BackupRows() }
                }
                Category.ABOUT -> {
                    Section("Get started") {
                        Row(Modifier.fillMaxWidth().clickable(onClick = onTour).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProIconView(ProIcon.STAR, MaterialTheme.colorScheme.primary, Modifier.size(22.dp)); Spacer(Modifier.size(16.dp))
                            Text("Take the tour", Modifier.weight(1f))
                        }
                        if (com.pronotes.app.support.Support.enabled) {
                            HorizontalDivider(Modifier.padding(start = 56.dp))
                            Row(Modifier.fillMaxWidth().clickable(onClick = onSupport).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                ProIconView(ProIcon.HEART, Color(0xFFE5484D), Modifier.size(22.dp), ink = Color(0xFFE5484D)); Spacer(Modifier.size(16.dp))
                                Text("Support Pro Notes", Modifier.weight(1f))
                            }
                            HorizontalDivider(Modifier.padding(start = 56.dp))
                            Toggle("Occasional support reminders", "A gentle note now and then", s.supportReminders) { v -> up { it.copy(supportReminders = v) } }
                        }
                    }
                    Section("Pro Notes") {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProIconView(ProIcon.INFO, MaterialTheme.colorScheme.primary, Modifier.size(22.dp))
                            Spacer(Modifier.size(16.dp))
                            Column(Modifier.weight(1f)) {
                                GradientTitle("Pro Notes", MaterialTheme.typography.titleMedium)
                                Text("Version ${BuildConfigInfo.versionName(context)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Made by ItzNikk · Apache License 2.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 56.dp))
                        UpdateRow()
                        HorizontalDivider(Modifier.padding(start = 56.dp))
                        Toggle("Check for updates automatically", "When the library opens", s.autoUpdateCheck) { v -> up { it.copy(autoUpdateCheck = v) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface) {
            Column { content() }
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text(title)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(value, onChange)
    }
}

@Composable
private fun Choice(title: String, value: String, options: List<String>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f))
            Text(value, color = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(open, { open = false }, modifier = Modifier.align(Alignment.CenterEnd)) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onPick(i) }) }
        }
    }
}

/** iOS-style segmented control. */
@Composable
private fun Segmented(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp),
    ) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent).clickable { onPick(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(o, style = MaterialTheme.typography.labelSmall, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Swatchy(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.size(width = 48.dp, height = 36.dp).clip(RoundedCornerShape(8.dp)).background(color)
                .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Info(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
}

/** Drag to reorder toolbar tools; switches hide tools you never use. */
@Composable
private fun ToolbarEditor(spec: String, onChange: (String) -> Unit) {
    var list by remember(spec) { mutableStateOf(com.pronotes.app.editor.ToolbarOrder.parse(spec)) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorder = sh.calvin.reorderable.rememberReorderableLazyListState(listState) { from, to ->
        list = list.toMutableList().apply { add(to.index, removeAt(from.index)) }
        onChange(com.pronotes.app.editor.ToolbarOrder.encode(list))
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().height((list.size * 56).dp), userScrollEnabled = false) {
        items(list.size, key = { list[it].first.name }) { i ->
            val (tool, visible) = list[i]
            ReorderableItem(reorder, key = tool.name) { dragging ->
                Row(
                    Modifier.fillMaxWidth().height(56.dp)
                        .background(if (dragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.DragHandle, "Drag", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.longPressDraggableHandle())
                    Spacer(Modifier.width(12.dp))
                    com.pronotes.app.ui.ProIconView(com.pronotes.app.ui.iconFor(tool), MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(tool.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.weight(1f))
                    Switch(visible, onCheckedChange = { v ->
                        if (!v && list.count { it.second } <= 1) return@Switch
                        list = list.toMutableList().also { it[i] = tool to v }
                        onChange(com.pronotes.app.editor.ToolbarOrder.encode(list))
                    })
                }
            }
        }
    }
}

@Composable
private fun BackupRows() {
    val context = LocalContext.current
    val repo = remember { com.pronotes.app.data.Repository.get(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    val save = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            status = runCatching { context.contentResolver.openOutputStream(uri)!!.use { repo.backup(it) }; "Backup saved." }.getOrElse { "Backup failed: ${it.message}" }
        }
    }
    val open = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            status = runCatching { val n = context.contentResolver.openInputStream(uri)!!.use { repo.restore(it) }; "Restored $n notebook(s)." }
                .getOrElse { "Restore failed: ${it.message}" }
        }
    }
    Row(Modifier.fillMaxWidth().clickable { save.launch("ProNotes-backup.zip") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ProIconView(ProIcon.SHARE, MaterialTheme.colorScheme.primary, Modifier.size(22.dp)); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Back up library")
            Text("Saves every notebook, folder and sticker to one file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(Modifier.padding(start = 54.dp))
    Row(Modifier.fillMaxWidth().clickable { open.launch(arrayOf("application/zip", "application/octet-stream")) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ProIconView(ProIcon.RESTORE, MaterialTheme.colorScheme.primary, Modifier.size(22.dp)); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Restore from backup")
            Text("Adds notebooks from a backup file; nothing is overwritten", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    status?.let { Info(it) }
}

/** Check, download and install updates without leaving the app. */
@Composable
private fun UpdateRow() {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val u = com.pronotes.app.data.Updater
    val (title, sub) = when (u.phase) {
        UPhase.IDLE -> "Check for updates" to "Version ${u.currentVersion(context)}"
        UPhase.CHECKING -> "Checking�" to "Version ${u.currentVersion(context)}"
        UPhase.UP_TO_DATE -> "Pro Notes is up to date" to "Version ${u.currentVersion(context)}"
        UPhase.AVAILABLE -> "Update to ${u.release?.tag}" to "Tap to download and install"
        UPhase.DOWNLOADING -> "Downloading ${(u.progress * 100).toInt()}%" to "Keep the app open"
        UPhase.READY -> "Install update" to "Tap to open the installer"
        UPhase.FAILED -> "Update failed" to (u.error ?: "Tap to try again")
    }
    Column(
        Modifier.fillMaxWidth().clickable {
            when (u.phase) {
                UPhase.AVAILABLE -> scope.launch { u.download(context) }
                UPhase.READY -> u.install(context)
                UPhase.DOWNLOADING, UPhase.CHECKING -> Unit
                else -> scope.launch { u.check(context) }
            }
        }.padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProIconView(ProIcon.DOWNLOAD, MaterialTheme.colorScheme.primary, Modifier.size(22.dp))
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
        if (u.phase == UPhase.DOWNLOADING) {
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.LinearProgressIndicator(progress = { u.progress }, modifier = Modifier.fillMaxWidth().padding(start = 40.dp))
        }
    }
}

/** Every pen used on this device, each with its own side button and click shortcuts. */
@Composable
private fun PensSection(store: SettingsStore, onSetup: () -> Unit) {
    val pens by store.pens.collectAsState()
    Section("Your pens") {
        if (pens.isEmpty()) Info("Pens appear here after you write with them.")
        pens.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider()
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProIconView(ProIcon.PEN, MaterialTheme.colorScheme.primary, Modifier.size(22.dp))
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.brand)
                        Text(p.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    androidx.compose.material3.TextButton(onClick = { store.forgetPen(p.name) }) { Text("Forget") }
                }
                Choice("Hold side button", p.sideButton.label, SideButton.entries.map { it.label }) { k -> store.updatePen(p.copy(sideButton = SideButton.entries[k])) }
                Choice("Click button", p.click.label, com.pronotes.app.data.PenAction.entries.map { it.label }) { k -> store.updatePen(p.copy(click = com.pronotes.app.data.PenAction.entries[k])) }
                Choice("Double-click button", p.doubleClick.label, com.pronotes.app.data.PenAction.entries.map { it.label }) { k -> store.updatePen(p.copy(doubleClick = com.pronotes.app.data.PenAction.entries[k])) }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().clickable(onClick = onSetup).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProIconView(ProIcon.ADD, MaterialTheme.colorScheme.primary, Modifier.size(22.dp)); Spacer(Modifier.size(16.dp))
            Text("Set up a pen", Modifier.weight(1f))
        }
    }
}
