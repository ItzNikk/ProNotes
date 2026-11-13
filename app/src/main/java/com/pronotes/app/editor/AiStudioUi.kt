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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.pronotes.app.data.CloudSync
import com.pronotes.app.data.CollabChatMsg
import com.pronotes.app.data.CollabMemberInfo
import com.pronotes.app.data.CollabRoomInfo
import com.pronotes.app.data.CollabSharedNote
import com.pronotes.app.data.Repository
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.elasticStretch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val PillShape = CircleShape

private val StudioGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4285F4),
        Color(0xFF9B72CB),
        Color(0xFFD96570),
    ),
)

private val OneUi9GradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF6366F1),
        Color(0xFFA855F7),
        Color(0xFFEC4899),
        Color(0xFF38BDF8),
    ),
)

// ========================================================================= 1. AI Notes & PDF Chat (Study Assistant)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiNotesChatDialog(vm: EditorViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clip = LocalClipboardManager.current
    val messages = remember { mutableStateListOf<Pair<String, String>>() }
    var corpus by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(vm.noteId, vm.pages.size, vm.pageIndex) {
        corpus = vm.extractPagesCorpus()
    }

    fun send(text: String) {
        val q = text.trim()
        if (q.isBlank() || busy) return
        input = ""
        messages += "user" to q
        busy = true
        scope.launch {
            val pagesSnap = vm.extractPagesCorpus().also { corpus = it }
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
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(14.dp)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .heightIn(max = 820.dp)
                .statusBarsPadding()
                .border(1.5.dp, StudioGradientBrush, RoundedCornerShape(32.dp)),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StudioGradientBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Study Assistant",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    brush = StudioGradientBrush,
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                            )
                            Surface(
                                shape = PillShape,
                                color = Color(0xFF9B72CB).copy(alpha = 0.14f),
                            ) {
                                Text(
                                    "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9B72CB),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            "${vm.aiEngineLabel} · Grounded in \"${vm.title}\" (${vm.pages.size} pages${if (vm.isPdf) " + PDF" else ""})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (messages.isNotEmpty()) {
                        TextButton(onClick = { messages.clear() }) { Text("Clear") }
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }

                // Grounding Context Pills
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(shape = PillShape, color = Color(0xFF4285F4).copy(alpha = 0.12f)) {
                        Text(
                            "📄 Current: Page ${vm.pageIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    Surface(shape = PillShape, color = Color(0xFF9B72CB).copy(alpha = 0.12f)) {
                        Text(
                            "📚 ${vm.pages.size} Pages Indexed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9B72CB),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    Surface(shape = PillShape, color = Color(0xFFD96570).copy(alpha = 0.12f)) {
                        Text(
                            "✨ Direct Question Answering",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD96570),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }

                val quickPrompts = listOf(
                    "Explain Page ${vm.pageIndex + 1} simply",
                    "Summarize key concepts",
                    "Extract formulas & definitions",
                    "Quiz me with 3 questions",
                    "Create a study checklist",
                )

                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp, horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "Hello, Scholar.",
                                    style = TextStyle(
                                        brush = StudioGradientBrush,
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    ),
                                )
                                Text(
                                    "How can I help you master \"${vm.title}\" today?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    quickPrompts.forEach { qp ->
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(18.dp))
                                                .clickable { send(qp) }
                                                .elasticStretch(),
                                        ) {
                                            Row(
                                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Icon(
                                                    Icons.Outlined.AutoAwesome,
                                                    null,
                                                    tint = Color(0xFF9B72CB),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Text(qp, style = MaterialTheme.typography.labelLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
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
                        }
                    }

                    items(messages) { (role, body) ->
                        val isUser = role == "user"
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Top,
                        ) {
                            if (!isUser) {
                                Box(
                                    Modifier
                                        .padding(top = 4.dp, end = 8.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(StudioGradientBrush),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = if (isUser) 22.dp else 6.dp,
                                    topEnd = if (isUser) 6.dp else 22.dp,
                                    bottomStart = 22.dp,
                                    bottomEnd = 22.dp,
                                ),
                                color = if (isUser) {
                                    Color(0xFF4285F4)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                                },
                                contentColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.widthIn(max = 580.dp),
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    ThinkingPill(engine = vm.aiEngineLabel, stage = "Analyzing notes and synthesizing answer…")
                }

                // Floating Prompt Pill Composer
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioGradientBrush, PillShape),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            null,
                            tint = Color(0xFF9B72CB),
                            modifier = Modifier.padding(start = 6.dp).size(20.dp),
                        )
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Ask anything about your notes, PDF, or study topic…") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            enabled = input.isNotBlank() && !busy,
                            onClick = { send(input) },
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            modifier = Modifier.elasticStretch(),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Send, "Ask", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ========================================================================= 2. Samsung Creative Studio (Drawing Assist + Lite Models + Pause/Resume)

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
    val settingsStore = remember { SettingsStore.get(context) }
    val settings by settingsStore.settings.collectAsState()
    val imgStates by LocalAiManager.imageStates.collectAsState()

    var kind by remember(initialKind) { mutableStateOf(initialKind) }
    var style by remember { mutableStateOf(CreativeStyle.CARTOON_3D) }
    var showModelDrawer by remember { mutableStateOf(false) }
    var prompt by remember(initialKind) {
        mutableStateOf(
            when (initialKind) {
                AiAssetKind.STICKER -> "Study Star Goal"
                AiAssetKind.COVER -> vm.title
                AiAssetKind.TEMPLATE -> "Cornell Study Notes"
            },
        )
    }
    var accent by remember { mutableIntStateOf(0xFF6366F1.toInt()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        LocalAiManager.refresh(context)
    }

    fun generateNow() {
        if (busy) return
        busy = true
        scope.launch {
            preview = LocalAiManager.generateAiAssetBitmap(context, kind, prompt, accent, style)
            busy = false
        }
    }

    LaunchedEffect(kind, style, settings.imageLiteMode, settings.activeImageModelId) {
        generateNow()
    }

    val presets = when (kind) {
        AiAssetKind.STICKER -> listOf("Gold Star Exam", "Key Formula Ribbon", "Focus Badge", "Science Atom", "Rocket Launch", "Brain Spark")
        AiAssetKind.COVER -> listOf("Cosmos Minimal", "Botanical Journal", "Physics Atlas", "Architecture Slate", "Cyber Neon", "Pastel Studio")
        AiAssetKind.TEMPLATE -> listOf("Cornell Notes", "Weekly Planner", "14-Day Habit Tracker", "Storyboard Grid", "Exam Revision Sheet")
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(14.dp)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .heightIn(max = 860.dp)
                .statusBarsPadding()
                .border(1.5.dp, OneUi9GradientBrush, RoundedCornerShape(32.dp)),
        ) {
            Column(
                Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // One UI 9 Inspired Creative Studio Iridescent Header Banner
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(OneUi9GradientBrush)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Creative Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                "Drawing Assist · ${LocalAiManager.activeImageModelLabel(context)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }
                        // ⚡ Lite Mode Toggle Chip
                        Surface(
                            shape = PillShape,
                            color = if (settings.imageLiteMode) Color.White else Color.White.copy(alpha = 0.22f),
                            modifier = Modifier
                                .clip(PillShape)
                                .clickable {
                                    settingsStore.update { it.copy(imageLiteMode = !it.imageLiteMode) }
                                }
                                .elasticStretch(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Bolt,
                                    null,
                                    tint = if (settings.imageLiteMode) Color(0xFF6366F1) else Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    if (settings.imageLiteMode) "⚡ Lite Mode ON" else "Lite Mode OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (settings.imageLiteMode) Color(0xFF6366F1) else Color.White,
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        TextButton(onClick = onClose) {
                            Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick Lite Image Switcher & Pause/Resume Drawer Toggle
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "CREATIVE STYLE (DRAWING ASSIST)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = { showModelDrawer = !showModelDrawer }) {
                        Icon(Icons.Outlined.Download, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (showModelDrawer) "Hide Lite Models" else "Lite Image Models (Pause / Resume)")
                    }
                }

                // Inline Lite Image Models Manager with Pause / Resume / Cancel
                if (showModelDrawer) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Lite Image Models · Pause & Resume Supported",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("⚡ Lite Mode", style = MaterialTheme.typography.labelSmall)
                                    Switch(
                                        checked = settings.imageLiteMode,
                                        onCheckedChange = { v -> settingsStore.update { it.copy(imageLiteMode = v) } },
                                    )
                                }
                            }
                            LocalAiManager.imageCatalog.forEach { m ->
                                val st = imgStates[m.id] ?: LocalModelRuntime()
                                val isActive = settings.activeImageModelId == m.id
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { settingsStore.update { it.copy(activeImageModelId = m.id) } },
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(m.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                                    Surface(shape = PillShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                                        Text(
                                                            "${m.sizeLabel} · ${m.backend}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        )
                                                    }
                                                }
                                                Text(m.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            when (st.status) {
                                                LocalModelStatus.READY -> {
                                                    Button(
                                                        onClick = { settingsStore.update { it.copy(activeImageModelId = m.id) } },
                                                        shape = PillShape,
                                                    ) {
                                                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(15.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(if (isActive) "Active" else "Use")
                                                    }
                                                }
                                                LocalModelStatus.DOWNLOADING -> {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        FilledTonalButton(
                                                            onClick = { LocalAiManager.pauseImageModelDownload(context, m.id) },
                                                            shape = PillShape,
                                                        ) {
                                                            Icon(Icons.Outlined.Pause, null, Modifier.size(15.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Pause")
                                                        }
                                                        TextButton(onClick = { LocalAiManager.cancelImageModelDownload(context, m.id) }) {
                                                            Text("Cancel")
                                                        }
                                                    }
                                                }
                                                LocalModelStatus.PAUSED -> {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Button(
                                                            onClick = {
                                                                LocalAiManager.startImageModelDownload(context, m.id)
                                                            },
                                                            shape = PillShape,
                                                        ) {
                                                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(15.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Resume ${(st.progress * 100).toInt()}%")
                                                        }
                                                        TextButton(onClick = { LocalAiManager.cancelImageModelDownload(context, m.id) }) {
                                                            Text("Cancel")
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            settingsStore.update { it.copy(activeImageModelId = m.id) }
                                                            LocalAiManager.startImageModelDownload(context, m.id)
                                                        },
                                                        shape = PillShape,
                                                    ) {
                                                        Icon(Icons.Outlined.Download, null, Modifier.size(15.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Download")
                                                    }
                                                }
                                            }
                                        }
                                        if (st.status == LocalModelStatus.DOWNLOADING || st.status == LocalModelStatus.PAUSED) {
                                            LinearProgressIndicator(
                                                progress = { st.progress.coerceIn(0f, 1f) },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(PillShape),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Samsung Creative Studio Visual Style Cards (3D Cartoon, Watercolor, Sketch to Art, Pop Art, Anime, Minimal)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CreativeStyle.entries.forEach { cs ->
                        val selected = style == cs
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) {
                                Color(0xFF6366F1)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            },
                            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { style = cs }
                                .elasticStretch(),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(cs.badge, fontSize = 16.sp)
                                Text(cs.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Asset Type Tabs (Sticker / Cover / Page Template)
                PrimaryTabRow(selectedTabIndex = kind.ordinal, containerColor = Color.Transparent) {
                    AiAssetKind.entries.forEach { k ->
                        Tab(
                            selected = kind == k,
                            onClick = { kind = k },
                            text = { Text(k.label, fontWeight = FontWeight.SemiBold) },
                        )
                    }
                }

                // Preset Chips
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
                    label = { Text("Sketch / Prompt for ${kind.label} (${style.label})") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Color Palette Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Studio Palette:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0xFF6366F1, 0xFF3D6CF4, 0xFFEC4899, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF06B6D4, 0xFF1E293B).forEach { c ->
                            val ci = c.toInt()
                            Swatch(ci, accent == ci, 26) { accent = ci; generateNow() }
                        }
                    }
                }

                // Samsung Creative Studio Canvas Preview Stage
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(1.dp, OneUi9GradientBrush, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    preview?.let {
                        Image(
                            it.asImageBitmap(),
                            "Creative Studio Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(14.dp),
                        )
                    } ?: Text("Tap Generate to create in Creative Studio", style = MaterialTheme.typography.bodySmall)

                    // Style & Lite Mode Badge Overlay
                    Surface(
                        shape = PillShape,
                        color = Color.Black.copy(alpha = 0.62f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    ) {
                        Text(
                            "${style.badge} ${style.label} · ${if (settings.imageLiteMode) "⚡ Lite" else "HD"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { generateNow() },
                        shape = PillShape,
                        modifier = Modifier.weight(1f).elasticStretch(),
                    ) {
                        Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (busy) "Rendering…" else "Remix Style")
                    }
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
                                AiAssetKind.TEMPLATE -> "Apply Page Template"
                                AiAssetKind.COVER -> "Set Notebook Cover"
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

// ========================================================================= 4. Code-Based Live Collab Study Room

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
    val clip = LocalClipboardManager.current
    val cloudUser by CloudSync.user.collectAsState()
    var roomCode by remember { mutableStateOf(CloudSync.generateRoomCode()) }
    var scholarName by remember { mutableStateOf(cloudUser?.displayName.orEmpty().ifBlank { "Scholar" }) }
    var roomTitle by remember { mutableStateOf(vm?.title?.let { "$it Study Room" } ?: "Live Study Room") }
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
            delay(4000)
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp).widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 840.dp).statusBarsPadding(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(activeRoom?.title ?: "Live Study Room (Room Code)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            activeRoom?.let { "Room Code: ${it.roomId} · ${members.size} scholars active" }
                                ?: "Share a 6-digit Room Code to study & share notes live for free",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (activeRoom != null) {
                        OutlinedButton(
                            onClick = {
                                clip.setText(AnnotatedString(activeRoom!!.roomId))
                                statusMsg = "Copied Room Code ${activeRoom!!.roomId}!"
                            },
                            shape = PillShape,
                        ) {
                            Icon(Icons.Outlined.ContentCopy, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Code: ${activeRoom!!.roomId}")
                        }
                        IconButton(onClick = { refreshRoom(activeRoom!!.roomId) }) {
                            Icon(Icons.Outlined.Refresh, "Refresh")
                        }
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }

                if (activeRoom == null) {
                    // Instant 6-Digit Room Code Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("INSTANT STUDY ROOM CODE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(roomCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalButton(
                                        onClick = { roomCode = CloudSync.generateRoomCode() },
                                        shape = PillShape,
                                    ) {
                                        Text("🎲 New Code")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            clip.setText(AnnotatedString(roomCode))
                                            statusMsg = "Copied code $roomCode to clipboard!"
                                        },
                                        shape = PillShape,
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, null, Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Copy")
                                    }
                                }
                            }
                            Text(
                                "Give this code to a classmate or enter their code below to join the same live room immediately.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = roomCode,
                            onValueChange = { roomCode = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(16) },
                            label = { Text("Enter or Edit Room Code") },
                            placeholder = { Text("e.g. 482-910 or BIO-101") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = scholarName,
                            onValueChange = { scholarName = it },
                            label = { Text("Your Display Name") },
                            placeholder = { Text("Alex") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    OutlinedTextField(
                        value = roomTitle,
                        onValueChange = { roomTitle = it },
                        label = { Text("Study Topic / Room Title") },
                        placeholder = { Text("e.g. Midterm Review") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Quick Preset Room Codes
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("BIO-101", "CALC-202", "PHYS-301", "CHEM-LAB", "EXAM-PREP").forEach { preset ->
                            FilterChip(
                                selected = roomCode == preset,
                                onClick = { roomCode = preset },
                                label = { Text("Join #$preset") },
                                shape = PillShape,
                            )
                        }
                    }

                    if (statusMsg.isNotBlank()) {
                        Text(statusMsg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (cloudUser == null) {
                            OutlinedButton(
                                onClick = { runCatching { context.startActivity(CloudSync.buildGoogleAuthIntent()) } },
                                shape = PillShape,
                                modifier = Modifier.elasticStretch(),
                            ) {
                                Text("Google Sign-In (Optional)")
                            }
                        }
                        Button(
                            enabled = !busy && roomCode.length >= 3,
                            onClick = {
                                busy = true; statusMsg = ""
                                scope.launch {
                                    CloudSync.createOrJoinRoom(context, roomCode, roomTitle, scholarName).onSuccess {
                                        activeRoom = it
                                    }.onFailure {
                                        statusMsg = it.message ?: "Could not join room"
                                    }
                                    busy = false
                                }
                            },
                            shape = PillShape,
                            modifier = Modifier.weight(1f).elasticStretch(),
                        ) {
                            Text(if (busy) "Joining Room $roomCode…" else "Join / Start Room ($roomCode)")
                        }
                    }
                } else {
                    val rid = activeRoom!!.roomId
                    val activeUid = CloudSync.ensureRoomParticipant(context, scholarName).uid
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        members.forEach { m ->
                            Surface(shape = PillShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("🟢 ${m.displayName}", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
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
                                            statusMsg = "Shared Page ${vm.pageIndex + 1} to Room $rid"
                                            refreshRoom(rid)
                                        }.onFailure { statusMsg = it.message ?: "Share failed" }
                                    }
                                },
                                shape = PillShape,
                                modifier = Modifier.weight(1f).elasticStretch(),
                            ) { Text("📤 Share Current Page to Room") }
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
                            item { Text("SHARED NOTES IN #$rid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
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

                        item { Text("LIVE STUDY CHAT (#$rid)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                        items(msgs, key = { it.id }) { m ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (m.authorId == activeUid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
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
                            placeholder = { Text("Message room #$rid…") },
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
