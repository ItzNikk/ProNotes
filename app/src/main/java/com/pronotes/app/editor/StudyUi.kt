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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
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
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Make cards for me:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.material3.AssistChip(onClick = { vm.generateCards(false) }, label = { Text("This page") }, shape = Pill)
                    androidx.compose.material3.AssistChip(onClick = { vm.generateCards(true) }, label = { Text("Whole notebook") }, shape = Pill)
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
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

/** Multiple-choice quiz: write your own questions or let the app make them from the notebook's text. */
@Composable
fun QuizDialog(vm: EditorViewModel, onClose: () -> Unit) {
    var taking by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<com.pronotes.app.data.QuizQuestion?>(null) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(24.dp).widthIn(max = 720.dp).fillMaxWidth().heightIn(max = 780.dp).statusBarsPadding(),
        ) {
            if (taking) TakeQuiz(vm, onDone = { taking = false })
            else Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Quiz", style = MaterialTheme.typography.headlineSmall)
                        Text("${vm.quiz.size} questions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { taking = true }, enabled = vm.quiz.isNotEmpty(), shape = Pill) { Text("Start quiz") }
                    OutlinedButton(onClick = {
                        editing = com.pronotes.app.data.QuizQuestion(java.util.UUID.randomUUID().toString(), "", listOf("", "", "", ""), 0)
                    }, shape = Pill) { Text("Add question") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Make questions:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.material3.AssistChip(onClick = { vm.generateQuiz(false) }, label = { Text("This page") }, shape = Pill)
                    androidx.compose.material3.AssistChip(onClick = { vm.generateQuiz(true) }, label = { Text("Whole notebook") }, shape = Pill)
                }
                if (vm.aiBusy) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                Spacer(Modifier.height(12.dp))
                if (vm.quiz.isEmpty()) {
                    Text(
                        "No questions yet. Add your own, or let Pro Notes write them from the PDF text and your notes.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vm.quiz, key = { it.id }) { q ->
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth().clickable { editing = q }) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(q.question, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                                    val stats = if (q.right + q.wrong == 0) "Not tried yet" else "${q.right} right · ${q.wrong} wrong"
                                    Text((if (q.generated) "Made for you · " else "") + stats, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { q -> QuestionEditor(q, onDismiss = { editing = null }, onDelete = { vm.deleteQuestion(q.id); editing = null }) { vm.saveQuestion(it); editing = null } }
}

@Composable
private fun QuestionEditor(q: com.pronotes.app.data.QuizQuestion, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (com.pronotes.app.data.QuizQuestion) -> Unit) {
    var question by remember { mutableStateOf(q.question) }
    val options = remember { (q.options + List(4) { "" }).take(4).toMutableList().let { androidx.compose.runtime.mutableStateListOf(*it.toTypedArray()) } }
    var answer by remember { mutableIntStateOf(q.answer.coerceIn(0, 3)) }
    var why by remember { mutableStateOf(q.explanation) }
    AlertDialog(
        onDismissRequest = onDismiss, shape = RoundedCornerShape(28.dp),
        title = { Text(if (q.question.isEmpty()) "New question" else "Edit question") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(question, { question = it }, minLines = 2, shape = RoundedCornerShape(16.dp), label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                Text("Tap the circle next to the right answer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                options.forEachIndexed { i, o ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(answer == i, { answer = i })
                        OutlinedTextField(o, { options[i] = it }, singleLine = true, shape = Pill, placeholder = { Text("Option ${'A' + i}") }, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(why, { why = it }, shape = RoundedCornerShape(16.dp), label = { Text("Why (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            val ok = question.isNotBlank() && options.count { it.isNotBlank() } >= 2 && options[answer].isNotBlank()
            Button(onClick = {
                val filled = options.mapIndexedNotNull { i, s -> if (s.isNotBlank()) i to s.trim() else null }
                onSave(q.copy(question = question.trim(), options = filled.map { it.second }, answer = filled.indexOfFirst { it.first == answer }.coerceAtLeast(0), explanation = why.trim()))
            }, enabled = ok, shape = Pill) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (q.question.isNotEmpty()) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun TakeQuiz(vm: EditorViewModel, onDone: () -> Unit) {
    val order = remember { vm.quiz.shuffled().map { it.id } }
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (index < order.size) "Question ${index + 1} of ${order.size}" else "Done", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("Score $score", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onDone) { Text("Back") }
        }
        androidx.compose.material3.LinearProgressIndicator(progress = { if (order.isEmpty()) 1f else index / order.size.toFloat() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        val q = order.getOrNull(index)?.let { id -> vm.quiz.firstOrNull { it.id == id } }
        if (q == null) {
            val pct = if (order.isEmpty()) 0 else score * 100 / order.size
            Text("$pct%", fontSize = 56.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 30.dp))
            Text("$score of ${order.size} right", modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp))
            Button(onClick = onDone, shape = Pill, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Finish") }
            return@Column
        }
        Text(q.question, fontSize = 20.sp, modifier = Modifier.padding(vertical = 12.dp))
        q.options.forEachIndexed { i, o ->
            val shown = picked != null
            val color = when {
                shown && i == q.answer -> Color(0xFF1BAF7A)
                shown && i == picked -> Color(0xFFE5484D)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Surface(
                shape = RoundedCornerShape(18.dp), color = if (shown && (i == q.answer || i == picked)) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.5.dp, color, RoundedCornerShape(18.dp)).clickable(enabled = !shown) {
                    picked = i
                    val right = i == q.answer
                    if (right) score++
                    vm.answerQuestion(q.id, right)
                },
            ) { Text("${'A' + i}.  $o", Modifier.padding(14.dp)) }
        }
        if (picked != null) {
            if (q.explanation.isNotBlank()) Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(14.dp))
            Button(onClick = { picked = null; index++ }, shape = Pill) { Text(if (index + 1 < order.size) "Next" else "See score") }
        }
    }
}

/**
 * GoodNotes-style template picker: templates grouped by kind with live previews, then line
 * spacing, line colour and paper colour, applied to this page, a new page, or all pages.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TemplateSheet(vm: EditorViewModel, onClose: () -> Unit) {
    val cur = vm.currentPage
    var style by remember { mutableStateOf(cur?.paper ?: com.pronotes.app.data.PaperStyle.RULED) }
    var scale by remember { mutableStateOf(cur?.lineScale ?: 1f) }
    var lineColor by remember { mutableStateOf(cur?.lineColor) }
    var paperColor by remember { mutableStateOf(cur?.paperColor) }
    val groups = com.pronotes.app.data.PaperStyle.entries.groupBy { it.group }
    var group by remember { mutableStateOf(style.group) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(20.dp).widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 820.dp).statusBarsPadding(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Templates", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text("Close") }
                }
                Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.keys.forEach { g -> androidx.compose.material3.FilterChip(group == g, { group = g }, label = { Text(g) }, shape = Pill) }
                }
                Spacer(Modifier.height(12.dp))
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    androidx.compose.foundation.lazy.grid.GridCells.Adaptive(104.dp),
                    Modifier.weight(1f, fill = false).heightIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(groups[group].orEmpty().size) { i ->
                        val p = groups[group].orEmpty()[i]
                        val on = p == style
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { style = p }) {
                            com.pronotes.app.library.PaperPreview(
                                p,
                                Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(10.dp))
                                    .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                                tint = paperColor ?: 0xFFFFFFFF.toInt(),
                            )
                            Text(p.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("LINE SPACING  ${"%.1f".format(scale)}×", style = MaterialTheme.typography.labelSmall)
                androidx.compose.material3.Slider(scale, { scale = (it * 10).toInt() / 10f }, valueRange = 0.6f..2f)
                Text("LINE COLOUR", style = MaterialTheme.typography.labelSmall)
                Row(Modifier.horizontalScrollCompat()) {
                    val lines = listOf<Int?>(null, 0xFFB8C7DC.toInt(), 0xFF9AA4B2.toInt(), 0xFF7FB2F0.toInt(), 0xFFF4A3A3.toInt(), 0xFF9ED8B0.toInt(), 0xFFE0C48A.toInt(), 0xFF4A505A.toInt())
                    lines.forEach { c -> ColorDot(c, c == lineColor) { lineColor = c } }
                }
                Text("PAPER", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.horizontalScrollCompat()) {
                    val papers = listOf<Int?>(null, 0xFFFFFFFF.toInt(), 0xFFFBF7EC.toInt(), 0xFFFFF4E0.toInt(), 0xFFEAF4FF.toInt(), 0xFFEFF8EC.toInt(), 0xFFFFFDE7.toInt(), 0xFF22252B.toInt())
                    papers.forEach { c -> ColorDot(c, c == paperColor) { paperColor = c } }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.applyTemplate(style, scale, lineColor, paperColor, 0); onClose() }, shape = Pill) { Text("This page") }
                    FilledTonalButton(onClick = { vm.applyTemplate(style, scale, lineColor, paperColor, 1); onClose() }, shape = Pill) { Text("New page") }
                    OutlinedButton(onClick = { vm.applyTemplate(style, scale, lineColor, paperColor, 2); onClose() }, shape = Pill) { Text("All pages") }
                }
            }
        }
    }
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier = this.then(Modifier.horizontalScroll(rememberScrollState()))

@Composable
private fun ColorDot(c: Int?, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.padding(4.dp).size(30.dp).clip(CircleShape)
            .background(if (c == null) MaterialTheme.colorScheme.surfaceVariant else Color(c))
            .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { if (c == null) Text("A", style = MaterialTheme.typography.labelSmall) }
}

/** Tape card extras: patterns and your own picture. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TapePatternPicker(vm: EditorViewModel, onPickImage: () -> Unit) {
    Text("PATTERN", style = MaterialTheme.typography.labelSmall)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TapePatterns.names.forEach { p ->
            val tile = remember(p, vm.tapeColor) { TapePatterns.tile(p, vm.tapeColor).asImageBitmap() }
            val on = vm.tapePattern == p
            Box(
                Modifier.size(width = 54.dp, height = 26.dp).clip(RoundedCornerShape(6.dp))
                    .border(if (on) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    .clickable { vm.tapePattern = p },
            ) { Image(tile, TapePatterns.label(p), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        androidx.compose.material3.AssistChip(onClick = onPickImage, label = { Text(if (vm.tapePattern.startsWith("image:")) "Picture ✓" else "Your picture…") }, shape = Pill)
    }
}
