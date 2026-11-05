package com.pronotes.app.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.pronotes.app.data.CoverColors
import com.pronotes.app.data.Folder
import com.pronotes.app.data.NotebookMeta
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.Repository
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class Section { DOCUMENTS, FAVORITES }

private sealed interface Entry {
    data class F(val folder: Folder, val count: Int) : Entry
    data class N(val note: NotebookMeta) : Entry
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpen: (String) -> Unit, pendingPdf: Uri?, onPdfConsumed: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val lib by repo.library.collectAsState()
    val scope = rememberCoroutineScope()

    var section by remember { mutableStateOf(Section.DOCUMENTS) }
    var folderId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var newNotebook by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Entry?>(null) }
    var moving by remember { mutableStateOf<Entry?>(null) }
    var deleting by remember { mutableStateOf<Entry?>(null) }

    val importPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { onOpen(repo.importPdf(uri, folderId)) }
    }
    if (pendingPdf != null) {
        androidx.compose.runtime.LaunchedEffect(pendingPdf) {
            val id = repo.importPdf(pendingPdf, null)
            onPdfConsumed()
            onOpen(id)
        }
    }

    BackHandler(enabled = folderId != null) {
        folderId = lib.folders.firstOrNull { it.id == folderId }?.parentId
    }

    val entries: List<Entry> = remember(lib, section, folderId, query) {
        val q = query.trim().lowercase()
        when {
            q.isNotEmpty() -> lib.notebooks.filter { it.title.lowercase().contains(q) }.map { Entry.N(it) } +
                lib.folders.filter { it.name.lowercase().contains(q) }.map { f -> Entry.F(f, lib.notebooks.count { it.folderId == f.id }) }
            section == Section.FAVORITES -> lib.notebooks.filter { it.favorite }.sortedByDescending { it.modified }.map { Entry.N(it) }
            else -> lib.folders.filter { it.parentId == folderId }.sortedBy { it.name.lowercase() }
                .map { f -> Entry.F(f, lib.notebooks.count { it.folderId == f.id } + lib.folders.count { it.parentId == f.id }) } +
                lib.notebooks.filter { it.folderId == folderId }.sortedByDescending { it.modified }.map { Entry.N(it) }
        }
    }

    val currentFolder = lib.folders.firstOrNull { it.id == folderId }
    val title = when {
        section == Section.FAVORITES -> "Favorites"
        currentFolder != null -> currentFolder.name
        else -> "Documents"
    }

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxHeight(),
            header = {
                Box(
                    Modifier.statusBarsPadding().padding(top = 12.dp, bottom = 20.dp).size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF5B8CFF), Color(0xFF2A4FD6)))),
                    contentAlignment = Alignment.Center,
                ) { Text("P", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
            },
        ) {
            NavigationRailItem(
                selected = section == Section.DOCUMENTS, onClick = { section = Section.DOCUMENTS; folderId = null },
                icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Documents") },
            )
            NavigationRailItem(
                selected = section == Section.FAVORITES, onClick = { section = Section.FAVORITES },
                icon = { Icon(Icons.Outlined.Star, null) }, label = { Text("Favorites") },
            )
        }

        Column(Modifier.weight(1f).fillMaxHeight().statusBarsPadding()) {
            // Header
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (folderId != null && section == Section.DOCUMENTS) {
                    IconButton(onClick = { folderId = currentFolder?.parentId }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Up") }
                }
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { (fadeIn() + slideInHorizontally { it / 6 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 6 }) },
                    label = "title", modifier = Modifier.weight(1f),
                ) { t -> Text(t, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Icon(Icons.Outlined.Search, "Search") }
                IconButton(onClick = { newFolder = true }) { Icon(Icons.Outlined.CreateNewFolder, "New folder") }
                IconButton(onClick = { importPdf.launch(arrayOf("application/pdf")) }) { Icon(Icons.Outlined.PictureAsPdf, "Import PDF") }
            }
            if (searching) {
                OutlinedTextField(
                    query, { query = it }, placeholder = { Text("Search notebooks and folders") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            AnimatedContent(
                targetState = Triple(section, folderId, query.isNotBlank()),
                transitionSpec = { (fadeIn() + slideInHorizontally { it / 10 }) togetherWith fadeOut() },
                label = "grid",
            ) { _ ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    if (section == Section.DOCUMENTS && query.isBlank()) {
                        item(key = "new") { NewTile { showNew = true } }
                    }
                    items(entries, key = { e -> if (e is Entry.F) "f" + e.folder.id else "n" + (e as Entry.N).note.id }) { e ->
                        Box(Modifier.animateItem()) {
                            when (e) {
                                is Entry.F -> FolderTile(e.folder, e.count,
                                    onClick = { section = Section.DOCUMENTS; query = ""; searching = false; folderId = e.folder.id },
                                    onRename = { renaming = e }, onMove = { moving = e }, onDelete = { deleting = e })
                                is Entry.N -> NotebookTile(e.note, repo.thumbFile(e.note.id).let { if (it.exists()) it else null },
                                    onClick = { onOpen(e.note.id) },
                                    onRename = { renaming = e }, onMove = { moving = e }, onDelete = { deleting = e },
                                    onFavorite = { scope.launch { repo.toggleFavorite(e.note.id) } })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNew) {
        ModalBottomSheet(onDismissRequest = { showNew = false }) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create", style = MaterialTheme.typography.headlineSmall)
                SheetRow(Icons.Outlined.Description, "Notebook") { showNew = false; newNotebook = true }
                SheetRow(Icons.Outlined.CreateNewFolder, "Folder") { showNew = false; newFolder = true }
                SheetRow(Icons.Outlined.PictureAsPdf, "Import PDF") { showNew = false; importPdf.launch(arrayOf("application/pdf")) }
            }
        }
    }

    if (newNotebook) NewNotebookDialog(onDismiss = { newNotebook = false }) { name, cover, paper ->
        newNotebook = false
        scope.launch { onOpen(repo.createNotebook(name, folderId, cover, paper)) }
    }

    if (newFolder) NameDialog("New folder", "Folder", onDismiss = { newFolder = false }) { name ->
        newFolder = false
        scope.launch { repo.createFolder(name, folderId, CoverColors.random()) }
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
                LazyColumn(Modifier.height(320.dp)) {
                    item {
                        MoveRow("Documents (top level)") {
                            moving = null
                            scope.launch { if (e is Entry.F) repo.moveFolder(e.folder.id, null) else repo.moveNotebook((e as Entry.N).note.id, null) }
                        }
                    }
                    items(lib.folders.filter { it.id != selfId }) { f ->
                        MoveRow(f.name) {
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
            text = { Text(if (e is Entry.F) "The folder and everything in it will be deleted." else "This notebook will be deleted.") },
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

@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    return s
}

@Composable
private fun NewTile(onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val outline = MaterialTheme.colorScheme.outline
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(pressScale(src))) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(14.dp))
                .clickable(interactionSource = src, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    outline, style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(36f),
                )
            }
            Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, "New", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("New", style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderTile(folder: Folder, count: Int, onClick: () -> Unit, onRename: () -> Unit, onMove: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(pressScale(src))) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(14.dp))
                .combinedClickable(interactionSource = src, indication = null, onClick = onClick, onLongClick = { menu = true }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Folder, null, tint = Color(folder.color), modifier = Modifier.fillMaxSize(0.9f))
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text("Move") }, leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) }, onClick = { menu = false; onMove() })
                DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; onDelete() })
            }
        }
        Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotebookTile(
    note: NotebookMeta, thumb: java.io.File?, onClick: () -> Unit, onRename: () -> Unit,
    onMove: () -> Unit, onDelete: () -> Unit, onFavorite: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(pressScale(src))) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.75f)
                .shadow(8.dp, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp))
                .background(Color.White)
                .combinedClickable(interactionSource = src, indication = null, onClick = onClick, onLongClick = { menu = true }),
        ) {
            if (thumb != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(thumb).memoryCacheKey(thumb.path + note.modified)
                        .diskCacheKey(thumb.path + note.modified).crossfade(true).build(),
                    contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
            }
            if (!note.fromPdf) {
                // Cover: coloured spine + label, so notebooks read as notebooks.
                Box(Modifier.fillMaxHeight().width(12.dp).background(Color(note.coverColor)))
            }
            if (note.favorite) {
                Icon(Icons.Filled.Star, null, tint = Color(0xFFF2C94C), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp))
            }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text(if (note.favorite) "Remove from favorites" else "Add to favorites") },
                    leadingIcon = { Icon(if (note.favorite) Icons.Outlined.StarBorder else Icons.Outlined.Star, null) },
                    onClick = { menu = false; onFavorite() })
                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text("Move") }, leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) }, onClick = { menu = false; onMove() })
                DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; onDelete() })
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(note.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(note.modified)) + " · ${note.pageCount} p",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MoveRow(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

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

@Composable
private fun NewNotebookDialog(onDismiss: () -> Unit, onCreate: (String, Int, PaperStyle) -> Unit) {
    var name by remember { mutableStateOf("Untitled notebook") }
    var cover by remember { mutableStateOf(CoverColors[0]) }
    var paper by remember { mutableStateOf(PaperStyle.RULED) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New notebook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Title") })
                Text("Cover", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoverColors.forEach { c ->
                        val sel = c == cover
                        val s by animateFloatAsState(if (sel) 1.15f else 1f, label = "c")
                        Box(
                            Modifier.size(28.dp).scale(s).clip(CircleShape).background(Color(c))
                                .border(if (sel) 3.dp else 0.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clickable { cover = c },
                        )
                    }
                }
                Text("Paper", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaperStyle.entries.forEach { p ->
                        val sel = p == paper
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Card(
                                modifier = Modifier.size(width = 54.dp, height = 72.dp)
                                    .border(if (sel) 2.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .clickable { paper = p },
                                shape = RoundedCornerShape(8.dp),
                            ) { PaperPreview(p) }
                            Text(p.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name.ifBlank { "Untitled notebook" }, cover, paper) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PaperPreview(p: PaperStyle) {
    val line = Color(0xFFD5DEEA)
    Canvas(Modifier.fillMaxSize().background(Color.White)) {
        when (p) {
            PaperStyle.BLANK -> Unit
            PaperStyle.RULED -> {
                var y = size.height * 0.2f
                while (y < size.height) { drawLine(line, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 2f); y += size.height / 9 }
            }
            PaperStyle.GRID -> {
                val step = size.width / 6
                var x = step
                while (x < size.width) { drawLine(line, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 2f); x += step }
                var y = step
                while (y < size.height) { drawLine(line, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 2f); y += step }
            }
            PaperStyle.DOTTED -> {
                val step = size.width / 6
                var y = step
                while (y < size.height) {
                    var x = step
                    while (x < size.width) { drawCircle(Color(0xFFC3CCD8), 2.5f, androidx.compose.ui.geometry.Offset(x, y)); x += step }
                    y += step
                }
            }
        }
    }
}
