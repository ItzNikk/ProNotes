package com.pronotes.app.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pronotes.app.ui.AppDialog

@Composable
internal fun StickyBoard(vm: EditorViewModel, close: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var pinned by rememberSaveable { mutableStateOf(false) }
    val notes = vm.pages.flatMapIndexed { index, page ->
        page.items.filterIsInstance<StickyItem>().map { index to it }
    }.filter { (_, note) ->
        (!pinned || note.pinned) && (note.title.contains(query, true) || note.text.contains(query, true))
    }.sortedByDescending { it.second.pinned }
    AppDialog(onDismissRequest = close, title = { Text("Sticky board") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(query, { query = it }, label = { Text("Search this notebook’s stickies") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                FilterChip(selected = pinned, onClick = { pinned = !pinned }, label = { Text("Pinned only") })
            }
            if (notes.isEmpty()) item { Text("No matching sticky notes.") }
            items(notes, key = { it.second.id }) { (index, note) ->
                Surface(color = Color(note.color), contentColor = Color(0xff202124),
                    shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(note.title.ifBlank { "Sticky note" }, style = MaterialTheme.typography.titleMedium)
                        Text(note.text, maxLines = 4)
                        TextButton(onClick = {
                            close()
                            vm.goToPage(index)
                            if (!vm.readOnly) vm.openSticky(index, note.x, note.y, note)
                        }) { Text("Open on page ${index + 1}", color = Color(0xff233b63)) }
                    }
                }
            }
        }
    }, dismissButton = { TextButton(onClick = close) { Text("Close") } }, confirmButton = {
        Button(enabled = !vm.readOnly && vm.currentPage != null, onClick = {
            close()
            vm.openSticky(vm.pageIndex, (vm.currentPage?.width ?: 600f) / 2, 240f, null)
        }) { Text("New sticky") }
    })
}
