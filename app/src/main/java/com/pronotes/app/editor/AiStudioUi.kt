package com.pronotes.app.editor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.CloudSync
import com.pronotes.app.data.CollabChatMsg
import com.pronotes.app.data.CollabMemberInfo
import com.pronotes.app.data.CollabRoomInfo
import com.pronotes.app.data.CollabSharedNote
import com.pronotes.app.data.Repository
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.ui.LiquidGlassCard
import com.pronotes.app.ui.LiquidGlassChip
import com.pronotes.app.ui.LiquidGlassDialogSurface
import com.pronotes.app.ui.LiquidGlassPill
import com.pronotes.app.ui.LiquidToggle
import com.pronotes.app.ui.LocalGlassTransparency
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.elasticStretch
import com.pronotes.app.ui.openLeafGlassColors
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.GlassLook
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.component.LiquidSegmentedControl
import io.github.ntvien.liquidglass.component.LiquidSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val PillShape = CircleShape

private val StudioGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2A78D6),
        Color(0xFF6366F1),
        Color(0xFF38BDF8),
    ),
)

private val OneUi9Purple = Color(0xFF8C65FF)

private val OneUi9GradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF66D7E6),
        Color(0xFF5FA8F8),
        Color(0xFF8C65FF),
        Color(0xFF9BECE4),
    ),
)

private val CreativeStageBackdropBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF66D7E6),
        Color(0xFF5EA8F5),
        Color(0xFF8A9AF8),
        Color(0xFF9BECE4),
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

    val aiEngine = remember(context) { com.pronotes.app.ai.ProNotesAiEngine(context) }

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
            val fullNotesText = pagesSnap.joinToString("\n\n") { (p, t) -> "Page ${p + 1}:\n$t" }
            val qLower = q.lowercase()
            val reply = when {
                qLower.contains("solve") && q.any { it.isDigit() || it == '=' } -> {
                    val eq = q.substringAfter("solve", q).trim()
                    val sol = aiEngine.solveMath(eq.ifBlank { q })
                    buildString {
                        appendLine("📐 **Symbolic Solution**: `${sol.result}`")
                        sol.steps.forEach { appendLine("• $it") }
                    }.trim()
                }
                qLower.contains("summarize") && fullNotesText.isNotBlank() -> {
                    val sum = aiEngine.summarize(fullNotesText, maxPoints = 6)
                    buildString {
                        appendLine("✨ **${sum.headline}**")
                        appendLine()
                        sum.bulletPoints.forEach { appendLine("• $it") }
                        if (sum.keyTerms.isNotEmpty()) {
                            appendLine()
                            appendLine("**Key Terms:** ${sum.keyTerms.joinToString(", ")}")
                        }
                    }.trim()
                }
                else -> {
                    val chatHistory = messages.map { (role, content) ->
                        com.pronotes.app.ai.ChatMessage(role = role, content = content)
                    }
                    aiEngine.chat(
                        query = q,
                        documentContext = fullNotesText,
                        history = chatHistory,
                    ).answer
                }
            }
            messages += "assistant" to reply
            busy = false
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiquidGlassDialogSurface(
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .heightIn(max = 820.dp)
                    .statusBarsPadding()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
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
                                LiquidGlassPill(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text(
                                        "CONTEXT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1),
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
                        LiquidGlassPill(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(
                                "📄 Current: Page ${vm.pageIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2A78D6),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        LiquidGlassPill(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(
                                "📚 ${vm.pages.size} Pages Indexed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8C65FF),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        LiquidGlassPill(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(
                                "✨ Direct Q&A + Equations",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0EA5E9),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                val quickPrompts = listOf(
                    "Explain Page ${vm.pageIndex + 1} simply",
                    "Summarize key concepts",
                    "Extract formulas & definitions",
                    "Quiz me with 3 questions",
                    "Solve equations step-by-step",
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
                                        LiquidGlassCard(
                                            shape = RoundedCornerShape(18.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                            modifier = Modifier.elasticStretch(),
                                            onClick = { send(qp) },
                                        ) {
                                            Row(
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
                                    LiquidGlassChip(
                                        selected = false,
                                        onClick = { send(qp) },
                                        label = qp,
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
                            LiquidSurface(
                                style = GlassDefaults.frost().copy(
                                    fallbackSurface = if (isUser) Color(0xFF2A78D6) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                                    contentColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                ),
                                shape = RoundedCornerShape(
                                    topStart = if (isUser) 22.dp else 6.dp,
                                    topEnd = if (isUser) 6.dp else 22.dp,
                                    bottomStart = 22.dp,
                                    bottomEnd = 22.dp,
                                ),
                                contentPadding = PaddingValues(14.dp),
                                modifier = Modifier.widthIn(max = 580.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                    )
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
                LiquidSurface(
                    style = GlassDefaults.frost(),
                    shape = PillShape,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            null,
                            tint = Color(0xFF6366F1),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A78D6)),
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
}

// ========================================================================= 2. Samsung Creative Studio (Drawing Assist + Lite Models + Pause/Resume)

@OptIn(ExperimentalLayoutApi::class)
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
    var style by remember { mutableStateOf(CreativeStyle.WATERCOLOR) }
    var showModelDrawer by remember { mutableStateOf(false) }
    var prompt by remember(initialKind) {
        mutableStateOf(
            when (initialKind) {
                AiAssetKind.STICKER -> "Cute Study Owl"
                AiAssetKind.COVER -> vm.title.ifBlank { "Botanical Emerald Journal" }
                AiAssetKind.TEMPLATE -> "Cornell Study Notes"
            },
        )
    }
    var variationNonce by remember { mutableIntStateOf(0) }
    var accent by remember { mutableIntStateOf(0xFF6366F1.toInt()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "creativeStudioShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    val generatingGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF66D7E6),
            Color(0xFF8C65FF),
            Color(0xFFEC4899),
            Color(0xFF38BDF8),
            Color(0xFF9BECE4),
            Color(0xFF66D7E6),
        ),
        start = Offset(shimmerOffset - 600f, shimmerOffset * 0.35f),
        end = Offset(shimmerOffset + 600f, shimmerOffset * 0.35f + 800f),
    )

    LaunchedEffect(Unit) {
        LocalAiManager.refresh(context)
    }

    fun generateNow(freshVariation: Boolean = false) {
        if (busy) return
        if (freshVariation) {
            variationNonce = kotlin.random.Random.nextInt(1, 9999999)
        }
        val nonceSnap = variationNonce
        busy = true
        scope.launch {
            preview = LocalAiManager.generateAiAssetBitmap(
                context = context,
                kind = kind,
                prompt = prompt,
                accentArgb = accent,
                style = style,
                liteMode = settings.imageLiteMode,
                variationNonce = nonceSnap,
            )
            busy = false
        }
    }

    LaunchedEffect(kind, style, settings.imageLiteMode, settings.activeImageModelId) {
        variationNonce = 0
        generateNow(false)
    }

    val presets = when (kind) {
        AiAssetKind.STICKER -> listOf("Cute Study Owl", "Physics Atom Spark", "Gold Exam Star", "Coffee Study Cat", "DNA Helix Badge")
        AiAssetKind.COVER -> listOf("Botanical Emerald Journal", "Deep Space Nebula", "Minimalist Bauhaus", "Calm Pastel Waves")
        AiAssetKind.TEMPLATE -> listOf("Cornell Notes", "Weekly Planner", "14-Day Habit Tracker", "Storyboard Grid", "Exam Revision Sheet")
    }
    val aspectLabel = when (kind) {
        AiAssetKind.STICKER -> "1:1"
        AiAssetKind.COVER -> "3:4"
        AiAssetKind.TEMPLATE -> "A4"
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiquidGlassDialogSurface(
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .heightIn(max = 880.dp)
                    .statusBarsPadding()
                    .border(1.2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(32.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            ) {
                Box(Modifier.background(CreativeStageBackdropBrush)) {
                    Column(
                        Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Sleek One UI 9 Creative Studio Top Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            LiquidSurface(
                                style = GlassDefaults.frost().copy(fallbackSurface = Color.White.copy(alpha = 0.24f), contentColor = Color.White),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                onClick = onClose,
                            ) {
                                Text("✕", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Creative Studio",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                )
                                Text(
                                    "${kind.label} · ${LocalAiManager.activeImageModelLabel(context)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            // Aspect badge matching studio screenshot (1:1 / 3:4 / A4)
                            LiquidSurface(
                                style = GlassDefaults.frost().copy(fallbackSurface = Color.White.copy(alpha = 0.22f), contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
                                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            ) {
                                Text(
                                    aspectLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                            // Lite Mode toggle chip
                            LiquidSurface(
                                style = GlassDefaults.frost().copy(
                                    fallbackSurface = if (settings.imageLiteMode) Color.White else Color.White.copy(alpha = 0.24f),
                                    contentColor = if (settings.imageLiteMode) OneUi9Purple else Color.White,
                                ),
                                shape = PillShape,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.elasticStretch(),
                                onClick = { settingsStore.update { it.copy(imageLiteMode = !it.imageLiteMode) } },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Bolt,
                                        null,
                                        tint = if (settings.imageLiteMode) OneUi9Purple else Color.White,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        if (settings.imageLiteMode) "Lite" else "HD",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (settings.imageLiteMode) OneUi9Purple else Color.White,
                                    )
                                }
                            }
                        }

                        // Asset Type Selector Pill Bar (Sticker / Cover / Template)
                        LiquidSegmentedControl(
                            options = AiAssetKind.entries.map { it.label },
                            selectedIndex = AiAssetKind.entries.indexOf(kind),
                            onSelect = { kind = AiAssetKind.entries[it] },
                            indicatorColor = Color.White,
                            selectedTextColor = Color(0xFF1E293B),
                            textColor = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                        )

                // Center Frosted Squircle Preview Stage (matching One UI Creative Studio reference)
                val centerStageGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD946EF).copy(alpha = 0.42f),
                        Color(0xFF8C65FF).copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.14f),
                    ),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (busy) generatingGradient else centerStageGlow)
                        .border(1.2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(20.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.26f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Text(
                                "Creating your ${kind.label.lowercase()}…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                "\"$prompt\" · ${style.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.width(180.dp).height(5.dp).clip(PillShape),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.35f),
                            )
                        }
                    } else if (preview != null) {
                        Image(
                            preview!!.asImageBitmap(),
                            "Creative Studio Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(28.dp),
                        ) {
                            Text(
                                "Imagine it, create it. Any image, instantly.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }

                    // Bottom-left style badge pill (matching studio reference card)
                    LiquidSurface(
                        style = GlassDefaults.frost().copy(fallbackSurface = Color.Black.copy(alpha = 0.50f), contentColor = Color.White),
                        shape = PillShape,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(style.badge, fontSize = 13.sp)
                            Text(
                                style.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    // Bottom-right Lite Models toggle pill
                    LiquidSurface(
                        style = GlassDefaults.frost().copy(fallbackSurface = Color.White.copy(alpha = 0.26f), contentColor = Color.White),
                        shape = PillShape,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                        onClick = { showModelDrawer = !showModelDrawer },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Outlined.Download, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                if (showModelDrawer) "Hide Models" else "Models",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }

                // Inline Lite Image Models Manager with Pause / Resume / Cancel
                if (showModelDrawer) {
                    LiquidGlassDialogSurface(
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Lite Image Models · Pause & Resume",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Lite", style = MaterialTheme.typography.labelSmall)
                                    LiquidToggle(
                                        selected = { settings.imageLiteMode },
                                        onSelect = { v -> settingsStore.update { it.copy(imageLiteMode = v) } },
                                    )
                                }
                            }
                            LocalAiManager.imageCatalog.forEach { m ->
                                val st = imgStates[m.id] ?: LocalModelRuntime()
                                val isActive = settings.activeImageModelId == m.id
                                LiquidGlassCard(
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(10.dp),
                                    modifier = Modifier.fillMaxWidth().let {
                                        if (isActive) it.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else it
                                    },
                                    onClick = { settingsStore.update { it.copy(activeImageModelId = m.id) } },
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text("${m.name} (${m.sizeLabel})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                                Text(m.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            when (st.status) {
                                                LocalModelStatus.READY -> {
                                                    Button(onClick = { settingsStore.update { it.copy(activeImageModelId = m.id) } }, shape = PillShape) {
                                                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(15.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(if (isActive) "Active" else "Use")
                                                    }
                                                }
                                                LocalModelStatus.DOWNLOADING -> {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        FilledTonalButton(onClick = { LocalAiManager.pauseImageModelDownload(context, m.id) }, shape = PillShape) {
                                                            Icon(Icons.Outlined.Pause, null, Modifier.size(15.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Pause")
                                                        }
                                                        TextButton(onClick = { LocalAiManager.cancelImageModelDownload(context, m.id) }) { Text("Cancel") }
                                                    }
                                                }
                                                LocalModelStatus.PAUSED -> {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Button(onClick = { LocalAiManager.startImageModelDownload(context, m.id) }, shape = PillShape) {
                                                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(15.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Resume ${(st.progress * 100).toInt()}%")
                                                        }
                                                        TextButton(onClick = { LocalAiManager.cancelImageModelDownload(context, m.id) }) { Text("Cancel") }
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

                // Style Chips Strip
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CreativeStyle.entries.forEach { cs ->
                        val selected = style == cs
                        LiquidSurface(
                            style = GlassDefaults.frost().copy(
                                fallbackSurface = if (selected) Color.White else Color.Black.copy(alpha = 0.28f),
                                contentColor = if (selected) Color(0xFF1E293B) else Color.White,
                            ),
                            shape = PillShape,
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                            modifier = Modifier.elasticStretch(),
                            onClick = { style = cs },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(cs.badge, fontSize = 15.sp)
                                Text(
                                    cs.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF1E293B) else Color.White,
                                )
                            }
                        }
                    }
                }

                // Inspiration Chips + Accent Palette
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    presets.forEach { p ->
                        val sel = prompt == p
                        LiquidSurface(
                            style = GlassDefaults.frost().copy(
                                fallbackSurface = if (sel) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.22f),
                                contentColor = if (sel) Color(0xFF1E293B) else Color.White,
                            ),
                            shape = PillShape,
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                            onClick = {
                                prompt = p
                                generateNow(true)
                            },
                        ) {
                            Text(
                                p,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (sel) Color(0xFF1E293B) else Color.White,
                            )
                        }
                    }
                }

                // Translucent Dark Prompt Box (matching uploaded screenshot bottom composer)
                LiquidSurface(
                    style = GlassDefaults.frost().copy(fallbackSurface = Color.Black.copy(alpha = 0.34f), contentColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            placeholder = {
                                Text(
                                    "Describe what you want to create",
                                    color = Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = Color.White,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (prompt.isNotBlank() && !busy) generateNow(true) }),
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { generateNow(true) },
                            enabled = !busy && prompt.isNotBlank(),
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1E293B),
                            ),
                            modifier = Modifier.height(44.dp).elasticStretch(),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OneUi9Purple, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = OneUi9Purple)
                                Spacer(Modifier.width(5.dp))
                                Text("Generate", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Bottom Action Pill Bar: Regenerate (always fresh variation) + Primary Apply CTA
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        enabled = !busy,
                        onClick = { generateNow(true) },
                        shape = PillShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.28f),
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f).height(48.dp).elasticStretch(),
                    ) {
                        Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (busy) "Creating…" else "Regenerate")
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.weight(1.3f).height(48.dp).elasticStretch(),
                    ) {
                        Text(
                            when (kind) {
                                AiAssetKind.STICKER -> "Insert Sticker"
                                AiAssetKind.TEMPLATE -> "Apply Template"
                                AiAssetKind.COVER -> "Set Cover"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    }
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
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.36f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiquidGlassDialogSurface(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(max = 800.dp).statusBarsPadding()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
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

                    LiquidGlassCard(
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    LiquidGlassCard(
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.36f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            LiquidGlassDialogSurface(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 840.dp).statusBarsPadding()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
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
                        LiquidGlassCard(
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            LiquidGlassChip(
                                selected = roomCode == preset,
                                onClick = { roomCode = preset },
                                label = "Join #$preset",
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
                            LiquidGlassPill(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("🟢 ${m.displayName}", style = MaterialTheme.typography.labelSmall)
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
                                LiquidGlassCard(
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            LiquidSurface(
                                style = GlassDefaults.frost().copy(
                                    fallbackSurface = if (m.authorId == activeUid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column {
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
}
