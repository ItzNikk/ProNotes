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
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.SideButton
import com.pronotes.app.data.ThemeMode
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.data.ToolbarSize
import com.pronotes.app.library.PaperPreview
import com.pronotes.app.ui.GradientTitle

private enum class Category(val label: String, val sub: String, val icon: ImageVector, val tint: Long) {
    STYLUS("Stylus & input", "Pens, side button, palm rejection", Icons.Outlined.Draw, 0xFF2A78D6),
    GESTURES("Gestures", "Scribble, circle, taps", Icons.Outlined.Gesture, 0xFF7161E0),
    PAGES("Pages & paper", "Scroll, templates, paper colour", Icons.Outlined.Description, 0xFF0F7E5C),
    TOOLBAR("Toolbar & tabs", "Position, size, tool order, tabs", Icons.Outlined.ViewQuilt, 0xFFC04873),
    LOOK("Appearance", "Theme, accent, gradients, desk", Icons.Outlined.ColorLens, 0xFFA66A00),
    LIBRARY("Library & backup", "Covers, backup and restore", Icons.Outlined.LibraryBooks, 0xFF256ABF),
    ABOUT("About", "Version, updates", Icons.Outlined.Info, 0xFF5F5E5A),
}

/**
 * Two-pane settings: categories on the left, details on the right.
 * On phones the category list opens each section full screen.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
                ) { c -> Detail(c, s, store) }
            }
        } else {
            AnimatedContent(targetState = selected, label = "phone") { c ->
                if (c == null) CategoryList(null, Modifier.fillMaxSize()) { selected = it } else Detail(c, s, store)
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
                Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onPick(c) }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(GBrush.linearGradient(listOf(Color(c.tint), Color(c.tint).copy(alpha = 0.7f)))),
                    contentAlignment = Alignment.Center,
                ) { Icon(c.icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
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
private fun Detail(c: Category, s: AppSettings, store: SettingsStore) {
    val context = LocalContext.current
    fun up(block: (AppSettings) -> AppSettings) = store.update(block)
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(bottom = 48.dp)) {
        item {
            when (c) {
                Category.STYLUS -> {
                    Section("Drawing") {
                        Toggle(Icons.Outlined.TouchApp, "Draw with stylus only", "Fingers scroll and zoom, they never draw", s.stylusOnly) { v -> up { it.copy(stylusOnly = v) } }
                        Toggle(Icons.Outlined.TouchApp, "Palm rejection", "Ignore touches right after the pen", s.palmRejection) { v -> up { it.copy(palmRejection = v) } }
                        Toggle(Icons.Outlined.TouchApp, "Haptic feedback", "Small buzz on shapes, selection and page add", s.haptics) { v -> up { it.copy(haptics = v) } }
                    }
                    Section("Pen side button") {
                        Choice("While held", s.sideButton.label, SideButton.entries.map { it.label }) { i -> up { it.copy(sideButton = SideButton.entries[i]) } }
                        Info("Works with S Pen, Wacom, USI 2.0, Lenovo, Xiaomi, OnePlus and other Android pens. Pens with an eraser end erase when flipped.")
                    }
                }
                Category.GESTURES -> {
                    Section("Pen gestures") {
                        Toggle(Icons.Outlined.AutoFixHigh, "Scribble to erase", "Zig-zag over ink to delete it", s.scribbleToErase) { v -> up { it.copy(scribbleToErase = v) } }
                        Toggle(Icons.Outlined.Gesture, "Draw and hold for shapes", "Hold the pen still at the end of a line", s.holdToShape) { v -> up { it.copy(holdToShape = v) } }
                        Toggle(Icons.Outlined.Gesture, "Circle and hold to select", "Loop around ink, then hold the pen still", s.circleToLasso) { v -> up { it.copy(circleToLasso = v) } }
                    }
                    Section("Finger gestures") {
                        Toggle(Icons.Outlined.TouchApp, "Tap to undo / redo", "Two-finger tap undo, three-finger tap redo", s.fingerTapUndo) { v -> up { it.copy(fingerTapUndo = v) } }
                        Toggle(Icons.Outlined.TouchApp, "Double-tap to zoom", "Double-tap with one finger", s.doubleTapZoom) { v -> up { it.copy(doubleTapZoom = v) } }
                    }
                }
                Category.PAGES -> {
                    Section("Scrolling") {
                        Segmented(ScrollDirection.entries.map { it.label }, s.scroll.ordinal) { i -> up { it.copy(scroll = ScrollDirection.entries[i]) } }
                        Toggle(Icons.Outlined.Description, "Pull to add a page", "Scroll past the last page to add one", s.pullToAddPage) { v -> up { it.copy(pullToAddPage = v) } }
                        Toggle(Icons.Outlined.Description, "Page numbers", "Show a number under each page", s.pageNumbers) { v -> up { it.copy(pageNumbers = v) } }
                        Toggle(Icons.Outlined.Description, "Dark PDF pages", "Invert PDF pages for reading at night", s.darkPdf) { v -> up { it.copy(darkPdf = v) } }
                        Toggle(Icons.Outlined.Description, "Keep screen on", "While a notebook is open", s.keepScreenOn) { v -> up { it.copy(keepScreenOn = v) } }
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
                        Info("Tip: drag the grip on a floating toolbar to move it to any edge.")
                    }
                    Section("Tools in the toolbar") {
                        ToolbarEditor(s.toolbarTools) { spec -> up { it.copy(toolbarTools = spec) } }
                        Info("Long-press and drag to reorder. Switch off tools you do not use. Tool settings open when you tap a tool that is already selected.")
                    }
                    Section("Tabs") {
                        Toggle(Icons.Outlined.ViewQuilt, "Notebook tabs", "Keep several notebooks open and switch between them", s.showTabs) { v -> up { it.copy(showTabs = v) } }
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
                        Toggle(Icons.Outlined.ColorLens, "Gradient titles", "Headings use the accent gradient", s.gradientTitles) { v -> up { it.copy(gradientTitles = v) } }
                    }
                    Section("Desk") {
                        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DeskColor.entries.forEach { d -> Swatchy(Color(d.argb), d.label, s.deskColor == d) { up { it.copy(deskColor = d) } } }
                        }
                    }
                }
                Category.LIBRARY -> {
                    Section("Covers") {
                        Toggle(Icons.Outlined.Description, "First page as cover", "New notebooks show page 1 on the cover", s.thumbCovers) { v -> up { it.copy(thumbCovers = v) } }
                    }
                    Section("Backup") { BackupRows() }
                }
                Category.ABOUT -> {
                    Section("Pro Notes") {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(16.dp))
                            Column(Modifier.weight(1f)) {
                                GradientTitle("Pro Notes", MaterialTheme.typography.titleMedium)
                                Text("Version ${BuildConfigInfo.versionName(context)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 56.dp))
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ItzNikk/ProNotes/releases/latest")))
                            }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(16.dp))
                            Text("Check for updates on GitHub", Modifier.weight(1f))
                        }
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
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column { content() }
        }
    }
}

@Composable
private fun Toggle(icon: ImageVector, title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
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
        Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Back up library")
            Text("Saves every notebook, folder and sticker to one file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(Modifier.padding(start = 54.dp))
    Row(Modifier.fillMaxWidth().clickable { open.launch(arrayOf("application/zip", "application/octet-stream")) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.LibraryBooks, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Restore from backup")
            Text("Adds notebooks from a backup file; nothing is overwritten", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    status?.let { Info(it) }
}
