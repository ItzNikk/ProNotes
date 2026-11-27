package com.pronotes.app.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import java.io.FileOutputStream
import com.pronotes.app.support.FeedbackDialog
import com.pronotes.app.editor.Tool
import com.pronotes.app.editor.toolDisplayName
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.BuildConfigInfo
import com.pronotes.app.R
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.AppFont
import com.pronotes.app.data.AppSettings
import com.pronotes.app.data.CloudSync
import com.pronotes.app.data.DeskColor
import com.pronotes.app.data.EraserMode
import com.pronotes.app.data.PagePreset
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.PaperTint
import com.pronotes.app.data.PenAction
import com.pronotes.app.data.PenBrands
import com.pronotes.app.data.PenClickDelay
import com.pronotes.app.data.PenProfile
import com.pronotes.app.data.PerformanceMode
import com.pronotes.app.data.Repository
import com.pronotes.app.data.ScribbleSensitivity
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.SideButton
import com.pronotes.app.data.ThemeMode
import com.pronotes.app.data.ToolbarDock
import com.pronotes.app.data.ToolbarSize
import com.pronotes.app.data.ToolbarStyle
import com.pronotes.app.data.UiColorScheme
import com.pronotes.app.data.Updater
import com.pronotes.app.data.Updater.Phase as UPhase
import com.pronotes.app.editor.HandwritingFontChars
import com.pronotes.app.editor.HandwritingFontStore
import com.pronotes.app.editor.HandwritingStudioDialog
import com.pronotes.app.editor.ToolbarOrder
import com.pronotes.app.library.PaperPreview
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppChoiceRow
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSliderRow
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.GradientTitle
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.appCardShape
import com.pronotes.app.ui.appPillShape
import com.pronotes.app.ui.iconFor
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.DateFormat
import java.util.Date

private enum class Category(val label: String, val sub: String, val icon: ProIcon, val tint: Long) {
    STYLUS("Stylus & input", "Drawing, feel, eraser targets, pens", ProIcon.PEN, 0xFF0044EA),
    GESTURES("Gestures", "Live scribble sensitivity, circle, taps", ProIcon.LASSO, 0xFF00A3F5),
    PAGES("Pages & paper", "Scroll, templates, paper colour", ProIcon.DOCUMENT, 0xFF10B981),
    TOOLBAR("Toolbar", "Floating capsule, left/right dock, timer", ProIcon.TOOLBAR, 0xFFF01E6B),
    LOOK("Appearance & Theme", "UI colour schemes, palette, typography, motion", ProIcon.PALETTE, 0xFFFF8C00),
    LANGUAGE("Language & Region", "App display language, 58+ handwriting packs, RTL", ProIcon.INFO, 0xFF0EA5E9),
    LIBRARY("Library & backup", "Covers, cloud sync, backup and restore", ProIcon.FOLDER, 0xFF7C3AED),
    ABOUT("About", "Version, resumable updates", ProIcon.INFO, 0xFF475569),
}

private fun Category.localizedLabel(strings: com.pronotes.app.data.AppStrings): String = when (this) {
    Category.STYLUS -> strings.catStylus
    Category.GESTURES -> strings.catGestures
    Category.PAGES -> strings.catPages
    Category.TOOLBAR -> strings.catToolbar
    Category.LOOK -> strings.catLook
    Category.LANGUAGE -> strings.catLanguage
    Category.LIBRARY -> strings.catLibrary
    Category.ABOUT -> strings.catAbout
}

private fun Category.localizedSub(strings: com.pronotes.app.data.AppStrings): String = when (this) {
    Category.STYLUS -> strings.catStylusSub
    Category.GESTURES -> strings.catGesturesSub
    Category.PAGES -> strings.catPagesSub
    Category.TOOLBAR -> strings.catToolbarSub
    Category.LOOK -> strings.catLookSub
    Category.LANGUAGE -> strings.catLanguageSub
    Category.LIBRARY -> strings.catLibrarySub
    Category.ABOUT -> strings.catAboutSub
}

private data class SettingSearchEntry(
    val title: String,
    val subtitle: String,
    val category: Category,
    val keywords: String = "",
)

private val SettingsSearchIndex = listOf(
    SettingSearchEntry("Draw with stylus only", "Fingers scroll and zoom, never draw", Category.STYLUS, "pen palm finger"),
    SettingSearchEntry("Palm rejection", "Ignore touches right after pen", Category.STYLUS, "hand wrist touch"),
    SettingSearchEntry("Touch sensitivity", "Adjust touch and palm responsiveness", Category.STYLUS, "slider pressure"),
    SettingSearchEntry("Haptic feedback & pen tick", "Buzz on shapes, selection, and pen landing", Category.STYLUS, "vibration feel"),
    SettingSearchEntry("Pressure sensitivity & nibs", "S Pen pressure curve and stroke feel", Category.STYLUS, "spen pressure"),
    SettingSearchEntry("Your pens & side button", "Hold to erase, click, double-click mapping", Category.STYLUS, "eraser quick actions s pen"),
    SettingSearchEntry("Eraser filters", "Ink, highlighter, shapes, text, images", Category.STYLUS, "pixel word erase"),
    SettingSearchEntry("Scribble to erase", "Sensitivity and toggle for pen scribble erase", Category.GESTURES, "delete scratch"),
    SettingSearchEntry("Circle to lasso", "Circle ink to select and transform", Category.GESTURES, "select loop"),
    SettingSearchEntry("Hold to shape", "Snap and adjust drawn shapes", Category.GESTURES, "line rectangle triangle"),
    SettingSearchEntry("Two & three finger tap undo/redo", "Quick undo and redo taps", Category.GESTURES, "undo redo"),
    SettingSearchEntry("Dynamic bold handwriting", "Auto-bold headings when converting handwriting", Category.GESTURES, "convert text auto bold"),
    SettingSearchEntry("Scroll direction", "Vertical or horizontal page swiping", Category.PAGES, "swipe flip page"),
    SettingSearchEntry("Default paper template & color", "Blank, ruled, dotted, Cornell, planner", Category.PAGES, "paper tint dark grid"),
    SettingSearchEntry("Page size & orientation", "A4, Letter, portrait, landscape", Category.PAGES, "preset portrait"),
    SettingSearchEntry("Toolbar style & position", "Fixed bar, floating pill, compact, dock edge", Category.TOOLBAR, "dock top bottom left right"),
    SettingSearchEntry("Tools in the toolbar", "Reorder and show/hide drawing tools", Category.TOOLBAR, "pen highlighter eraser laser"),
    SettingSearchEntry("Floating video player", "In-screen video overlay button in toolbar", Category.TOOLBAR, "video watch"),
    SettingSearchEntry("Notebook tabs & study timer", "Multi-tab editing and Pomodoro timer", Category.TOOLBAR, "tabs pomodoro"),
    SettingSearchEntry("Theme & color schemes", "Light, dark, system, accent themes", Category.LOOK, "dark mode accent palette"),
    SettingSearchEntry("Live blur & corner roundness", "Glass blur effects, scroll blur, corner radius", Category.LOOK, "glass slider gradient"),
    SettingSearchEntry("Typography & motion", "App font, spring animations, reduced motion", Category.LOOK, "font animation speed"),
    SettingSearchEntry("App display language", "Switch interface language and RTL", Category.LANGUAGE, "locale translation"),
    SettingSearchEntry("Handwriting recognition packs", "Download offline language models", Category.LANGUAGE, "ocr convert text"),
    SettingSearchEntry("Cloud storage & sync", "Google Drive, OneDrive, iCloud Drive, auto backup", Category.LIBRARY, "backup export sync drive"),
    SettingSearchEntry("Library covers & backups", "Page 1 thumbnails, export/import backup zip", Category.LIBRARY, "cover restore"),
    SettingSearchEntry("App version & updates", "Check for updates, changelog, feedback", Category.ABOUT, "update version support"),
)

@Composable
fun SettingsScreen(onBack: () -> Unit, onSupport: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val s by store.settings.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<Category?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var backProgress by remember { mutableStateOf(0f) }
    val reduceMotion = com.pronotes.app.ui.LocalReduceMotion.current
    val perfMode = com.pronotes.app.ui.LocalPerformanceMode.current
    val motionStyle = com.pronotes.app.ui.LocalMotionStyle.current
    val scrollState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val scheme = MaterialTheme.colorScheme
    val appStrings = com.pronotes.app.data.LocalAppStrings.current

    val searchMatches = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else SettingsSearchIndex.filter { entry ->
            entry.title.lowercase().contains(q) ||
                entry.subtitle.lowercase().contains(q) ||
                entry.keywords.lowercase().contains(q) ||
                entry.category.label.lowercase().contains(q)
        }
    }

    com.pronotes.app.ui.GlowingWorkspace {
        val wide = maxWidth >= 840.dp
        val current = selected ?: if (wide) Category.STYLUS else null
        val back: () -> Unit = {
            when {
                searchQuery.isNotEmpty() -> searchQuery = ""
                !wide && selected != null -> selected = null
                else -> onBack()
            }
        }
        PredictiveBackHandler(enabled = !wide && selected != null) { events ->
            try {
                events.collect { backProgress = it.progress }
                selected = null
            } finally {
                backProgress = 0f
            }
        }

        val settingsHazeState = remember { HazeState() }
        val liveBlur = com.pronotes.app.ui.LocalLiveBlur.current && !com.pronotes.app.ui.LocalReduceEffects.current

        Box(Modifier.fillMaxSize()) {
            // Scrollable Settings body — sits behind subtle frosted top bar & bottom blur + gradient search pill
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(settingsHazeState)
                    .statusBarsPadding()
                    .padding(top = 66.dp),
            ) {
                if (wide) {
                    Row(Modifier.fillMaxSize().padding(end = 16.dp)) {
                        scrollState.SaveableStateProvider("categories") {
                            CategoryList(current, Modifier.width(308.dp).fillMaxHeight()) { selected = it }
                        }
                        Box(Modifier.weight(1f)) {
                            val category = current ?: Category.STYLUS
                            AnimatedContent(
                                targetState = category,
                                transitionSpec = {
                                    com.pronotes.app.ui.ProMotion.contentSwap(reduceMotion, perfMode, motionStyle)
                                },
                                label = "Settings category",
                            ) { target ->
                                scrollState.SaveableStateProvider(target.name) { Detail(target, s, store, onSupport) }
                            }
                        }
                    }
                } else if (current == null) {
                    scrollState.SaveableStateProvider("categories") {
                        CategoryList(null, Modifier.fillMaxSize()) { selected = it }
                    }
                } else {
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        val scale = if (reduceMotion) 1f else 1f - backProgress * 0.04f
                        scaleX = scale
                        scaleY = scale
                    }) {
                        scrollState.SaveableStateProvider(current.name) { Detail(current, s, store, onSupport) }
                    }
                }
            }

            // Soft progressive frosted Haze backdrop along the top edge (feathered seamlessly into content)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(88.dp)
                    .then(
                        if (liveBlur) {
                            Modifier.hazeEffect(
                                state = settingsHazeState,
                                style = HazeMaterials.ultraThin(scheme.surface),
                            ) {
                                progressive = HazeProgressive.verticalGradient(
                                    startIntensity = 0.85f,
                                    endIntensity = 0f,
                                )
                            }
                        } else Modifier,
                    )
                    .background(
                        Brush.verticalGradient(
                            0f to scheme.surface.copy(alpha = if (liveBlur) 0.44f else 0.86f),
                            0.50f to scheme.surface.copy(alpha = if (liveBlur) 0.24f else 0.54f),
                            0.85f to scheme.surface.copy(alpha = if (liveBlur) 0.08f else 0.18f),
                            1f to Color.Transparent,
                        ),
                    ),
            )

            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppSurface(
                    role = SurfaceRole.Control,
                    containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.72f),
                    contentColor = scheme.onSurface,
                    borderColor = scheme.outlineVariant.copy(alpha = 0.28f),
                    shape = appPillShape(),
                    contentPadding = PaddingValues(12.dp),
                    onClick = back,
                    onClickLabel = appStrings.back,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, appStrings.back, tint = scheme.onSurface)
                }
                GradientTitle(
                    if (wide) appStrings.settings else current?.localizedLabel(appStrings) ?: appStrings.settings,
                    MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
            }

            // Soft progressive Haze blur along the bottom edge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(92.dp)
                    .then(
                        if (liveBlur) {
                            Modifier.hazeEffect(
                                state = settingsHazeState,
                                style = HazeMaterials.ultraThin(scheme.surface),
                            ) {
                                progressive = HazeProgressive.verticalGradient(
                                    startIntensity = 0f,
                                    endIntensity = 0.85f,
                                )
                            }
                        } else Modifier,
                    )
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.36f to scheme.surface.copy(alpha = if (liveBlur) 0.08f else 0.22f),
                            0.72f to scheme.surface.copy(alpha = if (liveBlur) 0.22f else 0.50f),
                            1f to scheme.surface.copy(alpha = if (liveBlur) 0.40f else 0.78f),
                        ),
                    ),
            )

            // Bigger bottom-left gradient pill search bar (color gradient belongs on the search bar itself, not the icon button)
            var searchFocused by remember { mutableStateOf(false) }
            val searchExpanded = searchFocused || searchQuery.isNotBlank()
            val pillWidth by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (searchExpanded) 396.dp else 264.dp,
                animationSpec = com.pronotes.app.ui.ProMotion.sheetSpring(reduceMotion, perfMode, motionStyle),
                label = "settingsSearchPillW",
            )
            val searchGradientBorder = remember(searchExpanded) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF3B82F6).copy(alpha = if (searchExpanded) 0.85f else 0.58f),
                        Color(0xFF8B5CF6).copy(alpha = if (searchExpanded) 0.85f else 0.58f),
                        Color(0xFFEC4899).copy(alpha = if (searchExpanded) 0.80f else 0.52f),
                        Color(0xFF06B6D4).copy(alpha = if (searchExpanded) 0.80f else 0.52f),
                    ),
                )
            }
            val searchGradientFill = remember(searchExpanded) {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF3B82F6).copy(alpha = if (searchExpanded) 0.34f else 0.26f),
                        Color(0xFF8B5CF6).copy(alpha = if (searchExpanded) 0.32f else 0.24f),
                        Color(0xFFEC4899).copy(alpha = if (searchExpanded) 0.28f else 0.21f),
                        Color(0xFF06B6D4).copy(alpha = if (searchExpanded) 0.28f else 0.20f),
                    ),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnimatedVisibility(
                    visible = searchQuery.isNotBlank(),
                    enter = fadeIn(tween(170)) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = tween(200)),
                    exit = fadeOut(tween(130)) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = tween(150)),
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = scheme.surfaceContainerHigh.copy(alpha = 0.94f),
                        border = BorderStroke(1.2.dp, searchGradientBorder),
                        shadowElevation = 10.dp,
                        modifier = Modifier.widthIn(min = 300.dp, max = 420.dp),
                    ) {
                        if (searchMatches.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    "No matching settings for \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                items(searchMatches.take(6).size) { idx ->
                                    val hit = searchMatches[idx]
                                    val accent = Color(hit.category.tint)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                selected = hit.category
                                                searchQuery = ""
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(
                                            Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(accent.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (hit.category == Category.LANGUAGE) {
                                                Icon(Icons.Outlined.Language, null, Modifier.size(18.dp), tint = accent)
                                            } else {
                                                com.pronotes.app.ui.WorkspaceIcon(hit.category.icon, accent, Modifier.size(18.dp))
                                            }
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                hit.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = scheme.onSurface,
                                            )
                                            Text(
                                                "${hit.category.localizedLabel(appStrings)} · ${hit.subtitle}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = scheme.onSurfaceVariant,
                                                maxLines = 1,
                                            )
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            null,
                                            Modifier.size(18.dp),
                                            tint = accent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bigger, smooth gradient-colored floating pill search bar on bottom-left (no RenderEffect during width resize)
                Box(
                    modifier = Modifier
                        .widthIn(min = 264.dp, max = 396.dp)
                        .width(pillWidth)
                        .defaultMinSize(minHeight = 56.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh.copy(alpha = 0.88f))
                        .background(searchGradientFill)
                        .border(width = if (searchExpanded) 1.6.dp else 1.2.dp, brush = searchGradientBorder, shape = CircleShape),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = "Search settings",
                            tint = scheme.onSurface.copy(alpha = 0.82f),
                            modifier = Modifier.size(20.dp),
                        )
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search settings…",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = scheme.onSurface.copy(alpha = 0.76f),
                                    maxLines = 1,
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                                cursorBrush = SolidColor(Color(0xFF8B5CF6)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { searchFocused = it.isFocused },
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(scheme.onSurface.copy(alpha = 0.14f))
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Close, "Clear search", Modifier.size(15.dp), tint = scheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryList(selected: Category?, modifier: Modifier, onPick: (Category) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val strings = com.pronotes.app.data.LocalAppStrings.current
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(Category.entries.size, key = { Category.entries[it].name }) { i ->
            val c = Category.entries[i]
            val active = c == selected
            val accent = Color(c.tint)
            Surface(
                onClick = { onPick(c) },
                modifier = Modifier.fillMaxWidth().semantics { this.selected = active },
                shape = RoundedCornerShape(22.dp),
                color = if (active) androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, accent, 0.17f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.07f)),
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(c.localizedLabel(strings), fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(c.localizedSub(strings), style = MaterialTheme.typography.bodySmall) },
                    leadingContent = {
                        Box(Modifier.size(44.dp).background(accent.copy(alpha = 0.15f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                            if (c == Category.LANGUAGE) Icon(Icons.Outlined.Language, null, Modifier.size(23.dp), tint = accent)
                            else com.pronotes.app.ui.WorkspaceIcon(c.icon, accent, Modifier.size(23.dp))
                        }
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp), tint = if (active) accent else scheme.onSurfaceVariant) },
                )
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Detail(c: Category, s: AppSettings, store: SettingsStore, onSupport: () -> Unit) {
    val context = LocalContext.current
    fun up(block: (AppSettings) -> AppSettings) = store.update(block)
    val detailListState = rememberLazyListState()
    val scrolled by remember(detailListState) {
        derivedStateOf { detailListState.firstVisibleItemIndex > 0 || detailListState.firstVisibleItemScrollOffset > 10 }
    }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = detailListState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 108.dp),
    ) {
        item(key = "category-heading") {
            val strings = com.pronotes.app.data.LocalAppStrings.current
            Row(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(52.dp).background(Color(c.tint).copy(alpha = 0.16f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    if (c == Category.LANGUAGE) Icon(Icons.Outlined.Language, null, Modifier.size(28.dp), tint = Color(c.tint))
                    else com.pronotes.app.ui.WorkspaceIcon(c.icon, Color(c.tint), Modifier.size(28.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(c.localizedLabel(strings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                    Text(c.localizedSub(strings), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            when (c) {
                Category.STYLUS -> {
                    Section("Drawing") {
                        Toggle("Draw with stylus only", "Fingers scroll and zoom, they never draw", s.stylusOnly) { v -> up { it.copy(stylusOnly = v) } }
                        Toggle("Palm rejection", "Ignore touches right after the pen", s.palmRejection) { v -> up { it.copy(palmRejection = v) } }
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            AppSliderRow(
                                title = "Touch sensitivity",
                                valueText = "${(s.touchSensitivity * 100).toInt()}%",
                                value = s.touchSensitivity.coerceIn(0.4f, 2.0f),
                                valueRange = 0.4f..2.0f,
                                onValueChange = { v -> up { it.copy(touchSensitivity = v.coerceIn(0.4f, 2.0f)) } },
                            )
                        }
                        Toggle("Haptic feedback", "Small buzz on shapes, selection and page add", s.haptics) { v -> up { it.copy(haptics = v) } }
                        Toggle("Pen touch tick", "A light tick when the pen lands", s.penHaptics) { v -> up { it.copy(penHaptics = v) } }
                    }
                    Section("What the eraser erases") {
                        Segmented(EraserMode.entries.map { it.label }, s.eraserMode.ordinal) { i ->
                            up { it.copy(eraserMode = EraserMode.entries[i]) }
                        }
                        val ef = s.eraserFilter
                        FlowRow(
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AppChip(
                                selected = ef.ink && !s.eraseHighlighterOnly,
                                onClick = { up { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(ink = !ef.ink)) } },
                                label = "Pen Ink",
                            )
                            AppChip(
                                selected = ef.highlighter || s.eraseHighlighterOnly,
                                onClick = {
                                    val next = !ef.highlighter
                                    up { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(highlighter = next)) }
                                },
                                label = "Highlighter",
                            )
                            AppChip(
                                selected = ef.shapes && !s.eraseHighlighterOnly,
                                onClick = { up { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(shapes = !ef.shapes)) } },
                                label = "Shapes & Fills",
                            )
                            AppChip(
                                selected = ef.text && !s.eraseHighlighterOnly,
                                onClick = { up { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(text = !ef.text)) } },
                                label = "Text & Write Boxes",
                            )
                            AppChip(
                                selected = ef.stickyAndTape && !s.eraseHighlighterOnly,
                                onClick = { up { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(stickyAndTape = !ef.stickyAndTape)) } },
                                label = "Sticky notes and masking tape",
                            )
                            AppChip(
                                selected = ef.images && !s.eraseHighlighterOnly,
                                onClick = { up { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(images = !ef.images)) } },
                                label = "Images & Stickers",
                            )
                        }
                        Toggle("Erase highlighter only", "Quickly restrict erasing to highlighter strokes only", s.eraseHighlighterOnly) { v ->
                            up {
                                it.copy(
                                    eraseHighlighterOnly = v,
                                    eraserFilter = if (v) it.eraserFilter.copy(highlighter = true) else it.eraserFilter,
                                )
                            }
                        }
                        Toggle("Auto-switch back to pen", "Return to your previous tool as soon as you lift the eraser", s.eraserAutoSwitch) { v ->
                            up { it.copy(eraserAutoSwitch = v) }
                        }
                    }
                    Section("Paper feel & sounds") {
                        Toggle("Writing sound", "A soft pencil-on-paper sound that follows your speed", s.paperSound) { v -> up { it.copy(paperSound = v) } }
                        Toggle("Book page-flip sound", "Realistic paper rustle & page turn sound in Kindle Mode", s.bookFlipSound) { v -> up { it.copy(bookFlipSound = v) } }
                        if (s.paperSound || s.bookFlipSound) {
                            Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                AppSliderRow(
                                    title = "Paper & page-flip volume",
                                    valueText = "${(s.paperVolume * 100).toInt()}%",
                                    value = s.paperVolume,
                                    valueRange = 0.05f..1f,
                                    onValueChange = { v -> up { it.copy(paperVolume = v) } },
                                )
                            }
                        }
                        Toggle("Paper haptics", "Tiny ticks as you write, like paper grain", s.paperHaptics) { v -> up { it.copy(paperHaptics = v) } }
                    }
                    Section("Stylus behaviour") {
                        Choice(
                            title = "Button click delay",
                            value = s.penClickDelay.label,
                            options = PenClickDelay.entries.map { it.label },
                            subtitle = "Timing window to distinguish single vs. double button clicks",
                        ) { i -> up { it.copy(penClickDelay = PenClickDelay.entries[i]) } }
                        Toggle("Hover preview", "Show where the pen tip will touch before writing", s.hoverCursor) { v -> up { it.copy(hoverCursor = v) } }
                        val hoverScope = rememberCoroutineScope()
                        val hoverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                            if (uri != null) {
                                hoverScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val savedPath = runCatching {
                                        val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
                                        val maxSide = maxOf(raw.width, raw.height)
                                        val scaled = if (maxSide > 192) {
                                            val f = 192f / maxSide
                                            android.graphics.Bitmap.createScaledBitmap(raw, (raw.width * f).toInt().coerceAtLeast(16), (raw.height * f).toInt().coerceAtLeast(16), true)
                                        } else raw
                                        val outFile = File(context.filesDir, "custom_hover_icon_${System.currentTimeMillis()}.png")
                                        FileOutputStream(outFile).use { scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                                        if (scaled !== raw) scaled.recycle()
                                        raw.recycle()
                                        outFile.absolutePath
                                    }.getOrNull()
                                    if (savedPath != null) {
                                        up { it.copy(customHoverIconPath = savedPath, hoverCursor = true) }
                                    }
                                }
                            }
                        }
                        val customBmp = remember(s.customHoverIconPath) {
                            if (s.customHoverIconPath.isNotBlank() && File(s.customHoverIconPath).exists()) {
                                runCatching { BitmapFactory.decodeFile(s.customHoverIconPath)?.asImageBitmap() }.getOrNull()
                            } else null
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { hoverPicker.launch("image/*") }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (customBmp != null) {
                                    Image(customBmp, contentDescription = "Custom hover icon", contentScale = ContentScale.Fit, modifier = Modifier.size(32.dp))
                                } else {
                                    Icon(Icons.Outlined.Image, contentDescription = null, tint = ProButtonPalette.ElectricBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Custom hover icon", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                Text(
                                    if (customBmp != null) "Custom image active · Tap to change" else "Import any PNG or photo as your stylus hover cursor",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (customBmp != null) {
                                TextButton(onClick = { up { it.copy(customHoverIconPath = "") } }) {
                                    Text("Reset")
                                }
                            }
                        }
                        if (customBmp != null) {
                            Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                AppSliderRow(
                                    title = "Hover icon size",
                                    valueText = "${s.hoverIconSizeDp.toInt()} dp",
                                    value = s.hoverIconSizeDp.coerceIn(14f, 64f),
                                    valueRange = 14f..64f,
                                    onValueChange = { v -> up { it.copy(hoverIconSizeDp = v.coerceIn(14f, 64f)) } },
                                )
                            }
                        }
                    }
                    Section("Personal handwriting") {
                        val hwContext = LocalContext.current
                        val hwStore = remember(hwContext) { HandwritingFontStore.get(hwContext) }
                        val hwProfile by hwStore.profile.collectAsStateWithLifecycle()
                        var showHwStudio by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHwStudio = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = ProButtonPalette.Emerald,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("My Handwriting Style")
                                Text(
                                    "${hwProfile.trainedCharCount()} / ${HandwritingFontChars.ALL.size} characters recorded · Type to place ink in your own hand",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (showHwStudio) {
                            HandwritingStudioDialog(onDismiss = { showHwStudio = false })
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Toggle(
                            "Writing guide line",
                            "Show a baseline guide under text in Align & Auto-Text modes",
                            s.toolDefaults.alignGuideLine,
                        ) { v -> up { it.copy(toolDefaults = it.toolDefaults.copy(alignGuideLine = v)) } }
                        Toggle(
                            "Dynamic bold for large handwriting",
                            "Automatically bold larger headings during handwriting conversion",
                            s.toolDefaults.dynamicBoldHandwriting,
                        ) { v -> up { it.copy(toolDefaults = it.toolDefaults.copy(dynamicBoldHandwriting = v)) } }
                        Toggle(
                            "Auto spell check",
                            "Automatically correct typos in converted handwriting and typed notes",
                            s.toolDefaults.autoSpellCheck,
                        ) { v -> up { it.copy(toolDefaults = it.toolDefaults.copy(autoSpellCheck = v)) } }
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            val activeAutoFont = remember(s.toolDefaults.autoScript) {
                                com.pronotes.app.editor.TextFont.entries.find { it.name == s.toolDefaults.autoScript }
                                    ?: com.pronotes.app.editor.TextFont.CURSIVE
                            }
                            com.pronotes.app.editor.CustomHandwritingFontPicker(
                                selectedPath = s.toolDefaults.customHandwritingFontPath,
                                fgColor = MaterialTheme.colorScheme.onSurface,
                                selectedFont = activeAutoFont,
                                onSelectFont = { font ->
                                    up { it.copy(toolDefaults = it.toolDefaults.copy(autoScript = font.name, customHandwritingFontPath = "")) }
                                },
                                onSelect = { path ->
                                    up { it.copy(toolDefaults = it.toolDefaults.copy(customHandwritingFontPath = path)) }
                                },
                            )
                        }
                    }
                    PensSection(store)
                }
                Category.GESTURES -> {
                    Section("Handwriting conversion") {
                        Toggle("Triple-tap ink to text", "Convert existing ink with three deliberate stylus taps", s.tripleTapToText) { value -> up { it.copy(tripleTapToText = value) } }
                    }
                    Section("Pen gestures") {
                        Toggle("Scribble to erase", "Zig-zag or scrub over ink to delete it", s.scribbleToErase) { v -> up { it.copy(scribbleToErase = v) } }
                        if (s.scribbleToErase) {
                            Text("SCRIBBLE SENSITIVITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                            Segmented(ScribbleSensitivity.entries.map { it.label }, s.scribbleSensitivity.ordinal) { i ->
                                up { it.copy(scribbleSensitivity = ScribbleSensitivity.entries[i]) }
                            }
                        }
                        Toggle("Draw and hold for shapes", "Hold the pen still at the end of a line", s.holdToShape) { v -> up { it.copy(holdToShape = v) } }
                        Toggle("Circle and hold to select", "Loop around ink, lift, then press and hold the circle dot", s.circleToLasso) { v -> up { it.copy(circleToLasso = v) } }
                    }
                    Section("Finger gestures") {
                        Toggle("Tap to undo / redo", "Two-finger tap undo, three-finger tap redo", s.fingerTapUndo) { v -> up { it.copy(fingerTapUndo = v) } }
                        Toggle("Double-tap to zoom", "Double-tap with one finger", s.doubleTapZoom) { v -> up { it.copy(doubleTapZoom = v) } }
                    }
                }
                Category.PAGES -> {
                    Section("Pages & Swiping") {
                        Text(
                            "SCROLL DIRECTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        )
                        Segmented(ScrollDirection.entries.map { it.label }, s.scroll.ordinal) { i ->
                            up { it.copy(scroll = ScrollDirection.entries[i]) }
                        }
                        Toggle("Swipe to add a page", "Swipe past the last page to add a new page", s.pullToAddPage) { v -> up { it.copy(pullToAddPage = v) } }
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
                        val presets = PagePreset.entries.filter { it != PagePreset.CUSTOM }
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
                            PaperStyle.pickerEntries.forEach { p ->
                                val sel = s.defaultPaper == p
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { up { it.copy(defaultPaper = p) } }) {
                                    PaperPreview(
                                        p,
                                        Modifier
                                            .size(width = 60.dp, height = 84.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(
                                                if (sel) 2.dp else 0.75.dp,
                                                if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(6.dp),
                                            ),
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
                        Segmented(ToolbarStyle.entries.map { it.label }, s.toolbarStyle.ordinal) { i ->
                            val nextStyle = ToolbarStyle.entries[i]
                            up {
                                val nextDock = if (nextStyle == ToolbarStyle.FIXED_BAR && it.toolbarDock == ToolbarDock.FREE) ToolbarDock.TOP else it.toolbarDock
                                it.copy(toolbarStyle = nextStyle, toolbarDock = nextDock)
                            }
                        }
                        Choice("Position", s.toolbarDock.label, ToolbarDock.entries.map { it.label }) { i ->
                            val nextDock = ToolbarDock.entries[i]
                            val defaultPos = when (nextDock) {
                                ToolbarDock.TOP -> "0.5,0.03"
                                ToolbarDock.BOTTOM, ToolbarDock.FREE -> "0.5,0.88"
                                ToolbarDock.LEFT -> "0.02,0.5"
                                ToolbarDock.RIGHT -> "0.94,0.5"
                            }
                            up {
                                val nextStyle = if (nextDock == ToolbarDock.FREE && it.toolbarStyle == ToolbarStyle.FIXED_BAR) ToolbarStyle.FLOATING_PILL else it.toolbarStyle
                                it.copy(toolbarDock = nextDock, toolbarStyle = nextStyle, toolbarPos = defaultPos)
                            }
                        }
                        Segmented(ToolbarSize.entries.map { it.label }, s.toolbarSize.ordinal) { i -> up { it.copy(toolbarSize = ToolbarSize.entries[i]) } }
                        Toggle("Hover tool name labels", "Show a floating tool badge when hovering with a pen or pointer", s.showHoverTooltips) { v -> up { it.copy(showHoverTooltips = v) } }
                        Toggle("In-screen video player", "Show the floating video player button inside the editor toolbar", s.videoInToolbar) { value -> up { it.copy(videoInToolbar = value) } }
                    }
                    Section("Tools in the toolbar") {
                        ToolbarEditor(s.toolbarTools) { spec -> up { it.copy(toolbarTools = spec) } }
                        Info("Drag the toolbar handle inside the editor to reposition it anywhere, or reorder tools above.")
                    }
                    Section("Workspace & Study Timer") {
                        Toggle("Notebook tabs", "Keep several notebooks open and switch between them", s.showTabs) { v -> up { it.copy(showTabs = v) } }
                        Toggle("Study timer", "Track focused study sessions in the top bar", s.studyTimer) { v -> up { it.copy(studyTimer = v) } }
                        if (s.studyTimer) {
                            val pomos = listOf(15, 25, 45)
                            Choice("Focus Pomodoro length", "${s.pomodoroMinutes} min", pomos.map { "$it min" }) { i -> up { it.copy(pomodoroMinutes = pomos[i]) } }
                        }
                    }
                }
                Category.LOOK -> {
                    Section("Appearance") {
                        Toggle("Live blur", "Soften the page behind editor panels and dialogs on Android 12 or later", s.liveBlur) { enabled ->
                            up { it.copy(liveBlur = enabled) }
                        }
                        Toggle("Top progressive scroll blur", "Apply a soft progressive top blur when scrolling down lists and pages", s.topScrollBlur) { enabled ->
                            up { it.copy(topScrollBlur = enabled) }
                        }
                        Segmented(ThemeMode.entries.map { it.label }, s.themeMode.ordinal) { i -> up { it.copy(themeMode = ThemeMode.entries[i]) } }
                        Toggle(
                            stringResource(R.string.setting_classic_appearance),
                            stringResource(R.string.setting_classic_appearance_sub),
                            s.classicAppearance,
                        ) { v -> up { it.copy(classicAppearance = v) } }
                        Toggle(
                            stringResource(R.string.setting_reduce_effects),
                            stringResource(R.string.setting_reduce_effects_sub),
                            s.reduceEffects,
                        ) { v -> up { it.copy(reduceEffects = v) } }
                        Toggle("Colorful gradient text", "Vibrant multi-color gradients on titles, headers, and sections", s.colorfulUiText) { v ->
                            up { it.copy(colorfulUiText = v, gradientTitles = v) }
                        }
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            AppSliderRow(
                                title = "Corner roundness",
                                valueText = "${s.cornerRoundnessDp.toInt()} dp",
                                value = s.cornerRoundnessDp.coerceIn(8f, 32f),
                                valueRange = 8f..32f,
                                onValueChange = { v -> up { it.copy(cornerRoundnessDp = v.coerceIn(8f, 32f)) } },
                            )
                        }
                    }
                    Section("Color themes") {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UiColorScheme.entries.chunked(2).forEach { pair ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEach { scheme ->
                                        val selected = s.uiColorScheme == scheme
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                )
                                                .border(
                                                    width = if (selected) 2.dp else 1.dp,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(14.dp),
                                                )
                                                .clickable { up { it.copy(uiColorScheme = scheme) } }
                                                .padding(12.dp),
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                // Preview swatch trio (light surface, dark surface, primary seed)
                                                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                                    Box(
                                                        Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(scheme.lightBg))
                                                            .border(1.dp, Color.Black.copy(alpha = 0.18f), CircleShape),
                                                    )
                                                    Box(
                                                        Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(scheme.darkBg))
                                                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                                                    )
                                                    Box(
                                                        Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(scheme.seedPrimary))
                                                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                                    )
                                                }
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        scheme.label,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                    Text(
                                                        scheme.subtitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Section("Typography") {
                        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppFont.entries.forEach { f ->
                                AppChip(
                                    selected = s.appFont == f,
                                    onClick = { up { it.copy(appFont = f) } },
                                    label = f.label,
                                    tint = ProButtonPalette.ElectricBlue,
                                )
                            }
                        }
                        Info("Applies across titles, studio panels, and interface controls.")
                    }
                    Section("Performance") {
                        Toggle(
                            title = "Performance Mode",
                            sub = "Boosts spring responsiveness, increases adjacent PDF/book page prefetching, and keeps crisp high-res thumbnails.",
                            value = s.performanceMode == PerformanceMode.MAXIMUM,
                            onChange = { enabled ->
                                up {
                                    it.copy(
                                        performanceMode = if (enabled) PerformanceMode.MAXIMUM else PerformanceMode.BALANCED,
                                    )
                                }
                            },
                        )
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Segmented(PerformanceMode.entries.map { it.label }, s.performanceMode.ordinal) { i ->
                            up { it.copy(performanceMode = PerformanceMode.entries[i]) }
                        }
                        val perfDesc = when (s.performanceMode) {
                            PerformanceMode.BATTERY_SAVER -> "Battery saver: reduced motion, 2.4 s autosave intervals, 1-page PDF prefetch, and compact cover thumbnails."
                            PerformanceMode.BALANCED -> "Balanced: crisp spring animations, 3-page PDF prefetch, balanced cover resolution, and 1.2 s debounced saves."
                            PerformanceMode.MAXIMUM -> "Maximum: snappiest spring physics, 5-page PDF & book prefetch, full-res GPU-uploaded covers, and instant responsiveness."
                        }
                        Info(perfDesc)
                    }
                    Section("Accent & Desk") {
                        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccentColor.entries.forEach { a ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { up { it.copy(accent = a) } }) {
                                    Box(
                                        Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(Color(a.argb), Color(a.argb2))))
                                            .border(if (s.accent == a) 2.5.dp else 1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (s.accent == a) 1f else 0.2f), CircleShape),
                                    )
                                    Text(a.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DeskColor.entries.forEach { d -> Swatchy(Color(d.argb), d.label, s.deskColor == d) { up { it.copy(deskColor = d) } } }
                        }
                    }
                }
                Category.LANGUAGE -> {
                    val strings = com.pronotes.app.data.LocalAppStrings.current
                    val scope = rememberCoroutineScope()
                    var showAppLangPicker by remember { mutableStateOf(false) }
                    var showHwLangPicker by remember { mutableStateOf(false) }
                    val activeAppLang = remember(s.appLanguage) { com.pronotes.app.data.AppLanguages.findByTag(s.appLanguage) }
                    val activeHwLang = remember(s.toolDefaults.handwritingLanguage) {
                        com.pronotes.app.editor.HandwritingRecognizer.findLanguage(s.toolDefaults.handwritingLanguage)
                    }
                    val installedUiPacks by com.pronotes.app.data.LanguagePackManager.installedTags.collectAsStateWithLifecycle()
                    val downloadedPacks by com.pronotes.app.editor.HandwritingRecognizer.downloadedTags.collectAsStateWithLifecycle()

                    androidx.compose.runtime.LaunchedEffect(context) {
                        com.pronotes.app.data.LanguagePackManager.refreshInstalled(context)
                        com.pronotes.app.editor.HandwritingRecognizer.refreshDownloadedModels()
                    }

                    Section(strings.appDisplayLanguage) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAppLangPicker = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Translate,
                                contentDescription = null,
                                tint = ProButtonPalette.ElectricBlue,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (activeAppLang.tag.isEmpty()) strings.systemDefault else activeAppLang.nativeName,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "English built-in · ${installedUiPacks.size} optional packs downloaded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        val readyUiOptions = remember(installedUiPacks) {
                            com.pronotes.app.data.AppLanguages.ALL.filter { it.builtIn || it.tag in installedUiPacks }
                        }
                        FlowRow(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            readyUiOptions.forEach { opt ->
                                AppChip(
                                    selected = s.appLanguage.equals(opt.tag, ignoreCase = true),
                                    onClick = { up { it.copy(appLanguage = opt.tag) } },
                                    label = if (opt.tag.isEmpty()) strings.systemDefault else opt.nativeName,
                                    tint = ProButtonPalette.ElectricBlue,
                                )
                            }
                            AppChip(
                                selected = false,
                                onClick = { showAppLangPicker = true },
                                label = "Download languages (25+ packs)…",
                                tint = ProButtonPalette.Violet,
                            )
                        }
                        if (installedUiPacks.isNotEmpty()) {
                            HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            val kb = (com.pronotes.app.data.LanguagePackManager.installedBytes(context) / 1024L).coerceAtLeast(1L)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${installedUiPacks.size} optional UI pack(s) · ~$kb KB on device",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = {
                                    com.pronotes.app.data.LanguagePackManager.clearAllPacks(context)
                                    if (!com.pronotes.app.data.AppLanguages.isBuiltIn(s.appLanguage)) {
                                        up { it.copy(appLanguage = "") }
                                    }
                                }) {
                                    Text("Remove all", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Info("Only English is bundled by default to keep Pro Notes lightweight. Download optional language packs anytime and remove them whenever you want.")
                    }

                    Section(strings.handwritingRecognitionLang) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHwLangPicker = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Language,
                                contentDescription = null,
                                tint = ProButtonPalette.Emerald,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (activeHwLang.nativeName == activeHwLang.name) activeHwLang.name
                                    else "${activeHwLang.nativeName} (${activeHwLang.name})",
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${activeHwLang.region} · ${downloadedPacks.size} ${strings.downloaded.lowercase()} · ${com.pronotes.app.editor.HandwritingRecognizer.supportedLanguages.size} optional packs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        FlowRow(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            com.pronotes.app.editor.HandwritingRecognizer.quickLanguages.forEach { lang ->
                                AppChip(
                                    selected = activeHwLang.tag.equals(lang.tag, ignoreCase = true),
                                    onClick = {
                                        up { it.copy(toolDefaults = it.toolDefaults.copy(handwritingLanguage = lang.tag)) }
                                    },
                                    label = lang.nativeName,
                                    tint = ProButtonPalette.Emerald,
                                )
                            }
                            AppChip(
                                selected = false,
                                onClick = { showHwLangPicker = true },
                                label = "Manage all ${com.pronotes.app.editor.HandwritingRecognizer.supportedLanguages.size}+ packs…",
                                tint = ProButtonPalette.ElectricBlue,
                            )
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Toggle(
                            "Auto spell check",
                            "Multilingual spell check & accent restoration for converted handwriting and typed notes",
                            s.toolDefaults.autoSpellCheck,
                        ) { v -> up { it.copy(toolDefaults = it.toolDefaults.copy(autoSpellCheck = v)) } }
                        Info("Handwriting packs are never bundled into the APK—download only the languages you write in (Latin, Devanagari, Bengali, Tamil, Telugu, CJK, Japanese, Korean, Arabic, Hebrew, Cyrillic, Thai, Vietnamese, and more) and delete unused models anytime.")
                    }

                    if (showAppLangPicker) {
                        com.pronotes.app.ui.AppLanguagePickerDialog(
                            selectedTag = s.appLanguage,
                            onDismiss = { showAppLangPicker = false },
                            onSelect = { opt ->
                                up { it.copy(appLanguage = opt.tag) }
                                showAppLangPicker = false
                            },
                        )
                    }
                    if (showHwLangPicker) {
                        com.pronotes.app.ui.HandwritingLanguagePickerDialog(
                            selectedTag = s.toolDefaults.handwritingLanguage,
                            onDismiss = { showHwLangPicker = false },
                            onSelect = { lang ->
                                up { it.copy(toolDefaults = it.toolDefaults.copy(handwritingLanguage = lang.tag)) }
                                showHwLangPicker = false
                            },
                        )
                    }
                }
                Category.LIBRARY -> {
                    CloudAccountSection(s)
                    Section("Cloud storage (Drive · OneDrive · iCloud)") {
                        val libRepo = remember { Repository.get(context) }
                        com.pronotes.app.data.CloudStorageProvidersPanel(
                            repo = libRepo,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    Section("Notes storage folder") {
                        StorageFolderRows(s) { block -> up(block) }
                    }
                    Section("Covers") {
                        Toggle("First page as cover", "New notebooks show page 1 on the cover", s.thumbCovers) { v -> up { it.copy(thumbCovers = v) } }
                    }
                    Section("Backup") { BackupRows() }
                    Section("Settings file") { SettingsJsonRows(store) }
                }
                Category.ABOUT -> {
                    var showFeedback by remember { mutableStateOf(false) }
                    Section("Pro Notes") {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProIconView(ProIcon.INFO, ProButtonPalette.ElectricBlue, Modifier.size(22.dp))
                            Spacer(Modifier.size(16.dp))
                            Column(Modifier.weight(1f)) {
                                GradientTitle("Pro Notes", MaterialTheme.typography.titleLarge)
                                Text("Version ${BuildConfigInfo.versionName(context)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Precision Note-Taking & PDF Studio · Apache License 2.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { showFeedback = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.BugReport, contentDescription = null, tint = ProButtonPalette.VibrantOrange, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.size(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Report a bug or give feedback", fontWeight = FontWeight.SemiBold)
                                Text("Free direct message to the developer · GitHub · Email", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        UpdateRow()
                        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Toggle("Check for updates automatically", "Check when the library opens", s.autoUpdateCheck) { v -> up { it.copy(autoUpdateCheck = v) } }
                    }
                    if (showFeedback) {
                        FeedbackDialog(onDismiss = { showFeedback = false })
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SettingsJsonRows(store: SettingsStore) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(store.exportSettingsJson().toByteArray(Charsets.UTF_8))
                }
                true
            }.getOrDefault(false)
            Toast.makeText(context, if (ok) "Settings exported" else "Could not export settings", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: return@runCatching false
                store.importSettingsJson(raw)
            }.getOrDefault(false)
            Toast.makeText(context, if (ok) "Settings imported" else "Invalid settings file", Toast.LENGTH_SHORT).show()
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { exportLauncher.launch("pronotes_settings.json") }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Export settings to file", fontWeight = FontWeight.Medium)
            Text("Save all preferences, palettes, and presets as JSON", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Import settings from file", fontWeight = FontWeight.Medium)
            Text("Restore preferences from a saved JSON file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 10.dp).semantics { heading() },
        )
        OutlinedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.fillMaxWidth().toggleable(value = value, role = Role.Switch, onValueChange = onChange),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(sub, style = MaterialTheme.typography.bodySmall) },
        trailingContent = { Switch(checked = value, onCheckedChange = null) },
    )
}
@Composable
private fun Choice(
    title: String,
    value: String,
    options: List<String>,
    subtitle: String? = null,
    onPick: (Int) -> Unit,
) {
    Box(Modifier.padding(horizontal = 12.dp)) {
        AppChoiceRow(title = title, value = value, options = options, onPick = onPick, subtitle = subtitle)
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(12.dp)) {
        AppSegmentedControl(
            options = options,
            selectedIndex = selected,
            onSelect = onPick,
        )
    }
}

@Composable
private fun Swatchy(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .size(width = 48.dp, height = 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(
                    if (selected) 3.dp else 0.75.dp,
                    if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(8.dp),
                ),
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Info(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun ToolbarEditor(spec: String, onChange: (String) -> Unit) {
    val entries = remember(spec) { ToolbarOrder.parse(spec) }
    Column(Modifier.fillMaxWidth()) {
        entries.forEachIndexed { i, (tool, visible) ->
            if (i > 0) {
                HorizontalDivider(Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onChange(ToolbarOrder.setVisible(spec, tool, !visible))
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProIconView(iconFor(tool), MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = toolDisplayName(tool),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    if (tool == Tool.AUTO_TEXT) {
                        Text(
                            "Optional · Write & auto-convert handwriting to text",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (tool == Tool.WEBLINK) {
                        Text(
                            "Embed live interactive web pages directly on your notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    enabled = i > 0,
                    onClick = {
                        val next = entries.toMutableList().apply {
                            val item = removeAt(i)
                            add(i - 1, item)
                        }
                        onChange(ToolbarOrder.encode(next))
                    },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    enabled = i < entries.lastIndex,
                    onClick = {
                        val next = entries.toMutableList().apply {
                            val item = removeAt(i)
                            add(i + 1, item)
                        }
                        onChange(ToolbarOrder.encode(next))
                    },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                AppToggle(
                    checked = visible,
                    onCheckedChange = { v ->
                        onChange(ToolbarOrder.setVisible(spec, tool, v))
                    },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { onChange("") }) {
                Text("Reset default tools")
            }
        }
    }
}

@Composable
private fun StorageFolderRows(
    s: AppSettings,
    up: ((AppSettings) -> AppSettings) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            val label = com.pronotes.app.data.StorageFolderSync.friendlyFolderName(uri)
            up { it.copy(storageFolderUri = uri.toString(), storageFolderLabel = label) }
            scope.launch {
                syncing = true
                status = runCatching {
                    val count = com.pronotes.app.data.StorageFolderSync.syncAllNotebooks(context, uri, repo)
                    "Saved $count notebook(s) to $label"
                }.getOrElse { "Could not save to folder: ${it.message}" }
                syncing = false
            }
        }
    }

    val hasCustom = s.storageFolderUri.isNotBlank()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { pickFolder.launch(null) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProIconView(ProIcon.FOLDER, Color(0xFF7C3AED), Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (hasCustom) "Storage location" else "Choose storage folder",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (hasCustom) s.storageFolderLabel.ifBlank { s.storageFolderUri }
                else "Default app storage · Tap to pick any folder on your device",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (hasCustom) {
        HorizontalDivider(Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !syncing) {
                    val uri = runCatching { android.net.Uri.parse(s.storageFolderUri) }.getOrNull() ?: return@clickable
                    scope.launch {
                        syncing = true
                        status = runCatching {
                            val count = com.pronotes.app.data.StorageFolderSync.syncAllNotebooks(context, uri, repo)
                            "Synced $count notebook(s) to folder."
                        }.getOrElse { "Sync failed: ${it.message}" }
                        syncing = false
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProIconView(ProIcon.RESTORE, ProButtonPalette.Emerald, Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (syncing) "Syncing notes to folder…" else "Sync all notes now")
                Text(
                    "Mirror every notebook into your selected storage folder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    up { it.copy(storageFolderUri = "", storageFolderLabel = "") }
                    status = "Switched back to default app storage."
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProIconView(ProIcon.INFO, ProButtonPalette.VibrantOrange, Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Use default app storage")
                Text(
                    "Stop mirroring notes to the custom folder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    status?.let { Info(it) }
}

@Composable
private fun BackupRows() {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            status = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { repo.backup(it) } ?: error("Unable to write file")
                "Backup saved."
            }.getOrElse { "Backup failed: ${it.message}" }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            status = runCatching {
                val n = context.contentResolver.openInputStream(uri)?.use { repo.restore(it) } ?: error("Unable to open file")
                "Restored $n notebook(s)."
            }.getOrElse { "Restore failed: ${it.message}" }
        }
    }
    Row(Modifier.fillMaxWidth().clickable { save.launch("ProNotes-backup.zip") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ProIconView(ProIcon.SHARE, ProButtonPalette.ElectricBlue, Modifier.size(22.dp)); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Back up library")
            Text("Saves every notebook, folder and sticker to one file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    Row(Modifier.fillMaxWidth().clickable { open.launch(arrayOf("application/zip", "application/octet-stream")) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ProIconView(ProIcon.RESTORE, ProButtonPalette.Emerald, Modifier.size(22.dp)); Spacer(Modifier.width(14.dp))
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
    val u = Updater
    val pct = (u.progress * 100).toInt().coerceIn(0, 100)
    val speedStr = if (u.speedBytesPerSec > 0L) " · ${"%.1f".format(u.speedBytesPerSec / (1024f * 1024f))} MB/s" else ""
    val etaStr = u.etaSeconds?.let { sec -> if (sec > 0) " · ~${sec}s left" else "" }.orEmpty()
    val streamTag = if (u.activeStreams > 1) " (${u.activeStreams}× Fast)" else ""
    val readyLocal = u.phase == UPhase.READY || (u.release != null && u.hasReadyApk(context, u.release?.tag))
    val (title, sub) = when (u.phase) {
        UPhase.IDLE -> "Check for updates" to "Version ${u.currentVersion(context)}"
        UPhase.CHECKING -> "Checking for updates" to "Version ${u.currentVersion(context)}"
        UPhase.UP_TO_DATE -> "Pro Notes is up to date" to "Version ${u.currentVersion(context)}"
        UPhase.AVAILABLE -> if (readyLocal) "Install ${u.release?.tag}" to "Already downloaded & verified — tap to install" else "Update to ${u.release?.tag}" to "Tap to see what changed"
        UPhase.DOWNLOADING -> "Downloading $pct%$speedStr$streamTag" to "Background download active$etaStr · Tap to manage"
        UPhase.PAUSED -> "Download paused at $pct%" to "Tap to resume anytime (progress saved)"
        UPhase.READY -> "Install update (${u.release?.tag ?: "Ready"})" to "Downloaded & verified — tap to install without re-downloading"
        UPhase.INSTALLING -> "Installing update..." to "Confirm when prompted · Tap to reopen installer"
        UPhase.FAILED -> if (readyLocal) "Install update (${u.release?.tag ?: "Ready"})" to (u.error ?: "Update already downloaded — tap to install") else "Update failed" to (u.error ?: "Tap to retry or resume")
    }
    Column(
        Modifier.fillMaxWidth().clickable {
            when (u.phase) {
                UPhase.AVAILABLE, UPhase.DOWNLOADING, UPhase.PAUSED -> u.showDialog = true
                UPhase.READY -> u.install(context)
                UPhase.INSTALLING -> if (readyLocal) u.install(context) else u.showDialog = true
                UPhase.FAILED -> if (readyLocal) u.install(context) else u.showDialog = true
                UPhase.CHECKING -> Unit
                else -> scope.launch { u.check(context) }
            }
        }.padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProIconView(ProIcon.DOWNLOAD, if (readyLocal) ProButtonPalette.Emerald else ProButtonPalette.ElectricBlue, Modifier.size(22.dp))
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = if (readyLocal) FontWeight.SemiBold else FontWeight.Normal)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (readyLocal && u.phase != UPhase.DOWNLOADING && u.phase != UPhase.PAUSED) {
                TextButton(onClick = { u.install(context) }) { Text("Install") }
                TextButton(onClick = { u.showDialog = true }) { Text("Details") }
            } else if (u.phase == UPhase.PAUSED) {
                TextButton(onClick = { u.resumeDownload(context) }) { Text("Resume") }
                TextButton(onClick = { u.cancelDownload(context) }) { Text("Cancel") }
            } else if (u.phase == UPhase.DOWNLOADING) {
                TextButton(onClick = { u.pauseDownload(context) }) { Text("Pause") }
            }
        }
        if (u.phase == UPhase.DOWNLOADING || u.phase == UPhase.PAUSED) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { u.progress }, modifier = Modifier.fillMaxWidth().padding(start = 40.dp))
        }
    }
}

@Composable
private fun PensSection(store: SettingsStore) {
    val pens by store.pens.collectAsStateWithLifecycle()
    var showSetup by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PenProfile?>(null) }

    Section("Your pens") {
        if (pens.isEmpty()) Info("Tap 'Set up a pen' below or write with a stylus to register it and map its buttons.")
        pens.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(ProButtonPalette.ElectricBlue.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        ProIconView(ProIcon.PEN, ProButtonPalette.ElectricBlue, Modifier.size(20.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.displayName(), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${p.brand} · ${p.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { renameTarget = p }) { Icon(Icons.Outlined.Edit, "Rename pen") }
                    TextButton(onClick = { store.forgetPen(p.name) }) { Text("Forget") }
                }
                Choice(
                    title = "Hold side button",
                    value = p.sideButton.label,
                    options = SideButton.entries.map { it.label },
                    subtitle = "Temporarily switches tool while held",
                ) { k -> store.updatePen(p.copy(sideButton = SideButton.entries[k])) }
                Choice(
                    title = "Click button",
                    value = p.click.label,
                    options = PenAction.entries.map { it.label },
                    subtitle = "Single press action",
                ) { k -> store.updatePen(p.copy(click = PenAction.entries[k])) }
                Choice(
                    title = "Double-click button",
                    value = p.doubleClick.label,
                    options = PenAction.entries.map { it.label },
                    subtitle = "Quick double press action",
                ) { k -> store.updatePen(p.copy(doubleClick = PenAction.entries[k])) }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Row(Modifier.fillMaxWidth().clickable { showSetup = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProIconView(ProIcon.ADD, ProButtonPalette.ElectricBlue, Modifier.size(22.dp)); Spacer(Modifier.size(16.dp))
            Text("Set up a pen", Modifier.weight(1f), fontWeight = FontWeight.Medium)
        }
    }

    if (showSetup) {
        var alias by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf(PenBrands.manual.first()) }
        AppDialog(
            onDismissRequest = { showSetup = false },
            title = { Text("Set up a pen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        alias,
                        { alias = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        label = { Text("Custom pen name") },
                        placeholder = { Text("e.g. Fine Lecture Stylus") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppChoiceRow("Stylus hardware", brand, PenBrands.manual, { idx -> brand = PenBrands.manual[idx] })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val label = alias.trim().ifBlank { brand }
                        val existingSameBrand = pens.firstOrNull { it.brand == brand || it.name.equals("Stylus", ignoreCase = true) }
                        if (existingSameBrand != null) {
                            store.updatePen(existingSameBrand.copy(brand = brand, alias = label))
                        } else {
                            val id = "pen-${System.currentTimeMillis()}"
                            store.registerPen(id, brand = brand, alias = label)
                        }
                        store.update { it.copy(penName = label) }
                        showSetup = false
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                ) { Text("Save Pen") }
            },
            dismissButton = { TextButton(onClick = { showSetup = false }) { Text("Cancel") } },
        )
    }

    renameTarget?.let { target ->
        var nextAlias by remember(target) { mutableStateOf(target.alias.ifBlank { target.displayName() }) }
        AppDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename pen") },
            text = {
                OutlinedTextField(nextAlias, { nextAlias = it }, singleLine = true, shape = RoundedCornerShape(16.dp), label = { Text("Pen name") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = nextAlias.trim()
                        store.updatePen(target.copy(alias = clean))
                        if (clean.isNotBlank()) store.update { it.copy(penName = clean) }
                        renameTarget = null
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CloudAccountSection(s: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudUser by CloudSync.user.collectAsStateWithLifecycle()
    val syncing by CloudSync.syncing.collectAsStateWithLifecycle()
    val status by CloudSync.status.collectAsStateWithLifecycle()

    Section("Google Account & Cloud Sync") {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val activeUser = cloudUser
            if (activeUser == null) {
                Text(
                    "Sign in with Google to sync your notebooks, folders, and settings across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { runCatching { context.startActivity(CloudSync.buildGoogleAuthIntent()) } },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                ) { Text("Sign in with Google") }
            } else {
                Text(activeUser.displayName.ifBlank { activeUser.email }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (activeUser.email.isNotBlank()) {
                    Text(activeUser.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (s.lastCloudSyncMs > 0L) {
                    Text(
                        "Last synced ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(s.lastCloudSyncMs))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !syncing,
                        onClick = { scope.launch { CloudSync.syncAll(context) } },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.Emerald, contentColor = Color.White),
                    ) { Text(if (syncing) "SyncingΓÇª" else "Sync Now") }
                    OutlinedButton(onClick = { CloudSync.signOut(context) }, shape = CircleShape) { Text("Sign Out") }
                }
            }
            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
