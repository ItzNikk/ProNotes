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
                remember(draft) { vm.cardImage(draft.card.backImage) }?.let {
                    Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(14.dp)))
                }
                OutlinedTextField(back, { back = it }, minLines = 3, shape = RoundedCornerShape(16.dp), placeholder = { Text("Answer") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { vm.captureBack(draft.copy(card = draft.card.copy(front = front, back = back))) }) { Text("✂  Take the answer from the page") }
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
                val due = vm.dueCards().size
                val mastered = vm.cards.count { it.box >= 4 }
                StudyHero("Flashcards", "${vm.cards.size} cards · $due due · $mastered mastered", StudyColors.cards, "🃏") {
                    TextButton(onClick = onClose) { Text("Close", color = Color.White) }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { studying = true }, enabled = due > 0, shape = Pill,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(if (due > 0) "Study $due due cards" else "All caught up", fontSize = 16.sp) }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onClose(); vm.captureForQuiz = false; vm.capturingCard = true }, shape = Pill) { Text("✂  Cut from page") }
                    OutlinedButton(onClick = { vm.newTextCard() }, shape = Pill) { Text("Aa  Text card") }
                    OutlinedButton(onClick = { vm.generateCards(false) }, shape = Pill) { Text("✨  From this page") }
                    OutlinedButton(onClick = { vm.generateCards(true) }, shape = Pill) { Text("✨  Whole notebook") }
                }
                if (vm.aiBusy) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
                Text(
                    "Powered by ${LocalAiManager.activeEngineLabel(context)}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(12.dp))
                if (vm.cards.isEmpty()) {
                    Text(
                        "No cards yet. Tap \"Cut from page\" and drag a box over a PDF or your notes, or select ink and choose Flashcard.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vm.cards, key = { it.id }) { c ->
                            Box(Modifier.animateItem()) { CardRow(vm, c, onShow = { onClose(); vm.showCardSource(c) }) }
                        }
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
    var elapsed by remember { mutableStateOf(0L) }
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (index < total) "Card ${index + 1} of $total" else "Done", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            SessionClock(running = index < total) { elapsed = it }
            TextButton(onClick = onDone) { Text("Back") }
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { if (total == 0) 1f else index / total.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(6.dp).clip(Pill),
            color = StudyColors.cards.last(),
        )
        val card = queue.getOrNull(index)?.let { id -> vm.cards.firstOrNull { it.id == id } }
        if (card == null) {
            val pct = if (total == 0) 100 else known * 100 / total
            Spacer(Modifier.height(10.dp))
            ScoreRing(pct, StudyColors.cards)
            val (emoji, line) = feedbackFor(pct)
            Text("$emoji  Session done", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 14.dp))
            Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            Text("$known of $total remembered · ${clock(elapsed)}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
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
                        remember(card.id) { vm.cardImage(card.backImage) }?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(12.dp))) }
                        if (card.back.isNotBlank() || card.backImage == null) Text(card.back.ifBlank { "No answer written" }, fontSize = 22.sp, textAlign = TextAlign.Center)
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

/** Quiz: your own questions, questions made from the notes, and picture questions. Timed, scored, with feedback. */
@Composable
fun QuizDialog(vm: EditorViewModel, onClose: () -> Unit) {
    var taking by remember { mutableStateOf<List<String>?>(null) }
    var editing by remember { mutableStateOf<com.pronotes.app.data.QuizQuestion?>(null) }
    androidx.compose.runtime.LaunchedEffect(vm.quizDraft) { vm.quizDraft?.let { editing = it; vm.quizDraft = null } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(20.dp).widthIn(max = 720.dp).fillMaxWidth().heightIn(max = 800.dp).statusBarsPadding(),
        ) {
            val order = taking
            if (order != null) TakeQuiz(vm, order, onDone = { taking = null }, onRetry = { taking = it })
            else Column(Modifier.padding(18.dp)) {
                val best = vm.quizBest()
                StudyHero(
                    "Quiz", "${vm.quiz.size} questions" + (best?.let { " · best ${it.first}% in ${clock(it.second)}" } ?: ""),
                    StudyColors.quiz, "🎯",
                ) { TextButton(onClick = onClose) { Text("Close", color = Color.White) } }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { taking = vm.quiz.shuffled().map { it.id } }, enabled = vm.quiz.isNotEmpty(), shape = Pill,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(if (vm.quiz.isEmpty()) "Add questions to start" else "Start quiz · ${vm.quiz.size}", fontSize = 16.sp) }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        editing = com.pronotes.app.data.QuizQuestion(java.util.UUID.randomUUID().toString(), "", listOf("", "", "", ""), 0)
                    }, shape = Pill) { Text("✎  Write a question") }
                    FilledTonalButton(onClick = { onClose(); vm.captureForQuiz = true; vm.capturingCard = true }, shape = Pill) { Text("🖼  From a picture") }
                    OutlinedButton(onClick = { vm.generateQuiz(false) }, shape = Pill) { Text("✨  From this page") }
                    OutlinedButton(onClick = { vm.generateQuiz(true) }, shape = Pill) { Text("✨  Whole notebook") }
                }
                if (vm.aiBusy) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
                val quizCtx = LocalContext.current
                Text(
                    "Powered by ${LocalAiManager.activeEngineLabel(quizCtx)}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(10.dp))
                if (vm.quiz.isEmpty()) {
                    Text(
                        "Write your own questions, box a diagram to quiz on it, or let Pro Notes write questions from the PDF and your notes.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp),
                    )
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vm.quiz, key = { it.id }) { q ->
                            val pic = remember(q.image) { vm.cardImage(q.image) }
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), modifier = Modifier.animateItem().fillMaxWidth().clickable { editing = q }) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (pic != null) {
                                        Image(pic.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                                        Spacer(Modifier.size(12.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(q.question.ifBlank { "Picture question" }, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                                        val tries = q.right + q.wrong
                                        val stats = if (tries == 0) "Not tried yet" else "${q.right * 100 / tries}% right over $tries tries"
                                        Text((if (q.generated) "✨ " else "") + stats, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { q ->
        QuestionEditor(vm, q, onDismiss = { editing = null }, onDelete = { vm.deleteQuestion(q.id); editing = null }) { vm.saveQuestion(it); editing = null }
    }
}

@Composable
private fun QuestionEditor(
    vm: EditorViewModel, q: com.pronotes.app.data.QuizQuestion, onDismiss: () -> Unit, onDelete: () -> Unit,
    onSave: (com.pronotes.app.data.QuizQuestion) -> Unit,
) {
    var question by remember { mutableStateOf(q.question) }
    val options = remember { androidx.compose.runtime.mutableStateListOf(*(q.options.ifEmpty { listOf("", "") }).toTypedArray()) }
    var answer by remember { mutableIntStateOf(q.answer.coerceIn(0, (q.options.size - 1).coerceAtLeast(0))) }
    var why by remember { mutableStateOf(q.explanation) }
    val pic = remember(q.image) { vm.cardImage(q.image) }
    val exists = vm.quiz.any { it.id == q.id }
    AlertDialog(
        onDismissRequest = onDismiss, shape = RoundedCornerShape(28.dp),
        title = { Text(if (exists) "Edit question" else "New question") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pic?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(14.dp))) }
                OutlinedTextField(question, { question = it }, minLines = 2, shape = RoundedCornerShape(16.dp), label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                Text("Tap the circle next to the right answer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                options.forEachIndexed { i, o ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(answer == i, { answer = i })
                        OutlinedTextField(o, { options[i] = it }, singleLine = true, shape = Pill, placeholder = { Text("Option ${'A' + i}") }, modifier = Modifier.weight(1f))
                        if (options.size > 2) TextButton(onClick = { options.removeAt(i); if (answer >= options.size) answer = options.size - 1 }) { Text("✕") }
                    }
                }
                if (options.size < 6) TextButton(onClick = { options.add("") }) { Text("+ Add option") }
                OutlinedTextField(why, { why = it }, shape = RoundedCornerShape(16.dp), label = { Text("Why (shown after answering)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            val ok = (question.isNotBlank() || pic != null) && options.count { it.isNotBlank() } >= 2 && options.getOrNull(answer)?.isNotBlank() == true
            Button(onClick = {
                val filled = options.mapIndexedNotNull { i, s -> if (s.isNotBlank()) i to s.trim() else null }
                onSave(q.copy(question = question.trim(), options = filled.map { it.second }, answer = filled.indexOfFirst { it.first == answer }.coerceAtLeast(0), explanation = why.trim()))
            }, enabled = ok, shape = Pill) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (exists) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun TakeQuiz(vm: EditorViewModel, order: List<String>, onDone: () -> Unit, onRetry: (List<String>) -> Unit) {
    var index by remember(order) { mutableIntStateOf(0) }
    var picked by remember(order) { mutableStateOf<Int?>(null) }
    var score by remember(order) { mutableIntStateOf(0) }
    var streak by remember(order) { mutableIntStateOf(0) }
    var bestStreak by remember(order) { mutableIntStateOf(0) }
    val wrong = remember(order) { androidx.compose.runtime.mutableStateListOf<String>() }
    var elapsed by remember(order) { mutableStateOf(0L) }
    var newBest by remember(order) { mutableStateOf<Boolean?>(null) }
    val finished = index >= order.size
    Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (!finished) "Question ${index + 1} of ${order.size}" else "Results", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (streak >= 2 && !finished) Text("🔥 $streak", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 8.dp))
            SessionClock(running = !finished) { elapsed = it }
            TextButton(onClick = onDone) { Text("Back") }
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { if (order.isEmpty()) 1f else index / order.size.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(6.dp).clip(Pill), color = StudyColors.quiz.last(),
        )
        if (finished) {
            val pct = if (order.isEmpty()) 0 else score * 100 / order.size
            androidx.compose.runtime.LaunchedEffect(Unit) { newBest = vm.recordQuiz(pct, elapsed) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ScoreRing(pct, StudyColors.quiz) }
            val (emoji, line) = feedbackFor(pct)
            Text("$emoji  $line", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("$score/${order.size}", "right"); Stat(clock(elapsed), "time"); Stat("$bestStreak", "best streak")
            }
            if (newBest == true) Text("New best score for this notebook!", color = StudyColors.quiz.last(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            if (wrong.isNotEmpty()) {
                Text("REVIEW", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                wrong.forEach { id ->
                    val q = vm.quiz.firstOrNull { it.id == id } ?: return@forEach
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(q.question.ifBlank { "Picture question" }, style = MaterialTheme.typography.bodyMedium)
                            Text("✓ " + (q.options.getOrNull(q.answer) ?: ""), color = Color(0xFF1BAF7A), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                            if (q.explanation.isNotBlank()) Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDone, shape = Pill) { Text("Done") }
                if (wrong.isNotEmpty()) FilledTonalButton(onClick = { onRetry(wrong.toList().shuffled()) }, shape = Pill) { Text("Retry ${wrong.size} missed") }
                OutlinedButton(onClick = { onRetry(order.shuffled()) }, shape = Pill) { Text("Again") }
            }
            return@Column
        }
        val q = vm.quiz.firstOrNull { it.id == order[index] }
        if (q == null) { androidx.compose.runtime.LaunchedEffect(index) { index++ }; return@Column }
        val pic = remember(q.image) { vm.cardImage(q.image) }
        pic?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(16.dp))) }
        Text(q.question.ifBlank { "What does this picture show?" }, fontSize = 21.sp, modifier = Modifier.padding(vertical = 12.dp))
        q.options.forEachIndexed { i, o ->
            val shown = picked != null
            val color = when {
                shown && i == q.answer -> Color(0xFF1BAF7A)
                shown && i == picked -> Color(0xFFE5484D)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            val scale by animateFloatAsState(if (shown && i == picked) 1.03f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f), label = "opt")
            Surface(
                shape = RoundedCornerShape(18.dp), color = if (shown && (i == q.answer || i == picked)) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                    .border(1.5.dp, color, RoundedCornerShape(18.dp)).clickable(enabled = !shown) {
                        picked = i
                        val right = i == q.answer
                        if (right) { score++; streak++; bestStreak = maxOf(bestStreak, streak) } else { streak = 0; wrong += q.id }
                        vm.answerQuestion(q.id, right)
                    },
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(if (shown && (i == q.answer || i == picked)) color else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Text(if (shown && i == q.answer) "✓" else if (shown && i == picked) "✕" else "${'A' + i}", color = if (shown && (i == q.answer || i == picked)) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(o, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (picked != null) {
            val right = picked == q.answer
            Text(if (right) "Correct!" else "Not quite.", color = if (right) Color(0xFF1BAF7A) else Color(0xFFE5484D), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
            if (q.explanation.isNotBlank()) Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Button(onClick = { picked = null; index++ }, shape = Pill) { Text(if (index + 1 < order.size) "Next" else "See results") }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
