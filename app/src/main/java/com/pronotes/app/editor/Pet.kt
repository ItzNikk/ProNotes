package com.pronotes.app.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CompanionMeta(
    val id: String,
    val name: String,
    val element: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val quotes: List<String>,
)

val Companions: List<CompanionMeta> = listOf(
    CompanionMeta(
        id = "sparkchu", name = "Sparkchu", element = "Electric",
        primary = Color(0xFFFFD644), secondary = Color(0xFFF5A623), accent = Color(0xFFFF4D4D),
        quotes = listOf("Pika-focus! ⚡", "High voltage brain!", "Shockingly good notes!", "Charge up & solve! ⚡"),
    ),
    CompanionMeta(
        id = "pip", name = "Pip", element = "Dream",
        primary = Color(0xFFC9A4FF), secondary = Color(0xFF8B6CF0), accent = Color(0xFFFF8FB1),
        quotes = listOf("You've got this!", "Step by step ✨", "Proud of your focus!", "Nice rhythm today!"),
    ),
    CompanionMeta(
        id = "emberkit", name = "Emberkit", element = "Fire",
        primary = Color(0xFFFF8A4C), secondary = Color(0xFFE5484D), accent = Color(0xFFFFE066),
        quotes = listOf("On fire today! 🔥", "Blaze through that chapter!", "Warm up that streak!", "Keep the spark alive!"),
    ),
    CompanionMeta(
        id = "aquapup", name = "Aquapup", element = "Water",
        primary = Color(0xFF66D1FF), secondary = Color(0xFF2A78D6), accent = Color(0xFFCFF4FF),
        quotes = listOf("Smooth like water 💧", "Take a sip of water!", "Flow state unlocked!", "Calm & clear mind 🌊"),
    ),
    CompanionMeta(
        id = "leafsprout", name = "Leafsprout", element = "Leaf",
        primary = Color(0xFF7AE582), secondary = Color(0xFF1BAF7A), accent = Color(0xFFD8F5A2),
        quotes = listOf("Growing smarter! 🌱", "Fresh ideas blooming!", "Deep roots, sharp notes 🌿", "Nature loves curiosity!"),
    ),
    CompanionMeta(
        id = "eevo", name = "Eevo", element = "Adapt",
        primary = Color(0xFFE3B178), secondary = Color(0xFFB57C4A), accent = Color(0xFFFFF1D6),
        quotes = listOf("Adapt & master anything!", "Evolving every page! ✨", "Fluffy focus buddy!", "Ready for any quiz!"),
    ),
    CompanionMeta(
        id = "snorblob", name = "Snorblob", element = "Zen",
        primary = Color(0xFF4CA6B8), secondary = Color(0xFF275C70), accent = Color(0xFFF9EED2),
        quotes = listOf("Steady & calm 😌", "Deep breaths, slow & steady", "Big brain power...", "No rush, you got it!"),
    ),
    CompanionMeta(
        id = "genglet", name = "Genglet", element = "Shadow",
        primary = Color(0xFF8E63D9), secondary = Color(0xFF4C2C85), accent = Color(0xFFFF5C8A),
        quotes = listOf("Hehe! Mastered that! 👾", "Secret genius mode!", "Outsmarting the exam!", "Spooky fast solving!"),
    ),
)

fun companionById(id: String): CompanionMeta = Companions.firstOrNull { it.id == id } ?: Companions.first()

/** Animated AI status pill with orbital glow, diagonal shimmer sweep, live stage & model badge. */
@Composable
fun ThinkingPill(engine: String = "", stage: String = "") {
    val t = rememberInfiniteTransition(label = "think")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1050, easing = LinearEasing)), label = "p")
    val shine by t.animateFloat(-0.6f, 1.8f, infiniteRepeatable(tween(1550, easing = LinearEasing)), label = "s")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2100, easing = LinearEasing)), label = "spin")
    val pulse by t.animateFloat(0.8f, 1.08f, infiniteRepeatable(tween(820, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    Surface(
        shape = CircleShape,
        color = Color(0xF5151926),
        shadowElevation = 12.dp,
        modifier = Modifier.border(1.dp, Color(0x55B45CFF), CircleShape),
    ) {
        Box(Modifier.clip(CircleShape)) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color(0x48B45CFF), Color(0x405AC8FA), Color.Transparent),
                        Offset(size.width * shine - 80f, 0f),
                        Offset(size.width * shine + 80f, size.height),
                    ),
                )
            }
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(22.dp).graphicsLayer { rotationZ = spin; scaleX = pulse; scaleY = pulse }) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(Color(0xFFE38BFF), Color(0xFF5AC8FA), Color(0xFF1BAF7A), Color(0xFFE38BFF))),
                        radius = r - 2f,
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawCircle(Color(0xFFE38BFF), radius = 2.8.dp.toPx(), center = center)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stage.ifBlank { "Thinking" },
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.width(6.dp))
                        for (i in 0 until 3) {
                            val k = ((phase * 3f - i) % 3f + 3f) % 3f
                            val up = if (k < 1f) kotlin.math.sin(k * Math.PI).toFloat() else 0f
                            Box(Modifier.padding(horizontal = 2.dp).size(5.dp).graphicsLayer { translationY = -up * 6.dp.toPx() }) {
                                Canvas(Modifier.size(5.dp)) { drawCircle(if (i % 2 == 0) Color(0xFFE38BFF) else Color(0xFF74C0FC)) }
                            }
                        }
                    }
                    if (engine.isNotBlank()) {
                        Text(engine, color = Color(0xFFAEC0D8), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyPet(vm: EditorViewModel, modifier: Modifier, compact: Boolean = false) {
    val settings by vm.settingsFlow.collectAsState()
    val pet = companionById(settings.petId)
    val level = 1 + settings.petXp / 100
    val levelProgress = (settings.petXp % 100) / 100f

    val t = rememberInfiniteTransition(label = "pet")
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val aiSpin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "ai")
    var blink by remember { mutableStateOf(false) }
    var say by remember { mutableStateOf<String?>(null) }
    var hubOpen by remember { mutableStateOf(false) }
    val jump = remember { Animatable(0f) }
    val wiggle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { while (true) { delay(2400L + (0..2200).random()); blink = true; delay(130); blink = false } }

    fun react(msg: String?, xp: Int = 0) = scope.launch {
        if (xp > 0) vm.recordPetXp(xp)
        say = msg
        jump.snapTo(0f); jump.animateTo(1f, tween(170)); jump.animateTo(0f, spring(dampingRatio = 0.36f, stiffness = 320f))
        for (k in listOf(1f, -1f, 0.5f, -0.5f, 0f)) wiggle.animateTo(k, tween(65))
        delay(2200)
        if (say == msg) say = null
    }

    LaunchedEffect(vm.toast) {
        val m = vm.toast ?: return@LaunchedEffect
        if (m.startsWith("Added") || m.startsWith("Saved") || m.startsWith("Generated") || m.startsWith("Switched")) {
            react(pet.quotes.random())
        }
    }
    var sleepy by remember { mutableStateOf(false) }
    LaunchedEffect(vm.pageIndex) { if ((0..2).random() == 0) react(null) }
    LaunchedEffect(vm.lastActivity) { sleepy = false; delay(65_000); sleepy = true }

    val writing = vm.penActive
    val thinking = vm.aiBusy

    Box(modifier) {
        Column(
            Modifier.graphicsLayer { alpha = if (writing && !compact) 0.35f else 1f },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val bubble = say ?: if (thinking) "Thinking…" else if (sleepy) "zzz…" else null
            if (bubble != null && !hubOpen) {
                val content = @Composable {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                        Text(bubble, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (compact) Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 110)) { content() } else content()
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    Modifier.size(if (compact) 36.dp else 58.dp).graphicsLayer {
                        translationY = -jump.value * 16.dp.toPx()
                        rotationZ = wiggle.value * 12f
                    }.clickable(remember { MutableInteractionSource() }, null) {
                        hubOpen = !hubOpen
                        if (!hubOpen) react(pet.quotes.random(), xp = 2)
                    },
                ) {
                    drawCompanion(
                        pet = pet,
                        breathe = breathe,
                        jump = jump.value,
                        blink = blink || (sleepy && say == null),
                        writing = writing,
                        thinking = thinking,
                        aiSpin = aiSpin,
                        compact = compact,
                    )
                }
                if (!compact) {
                    Spacer(Modifier.width(4.dp))
                    Surface(shape = CircleShape, color = pet.secondary.copy(alpha = 0.18f)) {
                        Text("Lv.$level", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = pet.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (hubOpen) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 110),
                onDismissRequest = { hubOpen = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                    modifier = Modifier.widthIn(min = 290.dp, max = 350.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(Modifier.size(54.dp)) {
                                drawCompanion(pet, breathe, jump.value, blink, writing, thinking, aiSpin, false)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(pet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Surface(shape = CircleShape, color = pet.primary.copy(alpha = 0.25f)) {
                                        Text("${pet.element} · Lv.$level", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text("${settings.petXp} Bond XP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { levelProgress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = pet.secondary,
                                )
                            }
                            TextButton(onClick = { hubOpen = false }) { Text("Close") }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { react("Loves the head pats! ♥", xp = 5) }, shape = CircleShape, modifier = Modifier.weight(1f)) {
                                Text("Pat +5", fontSize = 12.sp)
                            }
                            FilledTonalButton(onClick = { react("Yum! Berry boost! 🍓", xp = 15) }, shape = CircleShape, modifier = Modifier.weight(1f)) {
                                Text("Treat +15", fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { react(pet.quotes.random(), xp = 8) }, shape = CircleShape, modifier = Modifier.weight(1f)) {
                                Text("Cheer", fontSize = 12.sp)
                            }
                        }

                        Text("SWITCH COMPANION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Companions.forEach { c ->
                                val active = c.id == pet.id
                                Column(
                                    Modifier.width(70.dp).clip(RoundedCornerShape(14.dp))
                                        .background(if (active) c.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .border(if (active) 1.5.dp else 0.dp, if (active) c.secondary else Color.Transparent, RoundedCornerShape(14.dp))
                                        .clickable { vm.switchPet(c.id); react(c.quotes.first(), xp = 3) }
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Canvas(Modifier.size(30.dp)) {
                                        drawCompanion(c, breathe, 0f, false, false, false, 0f, true)
                                    }
                                    Text(c.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun DrawScope.drawCompanion(
    pet: CompanionMeta,
    breathe: Float,
    jump: Float,
    blink: Boolean,
    writing: Boolean,
    thinking: Boolean,
    aiSpin: Float,
    compact: Boolean,
) {
    val w = size.width
    val h = size.height
    if (thinking) {
        rotate(aiSpin, pivot = Offset(w / 2f, h * 0.56f)) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(Color(0xFFE38BFF), Color(0xFF5AC8FA), Color.Transparent, Color(0xFFE38BFF))),
                radius = w * 0.56f,
                center = Offset(w / 2f, h * 0.56f),
                style = Stroke(width = if (compact) 2f else 3.5f),
            )
        }
    }
    val squish = 1f + 0.04f * breathe - jump * 0.08f
    val bodyH = h * 0.72f * squish
    val bodyW = w * 0.86f / squish
    val left = (w - bodyW) / 2f
    val top = h - bodyH

    // Soft shadow
    drawOval(Color(0x22000000), Offset(w * 0.2f, h - 5f), Size(w * 0.6f, 7f))

    // Creature-specific ears / crests behind body
    when (pet.id) {
        "sparkchu", "eevo" -> {
            val lEar = Path().apply { moveTo(w * 0.24f, top + bodyH * 0.2f); lineTo(w * 0.14f, top - h * 0.18f); lineTo(w * 0.38f, top + bodyH * 0.08f); close() }
            val rEar = Path().apply { moveTo(w * 0.76f, top + bodyH * 0.2f); lineTo(w * 0.86f, top - h * 0.18f); lineTo(w * 0.62f, top + bodyH * 0.08f); close() }
            drawPath(lEar, pet.primary)
            drawPath(rEar, pet.primary)
            if (pet.id == "sparkchu") {
                val lTip = Path().apply { moveTo(w * 0.18f, top - h * 0.06f); lineTo(w * 0.14f, top - h * 0.18f); lineTo(w * 0.26f, top - h * 0.07f); close() }
                val rTip = Path().apply { moveTo(w * 0.82f, top - h * 0.06f); lineTo(w * 0.86f, top - h * 0.18f); lineTo(w * 0.74f, top - h * 0.07f); close() }
                drawPath(lTip, Color(0xFF2B2F3A))
                drawPath(rTip, Color(0xFF2B2F3A))
            }
        }
        "leafsprout" -> {
            val leaf = Path().apply {
                moveTo(w * 0.5f, top + 2f)
                quadraticTo(w * 0.32f, top - h * 0.22f, w * 0.55f, top - h * 0.12f)
                quadraticTo(w * 0.74f, top - h * 0.22f, w * 0.5f, top + 2f)
            }
            drawPath(leaf, Color(0xFF1BAF7A))
        }
        "emberkit" -> {
            val flame = Path().apply {
                moveTo(w * 0.4f, top + 4f)
                quadraticTo(w * 0.5f, top - h * 0.22f, w * 0.6f, top + 4f)
                close()
            }
            drawPath(flame, Color(0xFFFFE066))
        }
        "genglet" -> {
            for (dx in listOf(-0.22f, 0f, 0.22f)) {
                val spike = Path().apply {
                    moveTo(w * (0.44f + dx), top + bodyH * 0.14f)
                    lineTo(w * (0.5f + dx), top - h * 0.1f)
                    lineTo(w * (0.56f + dx), top + bodyH * 0.14f)
                    close()
                }
                drawPath(spike, pet.secondary)
            }
        }
    }

    // Main body
    drawOval(Brush.verticalGradient(listOf(pet.primary, pet.secondary), top, h), Offset(left, top), Size(bodyW, bodyH))
    drawOval(Color(0x48FFFFFF), Offset(w * 0.26f, top + bodyH * 0.09f), Size(bodyW * 0.28f, bodyH * 0.16f))

    // Belly patch for snorblob/eevo/aquapup
    if (pet.id in setOf("snorblob", "eevo", "aquapup")) {
        drawOval(pet.accent.copy(alpha = 0.7f), Offset(w * 0.28f, top + bodyH * 0.52f), Size(bodyW * 0.52f, bodyH * 0.42f))
    }

    // Cheeks
    drawCircle(pet.accent.copy(alpha = 0.78f), w * 0.08f, Offset(w * 0.25f, top + bodyH * 0.6f))
    drawCircle(pet.accent.copy(alpha = 0.78f), w * 0.08f, Offset(w * 0.75f, top + bodyH * 0.6f))

    // Eyes
    val ey = top + bodyH * (if (writing) 0.5f else if (thinking) 0.36f else 0.44f)
    val ex = if (thinking) w * 0.035f else 0f
    val eyeColor = if (pet.id == "genglet") Color(0xFFFF3B6B) else Color(0xFF232734)
    for (x in listOf(w * 0.37f, w * 0.63f)) {
        if (blink) {
            drawLine(eyeColor, Offset(x - 4f + ex, ey), Offset(x + 4f + ex, ey), 2.8f, StrokeCap.Round)
        } else {
            drawCircle(eyeColor, w * 0.058f, Offset(x + ex, ey))
            drawCircle(Color.White, w * 0.02f, Offset(x + ex + 1.8f, ey - 1.8f))
        }
    }

    // Mouth
    if (thinking) {
        drawCircle(Color(0xFF232734), w * 0.03f, Offset(w / 2f, top + bodyH * 0.66f), style = Stroke(2.2f))
    } else if (pet.id == "genglet") {
        val grin = Path().apply {
            moveTo(w * 0.36f, top + bodyH * 0.62f)
            quadraticTo(w / 2f, top + bodyH * 0.77f, w * 0.64f, top + bodyH * 0.62f)
        }
        drawPath(grin, Color.White, style = Stroke(3.2f, cap = StrokeCap.Round))
    } else {
        val smile = Path().apply {
            moveTo(w * 0.42f, top + bodyH * 0.62f)
            quadraticTo(w / 2f, top + bodyH * (0.72f + 0.06f * jump), w * 0.58f, top + bodyH * 0.62f)
        }
        drawPath(smile, Color(0xFF232734), style = Stroke(2.4f, cap = StrokeCap.Round))
    }
}
