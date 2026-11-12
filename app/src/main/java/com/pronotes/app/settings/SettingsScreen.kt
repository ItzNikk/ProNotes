package com.pronotes.app.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush as GBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pronotes.app.BuildConfigInfo
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.DeskColor
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PaperTint
import com.pronotes.app.data.PenAction
import com.pronotes.app.data.PenBrands
import com.pronotes.app.data.PenClickDelay
import com.pronotes.app.data.PenProfile
import com.pronotes.app.data.ScribbleSensitivity
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.SideButton
import com.pronotes.app.data.ThemeMode
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.data.ToolbarSize
import com.pronotes.app.data.Updater.Phase as UPhase
import com.pronotes.app.editor.Companions
import com.pronotes.app.editor.LocalAiManager
import com.pronotes.app.editor.LocalModelRuntime
import com.pronotes.app.editor.LocalModelStatus
import com.pronotes.app.editor.Nano
import com.pronotes.app.editor.companionById
import com.pronotes.app.editor.drawCompanion
import com.pronotes.app.library.PaperPreview
import com.pronotes.app.ui.GradientTitle
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem

private enum class Category(val label: String, val sub: String, val icon: ProIcon, val tint: Long) {
    STYLUS("Stylus & input", "Pen name, pens, side button", ProIcon.PEN, 0xFF2A78D6),
    AI("On-device AI", "9 free local models, offline study", ProIcon.STAR, 0xFF6B5CE7),
    GESTURES("Gestures", "Live scribble sensitivity, circle, taps", ProIcon.LASSO, 0xFF7161E0),
    PAGES("Pages & paper", "Scroll, templates, paper colour", ProIcon.DOCUMENT, 0xFF0F7E5C),
    TOOLBAR("Toolbar & Buddy", "Position, size, companion picker", ProIcon.TOOLBAR, 0xFFC04873),
    LOOK("Appearance", "Theme, accent, gradients, desk", ProIcon.PALETTE, 0xFFD97706),
    LIBRARY("Library & backup", "Covers, backup and restore", ProIcon.FOLDER, 0xFF256ABF),
    ABOUT("About", "Version, updates", ProIcon.INFO, 0xFF6B7280),
}

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
            Spacer(Modifier.weight(1f))
            com.pronotes.app.editor.CompanionBadge(compact = true)
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
                    var penNameDraft by remember(s.penName) { mutableStateOf(s.penName) }
                    Section("Active stylus identity") {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("PEN NAME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    penNameDraft,
                                    { penNameDraft = it; up { cur -> cur.copy(penName = it) } },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    placeholder = { Text("e.g. Samsung S Pen") },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text("Shown in Pen Studio and used as your default stylus label.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Section("Drawing") {
                        Toggle("Draw with stylus only", "Fingers scroll and zoom, they never draw", s.stylusOnly) { v -> up { it.copy(stylusOnly = v) } }
                        Toggle("Palm rejection", "Ignore touches right after the pen", s.palmRejection) { v -> up { it.copy(palmRejection = v) } }
                        Toggle("Haptic feedback", "Small buzz on shapes, selection and page add", s.haptics) { v -> up { it.copy(haptics = v) } }
                        Toggle("Pen touch tick", "A light tick when the pen lands", s.penHaptics) { v -> up { it.copy(penHaptics = v) } }
                    }
                    Section("Paper feel") {
                        Toggle("Writing sound", "A soft pencil-on-paper sound that follows your speed", s.paperSound) { v -> up { it.copy(paperSound = v) } }
                        Toggle("Paper haptics", "Tiny ticks as you write, like paper grain", s.paperHaptics) { v -> up { it.copy(paperHaptics = v) } }
                    }
                    Section("Smart ink (on device)") {
                        Toggle("Convert to text as I write", "Handwriting turns into typed text a moment after you stop", s.autoConvert) { v -> up { it.copy(autoConvert = v) } }
                        Toggle("Solve maths when I write =", "Write 12 x 4 = or 2x(x+1)=0 and the answer appears", s.mathAssist) { v -> up { it.copy(mathAssist = v) } }
                        Toggle("Search handwriting", "Search also finds words you wrote by hand", s.inkSearch) { v -> up { it.copy(inkSearch = v) } }
                        val langs = listOf("en-US" to "English (US)", "en-GB" to "English (UK)", "hi-IN" to "Hindi", "es-ES" to "Spanish", "fr-FR" to "French", "de-DE" to "German")
                        Choice("Handwriting language", langs.firstOrNull { it.first == s.handwritingLang }?.second ?: s.handwritingLang, langs.map { it.second }) { i -> up { it.copy(handwritingLang = langs[i].first) } }
                    }
                    Section("Pen side button") {
                        Choice("While held", s.sideButton.label, SideButton.entries.map { it.label }) { i -> up { it.copy(sideButton = SideButton.entries[i]) } }
                        Choice("Button click delay", s.penClickDelay.label, PenClickDelay.entries.map { it.label }) { i -> up { it.copy(penClickDelay = PenClickDelay.entries[i]) } }
                        Toggle("Hover preview", "Show where the pen will touch", s.hoverCursor) { v -> up { it.copy(hoverCursor = v) } }
                    }
                    PensSection(store)
                }
                Category.AI -> {
                    LocalAiSection(s)
                }
                Category.GESTURES -> {
                    Section("Pen gestures") {
                        Toggle("Scribble to erase", "Zig-zag or scrub over ink to delete it", s.scribbleToErase) { v -> up { it.copy(scribbleToErase = v) } }
                        if (s.scribbleToErase) {
                            Toggle("Erase live while writing", "Cuts scribbles immediately mid-stroke without lifting the pen", s.liveScribbleErase) { v -> up { it.copy(liveScribbleErase = v) } }
                            Text("SCRIBBLE SENSITIVITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                            Segmented(ScribbleSensitivity.entries.map { it.label }, s.scribbleSensitivity.ordinal) { i ->
                                up { it.copy(scribbleSensitivity = ScribbleSensitivity.entries[i]) }
                            }
                        }
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
                    Section("New notebooks") {
                        val presets = com.pronotes.app.data.PagePreset.entries.filter { it != com.pronotes.app.data.PagePreset.CUSTOM }
                        Choice(
                            "Page size",
                            if (s.defaultPreset.recommended) "${s.defaultPreset.label} (Recommended)" else s.defaultPreset.label,
                            presets.map { if (it.recommended) "${it.label} (Recommended)" else it.label },
                        ) { i -> up { it.copy(defaultPreset = presets[i]) } }
                        Toggle("Landscape", "Pages wider than tall", s.defaultLandscape) { v -> up { it.copy(defaultLandscape = v) } }
                        Toggle("Cover as first page", "Write on the cover of new notebooks", s.defaultCoverPage) { v -> up { it.copy(defaultCoverPage = v) } }
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
                    Section("Study Companion") {
                        Toggle("Study buddy", "A Pokemon-style companion that reacts & levels up", s.pet) { v -> up { it.copy(pet = v) } }
                        if (s.pet) {
                            val activePet = companionById(s.petId)
                            val lvl = 1 + s.petXp / 100
                            var petNameDraft by remember(s.petName) { mutableStateOf(s.petName) }
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${s.petName.ifBlank { activePet.name }} · Lv.$lvl", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("${s.petXp} XP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                OutlinedTextField(
                                    value = petNameDraft,
                                    onValueChange = { petNameDraft = it; up { cur -> cur.copy(petName = it.take(24)) } },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    label = { Text("Companion name") },
                                    placeholder = { Text(activePet.name) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Companions.forEach { cp ->
                                        val on = cp.id == s.petId
                                        Column(
                                            Modifier.width(74.dp).clip(RoundedCornerShape(14.dp))
                                                .background(if (on) cp.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                                .border(if (on) 1.5.dp else 0.dp, if (on) cp.secondary else Color.Transparent, RoundedCornerShape(14.dp))
                                                .clickable { up { it.copy(petId = cp.id) } }.padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Canvas(Modifier.size(30.dp)) {
                                                drawCompanion(cp, 0.5f, 0f, false, false, false, 0f, true)
                                            }
                                            Text(cp.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Section("Tabs & Study Timer") {
                        Toggle("Notebook tabs", "Keep several notebooks open and switch between them", s.showTabs) { v -> up { it.copy(showTabs = v) } }
                        Toggle("Liquid glass", "See-through bars with a glassy edge", s.liquidGlass) { v -> up { it.copy(liquidGlass = v) } }
                        Toggle("Hide toolbar while recording", "When the screen is being recorded (Android 15 and newer)", s.hideToolsWhenRecording) { v -> up { it.copy(hideToolsWhenRecording = v) } }
                        Toggle("Study timer", "A stopwatch for each study session", s.studyTimer) { v -> up { it.copy(studyTimer = v) } }
                        if (s.studyTimer) {
                            Toggle("Start by itself", "Count from the moment a notebook opens", s.studyAutoStart) { v -> up { it.copy(studyAutoStart = v) } }
                            Toggle("Show seconds", "Off shows hours and minutes only", s.studySeconds) { v -> up { it.copy(studySeconds = v) } }
                            val idle = listOf(0, 2, 5, 10)
                            Choice("Pause when idle", if (s.studyIdleMinutes == 0) "Never" else "${s.studyIdleMinutes} min", idle.map { if (it == 0) "Never" else "$it min" }) { i -> up { it.copy(studyIdleMinutes = idle[i]) } }
                        }
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
                        Toggle("Update reminders", "A daily notification and a reminder when the app opens", s.updateReminders) { v ->
                            up { it.copy(updateReminders = v) }
                            com.pronotes.app.UpdateCheckReceiver.schedule(context, v)
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
                    ProIconView(com.pronotes.app.ui.iconFor(tool), MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp))
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
    val scope = rememberCoroutineScope()
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

@Composable
private fun UpdateRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val u = com.pronotes.app.data.Updater
    val (title, sub) = when (u.phase) {
        UPhase.IDLE -> "Check for updates" to "Version ${u.currentVersion(context)}"
        UPhase.CHECKING -> "Checking for updates" to "Version ${u.currentVersion(context)}"
        UPhase.UP_TO_DATE -> "Pro Notes is up to date" to "Version ${u.currentVersion(context)}"
        UPhase.AVAILABLE -> "Update to ${u.release?.tag}" to "Tap to see what changed"
        UPhase.DOWNLOADING -> "Downloading ${(u.progress * 100).toInt()}%" to "Keep the app open"
        UPhase.READY -> "Install update" to "Tap to open the installer"
        UPhase.FAILED -> "Update failed" to (u.error ?: "Tap to try again")
    }
    Column(
        Modifier.fillMaxWidth().clickable {
            when (u.phase) {
                UPhase.AVAILABLE -> u.showDialog = true
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
            LinearProgressIndicator(progress = { u.progress }, modifier = Modifier.fillMaxWidth().padding(start = 40.dp))
        }
    }
}

/** Saved pens with inline rename/alias + dedicated Setup Stylus modal (fixed off onTour!). */
@Composable
private fun PensSection(store: SettingsStore) {
    val pens by store.pens.collectAsState()
    var showSetup by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PenProfile?>(null) }

    Section("Your pens") {
        if (pens.isEmpty()) Info("Tap 'Set up a pen' below or write with a stylus to register it.")
        pens.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider()
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProIconView(ProIcon.PEN, MaterialTheme.colorScheme.primary, Modifier.size(22.dp))
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.displayName(), fontWeight = FontWeight.SemiBold)
                        Text("${p.brand} · ${p.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = { renameTarget = p }) { Icon(Icons.Outlined.Edit, "Rename pen") }
                    TextButton(onClick = { store.forgetPen(p.name) }) { Text("Forget") }
                }
                Choice("Hold side button", p.sideButton.label, SideButton.entries.map { it.label }) { k -> store.updatePen(p.copy(sideButton = SideButton.entries[k])) }
                Choice("Click button", p.click.label, PenAction.entries.map { it.label }) { k -> store.updatePen(p.copy(click = PenAction.entries[k])) }
                Choice("Double-click button", p.doubleClick.label, PenAction.entries.map { it.label }) { k -> store.updatePen(p.copy(doubleClick = PenAction.entries[k])) }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().clickable { showSetup = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProIconView(ProIcon.ADD, MaterialTheme.colorScheme.primary, Modifier.size(22.dp)); Spacer(Modifier.size(16.dp))
            Text("Set up a pen", Modifier.weight(1f))
        }
    }

    if (showSetup) {
        var alias by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf(PenBrands.manual.first()) }
        AlertDialog(
            onDismissRequest = { showSetup = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Set up a pen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        alias, { alias = it }, singleLine = true, shape = RoundedCornerShape(16.dp),
                        label = { Text("Custom pen name") }, placeholder = { Text("e.g. Fine Lecture Stylus") }, modifier = Modifier.fillMaxWidth(),
                    )
                    Choice("Stylus hardware", brand, PenBrands.manual) { idx -> brand = PenBrands.manual[idx] }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val label = alias.trim().ifBlank { brand }
                    val id = "pen-${System.currentTimeMillis()}"
                    store.registerPen(id, brand = brand, alias = label)
                    store.update { it.copy(penName = label) }
                    showSetup = false
                }, shape = CircleShape) { Text("Save Pen") }
            },
            dismissButton = { TextButton(onClick = { showSetup = false }) { Text("Cancel") } },
        )
    }

    renameTarget?.let { target ->
        var nextAlias by remember(target) { mutableStateOf(target.alias.ifBlank { target.displayName() }) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Rename pen") },
            text = {
                OutlinedTextField(nextAlias, { nextAlias = it }, singleLine = true, shape = RoundedCornerShape(16.dp), label = { Text("Pen name") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    val clean = nextAlias.trim()
                    store.updatePen(target.copy(alias = clean))
                    if (clean.isNotBlank()) store.update { it.copy(penName = clean) }
                    renameTarget = null
                }, shape = CircleShape) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalAiSection(s: AppSettings) {
    val context = LocalContext.current
    val states by LocalAiManager.states.collectAsState()
    var cat by remember { mutableStateOf("All") }
    LaunchedEffect(Unit) { LocalAiManager.refresh(context) }

    Section("Active AI Engine") {
        Box(Modifier.fillMaxWidth()) {
            com.pronotes.app.editor.AiParticleBackdrop(
                modifier = Modifier.matchParentSize(),
                active = states.values.any { it.status == LocalModelStatus.DOWNLOADING },
            )
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProIconView(ProIcon.STAR, Color(0xFF6B5CE7), Modifier.size(24.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(LocalAiManager.activeEngineLabel(context), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (Nano.available) "Gemini Nano AICore is active on this device"
                            else "Download any of the 9 free lightweight models below for richer offline study AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (s.activeLocalModelId.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(start = 54.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Use built-in fallback rules only", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { LocalAiManager.activateModel(context, "") }) { Text("Reset") }
                    }
                }
            }
        }
    }

    Section("Free lightweight models (${LocalAiManager.catalog.size} on-device)") {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("All", "Fast", "Maths", "Reasoning").forEach { c ->
                FilterChip(cat == c, { cat = c }, label = { Text(c) }, shape = CircleShape)
            }
        }
        val filtered = LocalAiManager.catalog.filter { cat == "All" || it.category == cat }
        filtered.forEachIndexed { idx, spec ->
            if (idx > 0) HorizontalDivider()
            val rt = states[spec.id] ?: LocalModelRuntime()
            val isActive = s.activeLocalModelId == spec.id && rt.status == LocalModelStatus.READY
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(spec.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${spec.family} · ${spec.quantization} · ${spec.sizeLabel} · ${"⚡".repeat(spec.speedScore.coerceIn(1, 5))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (isActive) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("Active", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text(spec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    spec.tags.forEach { tag ->
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(tag, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                when (rt.status) {
                    LocalModelStatus.DOWNLOADING -> {
                        val dlMb = rt.downloadedBytes / (1024f * 1024f)
                        val totMb = (rt.totalBytes.coerceAtLeast(1L)) / (1024f * 1024f)
                        LinearProgressIndicator(progress = { rt.progress }, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Downloading ${"%.1f".format(dlMb)} / ${"%.0f".format(totMb)} MB", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { LocalAiManager.cancelDownload(context, spec.id) }) { Text("Cancel") }
                        }
                    }
                    LocalModelStatus.READY -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!isActive) {
                                Button(onClick = { LocalAiManager.activateModel(context, spec.id) }, shape = CircleShape) { Text("Use Model") }
                            }
                            OutlinedButton(onClick = { LocalAiManager.deleteModel(context, spec.id) }, shape = CircleShape) { Text("Delete") }
                        }
                    }
                    LocalModelStatus.ERROR -> {
                        Text(rt.error ?: "Download error", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { LocalAiManager.startDownload(context, spec.id) }, shape = CircleShape) { Text("Retry Download") }
                    }
                    LocalModelStatus.NOT_DOWNLOADED -> {
                        Button(onClick = { LocalAiManager.startDownload(context, spec.id) }, shape = CircleShape) { Text("Download Free (${spec.sizeLabel})") }
                    }
                }
            }
        }
        HorizontalDivider()
        Info("Downloaded GGUF weights are stored privately in app storage and run offline for summaries, flashcards, quizzes, mind maps, and explanations.")
    }
}
