package com.pronotes.app.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pronotes.app.data.AccentColor
import com.pronotes.app.data.SlideCard
import com.pronotes.app.data.SlideKind
import com.pronotes.app.data.SlideTheme
import com.pronotes.app.ui.LiquidGlassCard
import com.pronotes.app.ui.LiquidGlassChip
import com.pronotes.app.ui.LiquidGlassDialog
import com.pronotes.app.ui.LiquidGlassDialogSurface
import com.pronotes.app.ui.LocalGlassTransparency
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.openLeafGlassColors
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.GlassLook
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.component.LiquidSurface
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun SlideTheme.colors(): List<Color> = when (this) {
    SlideTheme.AURORA_INDIGO -> listOf(Color(gradA), Color(gradB), Color(accent))
    SlideTheme.SUNSET_CORAL -> listOf(Color(gradA), Color(gradB), Color(accent))
    SlideTheme.EMERALD_MINT -> listOf(Color(gradA), Color(gradB), Color(accent))
    SlideTheme.COSMIC_GOLD -> listOf(Color(gradA), Color(gradB), Color(accent))
    SlideTheme.MIDNIGHT_PRISM -> listOf(Color(gradA), Color(gradB), Color(accent))
    SlideTheme.ROSE_VELVET -> listOf(Color(gradA), Color(gradB), Color(accent))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlideShowDialog(vm: EditorViewModel, onClose: () -> Unit) {
    var presenting by remember { mutableStateOf(false) }
    var editingSlide by remember { mutableStateOf<SlideCard?>(null) }
    var scopeMode by remember { mutableIntStateOf(0) } // 0 = Current Page, 1 = Chapter Range, 2 = Full Notebook
    var startPage by remember(vm.pages.size) { mutableIntStateOf((vm.pageIndex + 1).coerceAtLeast(1)) }
    var endPage by remember(vm.pages.size) { mutableIntStateOf((vm.pageIndex + 3).coerceIn(1, vm.pages.size.coerceAtLeast(1))) }
    var slideCount by remember { mutableIntStateOf(7) }

    val studioScheme = MaterialTheme.colorScheme.copy(
        background = Color(0xFF070B18),
        surface = Color(0xFF0F162B),
        surfaceVariant = Color(0xFF182240),
        onSurface = Color(0xFFF8FAFC),
        onSurfaceVariant = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFF334155),
        primary = Color(0xFF38BDF8),
        onPrimary = Color(0xFF041224),
        primaryContainer = Color(0xFF1E3A8A),
        onPrimaryContainer = Color(0xFFE0F2FE),
    )

    val transparency = LocalGlassTransparency.current
    val cosmicGlass = remember(transparency) { openLeafGlassColors(dark = true, accent = AccentColor.BLUE, transparency = transparency) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        if (presenting && vm.slides.isNotEmpty()) {
            SlideShowPresenter(
                slides = vm.slides,
                deckTitle = vm.title,
                onExit = { presenting = false },
            )
        } else {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.44f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                LiquidGlassTheme(colors = cosmicGlass, look = GlassLook.Auto) {
                    LiquidGlassDialogSurface(
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.widthIn(max = 820.dp).fillMaxWidth().heightIn(max = 860.dp).statusBarsPadding()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
                    ) {
                        MaterialTheme(colorScheme = studioScheme) {
                            Box(Modifier.background(Color(0xD9070B18))) {
                                AiParticleBackdrop(
                                    modifier = Modifier.matchParentSize(),
                                    accentColors = listOf(Color(0xFF38BDF8), Color(0xFFA855F7)),
                                    active = vm.aiBusy,
                                    variant = if (vm.aiBusy) AiBackdropVariant.AI_WAVE else AiBackdropVariant.SPACE_NEBULA,
                                )
                                Column(Modifier.padding(20.dp)) {
                                    // Header
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(48.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFFEC4899)))),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            ProIconView(ProIcon.SLIDESHOW, Color.White, Modifier.size(26.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Slide Show Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${vm.slides.size} slides · Quick Chapter Revision · ${vm.aiEngineLabel}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFF93C5FD),
                                            )
                                        }
                                        TextButton(onClick = onClose) { Text("Close", color = Color.White) }
                                    }

                                    Spacer(Modifier.height(14.dp))

                                    // Generation Source & Controls Card
                                    LiquidSurface(
                                        style = GlassDefaults.frost().copy(fallbackSurface = Color(0xAA111A33)),
                                        shape = RoundedCornerShape(22.dp),
                                        contentPadding = PaddingValues(14.dp),
                                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x3360A5FA), RoundedCornerShape(22.dp)),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("AI CHAPTER REVISION GENERATOR", style = MaterialTheme.typography.labelSmall, color = Color(0xFF93C5FD))
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    listOf(5, 7, 10, 14).forEach { n ->
                                                        LiquidGlassChip(
                                                            selected = slideCount == n,
                                                            onClick = { slideCount = n },
                                                            label = "$n slides",
                                                        )
                                                    }
                                                }
                                            }

                                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                LiquidGlassChip(scopeMode == 0, { scopeMode = 0 }, "Current Page (${vm.pageIndex + 1})")
                                                LiquidGlassChip(scopeMode == 1, { scopeMode = 1 }, "Chapter Range")
                                                LiquidGlassChip(scopeMode == 2, { scopeMode = 2 }, "Whole Notebook (${vm.pages.size}p)")
                                            }

                                    AnimatedVisibility(visible = scopeMode == 1) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("Pages:", style = MaterialTheme.typography.labelMedium)
                                            OutlinedButton(
                                                onClick = { startPage = (startPage - 1).coerceAtLeast(1) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = CircleShape,
                                            ) { Text("-") }
                                            Text("$startPage", fontWeight = FontWeight.Bold)
                                            OutlinedButton(
                                                onClick = { startPage = (startPage + 1).coerceAtMost(vm.pages.size.coerceAtLeast(1)) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = CircleShape,
                                            ) { Text("+") }
                                            Text("to")
                                            OutlinedButton(
                                                onClick = { endPage = (endPage - 1).coerceAtLeast(startPage) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = CircleShape,
                                            ) { Text("-") }
                                            Text("${endPage.coerceAtLeast(startPage)}", fontWeight = FontWeight.Bold)
                                            OutlinedButton(
                                                onClick = { endPage = (endPage + 1).coerceAtMost(vm.pages.size.coerceAtLeast(1)) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = CircleShape,
                                            ) { Text("+") }
                                        }
                                    }

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                val range = when (scopeMode) {
                                                    0 -> vm.pageIndex to vm.pageIndex
                                                    1 -> (startPage - 1) to (endPage.coerceAtLeast(startPage) - 1)
                                                    else -> 0 to (vm.pages.size - 1).coerceAtLeast(0)
                                                }
                                                vm.generateSlideShow(range.first, range.second, slideCount)
                                            },
                                            enabled = !vm.aiBusy,
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color(0xFF041224)),
                                            modifier = Modifier.weight(1f).height(48.dp),
                                        ) {
                                            Text("✦ Generate Deck (${slideCount} Slides)", fontWeight = FontWeight.Bold)
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                val theme = SlideTheme.entries[vm.slides.size % SlideTheme.entries.size]
                                                editingSlide = SlideCard(
                                                    id = "sl-${UUID.randomUUID()}",
                                                    kind = SlideKind.CONCEPT,
                                                    theme = theme,
                                                    title = "",
                                                    badge = "CONCEPT",
                                                )
                                            },
                                            shape = CircleShape,
                                            modifier = Modifier.height(48.dp),
                                        ) {
                                            Text("+ Manual Slide")
                                        }
                                    }

                                    if (vm.aiBusy) {
                                        AiScanBanner(vm.aiStage, vm.aiEngineLabel, listOf(Color(0xFF38BDF8), Color(0xFFA855F7)))
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("REVISION DECK (${vm.slides.size})", style = MaterialTheme.typography.labelSmall, color = Color(0xFF93C5FD))
                                Button(
                                    onClick = { presenting = true },
                                    enabled = vm.slides.isNotEmpty(),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899), contentColor = Color.White),
                                ) {
                                    Text("▶  Present Slideshow", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            if (vm.slides.isEmpty()) {
                                LiquidSurface(
                                    style = GlassDefaults.frost().copy(fallbackSurface = Color(0x881E293B)),
                                    shape = RoundedCornerShape(22.dp),
                                    contentPadding = PaddingValues(28.dp),
                                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                                ) {
                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("✨", fontSize = 34.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("No slides yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Tap \"Generate Deck\" to turn your notes into colorful animated chapter revision slides, or build your own manually.",
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    itemsIndexed(vm.slides, key = { _, s -> s.id }) { idx, slide ->
                                        val cols = slide.theme.colors()
                                        LiquidGlassCard(
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(12.dp),
                                            modifier = Modifier
                                                .animateItem()
                                                .fillMaxWidth()
                                                .border(1.dp, cols.first().copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
                                            onClick = { editingSlide = slide },
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    Modifier
                                                        .size(width = 72.dp, height = 54.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(Brush.linearGradient(cols)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("${idx + 1}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                                        Text(slide.kind.label, color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, maxLines = 1)
                                                    }
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        slide.title.ifBlank { "Untitled Slide" },
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    val preview = when {
                                                        slide.kind == SlideKind.CHECK_QA && slide.question.isNotBlank() -> slide.question
                                                        slide.bullets.isNotEmpty() -> slide.bullets.joinToString(" · ")
                                                        slide.callout.isNotBlank() -> slide.callout
                                                        else -> slide.subtitle
                                                    }
                                                    if (preview.isNotBlank()) {
                                                        Text(
                                                            preview,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = { vm.moveSlide(idx, idx - 1) }, enabled = idx > 0) {
                                                        Text("↑", color = if (idx > 0) Color.White else Color.Gray)
                                                    }
                                                    IconButton(onClick = { vm.moveSlide(idx, idx + 1) }, enabled = idx < vm.slides.lastIndex) {
                                                        Text("↓", color = if (idx < vm.slides.lastIndex) Color.White else Color.Gray)
                                                    }
                                                    IconButton(onClick = { vm.deleteSlide(slide.id) }) {
                                                        Text("✕", color = Color(0xFFF87171))
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
            }
        }
    }
}
}

    editingSlide?.let { target ->
        SlideCardEditorDialog(
            initial = target,
            isExisting = vm.slides.any { it.id == target.id },
            onDismiss = { editingSlide = null },
            onDelete = { vm.deleteSlide(target.id); editingSlide = null },
            onSave = { vm.saveSlide(it); editingSlide = null },
        )
    }
}

@Composable
private fun SlideShowPresenter(
    slides: List<SlideCard>,
    deckTitle: String,
    onExit: () -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    var autoSec by remember { mutableIntStateOf(0) } // 0 = manual, 5 / 8 / 12
    var revealAnswer by remember(index) { mutableStateOf(false) }

    val safeIndex = index.coerceIn(0, (slides.size - 1).coerceAtLeast(0))
    val current = slides[safeIndex]
    val cols = current.theme.colors()

    LaunchedEffect(safeIndex, autoSec, slides.size) {
        if (autoSec > 0 && slides.size > 1) {
            delay(autoSec * 1000L)
            index = (safeIndex + 1) % slides.size
        }
    }

    val t = rememberInfiniteTransition(label = "presenter-bg")
    val drift by t.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )

    val transparency = LocalGlassTransparency.current
    val presenterGlass = remember(transparency) { openLeafGlassColors(dark = true, accent = AccentColor.BLUE, transparency = transparency) }

    LiquidGlassTheme(colors = presenterGlass, look = GlassLook.Auto) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF040711)),
        ) {
            // Vivid animated bokeh gradient canvas
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.linearGradient(cols.map { it.copy(alpha = 0.85f) }))
                val w = size.width
                val h = size.height
                val minDim = minOf(w, h)
                val orbs = listOf(
                    Triple(0.22f, 0.25f, Color.White.copy(alpha = 0.14f)),
                    Triple(0.82f, 0.28f, cols.last().copy(alpha = 0.36f)),
                    Triple(0.65f, 0.80f, Color(0xFFFDE047).copy(alpha = 0.15f)),
                    Triple(0.18f, 0.78f, Color(0xFF38BDF8).copy(alpha = 0.22f)),
                )
                orbs.forEachIndexed { idx, (fx, fy, col) ->
                    val cx = w * fx + cos(drift + idx * 1.4f) * (minDim * 0.08f)
                    val cy = h * fy + sin(drift * 0.8f + idx * 1.1f) * (minDim * 0.08f)
                    val r = minDim * (0.34f + idx * 0.06f)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(col, Color.Transparent), center = Offset(cx, cy), radius = r),
                        radius = r,
                        center = Offset(cx, cy),
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                // Segmented animated progress strip
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    slides.forEachIndexed { i, _ ->
                        val filled = i <= safeIndex
                        Box(
                            Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(if (filled) Color.White else Color.White.copy(alpha = 0.24f))
                                .clickable { index = i },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Top controls row
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(deckTitle, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Slide ${safeIndex + 1} of ${slides.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(0 to "Manual", 6 to "6s Auto", 10 to "10s Auto").forEach { (sec, label) ->
                            com.pronotes.app.ui.LiquidGlassPill(
                                selected = autoSec == sec,
                                onClick = { autoSec = sec },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    label,
                                    color = if (autoSec == sec) presenterGlass.onAccent else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        com.pronotes.app.ui.LiquidGlassPill(
                            onClick = onExit,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

            Spacer(Modifier.height(16.dp))

            // Animated kinetic slide stage
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = safeIndex,
                    transitionSpec = {
                        val dir = if (targetState >= initialState) 1 else -1
                        (slideInHorizontally(spring(dampingRatio = 0.78f, stiffness = 360f)) { it * dir / 2 } +
                            scaleIn(spring(dampingRatio = 0.8f), initialScale = 0.92f) +
                            fadeIn(tween(240))) togetherWith
                            (slideOutHorizontally(tween(220)) { -it * dir / 2 } +
                                scaleOut(tween(220), targetScale = 0.94f) +
                                fadeOut(tween(180)))
                    },
                    label = "slide-stage",
                ) { targetIdx ->
                    val slide = slides[targetIdx]
                    PresenterSlideCard(
                        slide = slide,
                        revealAnswer = revealAnswer,
                        onToggleReveal = { revealAnswer = !revealAnswer },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Bottom Navigation Dock
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = { if (safeIndex > 0) index = safeIndex - 1 },
                    enabled = safeIndex > 0,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                    ),
                ) { Text("← Previous", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) }

                if (current.kind == SlideKind.CHECK_QA) {
                    Button(
                        onClick = { revealAnswer = !revealAnswer },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDE047), contentColor = Color(0xFF0F172A)),
                    ) {
                        Text(if (revealAnswer) "Hide Answer" else "✦ Reveal Answer", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (safeIndex < slides.lastIndex) index = safeIndex + 1 else onExit()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A)),
                ) {
                    Text(
                        if (safeIndex < slides.lastIndex) "Next →" else "Finish ✓",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun PresenterSlideCard(
    slide: SlideCard,
    revealAnswer: Boolean,
    onToggleReveal: () -> Unit,
) {
    LiquidSurface(
        style = GlassDefaults.frost().copy(fallbackSurface = Color(0xBB090D1A)),
        shape = RoundedCornerShape(30.dp),
        contentPadding = PaddingValues(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .heightIn(min = 320.dp, max = 600.dp)
            .border(1.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(30.dp)),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Badge + Kind Chip
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.pronotes.app.ui.LiquidGlassPill(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        slide.badge.ifBlank { slide.kind.label.uppercase() },
                        color = Color(0xFFFDE047),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text(slide.kind.label, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }

            // Title & Subtitle
            Text(
                slide.title.ifBlank { "Chapter Insight" },
                color = Color.White,
                fontSize = if (slide.kind == SlideKind.TITLE) 32.sp else 26.sp,
                lineHeight = if (slide.kind == SlideKind.TITLE) 38.sp else 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            if (slide.subtitle.isNotBlank()) {
                Text(slide.subtitle, color = Color(0xFFE2E8F0), fontSize = 16.sp, lineHeight = 23.sp)
            }

            // Formula / Callout card
            if (slide.callout.isNotBlank()) {
                LiquidSurface(
                    style = GlassDefaults.frost().copy(fallbackSurface = Color(0x4438BDF8)),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x7738BDF8), RoundedCornerShape(18.dp)),
                ) {
                    Text(
                        slide.callout,
                        color = Color(0xFFE0F2FE),
                        fontFamily = if (slide.kind == SlideKind.FORMULA) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Staggered kinetic bullets
            if (slide.bullets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slide.bullets.forEachIndexed { idx, bullet ->
                        val anim = remember(slide.id, idx) { Animatable(0f) }
                        LaunchedEffect(slide.id, idx) {
                            delay(idx * 90L)
                            anim.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 360f))
                        }
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.graphicsLayer {
                                alpha = anim.value
                                translationX = (1f - anim.value) * 24f
                            },
                        ) {
                            Box(
                                Modifier
                                    .padding(top = 5.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.28f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${idx + 1}", color = Color(0xFF7DD3FC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(bullet, color = Color.White, fontSize = 17.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Interactive Q&A flip reveal
            if (slide.kind == SlideKind.CHECK_QA && slide.question.isNotBlank()) {
                val rot by animateFloatAsState(if (revealAnswer) 180f else 0f, spring(dampingRatio = 0.8f), label = "qa")
                LiquidSurface(
                    style = GlassDefaults.frost().copy(fallbackSurface = if (rot <= 90f) Color(0x336366F1) else Color(0x4410B981)),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp)
                        .graphicsLayer { rotationY = rot; cameraDistance = 14f * density }
                        .border(1.dp, if (rot <= 90f) Color(0x88818CF8) else Color(0xAA34D399), RoundedCornerShape(22.dp))
                        .clickable { onToggleReveal() },
                ) {
                    Box(
                        Modifier.padding(20.dp).graphicsLayer { if (rot > 90f) rotationY = 180f },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (rot <= 90f) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("QUICK CHECK · TAP TO REVEAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE))
                                Text(slide.question, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ANSWER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                                Text(slide.answer.ifBlank { "Tap to flip back" }, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlideCardEditorDialog(
    initial: SlideCard,
    isExisting: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (SlideCard) -> Unit,
) {
    var kind by remember { mutableStateOf(initial.kind) }
    var theme by remember { mutableStateOf(initial.theme) }
    var title by remember { mutableStateOf(initial.title) }
    var subtitle by remember { mutableStateOf(initial.subtitle) }
    var bulletsRaw by remember { mutableStateOf(initial.bullets.joinToString("\n")) }
    var callout by remember { mutableStateOf(initial.callout) }
    var question by remember { mutableStateOf(initial.question) }
    var answer by remember { mutableStateOf(initial.answer) }

    LiquidGlassDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isExisting) "Edit Slide" else "New Revision Slide") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SLIDE TYPE", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SlideKind.entries.forEach { k ->
                        LiquidGlassChip(selected = kind == k, onClick = { kind = k }, label = k.label)
                    }
                }
                Text("COLOR THEME", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SlideTheme.entries.forEach { th ->
                        val cols = th.colors()
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(cols))
                                .border(if (theme == th) 3.dp else 1.dp, if (theme == th) Color.White else Color.Gray.copy(alpha = 0.4f), CircleShape)
                                .clickable { theme = th },
                        )
                    }
                }
                OutlinedTextField(title, { title = it }, label = { Text("Slide Title") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(subtitle, { subtitle = it }, label = { Text("Subtitle (optional)") }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(callout, { callout = it }, label = { Text("Formula / Key Highlight") }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    bulletsRaw,
                    { bulletsRaw = it },
                    label = { Text("Key Bullets (one per line)") },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (kind == SlideKind.CHECK_QA) {
                    OutlinedTextField(question, { question = it }, label = { Text("Question") }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(answer, { answer = it }, label = { Text("Answer") }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bullets = bulletsRaw.lines().map { it.trim().removePrefix("•").trim() }.filter { it.isNotEmpty() }
                    onSave(
                        initial.copy(
                            kind = kind,
                            theme = theme,
                            title = title.trim(),
                            subtitle = subtitle.trim(),
                            bullets = bullets,
                            callout = callout.trim(),
                            question = question.trim(),
                            answer = answer.trim(),
                            badge = kind.label.uppercase(),
                        ),
                    )
                },
                enabled = title.isNotBlank() || bulletsRaw.isNotBlank() || question.isNotBlank(),
                shape = CircleShape,
            ) { Text("Save Slide") }
        },
        dismissButton = {
            Row {
                if (isExisting) TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
