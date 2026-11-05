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
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
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
fun LibraryScreen(onOpen: (String) -> Unit, onSettings: () -> Unit, pendingPdf: Uri?, onPdfConsumed: () -> Unit) {
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
    var folderEdit by remember { mutableStateOf<Folder?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Entry?>(null) }
    var moving by remember { mutableStateOf<Entry?>(null) }
    var deleting by remember { mutableStateOf<Entry?>(null) }
    var coverFor by remember { mutableStateOf<NotebookMeta?>(null) }
    var forever by remember { mutableStateOf<NotebookMeta?>(null) }
    var emptyTrash by remember { mutableStateOf(false) }

    val importPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { onOpen(repo.importPdf(uri, folderId)) }
    }
    if (pendingPdf != null) {
        LaunchedEffect(pendingPdf) {
            val id = repo.importPdf(pendingPdf, null)
            onPdfConsumed()
            onOpen(id)
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

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedVisibility(sidebar, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
            Sidebar(
                section, trashCount = lib.notebooks.count { it.deletedAt != null },
                onSelect = { section = it; folderId = null; if (!wide) sidebar = false },
                onCollapse = { sidebar = false }, onSettings = onSettings,
            )
        }

        Column(Modifier.weight(1f).fillMaxHeight().statusBarsPadding()) {
            // Top row
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!sidebar) IconButton(onClick = { sidebar = true }) { Icon(Icons.Outlined.Menu, "Sidebar") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Icon(Icons.Outlined.Search, "Search") }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
            }
            // Title row
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (folderId != null && section == Section.DOCUMENTS) {
                    IconButton(onClick = { folderId = currentFolder?.parentId }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Up") }
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
                        DropdownMenuItem(text = { Text("Quick note") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = {
                            newMenu = false
                            scope.launch { onOpen(repo.createNotebook("Quick note", folderId, CoverColors.random(), settings.defaultPaper)) }
                        })
                        DropdownMenuItem(text = { Text("Folder") }, leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) }, onClick = { newMenu = false; newFolder = true })
                        DropdownMenuItem(text = { Text("Import PDF") }, leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }, onClick = { newMenu = false; importPdf.launch(arrayOf("application/pdf")) })
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

    // ---------------------------------------------------------------- dialogs
    if (newNotebook) NewNotebookDialog(settings.defaultPaper, settings.thumbCovers, onDismiss = { newNotebook = false }) { name, cover, coverColor, paper, photo ->
        newNotebook = false
        scope.launch {
            val id = repo.createNotebook(name, folderId, coverColor, paper, cover)
            if (photo != null) repo.setCover(id, repo.saveCoverImage(id, photo))
            onOpen(id)
        }
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
            scope.launch { repo.setCover(n.id, if (photo != null) repo.saveCoverImage(n.id, photo) else spec) }
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
)

// ================================================================ sidebar

@Composable
private fun Sidebar(section: Section, trashCount: Int, onSelect: (Section) -> Unit, onCollapse: () -> Unit, onSettings: () -> Unit) {
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
                Row(
                    Modifier.fillMaxWidth().clip(CircleShape)
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(pressScale(src))) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.75f)
                .combinedClickable(interactionSource = src, indication = null, onClick = { a.onOpenFolder(e.folder) }, onLongClick = { menu = true }),
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
                .combinedClickable(
                    interactionSource = src, indication = null,
                    onClick = { if (note.deletedAt != null) menu = true else a.onOpenNote(note) }, onLongClick = { menu = true },
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
            CoverPatterns.flatMap { p -> listOf(CoverColors[0], CoverColors[3], CoverColors[1], CoverColors[9]).map { "pattern:$p:$it" } }
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

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NewNotebookDialog(
    defaultPaper: PaperStyle,
    thumbCovers: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, cover: String, coverColor: Int, paper: PaperStyle, photo: Uri?) -> Unit,
) {
    var name by remember { mutableStateOf("Untitled notebook") }
    val startColor = remember { CoverColors.random() }
    var cover by remember { mutableStateOf(if (thumbCovers) "thumb" else "solid:$startColor") }
    var paper by remember { mutableStateOf(defaultPaper) }
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
                    OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Title") })
                }
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(tab == 0, { tab = 0 }, text = { Text("Cover") })
                    Tab(tab == 1, { tab = 1 }, text = { Text("Paper") })
                }
                Box(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    if (tab == 0) {
                        CoverGallery(cover, { cover = it; photo = null }) { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PaperStyle.entries.forEach { p ->
                                val sel = p == paper
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { paper = p }) {
                                    PaperPreview(
                                        p,
                                        Modifier.size(width = 64.dp, height = 90.dp).clip(RoundedCornerShape(6.dp))
                                            .border(if (sel) 2.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                                    )
                                    Text(p.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val color = cover.substringAfterLast(':').toLongOrNull()?.toInt() ?: startColor
                onCreate(name.ifBlank { "Untitled notebook" }, if (cover == "photo") "" else cover, color, paper, photo)
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
