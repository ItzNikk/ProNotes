package com.pronotes.app.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.scale
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
import com.pronotes.app.ui.FannedColorfulCardsDeck
import com.pronotes.app.ui.MovingColorfulGradientBackground
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.appCardShape
import com.pronotes.app.ui.appContentBottomPadding
import com.pronotes.app.ui.appPillShape
import com.pronotes.app.ui.elasticStretch
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
private data class LibraryNewBookPrefs(
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
    onOpenPreview: () -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val store = remember { SettingsStore.get(context) }
    val newBookPrefs by remember(store) {
        store.select {
            LibraryNewBookPrefs(it.defaultPreset, it.defaultLandscape, it.defaultPaper, it.thumbCovers, it.defaultCoverPage)
        }
    }.collectAsStateWithLifecycle(
        initialValue = store.settings.value.let {
            LibraryNewBookPrefs(it.defaultPreset, it.defaultLandscape, it.defaultPaper, it.thumbCovers, it.defaultCoverPage)
        },
    )
    val lib by repo.library.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val wide = LocalConfiguration.current.screenWidthDp >= 720
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.45f

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
    var collabOpen by remember { mutableStateOf(false) }
    var supportCard by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val st = SettingsStore.get(context)
        if (Support.enabled && st.shouldRemindSupport()) {
            supportCard = true
            st.markSupportShown()
        }
    }
    var emptyTrash by remember { mutableStateOf(false) }

    val importFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch { repo.importFiles(uris, folderId)?.let(onOpen) }
    }
    if (pendingImport != null) {
        LaunchedEffect(pendingImport) {
            val id = when {
                pendingImport.uris.isNotEmpty() -> repo.importFiles(pendingImport.uris, null)
                !pendingImport.text.isNullOrBlank() -> repo.importStructuredText(pendingImport.title ?: "", pendingImport.text, null)
                else -> null
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

    val entries: ImmutableList<Entry> = remember(lib, section, folderId, query, sort) {
        val q = query.trim().lowercase()
        val live = lib.notebooks.filter { it.deletedAt == null }
        fun sortNotes(l: List<NotebookMeta>) = when (sort) {
            SortBy.MODIFIED -> l.sortedByDescending { it.modified }
            SortBy.CREATED -> l.sortedByDescending { it.created }
            SortBy.NAME -> l.sortedBy { it.title.lowercase() }
        }
        fun folderEntry(f: Folder) = Entry.F(f, lib.notebooks.count { it.folderId == f.id && it.deletedAt == null } + lib.folders.count { it.parentId == f.id })
        when {
            section == Section.TRASH -> lib.notebooks.filter { it.deletedAt != null }.sortedByDescending { it.deletedAt }.map { Entry.N(it) }
            q.isNotEmpty() -> lib.folders.filter { it.name.lowercase().contains(q) }.map(::folderEntry) +
                sortNotes(live.filter { it.title.lowercase().contains(q) }).map { Entry.N(it) }
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
                AnimatedVisibility(sidebar, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
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
                        onOpenPreview = onOpenPreview,
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
                                    AppMenuItem("Mind map canvas", icon = { Icon(Icons.Outlined.Hub, null, Modifier.size(18.dp)) }) {
                                        scope.launch {
                                            val k = PagePreset.UNITS_PER_MM
                                            val p = PagePreset.WHITEBOARD
                                            val id = repo.createNotebook(
                                                "Mind map", folderId, 0xFF9B51E0.toInt(), PaperStyle.DOTTED,
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
                                    AppMenuItem("Import PDF, MD, TXT, Images", icon = { Icon(Icons.Outlined.UploadFile, null, Modifier.size(18.dp)) }) {
                                        importFilesLauncher.launch(arrayOf("application/pdf", "text/*", "image/*", "application/json", "application/octet-stream"))
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
                                placeholder = { Text("Search notebooks and folders") },
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
                        onFavorite = { scope.launch { repo.toggleFavorite(it.id) } },
                        onDuplicate = { scope.launch { repo.duplicateNotebook(it.id) } },
                        onRestore = { scope.launch { repo.restoreNotebook(it.id) } },
                        onDeleteForever = { forever = it },
                        onDrop = { payload, target -> dropInto(payload, target) },
                    )

                    val listBottomPad = appContentBottomPadding()
                    val showHero = section == Section.DOCUMENTS && folderId == null && query.isBlank()

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (entries.isEmpty()) {
                            Column(Modifier.fillMaxSize()) {
                                if (showHero) {
                                    LibraryHeroBanner(
                                        onNewNotebook = { newNotebook = true },
                                        onQuickImport = {
                                            importFilesLauncher.launch(arrayOf("application/pdf", "text/*", "image/*"))
                                        },
                                        onOpenPreview = onOpenPreview,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                                    )
                                }
                                EmptyState(section, onNew = { newNotebook = true })
                            }
                        } else {
                            AnimatedContent(
                                targetState = Triple(section, folderId, listView),
                                transitionSpec = { (fadeIn() + slideInHorizontally { it / 12 }) togetherWith fadeOut() },
                                label = "grid",
                            ) { (_, _, asList) ->
                                if (asList) {
                                    LazyColumn(
                                        Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = listBottomPad),
                                    ) {
                                        if (showHero) {
                                            item(key = "hero-list") {
                                                LibraryHeroBanner(
                                                    onNewNotebook = { newNotebook = true },
                                                    onQuickImport = {
                                                        importFilesLauncher.launch(arrayOf("application/pdf", "text/*", "image/*"))
                                                    },
                                                    onOpenPreview = onOpenPreview,
                                                    modifier = Modifier.padding(bottom = 14.dp),
                                                )
                                            }
                                        }
                                        items(entries, key = { keyOf(it) }) { e ->
                                            Box(Modifier.animateItem()) { ListRow(e, repo, actions) }
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
                                        if (showHero) {
                                            item(key = "hero-grid", span = { GridItemSpan(maxLineSpan) }) {
                                                LibraryHeroBanner(
                                                    onNewNotebook = { newNotebook = true },
                                                    onQuickImport = {
                                                        importFilesLauncher.launch(arrayOf("application/pdf", "text/*", "image/*"))
                                                    },
                                                    onOpenPreview = onOpenPreview,
                                                )
                                            }
                                        }
                                        items(entries, key = { keyOf(it) }) { e ->
                                            Box(Modifier.animateItem()) {
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
                        AppTabItem("Recent") { Icon(Icons.Outlined.AccessTime, null, Modifier.size(18.dp)) },
                        AppTabItem("Favorites") { Icon(Icons.Outlined.StarBorder, null, Modifier.size(18.dp)) },
                        AppTabItem("Trash") { Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp)) },
                    ),
                    selectedIndex = when (section) {
                        Section.DOCUMENTS -> 0
                        Section.RECENT -> 1
                        Section.FAVORITES -> 2
                        Section.TRASH -> 3
                    },
                    onSelect = { idx ->
                        section = when (idx) {
                            0 -> Section.DOCUMENTS
                            1 -> Section.RECENT
                            2 -> Section.FAVORITES
                            else -> Section.TRASH
                        }
                        folderId = null
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
                        }
                        .scale(if (drag.overTarget) 1.12f else 1f),
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
}

/**
 * Main screen showcase card featuring the fanned colorful card stack in front of a
 * slowly moving colorful gradient plus colorful quick action pills.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryHeroBanner(
    onNewNotebook: () -> Unit,
    onQuickImport: () -> Unit,
    onOpenPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSurface(
        role = SurfaceRole.Frost,
        shape = appCardShape(4f),
        contentPadding = PaddingValues(0.dp),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            MovingColorfulGradientBackground(Modifier.matchParentSize(), intensity = 0.85f)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScaleInAnimatedText(
                        text = "Your Study Desk",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        staggerMs = 24L,
                    )
                    Text(
                        "Tap the card stack to cycle workflows: low-latency stylus ink, 3D Kindle Book Mode, Flashcards, and SlideShow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppPill(
                            selected = true,
                            tint = ProButtonPalette.ElectricBlue,
                            onClick = onNewNotebook,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("+ New Notebook", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        AppPill(
                            selected = true,
                            tint = ProButtonPalette.VibrantOrange,
                            onClick = onQuickImport,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("Import PDF / Notes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        AppPill(
                            selected = true,
                            tint = ProButtonPalette.MagentaPink,
                            onClick = onOpenPreview,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("Preview Tour", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                FannedColorfulCardsDeck(
                    autoCycle = false,
                    showLabels = true,
                    compact = true,
                )
            }
        }
    }
}

private fun keyOf(e: Entry) = if (e is Entry.F) "f" + e.folder.id else "n" + (e as Entry.N).note.id

private class TileActions(
    val onOpenFolder: (Folder) -> Unit,
    val onOpenNote: (NotebookMeta) -> Unit,
    val onRename: (Entry) -> Unit,
    val onMove: (Entry) -> Unit,
    val onDelete: (Entry) -> Unit,
    val onEditFolder: (Folder) -> Unit,
    val onCover: (NotebookMeta) -> Unit,
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
    onOpenPreview: () -> Unit,
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
                IconButton(onClick = onCollapse, modifier = Modifier.elasticStretch()) {
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
                    Text("v12 Studio", style = MaterialTheme.typography.labelSmall, color = activeIconTint)
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
                        .elasticStretch()
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
                    .elasticStretch()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenCollab)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Groups, null, tint = ProButtonPalette.SkyCyan)
                Spacer(Modifier.width(12.dp))
                Text("Live Study Room", color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .elasticStretch()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenPreview)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProIconView(ProIcon.STAR, ProButtonPalette.VibrantOrange, Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Preview & Setup Tour", color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            if (Support.enabled) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .elasticStretch()
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
                    .elasticStretch()
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
                modifier = Modifier.elasticStretch(),
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("New notebook")
            }
        }
    }
}

@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    return s
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
    val grow by animateFloatAsState(if (dropHover) 1.08f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "drop")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.elasticStretch().scale(pressScale(src) * grow)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .libraryDragSource("folder:${e.folder.id}", e.folder.name)
                .libraryDropTarget(onHover = { dropHover = it }) { payload -> a.onDrop(payload, e.folder.id) }
                .combinedClickable(interactionSource = src, indication = null, onClick = { a.onOpenFolder(e.folder) }),
            contentAlignment = Alignment.BottomCenter,
        ) {
            FolderArt(e.folder.color, e.folder.emoji, Modifier.fillMaxWidth(0.88f).aspectRatio(1f))
        }
        Caption(e.folder.name, "${e.count} items", menuItems = remember(e) { folderMenuItems(e, a) })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotebookTile(note: NotebookMeta, repo: Repository, a: TileActions) {
    var trashPrompt by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.elasticStretch().scale(pressScale(src))) {
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

@Composable
private fun MoveRow(label: String, folder: Folder?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (folder != null) FolderArt(folder.color, "", Modifier.size(28.dp)) else Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val FolderTags = listOf("", "Study", "Work", "Lab", "Art", "Math", "Bio", "Code", "Plan", "Read", "Idea", "Star")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderDialog(existing: Folder?, onDismiss: () -> Unit, onSave: (String, Int, String) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "New folder") }
    var color by remember { mutableStateOf(existing?.color ?: FolderColors.first()) }
    var tag by remember { mutableStateOf(existing?.emoji ?: "") }
    var custom by remember { mutableStateOf(false) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New folder" else "Edit folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                FolderArt(color, tag, Modifier.size(width = 120.dp, height = 104.dp).align(Alignment.CenterHorizontally))
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") })
                Text("Colour", style = MaterialTheme.typography.labelSmall)
                FlowRow {
                    FolderColors.forEach { c -> Swatch(c, c == color) { color = c } }
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { custom = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, "Custom colour", Modifier.size(16.dp)) }
                }
                Text("Label", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FolderTags.forEach { t ->
                        AppChip(selected = t == tag, onClick = { tag = t }, label = t.ifEmpty { "None" })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), color, tag) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (custom) ColorEditorDialog(color, FolderColors, emptyList(), onDismiss = { custom = false }) { color = it; custom = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverGallery(selected: String, onSelect: (String) -> Unit, onPhoto: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val options = CoverColors.map { "solid:$it" } +
            CoverPatterns.flatMap { p ->
                val tones = when (p) {
                    "leather" -> listOf(0xFF5D3A23, 0xFF1C1C1E, 0xFF0E2A47, 0xFF6B1E2A)
                    "kraft" -> listOf(0xFFC49A6C, 0xFFA77B4F)
                    "marble" -> listOf(0xFF8E9AAF, 0xFF2E3440, 0xFFB5838D)
                    "terrazzo" -> listOf(0xFFE9E4DA, 0xFF6C757D)
                    "linen" -> listOf(0xFF9DB4C0, 0xFFB5A397, 0xFF5C6B73)
                    "gradient" -> listOf(0xFF3D6CF4, 0xFF9B51E0, 0xFFEB5757, 0xFF2BB673)
                    else -> listOf(CoverColors[0], CoverColors[3], CoverColors[1], CoverColors[9]).map { it.toLong() and 0xFFFFFFFFL }
                }
                tones.map { "pattern:$p:${it.toInt()}" }
            }
        options.forEach { spec ->
            Box(
                Modifier
                    .size(width = 54.dp, height = 72.dp)
                    .border(if (spec == selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, NotebookShape)
                    .clickable { onSelect(spec) },
            ) { CoverArt(spec, "", null, 0, Modifier.fillMaxSize(), showTitle = false) }
        }
        Column(
            Modifier
                .size(width = 54.dp, height = 72.dp)
                .clip(NotebookShape)
                .background(if (selected == "thumb") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onSelect("thumb") },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Description, null)
            Text("Page 1", style = MaterialTheme.typography.labelSmall)
        }
        Column(
            Modifier
                .size(width = 54.dp, height = 72.dp)
                .clip(NotebookShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPhoto),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Image, null)
            Text("Photo", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CoverDialog(current: String, onDismiss: () -> Unit, onPick: (String, Uri?) -> Unit) {
    var spec by remember { mutableStateOf(current) }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPick(spec, uri) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cover") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { CoverGallery(spec, { spec = it }) { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } } },
        confirmButton = { TextButton(onClick = { onPick(spec, null) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

data class NewBook(
    val name: String,
    val cover: String,
    val coverColor: Int,
    val paper: PaperStyle,
    val photo: Uri?,
    val width: Float,
    val height: Float,
    val paperColor: Int?,
    val coverPage: Boolean,
    val isWhiteboard: Boolean = false,
)

private val PaperColors = listOf(0xFFFFFFFF, 0xFFFBF7EC, 0xFFF1F2F4, 0xFFFFF4E0, 0xFFEAF4FF, 0xFFEFF8EC, 0xFF22252B).map { it.toInt() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewNotebookDialog(
    defaults: LibraryNewBookPrefs,
    onDismiss: () -> Unit,
    onCreate: (NewBook) -> Unit,
) {
    var name by remember { mutableStateOf("Untitled notebook") }
    val startColor = remember { CoverColors.random() }
    var cover by remember { mutableStateOf(if (defaults.thumbCovers) "thumb" else "solid:$startColor") }
    var coverPage by remember { mutableStateOf(defaults.defaultCoverPage) }
    var paper by remember { mutableStateOf(defaults.defaultPaper) }
    var paperColor by remember { mutableStateOf<Int?>(null) }
    var customPaper by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf(defaults.defaultPreset) }
    var landscape by remember { mutableStateOf(defaults.defaultLandscape) }
    var wMm by remember { mutableStateOf("%.0f".format(defaults.defaultPreset.wMm)) }
    var hMm by remember { mutableStateOf("%.0f".format(defaults.defaultPreset.hMm)) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var tab by remember { mutableStateOf(0) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) { photo = uri; cover = "photo" } }

    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("New notebook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 72.dp, height = 96.dp)) {
                        if (cover == "photo" || cover == "thumb") {
                            Box(Modifier.fillMaxSize().clip(NotebookShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Text(if (cover == "photo") "Photo" else "Page 1", style = MaterialTheme.typography.labelSmall)
                            }
                        } else CoverArt(cover, name, null, 0, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(14.dp))
                    OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Title") }, shape = CircleShape)
                }
                AppSegmentedControl(
                    options = listOf("Cover", "Paper", "Size"),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    when (tab) {
                        0 -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CoverGallery(cover, { cover = it; photo = null }) { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            if (cover != "thumb" && !preset.isWhiteboard) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Cover as first page")
                                        Text("Write on your cover", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    AppToggle(checked = coverPage, onCheckedChange = { coverPage = it })
                                }
                            }
                        }
                        1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val effectiveLandscape = if (preset.isWhiteboard) true else landscape
                            AppCard(
                                hero = false,
                                shape = appCardShape(),
                                contentPadding = PaddingValues(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PaperPreview(
                                        paper,
                                        Modifier.size(
                                            width = if (effectiveLandscape) 128.dp else 92.dp,
                                            height = if (effectiveLandscape) 92.dp else 128.dp,
                                        ).clip(RoundedCornerShape(10.dp)),
                                        tint = paperColor ?: 0xFFFFFFFF.toInt(),
                                        landscape = effectiveLandscape,
                                    )
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Live page preview", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(paper.label, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            buildString {
                                                append(preset.label)
                                                if (preset.recommended) append(" · Recommended")
                                                if (preset.isWhiteboard) append(" · Infinite Canvas")
                                                else append(if (effectiveLandscape) " · Landscape" else " · Portrait")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PaperStyle.pickerEntries.forEach { p ->
                                    val sel = p == paper
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { paper = p }) {
                                        PaperPreview(
                                            p,
                                            Modifier
                                                .size(width = 60.dp, height = 84.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    if (sel) 2.dp else 0.75.dp,
                                                    if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                    RoundedCornerShape(8.dp),
                                                ),
                                            tint = paperColor ?: 0xFFFFFFFF.toInt(),
                                        )
                                        Text(p.label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Text("Paper colour", style = MaterialTheme.typography.labelSmall)
                            FlowRow {
                                Swatch(0x00000000, paperColor == null, 22) { paperColor = null }
                                PaperColors.forEach { c -> Swatch(c, c == paperColor, 22) { paperColor = c } }
                                Box(
                                    Modifier
                                        .padding(4.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { customPaper = true },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Filled.Add, "Custom", Modifier.size(14.dp)) }
                            }
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PagePreset.entries.forEach { p ->
                                    val chipLabel = when {
                                        p.recommended -> "${p.label} · Recommended"
                                        p.isWhiteboard -> "${p.label} · Infinite"
                                        else -> p.label
                                    }
                                    AppChip(
                                        selected = preset == p,
                                        onClick = {
                                            preset = p
                                            if (p != PagePreset.CUSTOM) {
                                                wMm = "%.0f".format(p.wMm)
                                                hMm = "%.0f".format(p.hMm)
                                            }
                                            if (p.isWhiteboard && paper == PaperStyle.BLANK) paper = PaperStyle.DOTTED
                                        },
                                        label = chipLabel,
                                    )
                                }
                            }
                            if (preset.isWhiteboard) {
                                Text(
                                    "Unbounded infinite canvas with pan & zoom — great for diagrams, sketching, and mind maps.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Landscape", Modifier.weight(1f))
                                    AppToggle(checked = landscape, onCheckedChange = { landscape = it })
                                }
                            }
                            if (preset == PagePreset.CUSTOM) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(wMm, { v -> wMm = v.filter { it.isDigit() }.take(4) }, label = { Text("Width mm") }, singleLine = true, modifier = Modifier.weight(1f), shape = CircleShape)
                                    OutlinedTextField(hMm, { v -> hMm = v.filter { it.isDigit() }.take(4) }, label = { Text("Height mm") }, singleLine = true, modifier = Modifier.weight(1f), shape = CircleShape)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val color = cover.substringAfterLast(':').toLongOrNull()?.toInt() ?: startColor
                    var w = (wMm.toFloatOrNull() ?: 210f).coerceIn(40f, 1000f)
                    var h = (hMm.toFloatOrNull() ?: 297f).coerceIn(40f, 1000f)
                    if (!preset.isWhiteboard && landscape != (w > h)) {
                        val t = w; w = h; h = t
                    }
                    val k = PagePreset.UNITS_PER_MM
                    onCreate(
                        NewBook(
                            name.ifBlank { if (preset.isWhiteboard) "Whiteboard" else "Untitled notebook" },
                            if (cover == "photo") "" else cover,
                            color,
                            paper,
                            photo,
                            w * k,
                            h * k,
                            paperColor,
                            coverPage && cover != "thumb" && !preset.isWhiteboard,
                            isWhiteboard = preset.isWhiteboard,
                        ),
                    )
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (customPaper) ColorEditorDialog(paperColor ?: 0xFFFFFFFF.toInt(), PaperColors, emptyList(), onDismiss = { customPaper = false }) { paperColor = it; customPaper = false }
}

@Composable
private fun UpdatePill() {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val updatePrefs by remember(store) {
        store.select { it.autoUpdateCheck to it.updateReminders }
    }.collectAsStateWithLifecycle(initialValue = store.settings.value.let { it.autoUpdateCheck to it.updateReminders })
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
