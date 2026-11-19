package com.pronotes.app.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.PendingImport
import com.pronotes.app.R
import com.pronotes.app.data.CloudSync
import com.pronotes.app.data.CoverColors
import com.pronotes.app.data.CoverPatterns
import com.pronotes.app.data.Folder
import com.pronotes.app.data.FolderColors
import com.pronotes.app.data.NotebookMeta
import com.pronotes.app.data.PagePreset
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.Repository
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.Updater
import com.pronotes.app.editor.ItemIds
import com.pronotes.app.editor.LiveCollabRoomDialog
import com.pronotes.app.editor.MindNodeItem
import com.pronotes.app.editor.Tool
import com.pronotes.app.support.Support
import com.pronotes.app.ui.AppBottomBar
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppMenuButton
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppTabItem
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.appCardShape
import com.pronotes.app.ui.appContentBottomPadding
import com.pronotes.app.ui.appPillShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class Section(val label: String) {
    DOCUMENTS("Documents"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
    TRASH("Trash"),
}

private enum class SortBy(val label: String) {
    MODIFIED("Date modified"),
    CREATED("Date created"),
    NAME("Name"),
}

private sealed interface Entry {
    data class F(val folder: Folder, val count: Int) : Entry
    data class N(val note: NotebookMeta) : Entry
}

@androidx.compose.runtime.Immutable
internal data class LibraryNewBookPrefs(
    val defaultPreset: PagePreset,
    val defaultLandscape: Boolean,
    val defaultPaper: PaperStyle,
    val thumbCovers: Boolean,
    val defaultCoverPage: Boolean,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpen: (String) -> Unit,
    onSettings: () -> Unit,
    pendingImport: PendingImport?,
    onImportConsumed: () -> Unit,
    onSupport: () -> Unit = {},
    onOpenBookReader: () -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val store = remember { SettingsStore.get(context) }
    val newBookPrefs by remember(store) {
        store.select {
            LibraryNewBookPrefs(it.defaultPreset, it.defaultLandscape, it.defaultPaper, it.thumbCovers, it.defaultCoverPage)
        }
    }.collectAsStateWithLifecycle(
        initialValue = remember(store) {
            store.settings.value.let {
                LibraryNewBookPrefs(it.defaultPreset, it.defaultLandscape, it.defaultPaper, it.thumbCovers, it.defaultCoverPage)
            }
        },
    )
    val lib by repo.library.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val wide = LocalConfiguration.current.screenWidthDp >= 720
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    val reduceMotion = LocalReduceMotion.current

    var section by rememberSaveable { mutableStateOf(Section.DOCUMENTS) }
    var folderId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(SortBy.MODIFIED) }
    var listView by rememberSaveable { mutableStateOf(false) }
    var sidebar by rememberSaveable { mutableStateOf(wide) }
    var newNotebook by remember { mutableStateOf(false) }
    var pasteImportOpen by remember { mutableStateOf(false) }
    var folderEdit by remember { mutableStateOf<Folder?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Entry?>(null) }
    var moving by remember { mutableStateOf<Entry?>(null) }
    var deleting by remember { mutableStateOf<Entry?>(null) }
    var coverFor by remember { mutableStateOf<NotebookMeta?>(null) }
    var forever by remember { mutableStateOf<NotebookMeta?>(null) }
    var taggingNotebook by remember { mutableStateOf<NotebookMeta?>(null) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var collabOpen by remember { mutableStateOf(false) }
    var supportCard by remember { mutableStateOf(false) }
    var emptyTrash by remember { mutableStateOf(false) }
    var pdfImportProgress by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }

    val importFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            try {
                repo.importFiles(uris, folderId) { title, page, total ->
                    pdfImportProgress = Triple(title, page, total)
                }?.let(onOpen)
            } finally {
                pdfImportProgress = null
            }
        }
    }
    if (pendingImport != null) {
        LaunchedEffect(pendingImport) {
            val id = try {
                when {
                    pendingImport.uris.isNotEmpty() -> repo.importFiles(pendingImport.uris, null) { title, page, total ->
                        pdfImportProgress = Triple(title, page, total)
                    }
                    !pendingImport.text.isNullOrBlank() -> repo.importStructuredText(pendingImport.title ?: "", pendingImport.text, null)
                    else -> null
                }
            } finally {
                pdfImportProgress = null
            }
            onImportConsumed()
            if (id != null) onOpen(id)
        }
    }

    fun dropInto(payload: String, target: String?): Boolean {
        val id = payload.substringAfter(':')
        return when {
            payload.startsWith("note:") -> {
                scope.launch { repo.moveNotebook(id, target) }
                true
            }
            payload.startsWith("folder:") -> {
                if (id == target) return false
                var p = target
                while (p != null) {
                    if (p == id) return false
                    p = lib.folders.firstOrNull { it.id == p }?.parentId
                }
                scope.launch { repo.moveFolder(id, target) }
                true
            }
            else -> false
        }
    }

    BackHandler(enabled = folderId != null) { folderId = lib.folders.firstOrNull { it.id == folderId }?.parentId }

    val allAvailableTags: List<String> = remember(lib.notebooks) {
        val defaults = listOf("Important", "Study", "Lecture", "Exam", "Work", "Math", "Review")
        val used = lib.notebooks.filter { it.deletedAt == null }.flatMap { it.tags }
        (used + defaults).distinctBy { it.lowercase() }
    }

    val entries: ImmutableList<Entry> = remember(lib, section, folderId, query, sort, selectedTag) {
        val q = query.trim().lowercase().removePrefix("#")
        val activeTag = selectedTag?.lowercase()
        val live = lib.notebooks.filter { n ->
            n.deletedAt == null && (activeTag == null || n.tags.any { it.equals(activeTag, ignoreCase = true) })
        }
        fun sortNotes(l: List<NotebookMeta>) = when (sort) {
            SortBy.MODIFIED -> l.sortedByDescending { it.modified }
            SortBy.CREATED -> l.sortedByDescending { it.created }
            SortBy.NAME -> l.sortedBy { it.title.lowercase() }
        }
        fun folderEntry(f: Folder) = Entry.F(f, lib.notebooks.count { it.folderId == f.id && it.deletedAt == null } + lib.folders.count { it.parentId == f.id })
        when {
            section == Section.TRASH -> lib.notebooks.filter { it.deletedAt != null }.sortedByDescending { it.deletedAt }.map { Entry.N(it) }
            q.isNotEmpty() -> lib.folders.filter { it.name.lowercase().contains(q) || it.emoji.lowercase().contains(q) }.map(::folderEntry) +
                sortNotes(live.filter { n -> n.title.lowercase().contains(q) || n.tags.any { it.lowercase().contains(q) } }).map { Entry.N(it) }
            activeTag != null -> sortNotes(live).map { Entry.N(it) }
            section == Section.FAVORITES -> sortNotes(live.filter { it.favorite }).map { Entry.N(it) }
            section == Section.RECENT -> live.sortedByDescending { it.opened }.take(30).map { Entry.N(it) }
            else -> lib.folders.filter { it.parentId == folderId }.let { fs ->
                if (sort == SortBy.NAME) fs.sortedBy { it.name.lowercase() } else fs.sortedByDescending { it.modified }
            }.map(::folderEntry) + sortNotes(live.filter { it.folderId == folderId }).map { Entry.N(it) }
        }.toImmutableList()
    }

    val currentFolder = lib.folders.firstOrNull { it.id == folderId }
    val title = if (section == Section.DOCUMENTS && currentFolder != null) currentFolder.name else section.label

    val drag = remember { DragController() }
    val trashCount = lib.notebooks.count { it.deletedAt != null }
    val deskBg = if (dark) Color(0xFF1C1C1C) else MaterialTheme.colorScheme.background
    val topStripBg = if (dark) Color(0xFF000000) else Color(0xFFE6E3DA)

    androidx.compose.runtime.CompositionLocalProvider(LocalDragController provides drag) {
        Box(Modifier.fillMaxSize().background(deskBg)) {
            Row(Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = sidebar,
                    enter = if (reduceMotion) fadeIn(snap()) else expandHorizontally() + fadeIn(),
                    exit = if (reduceMotion) fadeOut(snap()) else shrinkHorizontally() + fadeOut(),
                ) {
                    Sidebar(
                        section = section,
                        trashCount = trashCount,
                        onSelect = {
                            section = it
                            folderId = null
                            if (!wide) sidebar = false
                        },
                        onCollapse = { sidebar = false },
                        onSettings = onSettings,
                        onSupport = onSupport,
                        onOpenCollab = { collabOpen = true },
                        onOpenBookReader = onOpenBookReader,
                        onDropTo = { s, payload ->
                            when (s) {
                                Section.DOCUMENTS -> dropInto(payload, null)
                                Section.TRASH -> if (payload.startsWith("note:")) {
                                    scope.launch { repo.deleteNotebook(payload.substringAfter(':')) }
                                    true
                                } else false
                                Section.FAVORITES -> if (payload.startsWith("note:")) {
                                    val nid = payload.substringAfter(':')
                                    if (lib.notebooks.firstOrNull { it.id == nid }?.favorite == false) scope.launch { repo.toggleFavorite(nid) }
                                    true
                                } else false
                                else -> false
                            }
                        },
                    )
                }

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    // Browser-style dark/paper top tab strip aligned with home.svg
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(topStripBg)
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!sidebar) {
                            AppPill(
                                onClick = { sidebar = true },
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                ProIconView(ProIcon.SIDEBAR, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                        }

                        // Active Home tab indicator
                        AppSurface(
                            role = SurfaceRole.Frost,
                            containerColor = if (dark) Color(0xFF1C1C1C) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            ProIconView(ProIcon.HOME, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))

                        val tabs by store.tabs.collectAsStateWithLifecycle()
                        val open = tabs.mapNotNull { id -> lib.notebooks.firstOrNull { it.id == id && it.deletedAt == null } }
                        Row(
                            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            open.forEach { n ->
                                AppSurface(
                                    role = SurfaceRole.Control,
                                    containerColor = if (dark) Color(0xFF12141A) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                                    onClick = { onOpen(n.id) },
                                    modifier = Modifier.widthIn(max = 200.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            n.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            Modifier.size(20.dp).clip(CircleShape).clickable { store.closeTab(n.id) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            ProIconView(ProIcon.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(11.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(4.dp))
                        UpdatePill()

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppPill(
                                tint = ProButtonPalette.VibrantOrange,
                                onClick = onOpenBookReader,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.MenuBook, "Reader Mode", Modifier.size(17.dp))
                                if (wide) {
                                    Spacer(Modifier.width(5.dp))
                                    Text("Reader Mode", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            AppPill(
                                tint = ProButtonPalette.SkyCyan,
                                onClick = { collabOpen = true },
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                Icon(Icons.Outlined.Groups, "Live Study Room", Modifier.size(19.dp))
                            }
                            AppPill(
                                selected = searching,
                                tint = ProButtonPalette.VibrantOrange,
                                onClick = { searching = !searching; if (!searching) query = "" },
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                ProIconView(ProIcon.SEARCH, if (searching) Color.White else ProButtonPalette.VibrantOrange, Modifier.size(18.dp))
                            }
                            AppPill(
                                tint = ProButtonPalette.MintTeal,
                                onClick = onSettings,
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                ProIconView(ProIcon.SETTINGS, ProButtonPalette.MintTeal, Modifier.size(18.dp))
                            }
                        }
                    }

                    // Page header & crisp actions row
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (folderId != null && section == Section.DOCUMENTS) {
                            var upHot by remember { mutableStateOf(false) }
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .libraryDropTarget(onHover = { upHot = it }) { payload -> dropInto(payload, currentFolder?.parentId) }
                                    .background(if (upHot) ProButtonPalette.ElectricBlue.copy(alpha = 0.22f) else Color.Transparent),
                            ) {
                                IconButton(onClick = { folderId = currentFolder?.parentId }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Up")
                                }
                            }
                        }
                        AnimatedContent(
                            targetState = title,
                            transitionSpec = { (fadeIn() + slideInHorizontally { it / 6 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 6 }) },
                            label = "title",
                            modifier = Modifier.weight(1f),
                        ) { t ->
                            Text(
                                t,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (section == Section.TRASH) {
                            AppPill(
                                selected = true,
                                tint = ProButtonPalette.CoralRose,
                                onClick = { if (entries.isNotEmpty()) emptyTrash = true },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text("Empty trash", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            AppMenuButton(
                                contentPadding = PaddingValues(0.dp),
                                items = listOf(
                                    AppMenuItem("Notebook", icon = { Icon(Icons.Outlined.Description, null, Modifier.size(18.dp)) }) { newNotebook = true },
                                    AppMenuItem("Infinite whiteboard", icon = { Icon(Icons.Outlined.Draw, null, Modifier.size(18.dp)) }) {
                                        scope.launch {
                                            val k = PagePreset.UNITS_PER_MM
                                            val p = PagePreset.WHITEBOARD
                                            val id = repo.createNotebook(
                                                "Whiteboard", folderId, 0xFF3D6CF4.toInt(), PaperStyle.DOTTED,
                                                width = p.wMm * k, height = p.hMm * k, isWhiteboard = true,
                                            )
                                            onOpen(id)
                                        }
                                    },
                                    AppMenuItem("Concept map canvas", icon = { Icon(Icons.Outlined.Hub, null, Modifier.size(18.dp)) }) {
                                        scope.launch {
                                            val k = PagePreset.UNITS_PER_MM
                                            val p = PagePreset.WHITEBOARD
                                            val id = repo.createNotebook(
                                                "Concept Map", folderId, 0xFF9B51E0.toInt(), PaperStyle.DOTTED,
                                                width = p.wMm * k, height = p.hMm * k, isWhiteboard = true,
                                            )
                                            val pages = repo.loadPages(id).toMutableList()
                                            if (pages.isNotEmpty()) {
                                                val first = pages[0]
                                                val root = MindNodeItem(
                                                    ItemIds.next(),
                                                    first.width / 2f - 130f,
                                                    first.height / 2f - 44f,
                                                    260f,
                                                    88f,
                                                    "Central Topic",
                                                    null,
                                                    0xFF3D6CF4.toInt(),
                                                    "pill",
                                                )
                                                pages[0] = first.copy(items = listOf(root))
                                                repo.savePages(id, pages)
                                            }
                                            SettingsStore.get(context).setPref("startTool_$id", Tool.MINDMAP.name)
                                            onOpen(id)
                                        }
                                    },
                                    AppMenuItem("Quick note", icon = { Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)) }) {
                                        scope.launch {
                                            val k = PagePreset.UNITS_PER_MM
                                            val defaultPreset = newBookPrefs.defaultPreset
                                            val defaultLandscape = newBookPrefs.defaultLandscape
                                            val defaultPaper = newBookPrefs.defaultPaper
                                            var w = defaultPreset.wMm * k
                                            var h = defaultPreset.hMm * k
                                            if (!defaultPreset.isWhiteboard && defaultLandscape != (w > h)) {
                                                val t = w; w = h; h = t
                                            }
                                            onOpen(
                                                repo.createNotebook(
                                                    "Quick note", folderId, CoverColors.random(), defaultPaper,
                                                    width = w, height = h, isWhiteboard = defaultPreset.isWhiteboard,
                                                ),
                                            )
                                        }
                                    },
                                    AppMenuItem("Folder", icon = { Icon(Icons.Outlined.CreateNewFolder, null, Modifier.size(18.dp)) }) { newFolder = true },
                                    AppMenuItem("Import PDF, Word, PPT, Sheets, EPUB, Images", icon = { Icon(Icons.Outlined.UploadFile, null, Modifier.size(18.dp)) }) {
                                        importFilesLauncher.launch(arrayOf("*/*"))
                                    },
                                    AppMenuItem("Paste notes from another app…", icon = { Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp)) }) { pasteImportOpen = true },
                                ),
                                contentDescription = "Create new",
                                role = SurfaceRole.Control,
                                containerColor = ProButtonPalette.ElectricBlue,
                                shape = CircleShape,
                            ) {
                                Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp), tint = Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text("New", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Spacer(Modifier.width(2.dp))
                                    Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(18.dp), tint = Color.White)
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                        AppMenuButton(
                            contentPadding = PaddingValues(0.dp),
                            items = SortBy.entries.map { s ->
                                AppMenuItem(label = s.label, selected = sort == s) { sort = s }
                            },
                            contentDescription = "Sort notebooks",
                            role = SurfaceRole.Control,
                            shape = appPillShape(),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(sort.label.substringAfter("Date ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(Modifier.width(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppPill(
                                selected = !listView,
                                tint = ProButtonPalette.Emerald,
                                onClick = { listView = false },
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                Icon(Icons.Outlined.GridView, "Grid", Modifier.size(18.dp))
                            }
                            AppPill(
                                selected = listView,
                                tint = ProButtonPalette.SkyCyan,
                                onClick = { listView = true },
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ViewList, "List", Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    )

                    if (section != Section.TRASH) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppChip(
                                selected = selectedTag == null,
                                onClick = { selectedTag = null },
                                label = "All Tags",
                                tint = ProButtonPalette.ElectricBlue,
                            )
                            allAvailableTags.forEach { tag ->
                                val isSel = selectedTag?.equals(tag, ignoreCase = true) == true
                                AppChip(
                                    selected = isSel,
                                    onClick = { selectedTag = if (isSel) null else tag },
                                    label = "#$tag",
                                    tint = tagTintFor(tag),
                                )
                            }
                        }
                    }

                    AnimatedVisibility(searching) {
                        AppCard(
                            hero = false,
                            shape = appCardShape(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        ) {
                            OutlinedTextField(
                                query,
                                { query = it },
                                placeholder = { Text("Search notebooks, folders, or #tags") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    val actions = TileActions(
                        onOpenFolder = { f -> section = Section.DOCUMENTS; query = ""; searching = false; folderId = f.id },
                        onOpenNote = { onOpen(it.id) },
                        onRename = { renaming = it },
                        onMove = { moving = it },
                        onDelete = { deleting = it },
                        onEditFolder = { folderEdit = it },
                        onCover = { coverFor = it },
                        onTags = { taggingNotebook = it },
                        onTagFilter = { tag -> selectedTag = if (selectedTag.equals(tag, ignoreCase = true)) null else tag },
                        onFavorite = { scope.launch { repo.toggleFavorite(it.id) } },
                        onDuplicate = { scope.launch { repo.duplicateNotebook(it.id) } },
                        onRestore = { scope.launch { repo.restoreNotebook(it.id) } },
                        onDeleteForever = { forever = it },
                        onDrop = { payload, target -> dropInto(payload, target) },
                    )

                    val listBottomPad = appContentBottomPadding()

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (entries.isEmpty()) {
                            EmptyState(section, onNew = { newNotebook = true })
                        } else {
                            AnimatedContent(
                                targetState = Triple(section, folderId, listView),
                                transitionSpec = {
                                    if (reduceMotion) {
                                        fadeIn(snap()) togetherWith fadeOut(snap())
                                    } else {
                                        (fadeIn() + slideInHorizontally { it / 12 }) togetherWith fadeOut()
                                    }
                                },
                                label = "grid",
                            ) { (_, _, asList) ->
                                if (asList) {
                                    LazyColumn(
                                        Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = listBottomPad),
                                    ) {
                                        items(entries, key = { keyOf(it) }) { e ->
                                            Box(if (reduceMotion) Modifier else Modifier.animateItem()) { ListRow(e, repo, actions) }
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(154.dp),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = listBottomPad),
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(24.dp),
                                    ) {
                                        items(entries, key = { keyOf(it) }) { e ->
                                            Box(if (reduceMotion) Modifier else Modifier.animateItem()) {
                                                when (e) {
                                                    is Entry.F -> FolderTile(e, actions)
                                                    is Entry.N -> NotebookTile(e.note, repo, actions)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!wide || section != Section.DOCUMENTS) {
                AppBottomBar(
                    tabs = listOf(
                        AppTabItem("Library") { Icon(Icons.Outlined.Description, null, Modifier.size(18.dp)) },
                        AppTabItem("Books") { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp)) },
                        AppTabItem("Recent") { Icon(Icons.Outlined.AccessTime, null, Modifier.size(18.dp)) },
                        AppTabItem("Favorites") { Icon(Icons.Outlined.StarBorder, null, Modifier.size(18.dp)) },
                        AppTabItem("Trash") { Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp)) },
                    ),
                    selectedIndex = when (section) {
                        Section.DOCUMENTS -> 0
                        Section.RECENT -> 2
                        Section.FAVORITES -> 3
                        Section.TRASH -> 4
                    },
                    onSelect = { idx ->
                        if (idx == 1) {
                            onOpenBookReader()
                        } else {
                            section = when (idx) {
                                0 -> Section.DOCUMENTS
                                2 -> Section.RECENT
                                3 -> Section.FAVORITES
                                else -> Section.TRASH
                            }
                            folderId = null
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            drag.payload?.let { p ->
                AppSurface(
                    role = SurfaceRole.Control,
                    containerColor = ProButtonPalette.ElectricBlue,
                    contentColor = Color.White,
                    shape = appPillShape(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .offset {
                            IntOffset((drag.position.x - 70.dp.toPx()).toInt(), (drag.position.y - 80.dp.toPx()).toInt())
                        },
                ) {
                    Text(
                        drag.label.ifBlank { if (p.startsWith("folder")) "Folder" else "Notebook" },
                        Modifier.widthIn(max = 220.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            pdfImportProgress?.let { (pdfTitle, page, total) ->
                PdfImportLoaderOverlay(
                    title = pdfTitle,
                    page = page,
                    total = total,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (supportCard) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    AppCard(
                        hero = true,
                        shape = appCardShape(),
                        contentPadding = PaddingValues(18.dp),
                        modifier = Modifier.padding(20.dp).widthIn(max = 520.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProIconView(ProIcon.HEART, ProButtonPalette.MagentaPink, Modifier.size(26.dp), ink = ProButtonPalette.MagentaPink)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Enjoying Pro Notes?", style = MaterialTheme.typography.titleMedium)
                                Text("Made with care. A small tip helps keep development going.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { supportCard = false }) { Text("Not now") }
                            Button(
                                onClick = { supportCard = false; onSupport() },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.MagentaPink, contentColor = Color.White),
                            ) { Text("Support") }
                        }
                    }
                }
            }
        }
    }

    if (newNotebook) NewNotebookDialog(newBookPrefs, onDismiss = { newNotebook = false }) { b ->
        newNotebook = false
        scope.launch {
            val usePhotoPage = b.photo != null && b.coverPage
            val id = repo.createNotebook(
                b.name, folderId, b.coverColor, b.paper, b.cover,
                width = b.width, height = b.height, paperColor = b.paperColor, coverPage = b.coverPage,
                isWhiteboard = b.isWhiteboard,
            )
            if (b.photo != null) {
                val spec = repo.saveCoverImage(id, b.photo)
                if (!usePhotoPage || !repo.setCoverPage(id, spec)) repo.setCover(id, spec)
            }
            onOpen(id)
        }
    }

    if (pasteImportOpen) {
        var importTitle by remember { mutableStateOf("") }
        var importBody by remember { mutableStateOf("") }
        AppDialog(
            onDismissRequest = { pasteImportOpen = false },
            title = { Text("Import notes from another app") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste Markdown, HTML, CSV, or plain notes. Headings, checklists, bullets, and callouts are formatted across pages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = importTitle,
                        onValueChange = { importTitle = it },
                        singleLine = true,
                        label = { Text("Notebook title (optional)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = importBody,
                        onValueChange = { importBody = it },
                        minLines = 5,
                        maxLines = 9,
                        label = { Text("Note content") },
                        placeholder = { Text("# Lecture 1\n- Key idea\n- [ ] Action item\n> Callout summary") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = importBody.isNotBlank(),
                    onClick = {
                        val t = importTitle
                        val b = importBody
                        pasteImportOpen = false
                        scope.launch { onOpen(repo.importStructuredText(t, b, folderId)) }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                ) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { pasteImportOpen = false }) { Text("Cancel") } },
        )
    }

    if (newFolder) FolderDialog(null, onDismiss = { newFolder = false }) { name, color, tag ->
        newFolder = false
        scope.launch { repo.createFolder(name, folderId, color, tag) }
    }
    folderEdit?.let { f ->
        FolderDialog(f, onDismiss = { folderEdit = null }) { name, color, tag ->
            folderEdit = null
            scope.launch { repo.updateFolder(f.id, name, color, tag) }
        }
    }

    forever?.let { n ->
        AppDialog(
            onDismissRequest = { forever = null },
            title = { Text("Delete \"${n.title}\" forever?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { forever = null; scope.launch { repo.deleteForever(n.id) } }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { forever = null }) { Text("Cancel") } },
        )
    }
    if (emptyTrash) {
        AppDialog(
            onDismissRequest = { emptyTrash = false },
            title = { Text("Empty trash?") },
            text = { Text("All notebooks in Trash will be deleted forever.") },
            confirmButton = {
                TextButton(onClick = { emptyTrash = false; scope.launch { repo.emptyTrash() } }) {
                    Text("Empty", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { emptyTrash = false }) { Text("Cancel") } },
        )
    }

    coverFor?.let { n ->
        CoverDialog(n.coverSpec, onDismiss = { coverFor = null }) { spec, photo ->
            coverFor = null
            scope.launch {
                val s = if (photo != null) repo.saveCoverImage(n.id, photo) else spec
                if (s == "thumb" || !repo.setCoverPage(n.id, s)) repo.setCover(n.id, s)
            }
        }
    }

    renaming?.let { e ->
        val initial = if (e is Entry.F) e.folder.name else (e as Entry.N).note.title
        NameDialog("Rename", initial, onDismiss = { renaming = null }) { name ->
            renaming = null
            scope.launch { if (e is Entry.F) repo.renameFolder(e.folder.id, name) else repo.renameNotebook((e as Entry.N).note.id, name) }
        }
    }

    moving?.let { e ->
        val selfId = (e as? Entry.F)?.folder?.id
        AppDialog(
            onDismissRequest = { moving = null },
            confirmButton = { TextButton(onClick = { moving = null }) { Text("Cancel") } },
            title = { Text("Move to") },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item {
                        MoveRow("Documents", null) {
                            moving = null
                            scope.launch { if (e is Entry.F) repo.moveFolder(e.folder.id, null) else repo.moveNotebook((e as Entry.N).note.id, null) }
                        }
                    }
                    items(lib.folders.filter { it.id != selfId }, key = { it.id }) { f ->
                        MoveRow(f.name, f) {
                            moving = null
                            scope.launch { if (e is Entry.F) repo.moveFolder(e.folder.id, f.id) else repo.moveNotebook((e as Entry.N).note.id, f.id) }
                        }
                    }
                }
            },
        )
    }

    deleting?.let { e ->
        val name = if (e is Entry.F) e.folder.name else (e as Entry.N).note.title
        AppDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"$name\"?") },
            text = {
                Text(
                    if (e is Entry.F) "The folder will be deleted. Notebooks inside it move to Trash."
                    else "The notebook moves to Trash. You can restore it for 30 days.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch { if (e is Entry.F) repo.deleteFolder(e.folder.id) else repo.deleteNotebook((e as Entry.N).note.id) }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }

    if (collabOpen) {
        LiveCollabRoomDialog(
            vm = null,
            repo = repo,
            onOpenNotebook = { id -> collabOpen = false; onOpen(id) },
            onClose = { collabOpen = false },
        )
    }

    taggingNotebook?.let { targetNote ->
        ManageTagsDialog(
            note = targetNote,
            availableTags = allAvailableTags,
            onDismiss = { taggingNotebook = null },
            onSave = { updatedTags ->
                taggingNotebook = null
                scope.launch { repo.setNotebookTags(targetNote.id, updatedTags) }
            },
        )
    }
}

// PdfImportLoaderOverlay & PdfSkeletonPreviewBox extracted to LibraryDialogs.kt

private fun keyOf(e: Entry) = if (e is Entry.F) "f" + e.folder.id else "n" + (e as Entry.N).note.id

private class TileActions(
    val onOpenFolder: (Folder) -> Unit,
    val onOpenNote: (NotebookMeta) -> Unit,
    val onRename: (Entry) -> Unit,
    val onMove: (Entry) -> Unit,
    val onDelete: (Entry) -> Unit,
    val onEditFolder: (Folder) -> Unit,
    val onCover: (NotebookMeta) -> Unit,
    val onTags: (NotebookMeta) -> Unit,
    val onTagFilter: (String) -> Unit,
    val onFavorite: (NotebookMeta) -> Unit,
    val onDuplicate: (NotebookMeta) -> Unit,
    val onRestore: (NotebookMeta) -> Unit,
    val onDeleteForever: (NotebookMeta) -> Unit,
    val onDrop: (String, String?) -> Boolean,
)

@Composable
private fun Sidebar(
    section: Section,
    trashCount: Int,
    onSelect: (Section) -> Unit,
    onCollapse: () -> Unit,
    onSettings: () -> Unit,
    onSupport: () -> Unit,
    onOpenCollab: () -> Unit,
    onOpenBookReader: () -> Unit,
    onDropTo: (Section, String) -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudUser by CloudSync.user.collectAsStateWithLifecycle()
    val syncing by CloudSync.syncing.collectAsStateWithLifecycle()
    val syncStatus by CloudSync.status.collectAsStateWithLifecycle()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    val sidebarBg = if (dark) Color(0xFF181818) else Color(0xFFECE9E0)
    val activePillBg = if (dark) Color(0xFF1F3162) else MaterialTheme.colorScheme.primaryContainer
    val activeIconTint = if (dark) Color(0xFF4FB0F5) else MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxHeight()
            .width(244.dp)
            .background(sidebarBg),
    ) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Outlined.ChevronLeft, "Hide sidebar")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 20.dp)) {
                androidx.compose.foundation.Image(
                    painterResource(R.drawable.ic_launcher_foreground),
                    null,
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Pro Notes", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("v13 Studio", style = MaterialTheme.typography.labelSmall, color = activeIconTint)
                }
            }

            Section.entries.forEach { s ->
                if (s == Section.TRASH) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    )
                }
                val sel = s == section
                var hot by remember { mutableStateOf(false) }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .libraryDropTarget(onHover = { hot = it }) { payload -> onDropTo(s, payload) }
                        .background(
                            when {
                                hot -> ProButtonPalette.ElectricBlue.copy(alpha = 0.24f)
                                sel -> activePillBg
                                else -> Color.Transparent
                            },
                        )
                        .clickable { onSelect(s) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (s) {
                            Section.DOCUMENTS -> Icons.Outlined.Folder
                            Section.FAVORITES -> Icons.Outlined.StarBorder
                            Section.RECENT -> Icons.Outlined.AccessTime
                            Section.TRASH -> Icons.Outlined.Delete
                        },
                        null,
                        tint = if (sel) activeIconTint else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        s.label,
                        color = if (sel) Color.White.takeIf { dark } ?: MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (s == Section.TRASH && trashCount > 0) {
                        Text("$trashCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(2.dp))
            }

            Spacer(Modifier.height(10.dp))
            AppCard(
                hero = false,
                shape = appCardShape(),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudSync, null, Modifier.size(16.dp), tint = ProButtonPalette.SkyCyan)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        cloudUser?.let { it.displayName.ifBlank { it.email } } ?: "Google Cloud Sync",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (syncStatus.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(syncStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Spacer(Modifier.height(6.dp))
                if (cloudUser == null) {
                    AppPill(
                        selected = true,
                        tint = ProButtonPalette.ElectricBlue,
                        onClick = { runCatching { context.startActivity(CloudSync.buildGoogleAuthIntent()) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sign in with Google", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    AppPill(
                        selected = true,
                        tint = ProButtonPalette.Emerald,
                        onClick = { if (!syncing) scope.launch { CloudSync.syncAll(context) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (syncing) "Syncing…" else "Sync Now", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenBookReader)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = ProButtonPalette.VibrantOrange)
                Spacer(Modifier.width(12.dp))
                Text("Reader Mode", color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenCollab)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Groups, null, tint = ProButtonPalette.SkyCyan)
                Spacer(Modifier.width(12.dp))
                Text("Live Study Room", color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            if (Support.enabled) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onSupport)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProIconView(ProIcon.HEART, ProButtonPalette.MagentaPink, Modifier.size(20.dp), ink = ProButtonPalette.MagentaPink)
                    Spacer(Modifier.width(12.dp))
                    Text("Support Pro Notes")
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSettings)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("Settings")
            }
        }
    }
}

@Composable
private fun EmptyState(section: Section, onNew: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            when (section) {
                Section.FAVORITES -> "No favorites yet"
                Section.RECENT -> "Nothing opened yet"
                Section.TRASH -> "Trash is empty"
                else -> "Your library is empty"
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        if (section == Section.TRASH) {
            Text("Deleted notebooks stay here for 30 days.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Create a notebook or import a PDF to start.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onNew,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("New notebook")
            }
        }
    }
}

@Composable
fun FolderArt(color: Int, tag: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val c = Color(color)
            val back = Path().apply {
                moveTo(0f, h * 0.18f); lineTo(w * 0.36f, h * 0.18f); lineTo(w * 0.44f, h * 0.28f); lineTo(w, h * 0.28f)
                lineTo(w, h * 0.92f); lineTo(0f, h * 0.92f); close()
            }
            drawPath(back, c.copy(alpha = 0.78f))
            drawRoundRect(c, Offset(0f, h * 0.34f), Size(w, h * 0.6f), CornerRadius(w * 0.06f))
            drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(0f, h * 0.34f), Size(w, h * 0.06f), CornerRadius(w * 0.06f))
        }
        if (tag.isNotBlank()) {
            AppPill(
                selected = false,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.padding(top = 18.dp),
            ) {
                Text(tag, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun folderMenuItems(e: Entry.F, a: TileActions): List<AppMenuItem> = listOf(
    AppMenuItem("Colour & label", icon = { Icon(Icons.Outlined.Palette, null, Modifier.size(18.dp)) }) { a.onEditFolder(e.folder) },
    AppMenuItem("Rename", icon = { Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)) }) { a.onRename(e) },
    AppMenuItem("Move", icon = { Icon(Icons.Outlined.DriveFileMove, null, Modifier.size(18.dp)) }) { a.onMove(e) },
    AppMenuItem("Delete", destructive = true, icon = { Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp)) }) { a.onDelete(e) },
)

private fun noteMenuItems(note: NotebookMeta, a: TileActions): List<AppMenuItem> =
    if (note.deletedAt != null) {
        listOf(
            AppMenuItem("Restore", icon = { Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp)) }) { a.onRestore(note) },
            AppMenuItem("Delete forever", destructive = true, icon = { Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp)) }) { a.onDeleteForever(note) },
        )
    } else {
        listOf(
            AppMenuItem("Manage tags…", icon = { ProIconView(ProIcon.BOOKMARK, ProButtonPalette.ElectricBlue, Modifier.size(18.dp)) }) { a.onTags(note) },
            AppMenuItem("Duplicate", icon = { Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp)) }) { a.onDuplicate(note) },
            AppMenuItem(
                if (note.favorite) "Remove from favorites" else "Add to favorites",
                icon = { Icon(if (note.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null, Modifier.size(18.dp)) },
            ) { a.onFavorite(note) },
            AppMenuItem("Change cover", icon = { Icon(Icons.Outlined.Palette, null, Modifier.size(18.dp)) }) { a.onCover(note) },
            AppMenuItem("Rename", icon = { Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)) }) { a.onRename(Entry.N(note)) },
            AppMenuItem("Move", icon = { Icon(Icons.Outlined.DriveFileMove, null, Modifier.size(18.dp)) }) { a.onMove(Entry.N(note)) },
            AppMenuItem("Delete", destructive = true, icon = { Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp)) }) { a.onDelete(Entry.N(note)) },
        )
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderTile(e: Entry.F, a: TileActions) {
    val src = remember { MutableInteractionSource() }
    var dropHover by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .libraryDragSource("folder:${e.folder.id}", e.folder.name)
                .libraryDropTarget(onHover = { dropHover = it }) { payload -> a.onDrop(payload, e.folder.id) }
                .then(
                    if (dropHover) Modifier.border(2.dp, ProButtonPalette.ElectricBlue, RoundedCornerShape(14.dp))
                    else Modifier,
                )
                .combinedClickable(interactionSource = src, indication = null, onClick = { a.onOpenFolder(e.folder) }),
            contentAlignment = Alignment.BottomCenter,
        ) {
            FolderArt(e.folder.color, e.folder.emoji, Modifier.fillMaxWidth(0.88f).aspectRatio(1f))
        }
        Caption(e.folder.name, "${e.count} items", menuItems = remember(e) { folderMenuItems(e, a) })
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun NotebookTile(note: NotebookMeta, repo: Repository, a: TileActions) {
    var trashPrompt by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .then(if (note.deletedAt == null) Modifier.libraryDragSource("note:${note.id}", note.title) else Modifier)
                .combinedClickable(
                    interactionSource = src,
                    indication = null,
                    onClick = { if (note.deletedAt != null) trashPrompt = true else a.onOpenNote(note) },
                    onLongClick = if (note.deletedAt != null) ({ trashPrompt = true }) else null,
                ),
        ) {
            CoverArt(note.coverSpec, note.title, repo.noteDir(note.id), note.modified, Modifier.fillMaxSize())
            if (note.favorite) {
                Icon(Icons.Filled.Star, null, tint = Color(0xFFF2C94C), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp))
            }
        }
        Caption(
            note.title,
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(note.modified)),
            menuItems = remember(note) { noteMenuItems(note, a) },
        )
        if (note.tags.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                note.tags.take(3).forEach { tag ->
                    TagBadgePill(tag = tag, onClick = { a.onTagFilter(tag) })
                }
            }
        }
    }
    if (trashPrompt) {
        AppDialog(
            onDismissRequest = { trashPrompt = false },
            title = { Text(note.title) },
            text = { Text("Restore this notebook from Trash or delete it forever?") },
            confirmButton = { TextButton(onClick = { trashPrompt = false; a.onRestore(note) }) { Text("Restore") } },
            dismissButton = {
                TextButton(onClick = { trashPrompt = false; a.onDeleteForever(note) }) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}

@Composable
private fun Caption(title: String, sub: String, menuItems: List<AppMenuItem>) {
    Spacer(Modifier.height(10.dp))
    AppMenuButton(
        contentPadding = PaddingValues(0.dp),
        items = menuItems,
        contentDescription = "$title options",
        role = SurfaceRole.Frost,
        containerColor = Color.Transparent,
        shape = appPillShape(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Icon(Icons.Filled.KeyboardArrowDown, "Options", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListRow(e: Entry, repo: Repository, a: TileActions) {
    var trashPrompt by remember { mutableStateOf(false) }
    val items = remember(e) { if (e is Entry.N) noteMenuItems(e.note, a) else folderMenuItems(e as Entry.F, a) }
    AppCard(
        hero = false,
        shape = appCardShape(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        if (e is Entry.F) a.onOpenFolder(e.folder)
                        else (e as Entry.N).note.let { n -> if (n.deletedAt == null) a.onOpenNote(n) else trashPrompt = true }
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(width = 44.dp, height = 56.dp)) {
                    when (e) {
                        is Entry.F -> FolderArt(e.folder.color, e.folder.emoji, Modifier.fillMaxSize())
                        is Entry.N -> CoverArt(e.note.coverSpec, e.note.title, repo.noteDir(e.note.id), e.note.modified, Modifier.fillMaxSize(), showTitle = false)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (e is Entry.F) e.folder.name else (e as Entry.N).note.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        if (e is Entry.F) "${e.count} items"
                        else (e as Entry.N).note.let { "${it.pageCount} pages · " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it.modified)) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (e is Entry.N && e.note.tags.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            e.note.tags.forEach { tag ->
                                TagBadgePill(tag = tag, onClick = { a.onTagFilter(tag) })
                            }
                        }
                    }
                }
            }
            AppMenuButton(
                contentPadding = PaddingValues(0.dp),
                items = items,
                contentDescription = "Options",
                role = SurfaceRole.Control,
                shape = appPillShape(),
            ) {
                Box(Modifier.size(36.dp), Alignment.Center) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (trashPrompt && e is Entry.N) {
        AppDialog(
            onDismissRequest = { trashPrompt = false },
            title = { Text(e.note.title) },
            text = { Text("Restore this notebook from Trash or delete it forever?") },
            confirmButton = { TextButton(onClick = { trashPrompt = false; a.onRestore(e.note) }) { Text("Restore") } },
            dismissButton = {
                TextButton(onClick = { trashPrompt = false; a.onDeleteForever(e.note) }) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}

// Tag badges, ManageTagsDialog, MoveRow, NameDialog, FolderDialog, CoverDialog, and NewNotebookDialog extracted to LibraryDialogs.kt

@Composable
private fun UpdatePill() {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val updatePrefs by remember(store) {
        store.select { it.autoUpdateCheck to it.updateReminders }
    }.collectAsStateWithLifecycle(initialValue = remember(store) { store.settings.value.let { it.autoUpdateCheck to it.updateReminders } })
    val (autoUpdateCheck, updateReminders) = updatePrefs
    LaunchedEffect(autoUpdateCheck) {
        if (autoUpdateCheck || updateReminders) Updater.check(context, quiet = true)
        if (updateReminders && Updater.phase == Updater.Phase.AVAILABLE && !Updater.remindedThisLaunch) {
            Updater.remindedThisLaunch = true
            delay(1200)
            Updater.showDialog = true
        }
    }
    val pct = (Updater.progress * 100).toInt().coerceIn(0, 100)
    val label = when (Updater.phase) {
        Updater.Phase.AVAILABLE -> "Update to ${Updater.release?.tag}"
        Updater.Phase.DOWNLOADING -> "Downloading $pct%"
        Updater.Phase.PAUSED -> "Resume update ($pct%)"
        Updater.Phase.READY -> "Install update"
        Updater.Phase.INSTALLING -> "Installing..."
        Updater.Phase.FAILED -> if (Updater.release != null) "Update failed · Retry" else null
        else -> null
    } ?: return
    val tint = when (Updater.phase) {
        Updater.Phase.FAILED -> ProButtonPalette.CoralRose
        Updater.Phase.PAUSED -> ProButtonPalette.VibrantOrange
        else -> ProButtonPalette.Emerald
    }
    AppPill(
        selected = true,
        tint = tint,
        onClick = {
            when (Updater.phase) {
                Updater.Phase.AVAILABLE, Updater.Phase.DOWNLOADING, Updater.Phase.PAUSED -> Updater.showDialog = true
                Updater.Phase.READY -> Updater.install(context)
                Updater.Phase.FAILED -> if (Updater.hasReadyApk(context)) Updater.install(context) else Updater.showDialog = true
                else -> Unit
            }
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}
