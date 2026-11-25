package com.pronotes.app.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ContentPasteGo
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppMenuButton
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.GlowingWorkspace
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.WorkspaceIcon as ProIconView
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

private fun Section.localizedLabel(strings: com.pronotes.app.data.AppStrings): String = when (this) {
    Section.DOCUMENTS -> strings.documents
    Section.FAVORITES -> strings.favorites
    Section.RECENT -> strings.recent
    Section.TRASH -> strings.trash
}

private enum class SortBy(val label: String) {
    MODIFIED("Date modified"),
    CREATED("Date created"),
    NAME("Name"),
}

private fun SortBy.localizedLabel(strings: com.pronotes.app.data.AppStrings): String = when (this) {
    SortBy.MODIFIED -> strings.sortDateModified
    SortBy.CREATED -> strings.sortDateCreated
    SortBy.NAME -> strings.sortName
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
    val topScrollBlur: Boolean,
    val storageFolderLabel: String = "",
    val handwritingLanguage: String = "en-US",
)

@OptIn(ExperimentalFoundationApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
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
            LibraryNewBookPrefs(
                it.defaultPreset,
                it.defaultLandscape,
                it.defaultPaper,
                it.thumbCovers,
                it.defaultCoverPage,
                it.topScrollBlur,
                it.storageFolderLabel,
                it.toolDefaults.handwritingLanguage,
            )
        }
    }.collectAsStateWithLifecycle(
        initialValue = remember(store) {
            store.settings.value.let {
                LibraryNewBookPrefs(
                    it.defaultPreset,
                    it.defaultLandscape,
                    it.defaultPaper,
                    it.thumbCovers,
                    it.defaultCoverPage,
                    it.topScrollBlur,
                    it.storageFolderLabel,
                    it.toolDefaults.handwritingLanguage,
                )
            }
        },
    )
    val libScope = rememberCoroutineScope()
    val pickStorageFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            val label = com.pronotes.app.data.StorageFolderSync.friendlyFolderName(uri)
            store.update { it.copy(storageFolderUri = uri.toString(), storageFolderLabel = label) }
            libScope.launch {
                val count = runCatching {
                    com.pronotes.app.data.StorageFolderSync.syncAllNotebooks(context, uri, repo)
                }.getOrDefault(0)
                android.widget.Toast.makeText(
                    context,
                    "Notes storage set to $label ($count synced)",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    val lib by repo.library.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current

    var section by rememberSaveable { mutableStateOf(Section.DOCUMENTS) }
    var folderId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(SortBy.MODIFIED) }
    var listView by rememberSaveable { mutableStateOf(false) }
    var wideSidebar by rememberSaveable { mutableStateOf(true) }
    var compactSidebar by rememberSaveable { mutableStateOf(false) }
    var destination by rememberSaveable { mutableStateOf("") }
    if (destination == "Templates") NotebookTemplatesDialog(false, folderId, { destination = "" }, onOpen)
    if (destination == "Books" || destination == "Marketplace") {
        CatalogDialog(destination == "Books", folderId, { destination = "" }, onOpen)
    }
    val searchFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val searchKeyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var bulkMoveOpen by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var batchBusy by remember { mutableStateOf(false) }
    var selectedDocuments by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmBulkTrash by remember { mutableStateOf<Set<String>?>(null) }
    fun toggleDocument(id: String) {
        if (batchBusy) return
        selectionMode = true
        selectedDocuments = if (id in selectedDocuments) selectedDocuments - id else selectedDocuments + id
    }
    BackHandler(enabled = selectionMode) { selectionMode = false; selectedDocuments = emptySet() }
    LaunchedEffect(section, folderId) { selectionMode = false; selectedDocuments = emptySet() }
    confirmBulkTrash?.let { ids ->
        AppDialog(
            onDismissRequest = { confirmBulkTrash = null },
            title = { Text("Move ${ids.size} documents to Trash?") },
            text = { Text("You can restore them from Trash.") },
            confirmButton = { TextButton(onClick = {
                confirmBulkTrash = null
                scope.launch {
                    try {
                        ids.forEach { repo.deleteNotebook(it) }
                        selectedDocuments = emptySet(); selectionMode = false
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, "Could not move every document to Trash. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }) { Text("Move to Trash") } },
            dismissButton = { TextButton(onClick = { confirmBulkTrash = null }) { Text("Cancel") } },
        )
    }
    var newNotebook by remember { mutableStateOf(false) }
    var showGuide by rememberSaveable {
        mutableStateOf(store.pref("library_guide_seen") != "yes" && lib.notebooks.isEmpty())
    }
    if (showGuide && pendingImport == null) {
        fun closeGuide() {
            store.setPref("library_guide_seen", "yes")
            showGuide = false
        }
        LibraryGuide(
            onDismiss = ::closeGuide,
            onCreate = { closeGuide(); newNotebook = true },
        )
    }
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
    var emptyTrash by remember { mutableStateOf(false) }
    var pdfImportProgress by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }

    val importFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            try {
                runCatching {
                    repo.importFiles(uris, folderId) { title, page, total ->
                        pdfImportProgress = Triple(title, page, total)
                    }
                }.onSuccess { id ->
                    id?.let(onOpen)
                }.onFailure { err ->
                    val msg = if (err is SecurityException) {
                        "This PDF is password-protected or locked."
                    } else {
                        "Could not import file. It may be corrupted or unreadable."
                    }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                pdfImportProgress = null
            }
        }
    }
    if (destination == "PDF Station") PdfStation({ destination = "" }) {
        destination = ""
        importFilesLauncher.launch(arrayOf("*/*"))
    }
    if (pendingImport != null) {
        LaunchedEffect(pendingImport) {
            val id = try {
                runCatching {
                    when {
                        pendingImport.uris.isNotEmpty() -> repo.importFiles(pendingImport.uris, null) { title, page, total ->
                            pdfImportProgress = Triple(title, page, total)
                        }
                        !pendingImport.text.isNullOrBlank() -> repo.importStructuredText(pendingImport.title ?: "", pendingImport.text, null)
                        else -> null
                    }
                }.onFailure { err ->
                    val msg = if (err is SecurityException) {
                        "This PDF is password-protected or locked."
                    } else {
                        "Could not import shared file."
                    }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }.getOrNull()
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
            payload.startsWith("notes:") -> {
                if (batchBusy) return false
                val ids = payload.substringAfter(':').split(',').toSet()
                batchBusy = true
                scope.launch {
                    try {
                        lib.notebooks.filter { it.id in ids && it.deletedAt == null }.forEach { repo.moveNotebook(it.id, target) }
                        selectedDocuments = emptySet()
                        selectionMode = false
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, "Could not move every document. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                    } finally { batchBusy = false }
                }
                true
            }
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

    BackHandler(enabled = folderId != null && !selectionMode) { folderId = lib.folders.firstOrNull { it.id == folderId }?.parentId }

    BackHandler(enabled = searching && !selectionMode) {
        searching = false
        query = ""
    }



    val entries: ImmutableList<Entry> = remember(lib, section, folderId, query, sort) {
        val q = query.trim().lowercase().removePrefix("#")
        val live = lib.notebooks.filter { n ->
            (if (section == Section.TRASH) n.deletedAt != null else n.deletedAt == null) &&
                (section != Section.FAVORITES || n.favorite) &&
                n.title.contains(q, ignoreCase = true)
        }
        fun sortNotes(l: List<NotebookMeta>) = when (sort) {
            SortBy.MODIFIED -> l.sortedByDescending { it.modified }
            SortBy.CREATED -> l.sortedByDescending { it.created }
            SortBy.NAME -> l.sortedBy { it.title.lowercase() }
        }
        fun folderEntry(f: Folder) = Entry.F(f, lib.notebooks.count { it.folderId == f.id && it.deletedAt == null } + lib.folders.count { it.parentId == f.id })
        when {
            section == Section.TRASH -> live.sortedByDescending { it.deletedAt }.map { Entry.N(it) }
            section == Section.FAVORITES -> sortNotes(live).map { Entry.N(it) }
            section == Section.RECENT -> live.sortedByDescending { it.opened }.take(30).map { Entry.N(it) }
            q.isNotEmpty() -> lib.folders.filter { it.name.contains(q, ignoreCase = true) || it.emoji.contains(q) }
                .map(::folderEntry) + sortNotes(live).map { Entry.N(it) }
            else -> lib.folders.filter { it.parentId == folderId }.let { fs ->
                if (sort == SortBy.NAME) fs.sortedBy { it.name.lowercase() } else fs.sortedByDescending { it.modified }
            }.map(::folderEntry) + sortNotes(live.filter { it.folderId == folderId }).map { Entry.N(it) }
        }.toImmutableList()
    }

    val appStrings = com.pronotes.app.data.LocalAppStrings.current
    val currentFolder = lib.folders.firstOrNull { it.id == folderId }
    val title = if (section == Section.DOCUMENTS && currentFolder != null) currentFolder.name else section.localizedLabel(appStrings)

    val drag = remember { DragController() }
    val trashCount = lib.notebooks.count { it.deletedAt != null }
    LibraryWorkspace(drag) {
        val workspaceLayout = this
        GlowingWorkspace(paused = showGuide || compactSidebar) {
            val wide = maxWidth >= 840.dp
            val sidebar = wide && wideSidebar
            val closeSidebar = {
                if (wide) wideSidebar = false else compactSidebar = false
            }
            val sidebarContent: @Composable () -> Unit = {
                Sidebar(
                    section = section,
                    trashCount = trashCount,
                    onSelect = {
                        section = it
                        folderId = null
                        if (!wide) compactSidebar = false
                    },
                    onCollapse = closeSidebar,
                    onSettings = { compactSidebar = false; onSettings() },
                    onSupport = { compactSidebar = false; onSupport() },
                    onOpenCollab = { compactSidebar = false; collabOpen = true },
                    onOpenBookReader = { compactSidebar = false; onOpenBookReader() },
                    onDropTo = { s, payload ->
                        when (s) {
                            Section.DOCUMENTS -> dropInto(payload, null)
                            Section.TRASH -> if (payload.startsWith("notes:")) {
                                confirmBulkTrash = payload.substringAfter(':').split(',').toSet()
                                true
                            } else if (payload.startsWith("note:")) {
                                val noteId = payload.substringAfter(':')
                                val note = lib.notebooks.firstOrNull { it.id == noteId && it.deletedAt == null }
                                if (note != null) deleting = Entry.N(note)
                                note != null
                            } else false
                            Section.FAVORITES -> if (payload.startsWith("notes:")) {
                                val ids = payload.substringAfter(':').split(',').toSet()
                                scope.launch { lib.notebooks.filter { it.id in ids && !it.favorite }.forEach { repo.toggleFavorite(it.id) } }
                                true
                            } else if (payload.startsWith("note:")) {
                                val nid = payload.substringAfter(':')
                                if (lib.notebooks.firstOrNull { it.id == nid }?.favorite == false) scope.launch { repo.toggleFavorite(nid) }
                                true
                            } else false
                            else -> false
                        }
                    },
                )
            }
            LaunchedEffect(wide) {
                if (wide) compactSidebar = false
            }
            if (!wide && compactSidebar) {
                Dialog(onDismissRequest = closeSidebar) { sidebarContent() }
            }
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    AnimatedVisibility(
                        visible = sidebar,
                        enter = fadeIn(if (reduceMotion) snap() else tween(150)),
                        exit = fadeOut(if (reduceMotion) snap() else tween(120)),
                        content = { sidebarContent() },
                    )
                }

                val boundsMotion = if (reduceMotion) Modifier else Modifier.animateBounds(
                    lookaheadScope = workspaceLayout,
                    boundsTransform = BoundsTransform { _, _ -> tween(190) },
                )
                Column(Modifier.weight(1f).fillMaxHeight().then(boundsMotion)) {
                    // Browser-style dark/paper top tab strip aligned with home.svg
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!sidebar) {
                            AppPill(
                                onClick = { if (wide) wideSidebar = true else compactSidebar = true },
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                ProIconView(ProIcon.SIDEBAR, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                        }

                        val tabs by store.tabs.collectAsStateWithLifecycle()
                        val open = tabs.mapNotNull { id -> lib.notebooks.firstOrNull { it.id == id && it.deletedAt == null } }
                        Row(
                            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Section.entries.forEach { destination ->
                                TextButton(onClick = { section = destination; folderId = null }) {
                                    Text(
                                        destination.localizedLabel(appStrings),
                                        color = if (section == destination) Color(0xFF93C5FD) else Color(0xFFB9BDC7),
                                        fontWeight = if (section == destination) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                    )
                                }
                            }
                            open.forEach { n ->
                                AppSurface(
                                    role = SurfaceRole.Control,
                                    containerColor = MaterialTheme.colorScheme.surface,
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
                                Icon(Icons.AutoMirrored.Outlined.MenuBook, appStrings.readerMode, Modifier.size(17.dp))
                                if (wide) {
                                    Spacer(Modifier.width(5.dp))
                                    Text(appStrings.readerMode, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            AppPill(
                                tint = ProButtonPalette.SkyCyan,
                                onClick = { collabOpen = true },
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                Icon(Icons.Rounded.Groups, "Live Study Room", Modifier.size(19.dp))
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
                        val perfMode = com.pronotes.app.ui.LocalPerformanceMode.current
                        val motionStyle = com.pronotes.app.ui.LocalMotionStyle.current
                        AnimatedContent(
                            targetState = title,
                            transitionSpec = { com.pronotes.app.ui.ProMotion.contentSwap(reduceMotion, perfMode, motionStyle, 0.03f) },
                            label = "title",
                            modifier = Modifier.weight(1f),
                        ) { t ->
                            com.pronotes.app.ui.GradientTitle(t, MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                        }

                        if (section == Section.TRASH) {
                            AppPill(
                                selected = true,
                                tint = ProButtonPalette.CoralRose,
                                onClick = { if (entries.isNotEmpty()) emptyTrash = true },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(appStrings.emptyTrash, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            if (section == Section.DOCUMENTS) {
                                AppPill(
                                    selected = false,
                                    tint = ProButtonPalette.Violet,
                                    onClick = { newFolder = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Icon(Icons.Rounded.CreateNewFolder, appStrings.folder, Modifier.size(17.dp))
                                    if (wide) {
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            if (folderId != null) appStrings.subfolder else appStrings.folder,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            val guideLabel = stringResource(R.string.library_guide_title)
                            val createItems = remember(appStrings, folderId, newBookPrefs, guideLabel) {
                                listOf(
                                    AppMenuItem(appStrings.notebook, icon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(19.dp)) }) { newNotebook = true },
                                    AppMenuItem(appStrings.infiniteWhiteboard, icon = { Icon(Icons.Rounded.Gesture, null, Modifier.size(19.dp)) }) {
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
                                    AppMenuItem(appStrings.conceptMapCanvas, icon = { Icon(Icons.Rounded.Hub, null, Modifier.size(19.dp)) }) {
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
                                    AppMenuItem(appStrings.quickNote, icon = { Icon(Icons.Rounded.Bolt, null, Modifier.size(19.dp)) }) {
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
                                                    appStrings.quickNote, folderId, CoverColors.random(), defaultPaper,
                                                    width = w, height = h, isWhiteboard = defaultPreset.isWhiteboard,
                                                ),
                                            )
                                        }
                                    },
                                    AppMenuItem("Notebook templates", icon = { Icon(Icons.Rounded.DashboardCustomize, null, Modifier.size(19.dp)) }) { destination = "Templates" },
                                    AppMenuItem("Online books", icon = { Icon(Icons.Rounded.LocalLibrary, null, Modifier.size(19.dp)) }) { destination = "Books" },
                                    AppMenuItem("Community marketplace", icon = { Icon(Icons.Rounded.Storefront, null, Modifier.size(19.dp)) }) { destination = "Marketplace" },
                                    AppMenuItem("PDF Station", icon = { Icon(Icons.Rounded.PictureAsPdf, null, Modifier.size(19.dp)) }) { destination = "PDF Station" },
                                    AppMenuItem(guideLabel, icon = { Icon(Icons.Rounded.Explore, null, Modifier.size(19.dp)) }) { showGuide = true },
                                    AppMenuItem(appStrings.folder, icon = { Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(19.dp)) }) { newFolder = true },
                                    AppMenuItem(
                                        if (newBookPrefs.storageFolderLabel.isBlank()) "Choose Storage Folder…"
                                        else "Storage: ${newBookPrefs.storageFolderLabel}",
                                        icon = { Icon(Icons.Rounded.FolderSpecial, null, Modifier.size(19.dp)) },
                                    ) { pickStorageFolder.launch(null) },
                                    AppMenuItem(appStrings.importFiles, icon = { Icon(Icons.Rounded.UploadFile, null, Modifier.size(19.dp)) }) {
                                        importFilesLauncher.launch(arrayOf("*/*"))
                                    },
                                    AppMenuItem("Paste notes from another app…", icon = { Icon(Icons.Rounded.ContentPasteGo, null, Modifier.size(19.dp)) }) { pasteImportOpen = true },
                                )
                            }
                            LibraryCreateMenu(
                                label = appStrings.newAction,
                                items = createItems,
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                        AppMenuButton(
                            contentPadding = PaddingValues(0.dp),
                            items = SortBy.entries.map { s ->
                                AppMenuItem(label = s.localizedLabel(appStrings), selected = sort == s) { sort = s }
                            },
                            contentDescription = "Sort notebooks",
                            role = SurfaceRole.Control,
                            shape = appPillShape(),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(sort.localizedLabel(appStrings), style = MaterialTheme.typography.labelSmall, maxLines = 1)
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
                                Icon(Icons.Rounded.GridView, "Grid", Modifier.size(18.dp))
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

                    // Nested Folder Breadcrumb Trail (Documents > Folder > Subfolder)
                    if (section == Section.DOCUMENTS && folderId != null) {
                        val trail = remember(lib.folders, folderId) {
                            val list = ArrayList<Folder>()
                            var cur = folderId
                            var guard = 0
                            while (cur != null && guard < 16) {
                                val f = lib.folders.firstOrNull { it.id == cur } ?: break
                                list.add(0, f)
                                cur = f.parentId
                                guard++
                            }
                            list
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AppChip(
                                selected = false,
                                onClick = { folderId = null },
                                label = "Documents",
                                tint = ProButtonPalette.ElectricBlue,
                            )
                            trail.forEachIndexed { idx, f ->
                                Text("›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                AppChip(
                                    selected = idx == trail.lastIndex,
                                    onClick = { folderId = f.id },
                                    label = "${f.emoji.ifBlank { "📁" }} ${f.name}",
                                    tint = Color(f.color),
                                )
                            }
                        }
                    }

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
                                selected = newBookPrefs.storageFolderLabel.isNotBlank(),
                                onClick = { pickStorageFolder.launch(null) },
                                label = if (newBookPrefs.storageFolderLabel.isBlank()) "Save Folder…" else "💾 ${newBookPrefs.storageFolderLabel}",
                                tint = Color(0xFF7C3AED),
                            )

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
                                placeholder = {
                                    com.pronotes.app.ui.AnimatedGradientSearchPlaceholder(appStrings.searchPrompt)
                                },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                trailingIcon = { IconButton(onClick = { query = ""; searching = false; searchFocusManager.clearFocus(); searchKeyboard?.hide() }) { Icon(Icons.Filled.Close, "Cancel search") } },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    val actions = TileActions(
                        onOpenFolder = { f -> section = Section.DOCUMENTS; query = ""; searching = false; folderId = f.id },
                        onOpenNote = { if (selectionMode) toggleDocument(it.id) else onOpen(it.id) },
                        selectionMode = selectionMode,
                        selectedIds = selectedDocuments,
                        onToggleSelect = { toggleDocument(it) },
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

                    if (section != Section.TRASH) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(enabled = !batchBusy, onClick = { selectionMode = !selectionMode; selectedDocuments = emptySet() }) {
                                Text(if (selectionMode) "Done (${selectedDocuments.size})" else "Select")
                            }
                            if (selectionMode) {
                                TextButton(enabled = !batchBusy, onClick = { selectedDocuments = entries.filterIsInstance<Entry.N>().map { it.note.id }.toSet() }) { Text("Select all") }
                                TextButton(enabled = !batchBusy, onClick = { selectedDocuments = emptySet() }) { Text("Clear") }
                                TextButton(enabled = selectedDocuments.isNotEmpty() && !batchBusy, onClick = {
                                    val ids = selectedDocuments
                                    batchBusy = true
                                    scope.launch {
                                        try {
                                            lib.notebooks.filter { it.id in ids && !it.favorite }.forEach { repo.toggleFavorite(it.id) }
                                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            android.widget.Toast.makeText(context, "Could not update every favorite. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                                        } finally { batchBusy = false }
                                    }
                                }) { Text("Favorite") }
                                TextButton(enabled = selectedDocuments.isNotEmpty() && !batchBusy, onClick = { bulkMoveOpen = true }) { Text("Move") }
                                TextButton(enabled = selectedDocuments.isNotEmpty() && !batchBusy, onClick = { confirmBulkTrash = selectedDocuments.toSet() }) { Text("Trash") }
                            }
                        }
                    }
                    val listBottomPad = 24.dp
                    val listScrollState = rememberLazyListState()
                    val gridScrollState = rememberLazyGridState()
                    val libraryScrolled = if (listView) {
                        listScrollState.firstVisibleItemIndex > 0 || listScrollState.firstVisibleItemScrollOffset > 8
                    } else {
                        gridScrollState.firstVisibleItemIndex > 0 || gridScrollState.firstVisibleItemScrollOffset > 8
                    }

                    val dropFolderId = folderId
                    PdfDropZone(
                        enabled = section == Section.DOCUMENTS && !showGuide && !compactSidebar,
                        busy = pdfImportProgress != null,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onImport = { uris ->
                            val destinationFolder = dropFolderId
                            var failures = 0
                            try {
                                for (uri in uris) {
                                    try {
                                        repo.importPdf(uri, destinationFolder) { title, page, total ->
                                            pdfImportProgress = Triple(title, page, total)
                                        }
                                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        failures++
                                    }
                                }
                                if (failures > 0) android.widget.Toast.makeText(
                                    context, "Could not import $failures PDF file(s). Check that they are readable and unlocked.",
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                            } finally {
                                pdfImportProgress = null
                            }
                        },
                    ) {
                        if (entries.isEmpty()) {
                            if (query.isNotBlank()) {
                                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No matches", style = MaterialTheme.typography.titleLarge)
                                    TextButton(onClick = { query = "" }) { Text("Clear filters") }
                                }
                            } else {
                                EmptyState(section, inFolder = currentFolder != null, onNew = { newNotebook = true })
                            }
                        } else {
                            val gridPerfMode = com.pronotes.app.ui.LocalPerformanceMode.current
                            val gridMotionStyle = com.pronotes.app.ui.LocalMotionStyle.current
                            AnimatedContent(
                                targetState = Triple(section, folderId, listView),
                                transitionSpec = {
                                    com.pronotes.app.ui.ProMotion.contentSwap(reduceMotion, gridPerfMode, gridMotionStyle, 0.05f)
                                },
                                label = "grid",
                            ) { (_, _, asList) ->
                                if (asList) {
                                    LazyColumn(
                                        state = listScrollState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = listBottomPad),
                                    ) {
                                        items(
                                            entries,
                                            key = { keyOf(it) },
                                            contentType = { if (it is Entry.F) "folder_row" else "note_row" },
                                        ) { e ->
                                            Box(if (reduceMotion) Modifier else Modifier.animateItem()) { ListRow(e, repo, actions) }
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        state = gridScrollState,
                                        columns = GridCells.Adaptive(154.dp),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = listBottomPad),
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(24.dp),
                                    ) {
                                        items(
                                            entries,
                                            key = { keyOf(it) },
                                            contentType = { if (it is Entry.F) "folder_tile" else "note_tile" },
                                        ) { e ->
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
                        com.pronotes.app.ui.TopScrollCloudyBackdrop(
                            scrolled = libraryScrolled,
                            enabled = newBookPrefs.topScrollBlur,
                            height = 54.dp,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                }
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
                            ProIconView(ProIcon.HEART, ProButtonPalette.MagentaPink, Modifier.size(26.dp))
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
        b.languageTag?.takeIf { it.isNotBlank() && it != newBookPrefs.handwritingLanguage }?.let { lang ->
            store.update { it.copy(toolDefaults = it.toolDefaults.copy(handwritingLanguage = lang)) }
        }
        scope.launch {
            val usePhotoPage = b.photo != null && b.coverPage
            val id = repo.createNotebook(
                b.name, folderId, b.coverColor, b.paper, b.cover,
                width = b.width, height = b.height, paperColor = b.paperColor, coverPage = b.coverPage,
                isWhiteboard = b.isWhiteboard,
            )
            if (b.photo != null) {
                runCatching { repo.saveCoverImage(id, b.photo) }
                    .onSuccess { spec ->
                        if (!usePhotoPage || !repo.setCoverPage(id, spec)) repo.setCover(id, spec)
                    }
                    .onFailure {
                        android.widget.Toast.makeText(context, "Could not read cover photo. Using default cover.", android.widget.Toast.LENGTH_SHORT).show()
                    }
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
                val s = if (photo != null) {
                    runCatching { repo.saveCoverImage(n.id, photo) }
                        .onFailure {
                            android.widget.Toast.makeText(context, "Could not read cover photo.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .getOrNull() ?: return@launch
                } else spec
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

    if (bulkMoveOpen) {
        AppDialog(
            onDismissRequest = { bulkMoveOpen = false },
            title = { Text("Move "+selectedDocuments.size+" documents") },
            confirmButton = { TextButton(onClick = { bulkMoveOpen = false }) { Text("Cancel") } },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item { MoveRow("Documents", null) { bulkMoveOpen = false; dropInto("notes:"+selectedDocuments.joinToString(","), null) } }
                    items(lib.folders, key = { it.id }) { folder ->
                        MoveRow(folder.name, folder) { bulkMoveOpen = false; dropInto("notes:"+selectedDocuments.joinToString(","), folder.id) }
                    }
                }
            },
        )
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

// PdfImportLoaderOverlay & PdfSkeletonPreviewBox extracted to LibraryDialogs.kt

private fun keyOf(e: Entry) = if (e is Entry.F) "f" + e.folder.id else "n" + (e as Entry.N).note.id

private class TileActions(
    val onOpenFolder: (Folder) -> Unit,
    val onOpenNote: (NotebookMeta) -> Unit,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val onToggleSelect: (String) -> Unit = {},
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
    onOpenBookReader: () -> Unit,
    onDropTo: (Section, String) -> Boolean,
) {
    val context = LocalContext.current
    val strings = com.pronotes.app.data.LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val cloudUser by CloudSync.user.collectAsStateWithLifecycle()
    val syncing by CloudSync.syncing.collectAsStateWithLifecycle()
    val syncStatus by CloudSync.status.collectAsStateWithLifecycle()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    val sidebarBg = MaterialTheme.colorScheme.surface
    val activePillBg = MaterialTheme.colorScheme.primaryContainer
    val activeIconTint = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        Modifier
            .fillMaxHeight()
            .width(244.dp)
            .background(sidebarBg),
    ) {
        Column(
            Modifier.fillMaxHeight().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Rounded.ChevronLeft, "Hide sidebar")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 20.dp)) {
                androidx.compose.foundation.Image(
                    painterResource(R.drawable.ic_launcher_foreground),
                    null,
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                )
                Spacer(Modifier.width(10.dp))
                com.pronotes.app.ui.GradientTitle(
                    "Pro Notes",
                    MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                )
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
                            Section.DOCUMENTS -> Icons.Rounded.Folder
                            Section.FAVORITES -> Icons.Rounded.StarBorder
                            Section.RECENT -> Icons.Rounded.AccessTime
                            Section.TRASH -> Icons.Rounded.Delete
                        },
                        null,
                        tint = if (sel) activeIconTint else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        s.localizedLabel(strings),
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
                    Icon(Icons.Rounded.CloudSync, null, Modifier.size(16.dp), tint = ProButtonPalette.SkyCyan)
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
                        Text(strings.signInGoogle, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    AppPill(
                        selected = true,
                        tint = ProButtonPalette.Emerald,
                        onClick = { if (!syncing) scope.launch { CloudSync.syncAll(context) } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (syncing) strings.syncing else strings.syncNow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                Text(strings.readerMode, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenCollab)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Groups, null, tint = ProButtonPalette.SkyCyan)
                Spacer(Modifier.width(12.dp))
                Text(strings.studyFriendsAndFiles, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            if (Support.enabled) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onSupport)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProIconView(ProIcon.HEART, ProButtonPalette.MagentaPink, Modifier.size(20.dp))
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
                Icon(Icons.Rounded.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(strings.settings)
            }
        }
    }
}

@Composable
private fun EmptyState(section: Section, inFolder: Boolean, onNew: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            when (section) {
                Section.FAVORITES -> "No favorites yet"
                Section.RECENT -> "Nothing opened yet"
                Section.TRASH -> "Trash is empty"
                else -> if (inFolder) "This folder is empty" else "Your library is empty"
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        if (section == Section.FAVORITES || section == Section.RECENT) {
            Text(
                if (section == Section.FAVORITES) "Star a notebook to find it here." else "Open a notebook to find it here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (section == Section.TRASH) {
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
    AppMenuItem("Colour & label", icon = { Icon(Icons.Rounded.Palette, null, Modifier.size(18.dp)) }) { a.onEditFolder(e.folder) },
    AppMenuItem("Rename", icon = { Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)) }) { a.onRename(e) },
    AppMenuItem("Move", icon = { Icon(Icons.Rounded.DriveFileMove, null, Modifier.size(18.dp)) }) { a.onMove(e) },
    AppMenuItem("Delete", destructive = true, icon = { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)) }) { a.onDelete(e) },
)

private fun noteMenuItems(note: NotebookMeta, a: TileActions): List<AppMenuItem> =
    if (note.deletedAt != null) {
        listOf(
            AppMenuItem("Restore", icon = { Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp)) }) { a.onRestore(note) },
            AppMenuItem("Delete forever", destructive = true, icon = { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)) }) { a.onDeleteForever(note) },
        )
    } else {
        listOf(
            AppMenuItem("Duplicate", icon = { Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)) }) { a.onDuplicate(note) },
            AppMenuItem(
                if (note.favorite) "Remove from favorites" else "Add to favorites",
                icon = { Icon(if (note.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, null, Modifier.size(18.dp)) },
            ) { a.onFavorite(note) },
            AppMenuItem("Change cover", icon = { Icon(Icons.Rounded.Palette, null, Modifier.size(18.dp)) }) { a.onCover(note) },
            AppMenuItem("Rename", icon = { Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)) }) { a.onRename(Entry.N(note)) },
            AppMenuItem("Move", icon = { Icon(Icons.Rounded.DriveFileMove, null, Modifier.size(18.dp)) }) { a.onMove(Entry.N(note)) },
            AppMenuItem("Delete", destructive = true, icon = { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)) }) { a.onDelete(Entry.N(note)) },
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
                .then(if (note.deletedAt == null) Modifier.libraryDragSource(if (note.id in a.selectedIds) "notes:"+a.selectedIds.sorted().joinToString(",") else "note:"+note.id, if (note.id in a.selectedIds) "${a.selectedIds.size} documents" else note.title) else Modifier)
                .combinedClickable(
                    interactionSource = src,
                    indication = null,
                    onClick = { if (note.deletedAt != null) trashPrompt = true else a.onOpenNote(note) },
                    onLongClick = if (note.deletedAt != null) ({ trashPrompt = true }) else null,
                ),
        ) {
            CoverArt(note.coverSpec, note.title, repo.noteDir(note.id), note.modified, Modifier.fillMaxSize())
            if (note.id in a.selectedIds) Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).border(2.dp, MaterialTheme.colorScheme.primary, appCardShape()))
            if (a.selectionMode && note.deletedAt == null) Checkbox(
                checked = note.id in a.selectedIds, onCheckedChange = { a.onToggleSelect(note.id) },
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), CircleShape),
            )
            if (note.favorite) {
                Icon(Icons.Filled.Star, null, tint = Color(0xFFF2C94C), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp))
            }
        }
        Caption(
            note.title,
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(note.modified)),
            menuItems = remember(note, a) { noteMenuItems(note, a) },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListRow(e: Entry, repo: Repository, a: TileActions) {
    var trashPrompt by remember { mutableStateOf(false) }
    var dropHover by remember { mutableStateOf(false) }
    val items = remember(e, a) { if (e is Entry.N) noteMenuItems(e.note, a) else folderMenuItems(e as Entry.F, a) }
    AppCard(
        hero = false,
        shape = appCardShape(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .then(when (e) {
                is Entry.F -> Modifier.libraryDragSource("folder:"+e.folder.id, e.folder.name)
                    .libraryDropTarget(onHover = { dropHover = it }) { payload -> a.onDrop(payload, e.folder.id) }
                is Entry.N -> if (e.note.deletedAt == null) Modifier.libraryDragSource(
                    if (e.note.id in a.selectedIds) "notes:"+a.selectedIds.sorted().joinToString(",") else "note:"+e.note.id,
                    if (e.note.id in a.selectedIds) a.selectedIds.size.toString()+" documents" else e.note.title,
                ) else Modifier
            })
            .then(if (dropHover || (e is Entry.N && e.note.id in a.selectedIds)) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, appCardShape()) else Modifier),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (a.selectionMode && e is Entry.N && e.note.deletedAt == null) Checkbox(
                checked = e.note.id in a.selectedIds,
                onCheckedChange = { a.onToggleSelect(e.note.id) },
            )
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
