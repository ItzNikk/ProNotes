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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pronotes.app.R
import com.pronotes.app.data.CoverColors
import com.pronotes.app.data.CoverPatterns
import com.pronotes.app.data.Folder
import com.pronotes.app.data.FolderColors
import com.pronotes.app.data.NotebookMeta
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.Repository
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.Swatch
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class Section(val label: String) { DOCUMENTS("Documents"), FAVORITES("Favorites"), RECENT("Recent"), TRASH("Trash") }
private enum class SortBy(val label: String) { MODIFIED("Date modified"), CREATED("Date created"), NAME("Name") }

private sealed interface Entry {
    data class F(val folder: Folder, val count: Int) : Entry
    data class N(val note: NotebookMeta) : Entry
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpen: (String) -> Unit,
    onSettings: () -> Unit,
    pendingImport: com.pronotes.app.PendingImport?,
    onImportConsumed: () -> Unit,
    onSupport: () -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val settings by SettingsStore.get(context).settings.collectAsState()
    val lib by repo.library.collectAsState()
    val scope = rememberCoroutineScope()
    val wide = LocalConfiguration.current.screenWidthDp >= 720

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
    var supportCard by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val st = SettingsStore.get(context)
        if (com.pronotes.app.support.Support.enabled && st.shouldRemindSupport()) { supportCard = true; st.markSupportShown() }
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

    /** Moves a dragged notebook or folder. A folder cannot go inside itself or its own subfolders. */
    fun dropInto(payload: String, target: String?): Boolean {
        val id = payload.substringAfter(':')
        return when {
            payload.startsWith("note:") -> { scope.launch { repo.moveNotebook(id, target) }; true }
            payload.startsWith("folder:") -> {
                if (id == target) return false
                var p = target
                while (p != null) { if (p == id) return false; p = lib.folders.firstOrNull { it.id == p }?.parentId }
                scope.launch { repo.moveFolder(id, target) }; true
            }
            else -> false
        }
    }

    BackHandler(enabled = folderId != null) { folderId = lib.folders.firstOrNull { it.id == folderId }?.parentId }

    val entries: List<Entry> = remember(lib, section, folderId, query, sort) {
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
        }
    }

    val currentFolder = lib.folders.firstOrNull { it.id == folderId }
    val title = if (section == Section.DOCUMENTS && currentFolder != null) currentFolder.name else section.label

    // Real blur behind dialogs on Android 12+.
    val dialogOpen = newNotebook || pasteImportOpen || newFolder || folderEdit != null || renaming != null || moving != null ||
        deleting != null || coverFor != null || forever != null || emptyTrash
    val blur by androidx.compose.animation.core.animateDpAsState(if (dialogOpen) 14.dp else 0.dp, label = "blur")
    val drag = remember { DragController() }
    androidx.compose.runtime.CompositionLocalProvider(LocalDragController provides drag) {
    Box(Modifier.fillMaxSize()) {
    Row(
        Modifier.fillMaxSize()
            .then(if (android.os.Build.VERSION.SDK_INT >= 31 && blur > 0.dp) Modifier.blur(blur) else Modifier)
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedVisibility(sidebar, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
            Sidebar(
                section, trashCount = lib.notebooks.count { it.deletedAt != null },
                onSelect = { section = it; folderId = null; if (!wide) sidebar = false },
                onCollapse = { sidebar = false }, onSettings = onSettings,
                onDropTo = { s, payload ->
                    when (s) {
                        Section.DOCUMENTS -> dropInto(payload, null)
                        Section.TRASH -> if (payload.startsWith("note:")) { scope.launch { repo.deleteNotebook(payload.substringAfter(':')) }; true } else false
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

        Column(Modifier.weight(1f).fillMaxHeight().statusBarsPadding()) {
            // Top row
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!sidebar) IconButton(onClick = { sidebar = true }) { com.pronotes.app.ui.ProIconView(com.pronotes.app.ui.ProIcon.SIDEBAR, MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) }
                // Open notebooks, like browser tabs.
                val store = remember { SettingsStore.get(context) }
                val tabs by store.tabs.collectAsState()
                val open = tabs.mapNotNull { id -> lib.notebooks.firstOrNull { it.id == id && it.deletedAt == null } }
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    open.forEach { n ->
                        Row(
                            Modifier.widthIn(max = 200.dp).clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable { onOpen(n.id) }.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(n.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Box(Modifier.size(22.dp).clip(CircleShape).clickable { store.closeTab(n.id) }, contentAlignment = Alignment.Center) {
                                com.pronotes.app.ui.ProIconView(com.pronotes.app.ui.ProIcon.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(11.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                UpdatePill()
                IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { com.pronotes.app.ui.ProIconView(com.pronotes.app.ui.ProIcon.SEARCH, MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) }
                IconButton(onClick = onSettings) { com.pronotes.app.ui.ProIconView(com.pronotes.app.ui.ProIcon.SETTINGS, MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) }
            }
            // Title row
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (folderId != null && section == Section.DOCUMENTS) {
                    // Drop here to move an item up one level.
                    var upHot by remember { mutableStateOf(false) }
                    Box(
                        Modifier.clip(CircleShape)
                            .libraryDropTarget(onHover = { upHot = it }) { payload -> dropInto(payload, currentFolder?.parentId) }
                            .background(if (upHot) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent),
                    ) {
                        IconButton(onClick = { folderId = currentFolder?.parentId }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Up") }
                    }
                }
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { (fadeIn() + slideInHorizontally { it / 6 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 6 }) },
                    label = "title", modifier = Modifier.weight(1f),
                ) { t -> com.pronotes.app.ui.GradientTitle(t, MaterialTheme.typography.headlineLarge) }

                var newMenu by remember { mutableStateOf(false) }
                if (section == Section.TRASH) {
                    TextButton(onClick = { emptyTrash = true }, enabled = entries.isNotEmpty()) { Text("Empty trash", color = MaterialTheme.colorScheme.error) }
                } else Box {
                    Button(onClick = { newMenu = true }, shape = CircleShape, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("New")
                        Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(18.dp))
                    }
                    DropdownMenu(newMenu, { newMenu = false }) {
                        DropdownMenuItem(text = { Text("Notebook") }, leadingIcon = { Icon(Icons.Outlined.Description, null) }, onClick = { newMenu = false; newNotebook = true })
                        DropdownMenuItem(text = { Text("Infinite whiteboard") }, leadingIcon = { Icon(Icons.Outlined.Draw, null) }, onClick = {
                            newMenu = false
                            scope.launch {
                                val k = com.pronotes.app.data.PagePreset.UNITS_PER_MM
                                val p = com.pronotes.app.data.PagePreset.WHITEBOARD
                                val id = repo.createNotebook(
                                    "Whiteboard", folderId, 0xFF3D6CF4.toInt(), PaperStyle.DOTTED,
                                    width = p.wMm * k, height = p.hMm * k, isWhiteboard = true,
                                )
                                onOpen(id)
                            }
                        })
                        DropdownMenuItem(text = { Text("Mind map canvas") }, leadingIcon = { Icon(Icons.Outlined.Hub, null) }, onClick = {
                            newMenu = false
                            scope.launch {
                                val k = com.pronotes.app.data.PagePreset.UNITS_PER_MM
                                val p = com.pronotes.app.data.PagePreset.WHITEBOARD
                                val id = repo.createNotebook(
                                    "Mind map", folderId, 0xFF9B51E0.toInt(), PaperStyle.DOTTED,
                                    width = p.wMm * k, height = p.hMm * k, isWhiteboard = true,
                                )
                                val pages = repo.loadPages(id).toMutableList()
                                if (pages.isNotEmpty()) {
                                    val first = pages[0]
                                    val root = com.pronotes.app.editor.MindNodeItem(
                                        com.pronotes.app.editor.ItemIds.next(),
                                        first.width / 2f - 130f, first.height / 2f - 44f, 260f, 88f,
                                        "Central Topic", null, 0xFF3D6CF4.toInt(), "pill",
                                    )
                                    pages[0] = first.copy(items = listOf(root))
                                    repo.savePages(id, pages)
                                }
                                SettingsStore.get(context).setPref("startTool_$id", com.pronotes.app.editor.Tool.MINDMAP.name)
                                onOpen(id)
                            }
                        })
                        DropdownMenuItem(text = { Text("Quick note") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = {
                            newMenu = false
                            scope.launch {
                                val k = com.pronotes.app.data.PagePreset.UNITS_PER_MM
                                var w = settings.defaultPreset.wMm * k; var h = settings.defaultPreset.hMm * k
                                if (!settings.defaultPreset.isWhiteboard && settings.defaultLandscape != (w > h)) { val t = w; w = h; h = t }
                                onOpen(repo.createNotebook("Quick note", folderId, CoverColors.random(), settings.defaultPaper, width = w, height = h, isWhiteboard = settings.defaultPreset.isWhiteboard))
                            }
                        })
                        DropdownMenuItem(text = { Text("Folder") }, leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) }, onClick = { newMenu = false; newFolder = true })
                        DropdownMenuItem(
                            text = { Text("Import files (PDF, MD, TXT, Images)") },
                            leadingIcon = { Icon(Icons.Outlined.UploadFile, null) },
                            onClick = {
                                newMenu = false
                                importFilesLauncher.launch(arrayOf("application/pdf", "text/*", "image/*", "application/json", "application/octet-stream"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Paste notes from another app…") },
                            leadingIcon = { Icon(Icons.Outlined.ContentPaste, null) },
                            onClick = { newMenu = false; pasteImportOpen = true },
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                var sortMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { sortMenu = true }) {
                        Text("Sort by ${sort.label.substringAfter("Date ").lowercase()}", style = MaterialTheme.typography.labelSmall)
                        Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(16.dp))
                    }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        SortBy.entries.forEach { s -> DropdownMenuItem(text = { Text(s.label) }, onClick = { sortMenu = false; sort = s }) }
                    }
                }
                IconButton(onClick = { listView = false }) {
                    Icon(Icons.Outlined.GridView, "Grid", tint = if (!listView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { listView = true }) {
                    Icon(Icons.AutoMirrored.Outlined.ViewList, "List", tint = if (listView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AnimatedVisibility(searching) {
                OutlinedTextField(
                    query, { query = it }, placeholder = { Text("Search notebooks and folders") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 4.dp),
                )
            }

            val actions = TileActions(
                onOpenFolder = { f -> section = Section.DOCUMENTS; query = ""; searching = false; folderId = f.id },
                onOpenNote = { onOpen(it.id) },
                onRename = { renaming = it }, onMove = { moving = it }, onDelete = { deleting = it },
                onEditFolder = { folderEdit = it }, onCover = { coverFor = it },
                onFavorite = { scope.launch { repo.toggleFavorite(it.id) } },
                onDuplicate = { scope.launch { repo.duplicateNotebook(it.id) } },
                onRestore = { scope.launch { repo.restoreNotebook(it.id) } },
                onDeleteForever = { forever = it },
                onDrop = { payload, target -> dropInto(payload, target) },
            )

            if (entries.isEmpty()) {
                EmptyState(section, onNew = { newNotebook = true })
            } else AnimatedContent(
                targetState = Triple(section, folderId, listView),
                transitionSpec = { (fadeIn() + slideInHorizontally { it / 12 }) togetherWith fadeOut() },
                label = "grid",
            ) { (_, _, asList) ->
                if (asList) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                        items(entries, key = { keyOf(it) }) { e -> Box(Modifier.animateItem()) { ListRow(e, repo, actions) } }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
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

    // The tile that follows your finger or pen while dragging.
    drag.payload?.let { p ->
        Surface(
            shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 14.dp,
            modifier = Modifier.offset {
                androidx.compose.ui.unit.IntOffset(
                    (drag.position.x - 70.dp.toPx()).toInt(), (drag.position.y - 80.dp.toPx()).toInt(),
                )
            }.scale(if (drag.overTarget) 1.12f else 1f),
        ) {
            Text(
                drag.label.ifBlank { if (p.startsWith("folder")) "Folder" else "Notebook" },
                Modifier.widthIn(max = 220.dp).padding(horizontal = 18.dp, vertical = 10.dp),
                maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall,
            )
        }
    }
    }
    }

    if (supportCard) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), shadowElevation = 12.dp,
                modifier = Modifier.padding(20.dp).widthIn(max = 520.dp),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.pronotes.app.ui.ProIconView(
                        com.pronotes.app.ui.ProIcon.HEART, Color(0xFFE5484D), Modifier.size(26.dp), ink = Color(0xFFE5484D),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Enjoying Pro Notes?", style = MaterialTheme.typography.titleMedium)
                        Text("It is free and made by one developer. A small tip helps.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { supportCard = false }) { Text("Not now") }
                    Button(onClick = { supportCard = false; onSupport() }, shape = CircleShape) { Text("Support") }
                }
            }
        }
    }

    // ---------------------------------------------------------------- dialogs
    if (newNotebook) NewNotebookDialog(settings.defaultPaper, settings.thumbCovers, settings, onDismiss = { newNotebook = false }) { b ->
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
        AlertDialog(
            onDismissRequest = { pasteImportOpen = false },
            title = { Text("Import notes from another app") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste Markdown, HTML, CSV, or plain notes copied from Notion, Obsidian, Apple Notes, or Google Keep. Headings, checklists, bullets, and callouts are formatted across pages.",
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
                TextButton(
                    enabled = importBody.isNotBlank(),
                    onClick = {
                        val t = importTitle
                        val b = importBody
                        pasteImportOpen = false
                        scope.launch { onOpen(repo.importStructuredText(t, b, folderId)) }
                    },
                ) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { pasteImportOpen = false }) { Text("Cancel") } },
        )
    }

    if (newFolder) FolderDialog(null, onDismiss = { newFolder = false }) { name, color, emoji ->
        newFolder = false
        scope.launch { repo.createFolder(name, folderId, color, emoji) }
    }
    folderEdit?.let { f ->
        FolderDialog(f, onDismiss = { folderEdit = null }) { name, color, emoji ->
            folderEdit = null
            scope.launch { repo.updateFolder(f.id, name, color, emoji) }
        }
    }

    forever?.let { n ->
        AlertDialog(
            onDismissRequest = { forever = null },
            title = { Text("Delete \"${n.title}\" forever?") },
            text = { Text("This cannot be undone.") },
            confirmButton = { TextButton(onClick = { forever = null; scope.launch { repo.deleteForever(n.id) } }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { forever = null }) { Text("Cancel") } },
        )
    }
    if (emptyTrash) {
        AlertDialog(
            onDismissRequest = { emptyTrash = false },
            title = { Text("Empty trash?") },
            text = { Text("All notebooks in Trash will be deleted forever.") },
            confirmButton = { TextButton(onClick = { emptyTrash = false; scope.launch { repo.emptyTrash() } }) { Text("Empty", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { emptyTrash = false }) { Text("Cancel") } },
        )
    }

    coverFor?.let { n ->
        CoverDialog(n.coverSpec, onDismiss = { coverFor = null }) { spec, photo ->
            coverFor = null
            scope.launch {
                val s = if (photo != null) repo.saveCoverImage(n.id, photo) else spec
                // Notebooks with a cover page get the new design on that page.
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
        AlertDialog(
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
                    items(lib.folders.filter { it.id != selfId }) { f ->
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
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"$name\"?") },
            text = { Text(if (e is Entry.F) "The folder will be deleted. Notebooks inside it move to Trash." else "The notebook moves to Trash. You can restore it for 30 days.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch { if (e is Entry.F) repo.deleteFolder(e.folder.id) else repo.deleteNotebook((e as Entry.N).note.id) }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
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
    /** Drag and drop: "note:<id>" or "folder:<id>" dropped on a folder (null = top level). */
    val onDrop: (String, String?) -> Boolean,
)

// ================================================================ sidebar

@Composable
private fun Sidebar(
    section: Section, trashCount: Int, onSelect: (Section) -> Unit, onCollapse: () -> Unit, onSettings: () -> Unit,
    onDropTo: (Section, String) -> Boolean,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxHeight().width(232.dp)) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCollapse) { Icon(Icons.Outlined.ChevronLeft, "Hide sidebar") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 22.dp)) {
                androidx.compose.foundation.Image(
                    painterResource(R.drawable.ic_launcher_foreground), null,
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                )
                Spacer(Modifier.width(10.dp))
                Text("Pro Notes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Section.entries.forEach { s ->
                val sel = s == section
                var hot by remember { mutableStateOf(false) }
                Row(
                    Modifier.fillMaxWidth().clip(CircleShape)
                        .libraryDropTarget(onHover = { hot = it }) { payload -> onDropTo(s, payload) }
                        .background(
                            when {
                                hot -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                sel -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            },
                        )
                        .clickable { onSelect(s) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (s) {
                            Section.DOCUMENTS -> Icons.Outlined.Folder; Section.FAVORITES -> Icons.Outlined.StarBorder
                            Section.RECENT -> Icons.Outlined.AccessTime; Section.TRASH -> Icons.Outlined.Delete
                        },
                        null, tint = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(s.label, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    if (s == Section.TRASH && trashCount > 0) Text("$trashCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().clip(CircleShape).clickable(onClick = onSettings).padding(horizontal = 12.dp, vertical = 10.dp),
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
    Column(Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            when (section) {
                Section.FAVORITES -> "No favorites yet"; Section.RECENT -> "Nothing opened yet"
                Section.TRASH -> "Trash is empty"; else -> "Your library is empty"
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        if (section == Section.TRASH) {
            Text("Deleted notebooks stay here for 30 days.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Create a notebook or import a PDF to start.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onNew, shape = CircleShape) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("New notebook") }
        }
    }
}

// ================================================================ tiles

@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    return s
}

/** A folder drawn as a tabbed folder in its own colour, with an optional emoji. */
@Composable
fun FolderArt(color: Int, emoji: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val c = Color(color)
            val back = Path().apply {
                moveTo(0f, h * 0.18f); lineTo(w * 0.36f, h * 0.18f); lineTo(w * 0.44f, h * 0.28f); lineTo(w, h * 0.28f)
                lineTo(w, h * 0.92f); lineTo(0f, h * 0.92f); close()
            }
            drawPath(back, c.copy(alpha = 0.75f))
            drawRoundRect(c, Offset(0f, h * 0.34f), Size(w, h * 0.6f), CornerRadius(w * 0.06f))
            drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(0f, h * 0.34f), Size(w, h * 0.06f), CornerRadius(w * 0.06f))
        }
        if (emoji.isNotBlank()) Text(emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 18.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderTile(e: Entry.F, a: TileActions) {
    var menu by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    var dropHover by remember { mutableStateOf(false) }
    val grow by animateFloatAsState(if (dropHover) 1.08f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "drop")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(pressScale(src) * grow)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.75f)
                .libraryDragSource("folder:${e.folder.id}", e.folder.name)
                .libraryDropTarget(onHover = { dropHover = it }) { payload -> a.onDrop(payload, e.folder.id) }
                .combinedClickable(interactionSource = src, indication = null, onClick = { a.onOpenFolder(e.folder) }),
            contentAlignment = Alignment.BottomCenter,
        ) {
            FolderArt(e.folder.color, e.folder.emoji, Modifier.fillMaxWidth(0.88f).aspectRatio(1f))
        }
        Caption(e.folder.name, "${e.count} items", onMenu = { menu = true })
        Box {
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Colour & icon") }, leadingIcon = { Icon(Icons.Outlined.Palette, null) }, onClick = { menu = false; a.onEditFolder(e.folder) })
                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; a.onRename(e) })
                DropdownMenuItem(text = { Text("Move") }, leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) }, onClick = { menu = false; a.onMove(e) })
                DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; a.onDelete(e) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotebookTile(note: NotebookMeta, repo: Repository, a: TileActions) {
    var menu by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(pressScale(src))) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.75f)
                .then(if (note.deletedAt == null) Modifier.libraryDragSource("note:${note.id}", note.title) else Modifier)
                .combinedClickable(
                    interactionSource = src, indication = null,
                    onClick = { if (note.deletedAt != null) menu = true else a.onOpenNote(note) },
                    onLongClick = if (note.deletedAt != null) ({ menu = true }) else null,
                ),
        ) {
            CoverArt(note.coverSpec, note.title, repo.noteDir(note.id), note.modified, Modifier.fillMaxSize())
            if (note.favorite) Icon(Icons.Filled.Star, null, tint = Color(0xFFF2C94C), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp))
        }
        Caption(note.title, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(note.modified)), onMenu = { menu = true })
        Box { NoteMenu(menu, { menu = false }, note, a) }
    }
}

@Composable
private fun NoteMenu(open: Boolean, onClose: () -> Unit, note: NotebookMeta, a: TileActions) {
    if (note.deletedAt != null) {
        DropdownMenu(open, onClose, shape = RoundedCornerShape(18.dp)) {
            DropdownMenuItem(text = { Text("Restore") }, leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) }, onClick = { onClose(); a.onRestore(note) })
            DropdownMenuItem(text = { Text("Delete forever", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick = { onClose(); a.onDeleteForever(note) })
        }
        return
    }
    DropdownMenu(open, onClose, shape = RoundedCornerShape(18.dp)) {
        DropdownMenuItem(text = { Text("Duplicate") }, leadingIcon = { Icon(Icons.Outlined.Description, null) }, onClick = { onClose(); a.onDuplicate(note) })
        DropdownMenuItem(text = { Text(if (note.favorite) "Remove from favorites" else "Add to favorites") },
            leadingIcon = { Icon(if (note.favorite) Icons.Outlined.StarBorder else Icons.Outlined.Star, null) },
            onClick = { onClose(); a.onFavorite(note) })
        DropdownMenuItem(text = { Text("Change cover") }, leadingIcon = { Icon(Icons.Outlined.Image, null) }, onClick = { onClose(); a.onCover(note) })
        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { onClose(); a.onRename(Entry.N(note)) })
        DropdownMenuItem(text = { Text("Move") }, leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) }, onClick = { onClose(); a.onMove(Entry.N(note)) })
        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { onClose(); a.onDelete(Entry.N(note)) })
    }
}

@Composable
private fun Caption(title: String, sub: String, onMenu: () -> Unit) {
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onMenu)) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Icon(Icons.Filled.KeyboardArrowDown, "Options", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ListRow(e: Entry, repo: Repository, a: TileActions) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable {
                if (e is Entry.F) a.onOpenFolder(e.folder)
                else (e as Entry.N).note.let { n -> if (n.deletedAt == null) a.onOpenNote(n) else menu = true }
            }
            .padding(vertical = 8.dp, horizontal = 8.dp),
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
                if (e is Entry.F) "${e.count} items" else (e as Entry.N).note.let { "${it.pageCount} pages · " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it.modified)) },
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.KeyboardArrowDown, "Options") }
            if (e is Entry.N) NoteMenu(menu, { menu = false }, e.note, a)
            else DropdownMenu(menu, { menu = false }) {
                val f = (e as Entry.F).folder
                DropdownMenuItem(text = { Text("Colour & icon") }, onClick = { menu = false; a.onEditFolder(f) })
                DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; a.onRename(e) })
                DropdownMenuItem(text = { Text("Move") }, onClick = { menu = false; a.onMove(e) })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; a.onDelete(e) })
            }
        }
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

// ================================================================ dialogs

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val FolderEmojis = listOf("", "📚", "📝", "🎓", "💼", "🧪", "🎨", "💡", "📅", "❤️", "⭐", "🏠", "✈️", "🎵", "🧠", "📈")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderDialog(existing: Folder?, onDismiss: () -> Unit, onSave: (String, Int, String) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "New folder") }
    var color by remember { mutableStateOf(existing?.color ?: FolderColors.first()) }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "") }
    var custom by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New folder" else "Edit folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                FolderArt(color, emoji, Modifier.size(width = 120.dp, height = 104.dp).align(Alignment.CenterHorizontally))
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") })
                Text("Colour", style = MaterialTheme.typography.labelSmall)
                FlowRow {
                    FolderColors.forEach { c -> Swatch(c, c == color) { color = c } }
                    Box(
                        Modifier.padding(4.dp).size(26.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).clickable { custom = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, "Custom colour", Modifier.size(16.dp)) }
                }
                Text("Icon", style = MaterialTheme.typography.labelSmall)
                FlowRow {
                    FolderEmojis.forEach { em ->
                        Box(
                            Modifier.padding(2.dp).size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (em == emoji) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { emoji = em },
                            contentAlignment = Alignment.Center,
                        ) { Text(em.ifEmpty { "∅" }) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), color, emoji) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (custom) ColorEditorDialog(color, FolderColors, emptyList(), onDismiss = { custom = false }) { color = it; custom = false }
}

/** Cover choices shared by "New notebook" and "Change cover". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverGallery(selected: String, onSelect: (String) -> Unit, onPhoto: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val options = CoverColors.map { "solid:$it" } +
            CoverPatterns.flatMap { p ->
                // Colours that suit each material.
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
                Modifier.size(width = 54.dp, height = 72.dp)
                    .border(if (spec == selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, NotebookShape)
                    .clickable { onSelect(spec) },
            ) { CoverArt(spec, "", null, 0, Modifier.fillMaxSize(), showTitle = false) }
        }
        Column(
            Modifier.size(width = 54.dp, height = 72.dp).clip(NotebookShape).border(1.dp, MaterialTheme.colorScheme.outline, NotebookShape)
                .background(if (selected == "thumb") MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { onSelect("thumb") },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) { Icon(Icons.Outlined.Description, null); Text("Page 1", style = MaterialTheme.typography.labelSmall) }
        Column(
            Modifier.size(width = 54.dp, height = 72.dp).clip(NotebookShape).border(1.dp, MaterialTheme.colorScheme.outline, NotebookShape).clickable(onClick = onPhoto),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) { Icon(Icons.Outlined.Image, null); Text("Photo", style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun CoverDialog(current: String, onDismiss: () -> Unit, onPick: (String, Uri?) -> Unit) {
    var spec by remember { mutableStateOf(current) }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPick(spec, uri) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cover") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { CoverGallery(spec, { spec = it }) { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } } },
        confirmButton = { TextButton(onClick = { onPick(spec, null) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Everything chosen in the "New notebook" dialog. */
data class NewBook(
    val name: String, val cover: String, val coverColor: Int, val paper: PaperStyle, val photo: Uri?,
    val width: Float, val height: Float, val paperColor: Int?, val coverPage: Boolean,
    val isWhiteboard: Boolean = false,
)

private val PaperColors = listOf(0xFFFFFFFF, 0xFFFBF7EC, 0xFFF1F2F4, 0xFFFFF4E0, 0xFFEAF4FF, 0xFFEFF8EC, 0xFF22252B).map { it.toInt() }

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NewNotebookDialog(
    defaultPaper: PaperStyle,
    thumbCovers: Boolean,
    defaults: com.pronotes.app.data.AppSettings,
    onDismiss: () -> Unit,
    onCreate: (NewBook) -> Unit,
) {
    var name by remember { mutableStateOf("Untitled notebook") }
    val startColor = remember { CoverColors.random() }
    var cover by remember { mutableStateOf(if (thumbCovers) "thumb" else "solid:$startColor") }
    var coverPage by remember { mutableStateOf(defaults.defaultCoverPage) }
    var paper by remember { mutableStateOf(defaultPaper) }
    var paperColor by remember { mutableStateOf<Int?>(null) }
    var customPaper by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf(defaults.defaultPreset) }
    var landscape by remember { mutableStateOf(defaults.defaultLandscape) }
    var wMm by remember { mutableStateOf("%.0f".format(defaults.defaultPreset.wMm)) }
    var hMm by remember { mutableStateOf("%.0f".format(defaults.defaultPreset.hMm)) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var tab by remember { mutableStateOf(0) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) { photo = uri; cover = "photo" } }
    AlertDialog(
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
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(tab == 0, { tab = 0 }, text = { Text("Cover") })
                    Tab(tab == 1, { tab = 1 }, text = { Text("Paper") })
                    Tab(tab == 2, { tab = 2 }, text = { Text("Size") })
                }
                Box(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    when (tab) {
                        0 -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CoverGallery(cover, { cover = it; photo = null }) { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            if (cover != "thumb" && !preset.isWhiteboard) Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Cover as first page")
                                    Text("Write on your cover", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                androidx.compose.material3.Switch(coverPage, { coverPage = it })
                            }
                        }
                        1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val effectiveLandscape = if (preset.isWhiteboard) true else landscape
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PaperPreview(
                                        paper,
                                        Modifier.size(
                                            width = if (effectiveLandscape) 128.dp else 92.dp,
                                            height = if (effectiveLandscape) 92.dp else 128.dp,
                                        ).clip(RoundedCornerShape(10.dp))
                                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
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
                                                if (preset.isWhiteboard) append(" · ∞ Canvas")
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
                                            Modifier.size(width = 60.dp, height = 84.dp).clip(RoundedCornerShape(8.dp))
                                                .border(if (sel) 2.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
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
                                    Modifier.padding(4.dp).size(26.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).clickable { customPaper = true },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Filled.Add, "Custom", Modifier.size(14.dp)) }
                            }
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.pronotes.app.data.PagePreset.entries.forEach { p ->
                                    val chipLabel = when {
                                        p.recommended -> "${p.label} · Recommended"
                                        p.isWhiteboard -> "${p.label} · ∞"
                                        else -> p.label
                                    }
                                    androidx.compose.material3.FilterChip(preset == p, {
                                        preset = p
                                        if (p != com.pronotes.app.data.PagePreset.CUSTOM) {
                                            wMm = "%.0f".format(p.wMm); hMm = "%.0f".format(p.hMm)
                                        }
                                        if (p.isWhiteboard && paper == PaperStyle.BLANK) paper = PaperStyle.DOTTED
                                    }, label = { Text(chipLabel) }, shape = CircleShape)
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
                                    androidx.compose.material3.Switch(landscape, { landscape = it })
                                }
                            }
                            if (preset == com.pronotes.app.data.PagePreset.CUSTOM) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(wMm, { v -> wMm = v.filter { it.isDigit() }.take(4) }, label = { Text("Width mm") }, singleLine = true, modifier = Modifier.weight(1f), shape = CircleShape)
                                OutlinedTextField(hMm, { v -> hMm = v.filter { it.isDigit() }.take(4) }, label = { Text("Height mm") }, singleLine = true, modifier = Modifier.weight(1f), shape = CircleShape)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val color = cover.substringAfterLast(':').toLongOrNull()?.toInt() ?: startColor
                var w = (wMm.toFloatOrNull() ?: 210f).coerceIn(40f, 1000f)
                var h = (hMm.toFloatOrNull() ?: 297f).coerceIn(40f, 1000f)
                if (!preset.isWhiteboard && landscape != (w > h)) { val t = w; w = h; h = t }
                val k = com.pronotes.app.data.PagePreset.UNITS_PER_MM
                onCreate(NewBook(
                    name.ifBlank { if (preset.isWhiteboard) "Whiteboard" else "Untitled notebook" },
                    if (cover == "photo") "" else cover, color, paper, photo,
                    w * k, h * k, paperColor, coverPage && cover != "thumb" && !preset.isWhiteboard,
                    isWhiteboard = preset.isWhiteboard,
                ))
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (customPaper) ColorEditorDialog(paperColor ?: 0xFFFFFFFF.toInt(), PaperColors, emptyList(), onDismiss = { customPaper = false }) { paperColor = it; customPaper = false }
}

/** Small pill in the library header: offers in-app updates when a new release is out. */
@Composable
private fun UpdatePill() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by SettingsStore.get(context).settings.collectAsState()
    androidx.compose.runtime.LaunchedEffect(settings.autoUpdateCheck) {
        if (settings.autoUpdateCheck || settings.updateReminders) com.pronotes.app.data.Updater.check(context, quiet = true)
        // Launch reminder: show what is new once per app start while an update is waiting.
        val u0 = com.pronotes.app.data.Updater
        if (settings.updateReminders && u0.phase == com.pronotes.app.data.Updater.Phase.AVAILABLE && !u0.remindedThisLaunch) {
            u0.remindedThisLaunch = true
            kotlinx.coroutines.delay(1200)
            u0.showDialog = true
        }
    }
    val u = com.pronotes.app.data.Updater
    val label = when (u.phase) {
        com.pronotes.app.data.Updater.Phase.AVAILABLE -> "Update to ${u.release?.tag}"
        com.pronotes.app.data.Updater.Phase.DOWNLOADING -> "Downloading ${(u.progress * 100).toInt()}%"
        com.pronotes.app.data.Updater.Phase.READY -> "Install update"
        com.pronotes.app.data.Updater.Phase.INSTALLING -> "Installing..."
        else -> null
    } ?: return
    Surface(
        shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.padding(horizontal = 6.dp).clickable {
            when (u.phase) {
                com.pronotes.app.data.Updater.Phase.AVAILABLE -> u.showDialog = true
                com.pronotes.app.data.Updater.Phase.READY -> u.install(context)
                else -> Unit
            }
        },
    ) { Text(label, Modifier.padding(horizontal = 14.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall) }
}
