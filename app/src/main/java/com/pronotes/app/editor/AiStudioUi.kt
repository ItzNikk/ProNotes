package com.pronotes.app.editor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.pronotes.app.data.CloudSync
import com.pronotes.app.data.CollabChatMsg
import com.pronotes.app.data.CollabMemberInfo
import com.pronotes.app.data.CollabRoomInfo
import com.pronotes.app.data.CollabSharedNote
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.Repository
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.elasticStretch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val PillShape = CircleShape

// ========================================================================= 1. AI Notes & PDF Chat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiNotesChatDialog(vm: EditorViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clip = LocalClipboardManager.current
    val messages = remember {
        mutableStateListOf(
            "assistant" to "Hi! I've indexed \"${vm.title}\" (${vm.pages.size} pages${if (vm.isPdf) " + PDF" else ""}). Ask anything or tap a quick study prompt below.",
        )
    }
    var corpus by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(vm.noteId, vm.pages.size) {
        corpus = vm.extractPagesCorpus()
    }

    fun send(text: String) {
        val q = text.trim()
        if (q.isBlank() || busy) return
        input = ""
        messages += "user" to q
        busy = true
        scope.launch {
            val pagesSnap = if (corpus.isNotEmpty()) corpus else vm.extractPagesCorpus().also { corpus = it }
            val reply = LocalAiManager.chatAboutNotesAndPdf(
                context = context,
                notebookTitle = vm.title,
                pagesText = pagesSnap,
                history = messages.toList(),
                question = q,
            )
            messages += "assistant" to reply
            busy = false
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp).widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 800.dp).statusBarsPadding(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AI Notes & PDF Chat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(vm.aiEngineLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }

                val quickPrompts = listOf(
                    "Summarize key concepts",
                    "Extract formulas & definitions",
                    "Quiz me with 3 questions",
                    "What's on Page ${vm.pageIndex + 1}?",
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickPrompts.forEach { qp ->
                        FilterChip(
                            selected = false,
                            onClick = { send(qp) },
                            label = { Text(qp) },
                            shape = PillShape,
                            modifier = Modifier.elasticStretch(),
                        )
                    }
                }

                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages) { (role, body) ->
                        val isUser = role == "user"
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.widthIn(max = 560.dp),
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(body, style = MaterialTheme.typography.bodyMedium)
                                    if (!isUser) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            TextButton(
                                                onClick = { clip.setText(AnnotatedString(body)) },
                                                modifier = Modifier.height(30.dp),
                                            ) {
                                                Icon(Icons.Outlined.ContentCopy, null, Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                                            }
                                            TextButton(
                                                onClick = { vm.insertAiTextToPage(body) },
                                                modifier = Modifier.height(30.dp),
                                            ) {
                                                Icon(Icons.Outlined.PostAdd, null, Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Insert to Page", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (busy) {
                    ThinkingPill(engine = vm.aiEngineLabel, stage = "Reading notes & PDF…")
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Ask a question about your notes or PDF…") },
                        singleLine = true,
                        shape = PillShape,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        enabled = input.isNotBlank() && !busy,
                        onClick = { send(input) },
                        shape = PillShape,
                        modifier = Modifier.elasticStretch(),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, "Send")
                    }
                }
            }
        }
    }
}

// ========================================================================= 2. AI Asset Studio (Stickers, Covers & Templates)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiAssetStudioDialog(
    vm: EditorViewModel,
    initialKind: AiAssetKind = AiAssetKind.STICKER,
    onInsertSticker: (Bitmap) -> Unit,
    onApplyTemplate: (Bitmap) -> Unit,
    onApplyCover: (Bitmap) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var kind by remember(initialKind) { mutableStateOf(initialKind) }
    var prompt by remember(initialKind) {
        mutableStateOf(
            when (initialKind) {
                AiAssetKind.STICKER -> "Study Star Goal"
                AiAssetKind.COVER -> vm.title
                AiAssetKind.TEMPLATE -> "Cornell Study Notes"
            },
        )
    }
    var accent by remember { mutableIntStateOf(0xFF3D6CF4.toInt()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun generateNow() {
        if (busy) return
        busy = true
        scope.launch {
            preview = LocalAiManager.generateAiAssetBitmap(context, kind, prompt, accent)
            busy = false
        }
    }

    LaunchedEffect(kind) { generateNow() }

    val presets = when (kind) {
        AiAssetKind.STICKER -> listOf("Gold Star Exam", "Key Formula Ribbon", "Focus Badge", "Science Atom")
        AiAssetKind.COVER -> listOf("Cosmos Minimal", "Botanical Journal", "Physics Atlas", "Architecture Slate")
        AiAssetKind.TEMPLATE -> listOf("Cornell Notes", "Weekly Planner", "14-Day Habit Tracker", "Storyboard Grid")
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp).widthIn(max = 720.dp).fillMaxWidth().heightIn(max = 800.dp).statusBarsPadding(),
        ) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AI Creative Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Model: ${LocalAiManager.activeImageModelLabel(context)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }

                PrimaryTabRow(selectedTabIndex = kind.ordinal, containerColor = Color.Transparent) {
                    AiAssetKind.entries.forEach { k ->
                        Tab(
                            selected = kind == k,
                            onClick = { kind = k },
                            text = { Text(k.label) },
                        )
                    }
                }

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { p ->
                        FilterChip(
                            selected = prompt == p,
                            onClick = { prompt = p; generateNow() },
                            label = { Text(p) },
                            shape = PillShape,
                            modifier = Modifier.elasticStretch(),
                        )
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Describe your ${kind.label.lowercase()}") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0xFF3D6CF4, 0xFF8B5CF6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFFEC4899, 0xFF1E293B).forEach { c ->
                        val ci = c.toInt()
                        Swatch(ci, accent == ci, 24) { accent = ci; generateNow() }
                    }
                }

                Box(
                    Modifier.fillMaxWidth().height(230.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    preview?.let {
                        Image(it.asImageBitmap(), "AI Preview", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(12.dp))
                    } ?: Text("Tap Generate to preview", style = MaterialTheme.typography.bodySmall)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { generateNow() },
                        shape = PillShape,
                        modifier = Modifier.weight(1f).elasticStretch(),
                    ) { Text(if (busy) "Rendering…" else "Regenerate") }
                    Button(
                        enabled = preview != null && !busy,
                        onClick = {
                            val bmp = preview ?: return@Button
                            when (kind) {
                                AiAssetKind.STICKER -> onInsertSticker(bmp)
                                AiAssetKind.TEMPLATE -> onApplyTemplate(bmp)
                                AiAssetKind.COVER -> onApplyCover(bmp)
                            }
                            onClose()
                        },
                        shape = PillShape,
                        modifier = Modifier.weight(1f).elasticStretch(),
                    ) {
                        Text(
                            when (kind) {
                                AiAssetKind.STICKER -> "Insert Sticker"
                                AiAssetKind.TEMPLATE -> "Use as Page Template"
                                AiAssetKind.COVER -> "Set as Notebook Cover"
                            },
                        )
                    }
                }
            }
        }
    }
}

// ========================================================================= 3. AI Lecture Recorder

@Composable
fun AiLectureRecorderDialog(vm: EditorViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasMicPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPerm = granted
    }

    var recording by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableLongStateOf(0L) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    val bars = remember { mutableStateListOf<Float>().apply { repeat(28) { add(0.15f) } } }
    val bookmarks = remember { mutableStateListOf<String>() }
    var notesText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("${vm.title} Lecture") }
    var structuredOut by remember { mutableStateOf("") }
    var busyAi by remember { mutableStateOf(false) }

    fun stopRec() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        recording = false
    }

    fun startRec() {
        val dir = vm.noteDirFile().apply { mkdirs() }
        val f = File(dir, "lecture_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        val ok = runCatching {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
        }.isSuccess
        if (ok) {
            recorder = r
            audioFile = f
            elapsedSec = 0L
            recording = true
        } else {
            runCatching { r.release() }
            // Fallback timer-only capture if emulator/device has no mic hardware
            elapsedSec = 0L
            recording = true
        }
    }

    LaunchedEffect(recording) {
        var tick = 0
        while (recording) {
            delay(200)
            tick++
            if (tick % 5 == 0) elapsedSec++
            val amp = runCatching { (recorder?.maxAmplitude ?: 0) / 32767f }.getOrDefault(0f)
            val v = if (amp > 0.02f) amp.coerceIn(0.12f, 1f) else (0.14f + (tick % 4) * 0.07f)
            if (bars.size >= 28) bars.removeAt(0)
            bars.add(v)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopRec()
            runCatching { player?.release() }
            player = null
        }
    }

    fun fmtTime(s: Long): String = "%02d:%02d".format(s / 60, s % 60)

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp).widthIn(max = 680.dp).fillMaxWidth().heightIn(max = 800.dp).statusBarsPadding(),
        ) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Mic, null, tint = if (recording) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Lecture Recorder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text("Close") }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Lecture / Topic Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(fmtTime(elapsedSec), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Row(
                            Modifier.fillMaxWidth().height(48.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            bars.forEach { b ->
                                Box(
                                    Modifier.width(6.dp).height((8 + b * 36).dp)
                                        .clip(PillShape)
                                        .background(if (recording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!hasMicPerm && !recording) {
                                FilledTonalButton(
                                    onClick = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                    shape = PillShape,
                                ) { Text("Allow Microphone") }
                            }
                            Button(
                                onClick = { if (recording) stopRec() else startRec() },
                                shape = PillShape,
                                modifier = Modifier.elasticStretch(),
                            ) {
                                Icon(if (recording) Icons.Outlined.Stop else Icons.Outlined.Mic, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (recording) "Stop" else "Start Recording")
                            }
                            FilledTonalButton(
                                enabled = recording,
                                onClick = { bookmarks += "${fmtTime(elapsedSec)} — Key moment #${bookmarks.size + 1}" },
                                shape = PillShape,
                                modifier = Modifier.elasticStretch(),
                            ) {
                                Icon(Icons.Outlined.BookmarkAdd, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Bookmark")
                            }
                            val af = audioFile
                            if (af != null && af.exists() && !recording) {
                                OutlinedButton(
                                    onClick = {
                                        if (playing) {
                                            runCatching { player?.stop(); player?.release() }
                                            player = null; playing = false
                                        } else {
                                            runCatching {
                                                player = MediaPlayer().apply {
                                                    setDataSource(af.absolutePath)
                                                    prepare()
                                                    setOnCompletionListener { playing = false }
                                                    start()
                                                }
                                                playing = true
                                            }
                                        }
                                    },
                                    shape = PillShape,
                                ) {
                                    Icon(if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (playing) "Stop" else "Play")
                                }
                            }
                        }
                    }
                }

                if (bookmarks.isNotEmpty()) {
                    Text("BOOKMARKS (${bookmarks.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    bookmarks.forEach { b ->
                        Text("• $b", style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Lecture notes / transcript snippets (optional)") },
                    placeholder = { Text("Jot quick points while recording; AI will organize summary & takeaways…") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    enabled = !busyAi && (notesText.isNotBlank() || bookmarks.isNotEmpty() || elapsedSec > 0L),
                    onClick = {
                        busyAi = true
                        scope.launch {
                            val out = LocalAiManager.structureLectureRecording(
                                context,
                                title,
                                notesText,
                                bookmarks.toList(),
                                elapsedSec,
                            )
                            structuredOut = out
                            busyAi = false
                        }
                    },
                    shape = PillShape,
                    modifier = Modifier.fillMaxWidth().elasticStretch(),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (busyAi) "Structuring with AI…" else "AI Structure Summary & Takeaways")
                }

                if (structuredOut.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(structuredOut, style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = {
                                    vm.insertAiTextToPage(structuredOut)
                                    onClose()
                                },
                                shape = PillShape,
                                modifier = Modifier.align(Alignment.End).elasticStretch(),
                            ) { Text("Insert into Page") }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================================= 4. Live Collab Study Room

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveCollabRoomDialog(
    vm: EditorViewModel? = null,
    repo: Repository,
    onOpenNotebook: ((String) -> Unit)? = null,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudUser by CloudSync.user.collectAsState()
    var roomCode by remember { mutableStateOf("") }
    var roomTitle by remember { mutableStateOf("") }
    var activeRoom by remember { mutableStateOf<CollabRoomInfo?>(null) }
    var members by remember { mutableStateOf<List<CollabMemberInfo>>(emptyList()) }
    var msgs by remember { mutableStateOf<List<CollabChatMsg>>(emptyList()) }
    var sharedItems by remember { mutableStateOf<List<CollabSharedNote>>(emptyList()) }
    var chatDraft by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun refreshRoom(rid: String) {
        scope.launch {
            CloudSync.fetchRoomFeed(context, rid).onSuccess { (m, c, s) ->
                members = m; msgs = c; sharedItems = s
            }.onFailure { statusMsg = it.message ?: "Refresh error" }
        }
    }

    LaunchedEffect(activeRoom?.roomId) {
        val rid = activeRoom?.roomId ?: return@LaunchedEffect
        while (true) {
            refreshRoom(rid)
            delay(5000)
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp).widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 820.dp).statusBarsPadding(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(activeRoom?.title ?: "Live Study Room", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            activeRoom?.let { "Room Code: ${it.roomId} · ${members.size} active" }
                                ?: "Study together live with classmates",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (activeRoom != null) {
                        IconButton(onClick = { refreshRoom(activeRoom!!.roomId) }) {
                            Icon(Icons.Outlined.Refresh, "Refresh")
                        }
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }

                if (cloudUser == null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Sign in with Google first", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Live Study Rooms sync chat messages and shared note snippets via Firebase Firestore using your Google account.",
                                style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { runCatching { context.startActivity(CloudSync.buildGoogleAuthIntent()) } },
                                shape = PillShape,
                                modifier = Modifier.elasticStretch(),
                            ) { Text("Sign in with Google") }
                        }
                    }
                } else if (activeRoom == null) {
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { roomCode = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(16) },
                        label = { Text("Room Code (3–16 chars)") },
                        placeholder = { Text("e.g. BIO101 or CALC2") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = roomTitle,
                        onValueChange = { roomTitle = it },
                        label = { Text("Room Title (optional if creating)") },
                        placeholder = { Text("e.g. Midterm Review") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (statusMsg.isNotBlank()) {
                        Text(statusMsg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        enabled = !busy && roomCode.length >= 3,
                        onClick = {
                            busy = true; statusMsg = ""
                            scope.launch {
                                CloudSync.createOrJoinRoom(context, roomCode, roomTitle).onSuccess {
                                    activeRoom = it
                                }.onFailure {
                                    statusMsg = it.message ?: "Could not join room"
                                }
                                busy = false
                            }
                        },
                        shape = PillShape,
                        modifier = Modifier.fillMaxWidth().elasticStretch(),
                    ) { Text(if (busy) "Connecting…" else "Create / Join Study Room") }
                } else {
                    val rid = activeRoom!!.roomId
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        members.forEach { m ->
                            Surface(shape = PillShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(m.displayName, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (vm != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val body = vm.extractPageSummaryText(vm.pageIndex).ifBlank { "Studying ${vm.title} (Page ${vm.pageIndex + 1})" }
                                    scope.launch {
                                        CloudSync.sharePageSnippetToRoom(
                                            context, rid, vm.pageIndex,
                                            "${vm.title} · Page ${vm.pageIndex + 1}",
                                            body,
                                        ).onSuccess {
                                            statusMsg = "Shared current page to room"
                                            refreshRoom(rid)
                                        }.onFailure { statusMsg = it.message ?: "Share failed" }
                                    }
                                },
                                shape = PillShape,
                                modifier = Modifier.weight(1f).elasticStretch(),
                            ) { Text("Share Current Page") }
                        }
                    }

                    if (statusMsg.isNotBlank()) {
                        Text(statusMsg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (sharedItems.isNotEmpty()) {
                            item { Text("SHARED NOTES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                            items(sharedItems, key = { it.id }) { sn ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(sn.summary.ifBlank { "Shared Snippet" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                        Text(sn.itemsJson.take(400), style = MaterialTheme.typography.bodySmall, maxLines = 5, overflow = TextOverflow.Ellipsis)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = {
                                                if (vm != null) {
                                                    vm.insertAiTextToPage(sn.itemsJson.ifBlank { sn.summary })
                                                    statusMsg = "Inserted onto Page ${vm.pageIndex + 1}"
                                                } else {
                                                    scope.launch {
                                                        val nid = repo.importStructuredText(
                                                            sn.summary.ifBlank { "Room $rid Note" },
                                                            sn.itemsJson.ifBlank { sn.summary },
                                                            null,
                                                        )
                                                        onOpenNotebook?.invoke(nid)
                                                    }
                                                }
                                            }) { Text(if (vm != null) "Import onto Page" else "Import as Notebook") }
                                        }
                                    }
                                }
                            }
                            item { HorizontalDivider() }
                        }

                        item { Text("LIVE CHAT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                        items(msgs, key = { it.id }) { m ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (m.authorId == cloudUser?.uid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(m.authorName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(m.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = chatDraft,
                            onValueChange = { chatDraft = it },
                            placeholder = { Text("Message study group…") },
                            singleLine = true,
                            shape = PillShape,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            enabled = chatDraft.isNotBlank(),
                            onClick = {
                                val t = chatDraft.trim()
                                chatDraft = ""
                                scope.launch {
                                    CloudSync.sendRoomMessage(context, rid, t).onSuccess { refreshRoom(rid) }
                                }
                            },
                            shape = PillShape,
                            modifier = Modifier.elasticStretch(),
                        ) { Icon(Icons.AutoMirrored.Outlined.Send, "Send") }
                    }
                }
            }
        }
    }
}
