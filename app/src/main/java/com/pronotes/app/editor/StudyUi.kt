package com.pronotes.app.editor

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pronotes.app.data.Flashcard
import com.pronotes.app.data.PageSize
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppDialogSurface
import com.pronotes.app.ui.AppMenuButton
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSheet
import com.pronotes.app.ui.AppSliderRow
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.FanCardSpec
import com.pronotes.app.ui.FannedColorfulCardsDeck
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.appCardShape

private val Pill = CircleShape

@Composable
fun CardEditorDialog(vm: EditorViewModel, draft: EditorViewModel.CardDraft) {
    var front by remember(draft) { mutableStateOf(draft.card.front) }
    var back by remember(draft) { mutableStateOf(draft.card.back) }
    var hint by remember(draft) { mutableStateOf(draft.card.hint) }
    var starred by remember(draft) { mutableStateOf(draft.card.starred) }
    val picture = remember(draft) { vm.cardImage(draft.card.frontImage) }
    AppDialog(
        onDismissRequest = { vm.cardDraft = null },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (draft.isNew) "New flashcard" else "Edit flashcard", modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Star card",
                    tint = if (starred) ProButtonPalette.VibrantOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(Pill)
                        .clickable { starred = !starred }
                        .padding(6.dp),
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("FRONT (QUESTION / TERM)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                picture?.let {
                    Image(
                        it.asImageBitmap(),
                        null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                }
                OutlinedTextField(
                    front,
                    { front = it },
                    minLines = if (picture == null) 2 else 1,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(if (picture == null) "Question or key concept" else "Question (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("BACK (ANSWER / DEFINITION)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                remember(draft) { vm.cardImage(draft.card.backImage) }?.let {
                    Image(
                        it.asImageBitmap(),
                        null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                }
                OutlinedTextField(
                    back,
                    { back = it },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Answer or detailed explanation") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    hint,
                    { hint = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Memory hint (optional)") },
                    placeholder = { Text("Mnemonic or cue shown before flipping") },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { vm.captureBack(draft.copy(card = draft.card.copy(front = front, back = back, hint = hint, starred = starred))) },
                ) { Text("Cut the answer from the page") }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.saveCard(draft.card.copy(front = front.trim(), back = back.trim(), hint = hint.trim(), starred = starred)) },
                enabled = front.isNotBlank() || picture != null,
                shape = Pill,
                colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { vm.cardDraft = null }) { Text("Cancel") } },
    )
}

/** Flashcards Studio: 4-metric chips, Study/Manage tabs, search & filter, and 4-grade SM-2 StudyView. */
@Composable
fun FlashcardsDialog(vm: EditorViewModel, onClose: () -> Unit) {
    var studying by remember { mutableStateOf(false) }
    var studyAll by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppDialogSurface(
                shape = appCardShape(4f),
                modifier = Modifier
                    .widthIn(max = if (studying) 1000.dp else 760.dp)
                    .fillMaxWidth()
                    .heightIn(max = 800.dp)
                    .statusBarsPadding()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            ) {
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    if (studying) {
                        val deck = if (studyAll) vm.cards else vm.dueCards()
                        StudyView(vm, deck, onDone = { studying = false })
                    } else {
                        val due = vm.dueCards().size
                        val mastered = vm.cards.count { it.box >= 4 }
                        val learning = vm.cards.size - mastered
                        val visualCount = vm.cards.count { it.frontImage != null || it.backImage != null }
                        val flashcardDeckSpecs = remember(vm.cards.size, due, mastered, visualCount) {
                            listOf(
                                FanCardSpec(
                                    0,
                                    "Due Today",
                                    if (due > 0) "$due cards queued for recall" else "All caught up for today",
                                    Color(0xFF0044EA),
                                    Color(0xFF0032B5),
                                    Color(0xFF4BA6FF),
                                ),
                                FanCardSpec(
                                    1,
                                    "Visual Cutouts",
                                    "$visualCount diagram & capture cards",
                                    Color(0xFF00A3F5),
                                    Color(0xFF0077CC),
                                    Color(0xFF8BE2FF),
                                ),
                                FanCardSpec(
                                    2,
                                    "Practice All",
                                    "${vm.cards.size} cards in this notebook",
                                    Color(0xFFFF8C00),
                                    Color(0xFFD96400),
                                    Color(0xFFFFCF6E),
                                ),
                                FanCardSpec(
                                    3,
                                    "Mastered",
                                    "$mastered cards locked in memory",
                                    Color(0xFFF01E6B),
                                    Color(0xFFC20E4F),
                                    Color(0xFFFF86B4),
                                ),
                            )
                        }
                        Column(Modifier.padding(20.dp)) {
                            StudyHero(
                                title = "Flashcards Studio",
                                subtitle = "${vm.cards.size} cards · spaced recall deck",
                                colors = StudyColors.cards,
                                glyph = "FC",
                            ) {
                                TextButton(onClick = onClose) { Text("Close", color = Color.White) }
                            }
                            Spacer(Modifier.height(12.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatChip("Total", "${vm.cards.size}", ProButtonPalette.SkyCyan, Modifier.weight(1f))
                                StatChip("Due", "$due", ProButtonPalette.VibrantOrange, Modifier.weight(1f))
                                StatChip("Mastered", "$mastered", ProButtonPalette.Emerald, Modifier.weight(1f))
                                StatChip("Learning", "$learning", ProButtonPalette.Violet, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))

                            AppSegmentedControl(
                                options = listOf("Study Deck", "Manage Cards (${vm.cards.size})"),
                                selectedIndex = tab,
                                onSelect = { tab = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))

                            if (tab == 0) {
                                AppCard(
                                    hero = false,
                                    shape = appCardShape(),
                                    contentPadding = PaddingValues(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        FannedColorfulCardsDeck(
                                            cards = flashcardDeckSpecs,
                                            autoCycle = false,
                                            showLabels = true,
                                            compact = true,
                                            onFrontCardClick = { specId ->
                                                when (specId) {
                                                    0 -> if (due > 0) { studyAll = false; studying = true } else tab = 1
                                                    1 -> { onClose(); vm.capturingCard = true }
                                                    2 -> if (vm.cards.isNotEmpty()) { studyAll = true; studying = true } else vm.newTextCard()
                                                    else -> tab = 1
                                                }
                                            },
                                        )
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("DECK OVERVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Button(
                                                onClick = { studyAll = false; studying = true },
                                                enabled = due > 0,
                                                shape = Pill,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ProButtonPalette.ElectricBlue,
                                                    contentColor = Color.White,
                                                ),
                                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                            ) {
                                                Text(
                                                    if (due > 0) "Study $due due" else "Due caught up",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                )
                                            }
                                            if (vm.cards.isNotEmpty()) {
                                                AppPill(
                                                    selected = true,
                                                    tint = ProButtonPalette.VibrantOrange,
                                                    onClick = { studyAll = true; studying = true },
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    Text("Practice all (${vm.cards.size})", fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("CREATE & GENERATE CARDS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AppPill(
                                        selected = true,
                                        tint = ProButtonPalette.Emerald,
                                        onClick = { vm.autoGenerateFlashcards() },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    ) {
                                        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp), tint = Color.White)
                                        Spacer(Modifier.width(5.dp))
                                        Text("Auto-Generate from Notes", fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                    AppPill(
                                        selected = true,
                                        tint = ProButtonPalette.SkyCyan,
                                        onClick = { onClose(); vm.capturingCard = true },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                    ) {
                                        Text("Cut from page", fontWeight = FontWeight.SemiBold)
                                    }
                                    AppPill(
                                        tint = ProButtonPalette.Violet,
                                        onClick = { vm.newTextCard() },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                    ) {
                                        Text("New text card", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                if (vm.cards.isEmpty()) {
                                    AppCard(hero = false, shape = appCardShape(), contentPadding = PaddingValues(18.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "Your deck is empty. Tap 'Auto-Generate from Notes' to build cards from your text, sticky notes, and concept map, or cut any diagram directly from a page.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(vm.cards.take(6), key = { it.id }) { c ->
                                            CardRow(vm, c, onShow = { onClose(); vm.showCardSource(c) })
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    query,
                                    { query = it },
                                    placeholder = { com.pronotes.app.ui.AnimatedGradientSearchPlaceholder("Search cards front, back & hints…") },
                                    singleLine = true,
                                    shape = Pill,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    Modifier.padding(vertical = 8.dp).horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    listOf("All", "Due", "Starred ★", "Mastered").forEachIndexed { idx, label ->
                                        AppChip(
                                            selected = filter == idx,
                                            onClick = { filter = idx },
                                            label = label,
                                            tint = when (idx) {
                                                1 -> ProButtonPalette.VibrantOrange
                                                2 -> ProButtonPalette.CoralRose
                                                3 -> ProButtonPalette.Emerald
                                                else -> ProButtonPalette.ElectricBlue
                                            },
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (vm.cards.isNotEmpty()) {
                                        TextButton(onClick = {
                                            val send = Intent(Intent.ACTION_SEND).setType("text/csv")
                                                .putExtra(Intent.EXTRA_SUBJECT, vm.title + " flashcards")
                                                .putExtra(Intent.EXTRA_TEXT, vm.cardsCsv())
                                            context.startActivity(Intent.createChooser(send, "Export flashcards"))
                                        }) { Text("Export CSV") }
                                    }
                                }
                                val now = System.currentTimeMillis()
                                val shown = vm.cards.filter { c ->
                                    val matchesQ = query.isBlank() || c.front.contains(query, true) || c.back.contains(query, true) || c.hint.contains(query, true)
                                    val matchesF = when (filter) {
                                        1 -> c.due <= now
                                        2 -> c.starred
                                        3 -> c.box >= 4
                                        else -> true
                                    }
                                    matchesQ && matchesF
                                }
                                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(shown, key = { it.id }) { c ->
                                        Box(Modifier.animateItem()) { CardRow(vm, c, onShow = { onClose(); vm.showCardSource(c) }) }
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

@Composable
private fun StatChip(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    AppSurface(
        role = SurfaceRole.Frost,
        containerColor = tint.copy(alpha = 0.14f),
        shape = appCardShape(),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tint)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CardRow(vm: EditorViewModel, c: Flashcard, onShow: () -> Unit) {
    val pic = remember(c.frontImage) { vm.cardImage(c.frontImage) }
    AppCard(
        hero = false,
        shape = appCardShape(),
        contentPadding = PaddingValues(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (pic != null) Image(pic.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text("Aa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(c.front.ifBlank { "Picture card" }, maxLines = 2, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(c.back.ifBlank { "No answer yet" }, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    Text("Box ${c.box + 1}/6", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (c.hint.isNotBlank()) Text("· Hint", style = MaterialTheme.typography.labelSmall, color = ProButtonPalette.Emerald)
                    if (c.lapses > 0) Text("· ${c.lapses} lapses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Icon(
                imageVector = if (c.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Star card",
                tint = if (c.starred) ProButtonPalette.VibrantOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(Pill)
                    .clickable { vm.toggleStarCard(c.id) }
                    .padding(6.dp)
                    .size(20.dp),
            )
            AppMenuButton(
                contentPadding = PaddingValues(0.dp),
                contentDescription = "Card options",
                items = buildList {
                    add(
                        AppMenuItem(
                            label = "Edit",
                            icon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)) },
                            onClick = { vm.cardDraft = EditorViewModel.CardDraft(c, false) },
                        ),
                    )
                    if (c.pageId != null) {
                        add(
                            AppMenuItem(
                                label = "Show in notebook",
                                icon = { Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp)) },
                                onClick = onShow,
                            ),
                        )
                    }
                    add(
                        AppMenuItem(
                            label = "Delete",
                            destructive = true,
                            icon = { Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp)) },
                            onClick = { vm.deleteCard(c.id) },
                        ),
                    )
                },
            ) {
                Icon(Icons.Outlined.MoreHoriz, "Card options", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

/** Expressive 3D Flip & Multiple-Choice Quiz StudyView with 4-grade SM-2, ergonomic action bar, hints, stars & tablet layout. */
@Composable
private fun StudyView(vm: EditorViewModel, deck: List<Flashcard>, onDone: () -> Unit) {
    var shuffleSeed by remember { mutableIntStateOf(0) }
    val sessionIds = remember { deck.map { it.id } }
    var retryIds by remember { mutableStateOf<List<String>?>(null) }
    val queue = remember(shuffleSeed, retryIds) { (retryIds ?: sessionIds).shuffled() }
    var index by remember(queue) { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var studyMode by remember { mutableIntStateOf(0) } // 0 = 3D Flip Card, 1 = Multiple-Choice Quiz
    var selectedQuizChoice by remember { mutableStateOf<String?>(null) }
    var known by remember { mutableIntStateOf(0) }
    val againIds = remember { mutableStateListOf<String>() }
    val total = queue.size
    var elapsed by remember { mutableStateOf(0L) }

    val focusRequester = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val scroll = rememberScrollState()
    androidx.compose.runtime.LaunchedEffect(index, studyMode, shuffleSeed) { scroll.scrollTo(0) }
    val colors = MaterialTheme.colorScheme
    val card = queue.getOrNull(index)?.let { id -> vm.cards.firstOrNull { it.id == id } }

    fun gradeCard(g: Int) {
        val currentCard = card ?: return
        vm.gradeCard(currentCard.id, g)
        if (g == 0) againIds += currentCard.id else known++
        flipped = false
        showHint = false
        index++
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surface,
                        colors.surfaceVariant.copy(alpha = 0.35f),
                        colors.surface,
                    )
                )
            )
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Spacebar -> {
                            if (studyMode == 0) {
                                flipped = !flipped
                                true
                            } else false
                        }
                        Key.One, Key.NumPad1 -> {
                            if (flipped && studyMode == 0) { gradeCard(0); true } else false
                        }
                        Key.Two, Key.NumPad2 -> {
                            if (flipped && studyMode == 0) { gradeCard(1); true } else false
                        }
                        Key.Three, Key.NumPad3 -> {
                            if (flipped && studyMode == 0) { gradeCard(2); true } else false
                        }
                        Key.Four, Key.NumPad4 -> {
                            if (flipped && studyMode == 0) { gradeCard(3); true } else false
                        }
                        Key.Escape -> { onDone(); true }
                        else -> false
                    }
                } else false
            }
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Navigation & Stats Bar
        Row(
            Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (index < total) "Card ${index + 1} of $total" else "Session Complete",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            if (index < total) {
                Icon(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = "Reshuffle deck",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(Pill)
                        .clickable {
                            known = 0
                            againIds.clear()
                            shuffleSeed++
                            index = 0
                            flipped = false
                            showHint = false
                            selectedQuizChoice = null
                        }
                        .padding(6.dp)
                        .size(20.dp),
                )
            }
            SessionClock(running = index < total) { elapsed = it }
            TextButton(onClick = onDone) { Text("Exit") }
        }

        // Mode Segmented Control (Flashcards / Quiz)
        if (index < total) {
            AppSegmentedControl(
                options = listOf("Flashcards", "Quiz"),
                selectedIndex = studyMode,
                onSelect = {
                    studyMode = it
                    flipped = false
                    selectedQuizChoice = null
                },
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }

        // Smooth Progress Bar
        LinearProgressIndicator(
            progress = { if (total == 0) 1f else index / total.toFloat() },
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(6.dp)
                .clip(Pill),
            color = StudyColors.cards.last(),
            trackColor = colors.surfaceVariant.copy(alpha = 0.50f),
        )

        // Session Complete Screen
        if (card == null) {
            val pct = if (total == 0) 100 else known * 100 / total
            val (grade, line) = feedbackFor(pct)

            AppSurface(
                role = SurfaceRole.Frost,
                containerColor = colors.surfaceVariant.copy(alpha = 0.60f),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(28.dp),
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ScoreRing(pct, StudyColors.cards)
                    Text(
                        "$grade · Review Done",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        line,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatChip("Accuracy", "$pct%", ProButtonPalette.Emerald)
                        StatChip("Recalled", "$known / $total", ProButtonPalette.ElectricBlue)
                        StatChip("Time", clock(elapsed), ProButtonPalette.VibrantOrange)
                    }

                    if (againIds.isNotEmpty()) {
                        Button(
                            onClick = {
                                retryIds = againIds.distinct()
                                againIds.clear()
                                known = 0
                                shuffleSeed++
                                flipped = false
                                selectedQuizChoice = null
                                showHint = false
                            },
                            shape = Pill,
                            colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.CoralRose),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        ) {
                            Text("Practice Missed Cards (${againIds.distinct().size})", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onDone,
                        shape = Pill,
                        colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Finish Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Column
        }

        // Source page preview toggle
        val source = vm.pages.indexOfFirst { it.id == card.pageId }
        var showingSource by remember(card.id) { mutableStateOf(false) }
        if (source >= 0) {
            Row(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showingSource = !showingSource }) {
                    Text(if (showingSource) "Hide Source Page" else "View Source Page (Page ${source + 1})")
                }
            }
            if (showingSource) {
                val result by androidx.compose.runtime.produceState<Pair<Boolean, android.graphics.Bitmap?>>(false to null, card.pageId) {
                    value = true to vm.thumbnail(source, 880)
                }
                val preview = result.second
                if (preview != null) {
                    Image(
                        preview.asImageBitmap(),
                        "Source page " + (source + 1),
                        Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit,
                    )
                } else if (result.first) {
                    Text("Source preview unavailable", style = MaterialTheme.typography.bodySmall)
                } else {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        val correctAnswer = card.back.trim()
        val choices = remember(card.id, vm.cards.size) {
            val alternatives = vm.cards.map { it.back.trim() }
                .filter { it.isNotEmpty() && !it.equals(correctAnswer, ignoreCase = true) }
                .distinctBy { it.lowercase() }.shuffled().take(3)
            (alternatives + correctAnswer).shuffled()
        }

        // Multiple-Choice Quiz Mode
        if (studyMode == 1 && correctAnswer.isNotEmpty() && choices.size > 1) {
            var submitted by remember(card.id, shuffleSeed) { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Quiz Question Card
                AppSurface(
                    role = SurfaceRole.Frost,
                    containerColor = colors.primaryContainer.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "QUESTION",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = if (card.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Star",
                                tint = if (card.starred) ProButtonPalette.VibrantOrange else colors.onSurfaceVariant,
                                modifier = Modifier.size(22.dp).clickable { vm.toggleStarCard(card.id) },
                            )
                        }
                        remember(card.id) { vm.cardImage(card.frontImage) }?.let {
                            Image(
                                it.asImageBitmap(),
                                "Question image",
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Text(
                            card.front.ifBlank { "Select the matching answer for this flashcard:" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        if (card.hint.isNotBlank()) {
                            if (showHint) {
                                Text(
                                    "Hint: ${card.hint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ProButtonPalette.Emerald,
                                )
                            } else {
                                TextButton(onClick = { showHint = true }) {
                                    Text("Show Hint", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Answer Options List
                val letters = listOf("A", "B", "C", "D")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    choices.forEachIndexed { optIdx, option ->
                        val chosen = selectedQuizChoice == option
                        val isRight = option == correctAnswer
                        val optBg = when {
                            !submitted && chosen -> colors.primaryContainer
                            !submitted -> colors.surface
                            isRight -> Color(0xFF10B981).copy(alpha = 0.20f)
                            chosen -> Color(0xFFEF4444).copy(alpha = 0.20f)
                            else -> colors.surfaceVariant.copy(alpha = 0.35f)
                        }
                        val optBorder = when {
                            !submitted && chosen -> colors.primary
                            !submitted -> colors.outlineVariant.copy(alpha = 0.60f)
                            isRight -> Color(0xFF10B981)
                            chosen -> Color(0xFFEF4444)
                            else -> Color.Transparent
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(optBg)
                                .border(1.8.dp, optBorder, RoundedCornerShape(16.dp))
                                .clickable(enabled = !submitted) {
                                    selectedQuizChoice = option
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (chosen || (submitted && isRight)) colors.primary else colors.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    letters.getOrElse(optIdx) { "${optIdx + 1}" },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (chosen || (submitted && isRight)) Color.White else colors.onSurface,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                option,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (chosen || (submitted && isRight)) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Bottom Action for Quiz
                if (!submitted) {
                    Button(
                        onClick = { submitted = true },
                        enabled = selectedQuizChoice != null,
                        shape = Pill,
                        colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Check Answer", fontWeight = FontWeight.Bold)
                    }
                } else {
                    val wasCorrect = selectedQuizChoice == correctAnswer
                    Button(
                        onClick = {
                            vm.gradeCard(card.id, if (wasCorrect) 2 else 0)
                            if (wasCorrect) known++ else againIds += card.id
                            selectedQuizChoice = null
                            showHint = false
                            index++
                        },
                        shape = Pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wasCorrect) ProButtonPalette.Emerald else ProButtonPalette.ElectricBlue,
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(
                            if (wasCorrect) "Correct! Next Card" else "Next Card (Answer: $correctAnswer)",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            return@Column
        }

        // 3D Flip Flashcards Mode
        val turn by animateFloatAsState(
            targetValue = if (flipped) 180f else 0f,
            animationSpec = tween(
                durationMillis = if (com.pronotes.app.ui.LocalReduceMotion.current) 0 else 380,
                easing = FastOutSlowInEasing,
            ),
            label = "3DCardFlip",
        )
        val pic = remember(card.id) { vm.cardImage(card.frontImage) }

        AppSurface(
            role = SurfaceRole.Frost,
            containerColor = colors.primaryContainer.copy(alpha = if (turn > 90f) 0.82f else 0.95f),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(24.dp),
            onClick = { flipped = !flipped },
            modifier = Modifier
                .widthIn(max = 660.dp)
                .fillMaxWidth()
                .heightIn(min = 280.dp)
                .graphicsLayer {
                    rotationY = turn
                    cameraDistance = 18f * density
                },
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { if (turn > 90f) rotationY = 180f },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Card Top Metadata
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(Pill)
                                .background(colors.primary.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                if (turn <= 90f) "QUESTION · BOX ${card.box + 1}/6" else "ANSWER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = colors.primary,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = if (card.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star card",
                            tint = if (card.starred) ProButtonPalette.VibrantOrange else colors.onSurfaceVariant,
                            modifier = Modifier.size(22.dp).clickable { vm.toggleStarCard(card.id) },
                        )
                    }

                    // Card Body (Front or Back)
                    if (turn <= 90f) {
                        pic?.let {
                            Image(
                                it.asImageBitmap(),
                                null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                        }
                        if (card.front.isNotBlank()) {
                            Text(
                                card.front,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                        if (card.hint.isNotBlank()) {
                            if (showHint) {
                                Text(
                                    "Hint: ${card.hint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ProButtonPalette.Emerald,
                                    textAlign = TextAlign.Center,
                                )
                            } else {
                                TextButton(onClick = { showHint = true }) {
                                    Text("Show Hint", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap card or press Space to reveal answer",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    } else {
                        remember(card.id) { vm.cardImage(card.backImage) }?.let {
                            Image(
                                it.asImageBitmap(),
                                null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                        }
                        Text(
                            card.back.ifBlank { "No answer recorded" },
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Ergonomic Rating Action Bar (Again, Hard, Good, Easy)
        if (flipped) {
            val grades = listOf(
                Triple("Again", "< 1m", Color(0xFFE11D48)),
                Triple("Hard", "1d", Color(0xFFD97706)),
                Triple("Good", "3d", Color(0xFF059669)),
                Triple("Easy", "7d", Color(0xFF2563EB)),
            )

            Row(
                modifier = Modifier
                    .widthIn(max = 660.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                grades.forEachIndexed { gIdx, (label, interval, tint) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(tint.copy(alpha = 0.14f))
                            .border(1.5.dp, tint.copy(alpha = 0.50f), RoundedCornerShape(16.dp))
                            .clickable { gradeCard(gIdx) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = tint,
                        )
                        Text(
                            interval,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LinkSheet(vm: EditorViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var url by remember { mutableStateOf("") }
    Dialog(onDismissRequest = { vm.linkSheet = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.linkSheet = null }
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppDialogSurface(
                shape = appCardShape(4f),
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Add link", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Tap linked content with a finger to follow it. Long-press its blue badge to remove.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    AppSegmentedControl(
                        options = listOf("Page", "Website"),
                        selectedIndex = tab,
                        onSelect = { tab = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    if (tab == 0) {
                        LazyVerticalGrid(
                            GridCells.Adaptive(96.dp),
                            Modifier.weight(1f, fill = false),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(vm.pages.size, key = { vm.pages[it].id }) { i ->
                                val thumb by produceState<android.graphics.Bitmap?>(null, i) { value = vm.thumbnail(i, 200) }
                                val current = i == vm.pageIndex
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { vm.linkToPage(i) }
                                        .padding(2.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 120.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (current) ProButtonPalette.ElectricBlue.copy(alpha = 0.20f)
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                            ),
                                    ) {
                                        thumb?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth()) }
                                    }
                                    Text(
                                        "Page ${i + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (current) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            url,
                            { url = it },
                            singleLine = true,
                            shape = Pill,
                            placeholder = { Text("example.com") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { vm.linkToUrl(url) },
                            enabled = url.trim().contains('.'),
                            shape = Pill,
                            colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                        ) { Text("Add link") }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { vm.linkSheet = null }, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateSheet(vm: EditorViewModel, onClose: () -> Unit) {
    val cur = vm.currentPage
    var pickedStyle by remember { mutableStateOf(cur?.paper ?: PaperStyle.RULED) }
    var lineScale by remember { mutableFloatStateOf(cur?.lineScale ?: 1f) }
    var lineColor by remember { mutableStateOf(cur?.lineColor) }
    var paperColor by remember { mutableStateOf(cur?.paperColor) }
    var target by remember { mutableIntStateOf(0) }
    val groups = remember { listOf("Writing", "Grids", "Planning", "Study", "Music") }
    var activeGroup by remember { mutableStateOf(pickedStyle.group) }
    val byGroup = remember { PaperStyle.pickerEntries.groupBy { it.group } }
    val previewRenderer = remember { vm.renderer() }

    AppSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScaleInAnimatedText(
                text = "Page Templates",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                groups.forEach { g ->
                    AppChip(
                        selected = activeGroup == g,
                        onClick = { activeGroup = g },
                        label = g,
                        tint = ProButtonPalette.ElectricBlue,
                    )
                }
            }
            val styles = byGroup[activeGroup].orEmpty()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                styles.forEach { style ->
                    val selected = style == pickedStyle
                    AppCard(
                        onClick = { pickedStyle = style },
                        containerColor = if (selected) ProButtonPalette.ElectricBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier
                            .width(104.dp)
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) ProButtonPalette.ElectricBlue else Color.Transparent,
                                shape = appCardShape(),
                            ),
                    ) {
                        val previewPage = remember(style, lineScale, lineColor, paperColor) {
                            Page(
                                id = "preview-${style.name}",
                                width = PageSize.WIDTH,
                                height = PageSize.HEIGHT,
                                paper = style,
                                paperColor = paperColor,
                                lineScale = lineScale,
                                lineColor = lineColor,
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(paperColor ?: 0xFFFFFFFF.toInt())),
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val sx = size.width / previewPage.width
                                val sy = size.height / previewPage.height
                                drawIntoCanvas { c ->
                                    c.nativeCanvas.save()
                                    c.nativeCanvas.scale(sx, sy)
                                    previewRenderer.drawPaper(c.nativeCanvas, previewPage, null, false)
                                    c.nativeCanvas.restore()
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            style.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }

            AppSliderRow(
                title = "LINE SPACING",
                valueText = "%.0f%%".format(lineScale * 100f),
                value = lineScale,
                valueRange = 0.6f..1.6f,
                onValueChange = { lineScale = it },
            )

            Text("LINE COLOUR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AppChip(selected = lineColor == null, onClick = { lineColor = null }, label = "Auto")
                Spacer(Modifier.width(6.dp))
                listOf(0xFFD5DEEA, 0xFFB0BEC5, 0xFF93C5FD, 0xFFA7F3D0, 0xFFFCA5A5, 0xFF475569).forEach { c ->
                    val ci = c.toInt()
                    Swatch(ci, lineColor == ci, 20) { lineColor = ci }
                }
            }

            Text("PAPER TINT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AppChip(selected = paperColor == null, onClick = { paperColor = null }, label = "Default")
                Spacer(Modifier.width(6.dp))
                listOf(0xFFFFFFFF, 0xFFFBF7EC, 0xFFF5F4E6, 0xFFF1F2F4, 0xFFEAF4FF, 0xFF22252B).forEach { c ->
                    val ci = c.toInt()
                    Swatch(ci, paperColor == ci, 20) { paperColor = ci }
                }
            }

            AppSegmentedControl(
                options = listOf("This page", "New page", "All pages"),
                selectedIndex = target,
                onSelect = { target = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                TextButton(onClick = onClose) { Text("Cancel") }
                Button(
                    onClick = {
                        vm.applyTemplate(pickedStyle, lineScale, lineColor, paperColor, target)
                        onClose()
                    },
                    shape = Pill,
                    colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                ) {
                    Text("Apply template")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TapePatternPicker(vm: EditorViewModel, onPickImage: () -> Unit) {
    val context = LocalContext.current
    val customTapes = vm.customTapes
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TapePatterns.names.forEach { name ->
                val selected = vm.tapePattern == name
                val tileBmp by produceState<Bitmap?>(null, name, vm.tapeColor) {
                    value = TapePatterns.tile(name, vm.tapeColor)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable { vm.tapePattern = name }
                        .padding(4.dp),
                ) {
                    tileBmp?.let { bmp ->
                        Image(
                            bmp.asImageBitmap(),
                            contentDescription = TapePatterns.label(name),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        TapePatterns.label(name),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            AppChip(
                selected = false,
                onClick = onPickImage,
                label = "Import tape",
                leadingIcon = { Icon(Icons.Outlined.Add, null, Modifier.size(15.dp)) },
                tint = ProButtonPalette.SkyCyan,
            )
        }
        if (customTapes.isNotEmpty()) {
            Text(
                "SAVED CUSTOM TAPES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                customTapes.forEach { fileName ->
                    val pat = "apptape:$fileName"
                    val selected = vm.tapePattern == pat
                    val thumb by produceState<Bitmap?>(null, fileName) {
                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.pronotes.app.data.CustomTapeStore.loadBitmap(context, fileName, maxDim = 256)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(66.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { vm.tapePattern = pat },
                    ) {
                        thumb?.let { bmp ->
                            Image(
                                bmp.asImageBitmap(),
                                contentDescription = "Custom tape",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable { vm.deleteCustomTape(fileName) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove custom tape",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
