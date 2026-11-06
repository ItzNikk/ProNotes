package com.pronotes.app.editor

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pronotes.app.data.Flashcard

private val Pill = CircleShape

/** Create or edit one card: the cut-out picture (if any) on the front, text on either side. */
@Composable
fun CardEditorDialog(vm: EditorViewModel, draft: EditorViewModel.CardDraft) {
    var front by remember(draft) { mutableStateOf(draft.card.front) }
    var back by remember(draft) { mutableStateOf(draft.card.back) }
    val picture = remember(draft) { vm.cardImage(draft.card.frontImage) }
    AlertDialog(
        onDismissRequest = { vm.cardDraft = null },
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (draft.isNew) "New flashcard" else "Edit flashcard") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("FRONT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                picture?.let {
                    Image(
                        it.asImageBitmap(), null, contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    )
                }
                OutlinedTextField(
                    front, { front = it }, minLines = if (picture == null) 2 else 1, shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(if (picture == null) "Question" else "Question (optional)") }, modifier = Modifier.fillMaxWidth(),
                )
                Text("BACK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(back, { back = it }, minLines = 3, shape = RoundedCornerShape(16.dp), placeholder = { Text("Answer") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.saveCard(draft.card.copy(front = front.trim(), back = back.trim())) },
                enabled = front.isNotBlank() || picture != null, shape = Pill,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { vm.cardDraft = null }) { Text("Cancel") } },
    )
}

/** All cards of this notebook: study the due ones, browse, edit, export. */
@Composable
fun FlashcardsDialog(vm: EditorViewModel, onClose: () -> Unit) {
    var studying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(24.dp).widthIn(max = 720.dp).fillMaxWidth().heightIn(max = 760.dp).statusBarsPadding(),
        ) {
            if (studying) StudyView(vm, onDone = { studying = false })
            else Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Flashcards", style = MaterialTheme.typography.headlineSmall)
                        val due = vm.dueCards().size
                        Text("${vm.cards.size} cards · $due due", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { studying = true }, enabled = vm.dueCards().isNotEmpty(), shape = Pill) { Text("Study due cards") }
                    FilledTonalButton(onClick = { onClose(); vm.capturingCard = true }, shape = Pill) { Text("Cut from page") }
                    OutlinedButton(onClick = { vm.newTextCard() }, shape = Pill) { Text("Text card") }
                }
                Spacer(Modifier.height(12.dp))
                if (vm.cards.isEmpty()) {
                    Text(
                        "No cards yet. Tap \"Cut from page\" and drag a box over a PDF or your notes, or select ink and choose Flashcard.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vm.cards, key = { it.id }) { c -> CardRow(vm, c, onShow = { onClose(); vm.showCardSource(c) }) }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/csv")
                            .putExtra(Intent.EXTRA_SUBJECT, vm.title + " flashcards")
                            .putExtra(Intent.EXTRA_TEXT, vm.cardsCsv())
                        context.startActivity(Intent.createChooser(send, "Export flashcards"))
                    }) { Text("Export as CSV") }
                }
            }
        }
    }
}

@Composable
private fun CardRow(vm: EditorViewModel, c: Flashcard, onShow: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val pic = remember(c.frontImage) { vm.cardImage(c.frontImage) }
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                if (pic != null) Image(pic.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text("Aa", style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(c.front.ifBlank { "Picture card" }, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                Text(c.back.ifBlank { "No answer yet" }, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Box ${c.box + 1} of 6", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Box {
                TextButton(onClick = { menu = true }) { Text("•••") }
                androidx.compose.material3.DropdownMenu(menu, { menu = false }, shape = RoundedCornerShape(16.dp)) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; vm.cardDraft = EditorViewModel.CardDraft(c, false) })
                    if (c.pageId != null) androidx.compose.material3.DropdownMenuItem(text = { Text("Show in notebook") }, onClick = { menu = false; onShow() })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; vm.deleteCard(c.id) })
                }
            }
        }
    }
}

/** One card at a time: tap to flip, then grade it. */
@Composable
private fun StudyView(vm: EditorViewModel, onDone: () -> Unit) {
    val queue = remember { vm.dueCards().shuffled().map { it.id }.toMutableList() }
    var index by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var known by remember { mutableIntStateOf(0) }
    val total = queue.size
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (index < total) "Card ${index + 1} of $total" else "Done", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Back") }
        }
        Spacer(Modifier.height(12.dp))
        val card = queue.getOrNull(index)?.let { id -> vm.cards.firstOrNull { it.id == id } }
        if (card == null) {
            Text("Session done", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 40.dp))
            Text("$known of $total remembered", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 40.dp))
            Button(onClick = onDone, shape = Pill) { Text("Finish") }
            return@Column
        }
        val turn by animateFloatAsState(if (flipped) 180f else 0f, tween(420), label = "flip")
        val pic = remember(card.id) { vm.cardImage(card.frontImage) }
        Surface(
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 460.dp)
                .graphicsLayer { rotationY = turn; cameraDistance = 12f * density }
                .clickable { flipped = !flipped },
        ) {
            Box(Modifier.padding(20.dp).graphicsLayer { if (turn > 90f) rotationY = 180f }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (turn <= 90f) {
                        pic?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(12.dp))) }
                        if (card.front.isNotBlank()) Text(card.front, fontSize = 22.sp, textAlign = TextAlign.Center)
                        Text("Tap to flip", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(card.back.ifBlank { "No answer written" }, fontSize = 22.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (flipped) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fun grade(g: Int) {
                    vm.gradeCard(card.id, g)
                    if (g > 0) known++
                    flipped = false
                    index++
                }
                OutlinedButton(onClick = { grade(0) }, shape = Pill) { Text("Again") }
                FilledTonalButton(onClick = { grade(1) }, shape = Pill) { Text("Good") }
                Button(onClick = { grade(2) }, shape = Pill) { Text("Easy") }
            }
        } else {
            Text("Think of the answer, then flip.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "Add link" for selected content, like GoodNotes: pick a page of this notebook, or type a website. */
@Composable
fun LinkSheet(vm: EditorViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var url by remember { mutableStateOf("") }
    Dialog(onDismissRequest = { vm.linkSheet = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(24.dp).widthIn(max = 640.dp).fillMaxWidth().heightIn(max = 640.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Add link", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Tap the linked content with a finger to follow it. Long-press its blue badge to remove.",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.PrimaryTabRow(selectedTabIndex = tab) {
                    androidx.compose.material3.Tab(tab == 0, { tab = 0 }, text = { Text("Page") })
                    androidx.compose.material3.Tab(tab == 1, { tab = 1 }, text = { Text("Website") })
                }
                Spacer(Modifier.height(12.dp))
                if (tab == 0) {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        androidx.compose.foundation.lazy.grid.GridCells.Adaptive(96.dp),
                        Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(vm.pages.size) { i ->
                            val thumb by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(null, i) { value = vm.thumbnail(i, 200) }
                            val current = i == vm.pageIndex
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { vm.linkToPage(i) }) {
                                Box(
                                    Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(if (current) 2.dp else 1.dp, if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                                ) { thumb?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth()) } }
                                Text("Page ${i + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        url, { url = it }, singleLine = true, shape = Pill, placeholder = { Text("example.com") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.linkToUrl(url) }, enabled = url.trim().contains('.'), shape = Pill) { Text("Add link") }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { vm.linkSheet = null }, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}
