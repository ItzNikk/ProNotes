package com.pronotes.app.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FormatShapes
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import com.pronotes.app.ui.elasticStretch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AspectRatioChoice(val label: String, val ratio: Float) {
    SQUARE("1:1", 1f),
    PORTRAIT("3:4", 3f / 4f),
    LANDSCAPE("16:9", 16f / 9f)
}

enum class StudioAssetMode(val label: String, val badge: String) {
    STICKER("Sticker", "⭐"),
    COVER("Book Cover", "📖"),
    TEMPLATE("Study Template", "📐")
}

@Composable
fun GalaxyCreativeStudioScreen(
    onBack: () -> Unit,
    onInsertBitmap: ((Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val engine = remember { ProNotesAiEngine(context) }

    var assetMode by remember { mutableStateOf(StudioAssetMode.STICKER) }
    var selectedStyle by remember { mutableStateOf(StickerStyle.CARTOON_3D) }
    var selectedCoverTheme by remember { mutableStateOf(CoverTheme.GALAXY) }
    var selectedTemplateType by remember { mutableStateOf(TemplateType.CORNELL) }
    var aspectRatio by remember { mutableStateOf(AspectRatioChoice.SQUARE) }

    var promptText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val sessionHistory = remember { mutableStateListOf<Bitmap>() }
    var showStylePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var generationStageIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var previewBgMode by remember { androidx.compose.runtime.mutableIntStateOf(0) } // 0: Aura, 1: Checkerboard, 2: Dark
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "studioAura")
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // Stage text cycling while generating
    androidx.compose.runtime.LaunchedEffect(isGenerating) {
        if (isGenerating) {
            generationStageIndex = 0
            while (isGenerating) {
                kotlinx.coroutines.delay(2200)
                generationStageIndex = (generationStageIndex + 1) % 4
            }
        }
    }

    fun handleGenerate() {
        if (isGenerating) return
        val q = promptText.trim()
        isGenerating = true
        errorMessage = null

        scope.launch {
            try {
                val bmp = when (assetMode) {
                    StudioAssetMode.STICKER -> {
                        val (w, h) = when (aspectRatio) {
                            AspectRatioChoice.SQUARE -> 512 to 512
                            AspectRatioChoice.PORTRAIT -> 450 to 600
                            AspectRatioChoice.LANDSCAPE -> 640 to 360
                        }
                        engine.generateSticker(prompt = q, style = selectedStyle, width = w, height = h)
                    }
                    StudioAssetMode.COVER -> {
                        val (w, h) = when (aspectRatio) {
                            AspectRatioChoice.SQUARE -> 800 to 800
                            AspectRatioChoice.PORTRAIT -> 900 to 1200
                            AspectRatioChoice.LANDSCAPE -> 1200 to 675
                        }
                        engine.generateCover(title = q, theme = selectedCoverTheme, width = w, height = h)
                    }
                    StudioAssetMode.TEMPLATE -> {
                        val (w, h) = when (aspectRatio) {
                            AspectRatioChoice.SQUARE -> 1240 to 1240
                            AspectRatioChoice.PORTRAIT -> 1240 to 1754
                            AspectRatioChoice.LANDSCAPE -> 1754 to 1240
                        }
                        engine.generateTemplate(type = selectedTemplateType, width = w, height = h)
                    }
                }
                generatedBitmap = bmp
                if (bmp != null) {
                    sessionHistory.add(0, bmp)
                    zoomScale = 1f
                    panOffset = Offset.Zero
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            } catch (t: Throwable) {
                errorMessage = t.message ?: "Generation failed. Please try again."
            } finally {
                isGenerating = false
            }
        }
    }

    fun saveToGallery(bmp: Bitmap) {
        scope.launch(Dispatchers.IO) {
            try {
                val filename = "ProNotes_Studio_${System.currentTimeMillis()}.png"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProNotes")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Samsung Galaxy AI Cyan/Turquoise to Blue Radial Mesh Gradient
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF38BDF8), // Cyan
                Color(0xFF4DE8D8), // Turquoise
                Color(0xFF2A78D6), // Samsung One UI Blue
                Color(0xFF1E3A8A)  // Deep Blue
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ---------------------------------------------------------------- Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .elasticStretch()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Creative Studio",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Aspect Ratio Selector Cycle Button
                IconButton(
                    onClick = {
                        aspectRatio = when (aspectRatio) {
                            AspectRatioChoice.SQUARE -> AspectRatioChoice.PORTRAIT
                            AspectRatioChoice.PORTRAIT -> AspectRatioChoice.LANDSCAPE
                            AspectRatioChoice.LANDSCAPE -> AspectRatioChoice.SQUARE
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp)
                        .elasticStretch()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = aspectRatio.label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ---------------------------------------------------------------- Central Viewport Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 14.dp)
                    .aspectRatio(aspectRatio.ratio, matchHeightConstraintsFirst = true),
                contentAlignment = Alignment.Center
            ) {
                // One UI 9 Liquid Glass Outer Squircle Card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.20f),
                                    Color(0xFF93C5FD).copy(alpha = 0.40f)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner Viewport Background: Aura vs Checkerboard vs Dark
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val radius = max(size.width, size.height) * 0.65f * auraPulse
                                val auraBrush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFEC4899).copy(alpha = 0.55f), // Hot Pink
                                        Color(0xFFA855F7).copy(alpha = 0.38f), // Purple
                                        Color.Transparent
                                    ),
                                    radius = radius
                                )
                                onDrawBehind {
                                    when (previewBgMode) {
                                        0 -> drawCircle(brush = auraBrush, radius = radius)
                                        1 -> {
                                            // Checkerboard transparency pattern
                                            val tileSize = 20.dp.toPx()
                                            val cols = (size.width / tileSize).toInt() + 1
                                            val rows = (size.height / tileSize).toInt() + 1
                                            for (r in 0 until rows) {
                                                for (c in 0 until cols) {
                                                    val color = if ((r + c) % 2 == 0) Color(0x33FFFFFF) else Color(0x11000000)
                                                    drawRect(color, Offset(c * tileSize, r * tileSize), androidx.compose.ui.geometry.Size(tileSize, tileSize))
                                                }
                                            }
                                        }
                                        2 -> drawRect(Color(0xFF0F172A).copy(alpha = 0.90f))
                                    }
                                }
                            }
                    )

                    // Card Content: Result or Placeholder or Loading
                    AnimatedContent(
                        targetState = Triple(isGenerating, generatedBitmap, errorMessage),
                        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                        label = "studioViewport"
                    ) { (generating, bmp, err) ->
                        when {
                            generating -> {
                                val stages = when (assetMode) {
                                    StudioAssetMode.STICKER -> listOf(
                                        "Dreaming composition & 3D forms…",
                                        "Applying ${selectedStyle.label} texture…",
                                        "Tracing die-cut white vinyl border…",
                                        "Polishing studio lighting & alpha…"
                                    )
                                    StudioAssetMode.COVER -> listOf(
                                        "Imagining panoramic ${selectedCoverTheme.label} scene…",
                                        "Painting atmospheric depth & lighting…",
                                        "Engraving title typography plate…",
                                        "Applying bookbinding spine finish…"
                                    )
                                    StudioAssetMode.TEMPLATE -> listOf(
                                        "Structuring ${selectedTemplateType.label} grid…",
                                        "Ruling clean vector guidelines…",
                                        "Adding Cornell & planning headers…",
                                        "Exporting print-ready 300DPI canvas…"
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    // Animated Orbiting Stardust & Center Glow Badge
                                    Box(
                                        modifier = Modifier.size(110.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Rotating Conic Multi-Color Nebula Aura Ring
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .drawWithCache {
                                                    val brush = Brush.sweepGradient(
                                                        listOf(
                                                            Color(0xFFEC4899), // Pink
                                                            Color(0xFF8C65FF), // Purple
                                                            Color(0xFF38BDF8), // Cyan
                                                            Color(0xFF4DE8D8), // Turquoise
                                                            Color(0xFFEC4899)
                                                        )
                                                    )
                                                    onDrawBehind {
                                                        androidx.compose.ui.graphics.drawscope.rotate(spinAngle) {
                                                            drawCircle(
                                                                brush = brush,
                                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                                                            )
                                                        }
                                                    }
                                                }
                                        )

                                        // Central Pulsing Orb
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(
                                                            Color.White.copy(alpha = 0.95f),
                                                            Color(0xFF8C65FF).copy(alpha = 0.85f),
                                                            Color(0xFF38BDF8).copy(alpha = 0.50f)
                                                        )
                                                    )
                                                )
                                                .border(1.5.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (assetMode) {
                                                    StudioAssetMode.STICKER -> selectedStyle.badge
                                                    StudioAssetMode.COVER -> "📖"
                                                    StudioAssetMode.TEMPLATE -> "📐"
                                                },
                                                fontSize = 30.sp
                                            )
                                        }

                                        // 3 Orbiting Particles/Sparkles at 120-degree offsets
                                        val orbitRadius = 48.dp
                                        val radAngle = (spinAngle * PI / 180f).toFloat()

                                        val p1X = (cos(radAngle) * orbitRadius.value).dp
                                        val p1Y = (sin(radAngle) * orbitRadius.value).dp
                                        val p2X = (cos(radAngle + 2.094f) * orbitRadius.value).dp
                                        val p2Y = (sin(radAngle + 2.094f) * orbitRadius.value).dp
                                        val p3X = (cos(radAngle + 4.188f) * orbitRadius.value).dp
                                        val p3Y = (sin(radAngle + 4.188f) * orbitRadius.value).dp

                                        Text(
                                            text = "✨",
                                            fontSize = 14.sp,
                                            modifier = Modifier.androidx.compose.foundation.layout.offset(x = p1X, y = p1Y)
                                        )
                                        Text(
                                            text = "⭐",
                                            fontSize = 12.sp,
                                            modifier = Modifier.androidx.compose.foundation.layout.offset(x = p2X, y = p2Y)
                                        )
                                        Text(
                                            text = "💫",
                                            fontSize = 13.sp,
                                            modifier = Modifier.androidx.compose.foundation.layout.offset(x = p3X, y = p3Y)
                                        )
                                    }

                                    // Dynamic Cycling Stage Text with Fade Transition
                                    AnimatedContent(
                                        targetState = stages[generationStageIndex % stages.size],
                                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(250)) },
                                        label = "stageTextAnim"
                                    ) { stageDesc ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = stageDesc,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "FLUX.1 Neural Studio · One UI 9",
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                            bmp != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(32.dp))
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    zoomScale = 1f
                                                    panOffset = Offset.Zero
                                                }
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                zoomScale = (zoomScale * zoom).coerceIn(0.8f, 4.5f)
                                                panOffset += pan
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Generated Result",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                            .graphicsLayer {
                                                scaleX = zoomScale
                                                scaleY = zoomScale
                                                translationX = panOffset.x
                                                translationY = panOffset.y
                                            }
                                    )
                                }
                            }
                            err != null -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "⚠️ $err",
                                        color = Color(0xFFFFD1D1),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "Imagine it, create it.\nAny image, instantly.",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 26.sp,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }

                    // Floating Style Badge Pill (Bottom Left of Card)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable { showStylePicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .elasticStretch()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when (assetMode) {
                                    StudioAssetMode.STICKER -> selectedStyle.badge
                                    StudioAssetMode.COVER -> "📖"
                                    StudioAssetMode.TEMPLATE -> "📐"
                                },
                                fontSize = 16.sp
                            )
                            Text(
                                text = when (assetMode) {
                                    StudioAssetMode.STICKER -> selectedStyle.label
                                    StudioAssetMode.COVER -> selectedCoverTheme.label
                                    StudioAssetMode.TEMPLATE -> selectedTemplateType.label
                                },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Background Transparency Mode Toggle (Top Right of Card when result exists)
                    if (generatedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A).copy(alpha = 0.75f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                .clickable { previewBgMode = (previewBgMode + 1) % 3 }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .elasticStretch()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (previewBgMode) {
                                        0 -> "✨ Aura"
                                        1 -> "🏁 Grid"
                                        else -> "🌑 Dark"
                                    },
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Session History Strip Carousel (if user has generated multiple in this session)
            if (sessionHistory.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sessionHistory.forEachIndexed { idx, histBmp ->
                        val isCurrent = histBmp == generatedBitmap
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    generatedBitmap = histBmp
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                }
                                .elasticStretch(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = histBmp.asImageBitmap(),
                                contentDescription = "History Item #$idx",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }
            }

            // ---------------------------------------------------------------- Controls & Prompt Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Asset Type Selector Pill Bar (Stickers, Covers, Templates)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    StudioAssetMode.entries.forEach { mode ->
                        val selected = assetMode == mode
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) Color.White.copy(alpha = 0.32f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.20f),
                                    shape = CircleShape
                                )
                                .clickable { assetMode = mode }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .elasticStretch()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(mode.badge, fontSize = 13.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = mode.label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // If Result Exists: Show Action Bar
                AnimatedVisibility(visible = generatedBitmap != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Save button
                        IconButton(
                            onClick = { generatedBitmap?.let { saveToGallery(it) } },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f))
                                .elasticStretch()
                        ) {
                            Icon(Icons.Outlined.Download, "Save", tint = Color.White)
                        }

                        // Add to note button
                        if (onInsertBitmap != null) {
                            IconButton(
                                onClick = { generatedBitmap?.let { onInsertBitmap(it); onBack() } },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .elasticStretch()
                            ) {
                                Icon(Icons.Outlined.PostAdd, "Insert", tint = Color.White)
                            }
                        }

                        // Regenerate
                        IconButton(
                            onClick = { handleGenerate() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f))
                                .elasticStretch()
                        ) {
                            Icon(Icons.Outlined.Refresh, "Regenerate", tint = Color.White)
                        }
                    }
                }

                // Quick Inspiration Chips Row
                val suggestionChips = when (assetMode) {
                    StudioAssetMode.STICKER -> listOf("Cute Shiba Astronaut 🐕", "Golden Trophy Award 🏆", "Coffee Cup Doodled ☕", "Brain Power Spark 🧠", "Retro Boombox ⚡", "Lofi Plant Pot 🌿")
                    StudioAssetMode.COVER -> listOf("Deep Space Nebula 🌌", "Golden Ratio Minimal 📐", "Botanical Leaves 🌿", "Vintage Leather Book 📚", "Pastel Aura Mesh ✨")
                    StudioAssetMode.TEMPLATE -> listOf("Cornell Study Grid", "Weekly Habit Tracker", "Eisenhower Matrix", "Dot Grid Journal", "Sprint Meeting Minutes")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestionChips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                                .clickable { promptText = chip }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .elasticStretch()
                        ) {
                            Text(
                                text = chip,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Bottom-Anchored Translucent Prompt Input
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = {
                        Text(
                            text = when (assetMode) {
                                StudioAssetMode.STICKER -> "Describe the sticker you want to create…"
                                StudioAssetMode.COVER -> "Enter notebook title or journal theme…"
                                StudioAssetMode.TEMPLATE -> "Study topic or custom layout name…"
                            },
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.40f)),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.7f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        cursorColor = Color.White
                    ),
                    maxLines = 3
                )

                // Generate Button with Spring Dynamic Feedback
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF8C65FF), // One UI 9 Purple
                                    Color(0xFF38BDF8)  // One UI 9 Cyan
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                        .clickable(enabled = !isGenerating) { handleGenerate() }
                        .elasticStretch(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Generate",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isGenerating) "Creating…" else "Generate",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------- Style Picker Dialog
        if (showStylePicker) {
            Dialog(
                onDismissRequest = { showStylePicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Artistic Style",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showStylePicker = false }) {
                                Icon(Icons.Outlined.Close, "Close", tint = Color.White)
                            }
                        }

                        when (assetMode) {
                            StudioAssetMode.STICKER -> {
                                StickerStyle.entries.forEach { style ->
                                    val isSelected = selectedStyle == style
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable {
                                                selectedStyle = style
                                                showStylePicker = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(style.badge, fontSize = 22.sp)
                                            Spacer(Modifier.width(12.dp))
                                            Text(style.label, color = Color.White, fontSize = 15.sp)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Outlined.Check, "Selected", tint = Color(0xFF38BDF8))
                                        }
                                    }
                                }
                            }
                            StudioAssetMode.COVER -> {
                                CoverTheme.entries.forEach { theme ->
                                    val isSelected = selectedCoverTheme == theme
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable {
                                                selectedCoverTheme = theme
                                                showStylePicker = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(theme.label, color = Color.White, fontSize = 15.sp)
                                        if (isSelected) {
                                            Icon(Icons.Outlined.Check, "Selected", tint = Color(0xFF38BDF8))
                                        }
                                    }
                                }
                            }
                            StudioAssetMode.TEMPLATE -> {
                                TemplateType.entries.forEach { tmpl ->
                                    val isSelected = selectedTemplateType == tmpl
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable {
                                                selectedTemplateType = tmpl
                                                showStylePicker = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(tmpl.label, color = Color.White, fontSize = 15.sp)
                                        if (isSelected) {
                                            Icon(Icons.Outlined.Check, "Selected", tint = Color(0xFF38BDF8))
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
