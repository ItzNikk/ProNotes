package com.pronotes.app.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SwapVert
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pronotes.app.BuildConfigInfo
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PaperTint
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.SideButton
import com.pronotes.app.data.ToolbarDock

/** Grouped list of every setting, iOS-style inset sections. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val s by store.settings.collectAsState()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Settings", style = MaterialTheme.typography.headlineLarge)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp),
        ) {
            item {
                Section("Stylus") {
                    Toggle(Icons.Outlined.TouchApp, "Draw with stylus only", "Fingers scroll and zoom", s.stylusOnly) { v -> store.update { it.copy(stylusOnly = v) } }
                    Choice(Icons.Outlined.Brush, "Pen side button", s.sideButton.label, SideButton.entries.map { it.label }) { i ->
                        store.update { it.copy(sideButton = SideButton.entries[i]) }
                    }
                    Info("Works with S Pen, Wacom, USI 2.0, Lenovo, Xiaomi, OnePlus and other Android pens. Pens with an eraser end erase when flipped.")
                }
            }
            item {
                Section("Gestures") {
                    Toggle(Icons.Outlined.AutoFixHigh, "Scribble to erase", "Zig-zag over ink to delete it", s.scribbleToErase) { v -> store.update { it.copy(scribbleToErase = v) } }
                    Toggle(Icons.Outlined.Gesture, "Draw and hold for shapes", "Hold the pen still at the end of a line", s.holdToShape) { v -> store.update { it.copy(holdToShape = v) } }
                    Toggle(Icons.Outlined.Gesture, "Circle and hold to select", "Loop around ink with the pen, then hold", s.circleToLasso) { v -> store.update { it.copy(circleToLasso = v) } }
                    Toggle(Icons.Outlined.TouchApp, "Finger taps undo", "Two-finger tap undo, three-finger tap redo", s.fingerTapUndo) { v -> store.update { it.copy(fingerTapUndo = v) } }
                }
            }
            item {
                Section("Pages") {
                    Choice(Icons.Outlined.SwapVert, "Scroll direction", s.scroll.label, ScrollDirection.entries.map { it.label }) { i ->
                        store.update { it.copy(scroll = ScrollDirection.entries[i]) }
                    }
                    Toggle(Icons.Outlined.Description, "Pull to add a page", "Scroll past the last page to add one", s.pullToAddPage) { v -> store.update { it.copy(pullToAddPage = v) } }
                    Choice(Icons.Outlined.LightMode, "Paper colour", s.paperTint.label, PaperTint.entries.map { it.label }) { i ->
                        store.update { it.copy(paperTint = PaperTint.entries[i]) }
                    }
                    Choice(Icons.Outlined.Description, "Default template", s.defaultPaper.label, PaperStyle.entries.map { it.label }) { i ->
                        store.update { it.copy(defaultPaper = PaperStyle.entries[i]) }
                    }
                    Toggle(Icons.Outlined.LightMode, "Keep screen on", "While a notebook is open", s.keepScreenOn) { v -> store.update { it.copy(keepScreenOn = v) } }
                }
            }
            item {
                Section("Toolbar") {
                    Choice(Icons.Outlined.ViewQuilt, "Position", s.toolbarDock.label, ToolbarDock.entries.map { it.label }) { i ->
                        store.update { it.copy(toolbarDock = ToolbarDock.entries[i]) }
                    }
                    Info("Tip: drag the grip on the toolbar to move it to any edge.")
                }
            }
            item {
                Section("Laser pointer") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Ink stays for", Modifier.weight(1f))
                        Text("%.0f s".format(s.laserSeconds), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(s.laserSeconds, { v -> store.update { it.copy(laserSeconds = v) } }, valueRange = 1f..8f, steps = 6,
                        modifier = Modifier.padding(horizontal = 16.dp))
                    Info("Draw a circle with the laser around ink to select it.")
                }
            }
            item {
                Section("Library") {
                    Toggle(Icons.Outlined.Description, "First page as cover", "New notebooks show page 1 on the cover", s.thumbCovers) { v -> store.update { it.copy(thumbCovers = v) } }
                }
            }
            item {
                Section("About") {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Pro Notes")
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
        IconChip(icon)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(value, onChange)
    }
}

@Composable
private fun Choice(icon: ImageVector, title: String, value: String, options: List<String>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(icon)
            Text(title, Modifier.weight(1f).padding(horizontal = 14.dp))
            Text(value, color = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(open, { open = false }, modifier = Modifier.align(Alignment.CenterEnd)) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onPick(i) }) }
        }
    }
}

@Composable
private fun Info(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
}

@Composable
private fun IconChip(icon: ImageVector) {
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Suppress("unused")
private val unusedColor = Color.Unspecified
