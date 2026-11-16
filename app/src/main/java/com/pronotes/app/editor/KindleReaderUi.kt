package com.pronotes.app.editor

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.SurfaceRole
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class KindleTheme(
    val label: String,
    val deskBg: Color,
    val pageBg: Color,
    val chromeBg: Color,
    val inkText: Color,
    val mutedText: Color,
    val accent: Color,
) {
    PAPER(
        label = "Paper",
        deskBg = Color(0xFFECE7DC),
        pageBg = Color(0xFFFAF7F0),
        chromeBg = Color(0xF2F7F4EC),
        inkText = Color(0xFF1F2421),
        mutedText = Color(0xFF5D625C),
        accent = Color(0xFF1A84FF),
    ),
    SEPIA(
        label = "Sepia",
        deskBg = Color(0xFFE3D5B8),
        pageBg = Color(0xFFF4ECD8),
        chromeBg = Color(0xF2EFE3C8),
        inkText = Color(0xFF3E2F1E),
        mutedText = Color(0xFF6F5B42),
        accent = Color(0xFFB45309),
    ),
    DARK(
        label = "Night",
        deskBg = Color(0xFF0A0B0E),
        pageBg = Color(0xFF14161A),
        chromeBg = Color(0xF21B1E26),
        inkText = Color(0xFFE5E7EB),
        mutedText = Color(0xFF9CA3AF),
        accent = Color(0xFF4FB0F5),
    ),
}

/**
 * Immersive Kindle book-reading overlay with Paper / Sepia / Night themes, tablet 2-page open book
 * spread option, bottom reading progress scrubber, and interactive 3D page-turn curl animations.
 */
@Composable
fun KindleReaderOverlay(
    vm: EditorViewModel,
    onExit: () -> Unit,
) {
    val totalPages = vm.pages.size.coerceAtLeast(1)
    var currentPage by remember { mutableIntStateOf(vm.pageIndex.coerceIn(0, totalPages - 1)) }
    var theme by remember { mutableStateOf(KindleTheme.SEPIA) }
    var showChrome by remember { mutableStateOf(true) }
    var twoPageSpread by remember { mutableStateOf(false) }

    // 0f = resting, -1f = turning forward (right-to-left page curl), +1f = turning backward (left-to-right curl)
    val turnProgress = remember { Animatable(0f) }
    var turnDir by remember { mutableIntStateOf(0) } // +1 = next page, -1 = prev page
    val scope = rememberCoroutineScope()

    val pageStep = if (twoPageSpread) 2 else 1

    fun canGoNext(): Boolean = currentPage + pageStep < totalPages
    fun canGoPrev(): Boolean = currentPage - pageStep >= 0

    fun animatePageTurn(direction: Int) {
        if (direction > 0 && !canGoNext()) return
        if (direction < 0 && !canGoPrev()) return
        if (turnProgress.isRunning) return
        scope.launch {
            turnDir = direction
            turnProgress.snapTo(0f)
            turnProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing),
            )
            currentPage = (currentPage + direction * pageStep).coerceIn(0, totalPages - 1)
            vm.goToPage(currentPage)
            turnDir = 0
            turnProgress.snapTo(0f)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.deskBg)
            .safeDrawingPadding(),
    ) {
        val isWideTablet = maxWidth >= 740.dp
        val effectiveSpread = twoPageSpread && isWideTablet
        val density = LocalDensity.current
        val renderWidthPx = with(density) {
            val targetDp = if (effectiveSpread) (maxWidth / 2f).coerceAtMost(560.dp) else maxWidth.coerceAtMost(780.dp)
            (targetDp.toPx() * 1.35f).toInt().coerceIn(720, 1480)
        }

        val activeProgress = turnProgress.value.coerceIn(0f, 1f)
        val previewPageIdx = when {
            turnDir > 0 -> (currentPage + (if (effectiveSpread) 2 else 1)).coerceAtMost(totalPages - 1)
            turnDir < 0 -> (currentPage - (if (effectiveSpread) 2 else 1)).coerceAtLeast(0)
            else -> currentPage
        }

        val curBmp by produceState<Bitmap?>(initialValue = null, currentPage, renderWidthPx) {
            value = vm.renderReaderPage(currentPage, renderWidthPx)
        }
        val curRightBmp by produceState<Bitmap?>(initialValue = null, currentPage, effectiveSpread, renderWidthPx) {
            value = if (effectiveSpread && currentPage + 1 < totalPages) {
                vm.renderReaderPage(currentPage + 1, renderWidthPx)
            } else null
        }
        val nextBmp by produceState<Bitmap?>(initialValue = null, previewPageIdx, turnDir, renderWidthPx) {
            value = if (turnDir != 0) vm.renderReaderPage(previewPageIdx, renderWidthPx) else null
        }
        val nextRightBmp by produceState<Bitmap?>(initialValue = null, previewPageIdx, turnDir, effectiveSpread, renderWidthPx) {
            value = if (turnDir != 0 && effectiveSpread && previewPageIdx + 1 < totalPages) {
                vm.renderReaderPage(previewPageIdx + 1, renderWidthPx)
            } else null
        }

        // Interactive book canvas (swipe drag or tap left/center/right zones)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isWideTablet) 32.dp else 12.dp,
                    vertical = if (showChrome) 76.dp else 18.dp,
                )
                .pointerInput(currentPage, totalPages, effectiveSpread) {
                    detectTapGestures { offset ->
                        when {
                            offset.x < size.width * 0.28f -> animatePageTurn(-1)
                            offset.x > size.width * 0.72f -> animatePageTurn(+1)
                            else -> showChrome = !showChrome
                        }
                    }
                }
                .pointerInput(currentPage, totalPages, effectiveSpread) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f; turnDir = 0 },
                        onHorizontalDrag = { _, delta ->
                            dragged += delta
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            if (dragged < -18f && canGoNext()) {
                                turnDir = +1
                                scope.launch { turnProgress.snapTo((abs(dragged) / (w * 0.65f)).coerceIn(0f, 1f)) }
                            } else if (dragged > 18f && canGoPrev()) {
                                turnDir = -1
                                scope.launch { turnProgress.snapTo((abs(dragged) / (w * 0.65f)).coerceIn(0f, 1f)) }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                turnProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                turnDir = 0
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val p = turnProgress.value
                                if (p >= 0.24f && turnDir != 0) {
                                    turnProgress.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
                                    currentPage = (currentPage + turnDir * (if (effectiveSpread) 2 else 1)).coerceIn(0, totalPages - 1)
                                    vm.goToPage(currentPage)
                                } else {
                                    turnProgress.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 320f))
                                }
                                turnDir = 0
                                turnProgress.snapTo(0f)
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // Underneath revealed destination sheet
            if (turnDir != 0) {
                BookSheetContainer(
                    theme = theme,
                    spread = effectiveSpread,
                    leftBmp = nextBmp,
                    rightBmp = nextRightBmp,
                    leftPageNum = previewPageIdx + 1,
                    rightPageNum = if (effectiveSpread && previewPageIdx + 1 < totalPages) previewPageIdx + 2 else null,
                    totalPages = totalPages,
                    title = vm.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Subtle elevation settle as incoming page is uncovered
                            val s = 0.975f + 0.025f * activeProgress
                            scaleX = s
                            scaleY = s
                            alpha = 0.72f + 0.28f * activeProgress
                        },
                )
            }

            // Active turning sheet with 3D page-curl rotation + fold lighting
            val foldAngle = when {
                turnDir > 0 -> -118f * activeProgress
                turnDir < 0 -> 118f * activeProgress
                else -> 0f
            }
            val hingeOrigin = when {
                turnDir < 0 -> TransformOrigin(1f, 0.5f)
                else -> TransformOrigin(0f, 0.5f)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        cameraDistance = 16f * density.density
                        rotationY = foldAngle
                        transformOrigin = hingeOrigin
                        alpha = if (turnDir == 0) 1f else (1f - (activeProgress - 0.65f).coerceAtLeast(0f) / 0.35f).coerceIn(0f, 1f)
                    },
            ) {
                BookSheetContainer(
                    theme = theme,
                    spread = effectiveSpread,
                    leftBmp = curBmp,
                    rightBmp = curRightBmp,
                    leftPageNum = currentPage + 1,
                    rightPageNum = if (effectiveSpread && currentPage + 1 < totalPages) currentPage + 2 else null,
                    totalPages = totalPages,
                    title = vm.title,
                    modifier = Modifier.fillMaxSize(),
                )

                // Realistic moving page crease highlight & curl shadow during turn
                if (turnDir != 0 && activeProgress > 0.01f) {
                    val shadowAlpha = (0.36f * kotlin.math.sin(activeProgress * Math.PI).toFloat()).coerceIn(0f, 0.4f)
                    val highlightAlpha = (0.28f * kotlin.math.sin(activeProgress * Math.PI).toFloat()).coerceIn(0f, 0.32f)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = if (turnDir > 0) {
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = highlightAlpha),
                                            Color.Black.copy(alpha = shadowAlpha),
                                        )
                                    } else {
                                        listOf(
                                            Color.Black.copy(alpha = shadowAlpha),
                                            Color.White.copy(alpha = highlightAlpha),
                                            Color.Transparent,
                                        )
                                    },
                                ),
                                RoundedCornerShape(16.dp),
                            ),
                    )
                }
            }
        }

        // Top Reading Bar (fade toggle on tap)
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            AppSurface(
                role = SurfaceRole.Chrome,
                containerColor = theme.chromeBg,
                contentColor = theme.inkText,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 6.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to editor", tint = theme.inkText)
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        ScaleInAnimatedText(
                            text = vm.title.ifBlank { "Book Mode" },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = theme.inkText,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Reading Mode · Page ${currentPage + 1} of $totalPages",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.mutedText,
                        )
                    }

                    // Theme Switchers (Paper / Sepia / Night)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        KindleTheme.entries.forEach { t ->
                            val selected = t == theme
                            AppPill(
                                selected = selected,
                                tint = t.accent,
                                onClick = { theme = t },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(t.pageBg),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(t.label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (isWideTablet) {
                        IconButton(onClick = { twoPageSpread = !twoPageSpread }) {
                            Icon(
                                Icons.Outlined.ViewColumn,
                                contentDescription = "Toggle book spread",
                                tint = if (twoPageSpread) theme.accent else theme.mutedText,
                            )
                        }
                    }

                    val bookmarked = vm.pages.getOrNull(currentPage)?.bookmarked == true
                    IconButton(onClick = {
                        vm.goToPage(currentPage)
                        vm.toggleBookmark()
                    }) {
                        Icon(
                            if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark page",
                            tint = if (bookmarked) theme.accent else theme.mutedText,
                        )
                    }
                }
            }
        }

        // Bottom Book Scrubber & Turn Controls
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            AppSurface(
                role = SurfaceRole.Chrome,
                containerColor = theme.chromeBg,
                contentColor = theme.inkText,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 6.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(
                        enabled = canGoPrev(),
                        onClick = { animatePageTurn(-1) },
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous page",
                            tint = if (canGoPrev()) theme.inkText else theme.mutedText.copy(alpha = 0.35f),
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        val pct = (((currentPage + 1).toFloat() / totalPages) * 100).toInt().coerceIn(1, 100)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Page ${currentPage + 1} of $totalPages",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.inkText,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "$pct% read",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.mutedText,
                            )
                        }
                        if (totalPages > 1) {
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { v ->
                                    val idx = v.toInt().coerceIn(0, totalPages - 1)
                                    if (idx != currentPage) {
                                        currentPage = idx
                                        vm.goToPage(idx)
                                    }
                                },
                                valueRange = 0f..(totalPages - 1).toFloat(),
                                steps = (totalPages - 2).coerceAtLeast(0),
                                colors = SliderDefaults.colors(
                                    thumbColor = theme.accent,
                                    activeTrackColor = theme.accent,
                                    inactiveTrackColor = theme.mutedText.copy(alpha = 0.25f),
                                ),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }

                    IconButton(
                        enabled = canGoNext(),
                        onClick = { animatePageTurn(+1) },
                    ) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Next page",
                            tint = if (canGoNext()) theme.inkText else theme.mutedText.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookSheetContainer(
    theme: KindleTheme,
    spread: Boolean,
    leftBmp: Bitmap?,
    rightBmp: Bitmap?,
    leftPageNum: Int,
    rightPageNum: Int?,
    totalPages: Int,
    title: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    // Gentle tint filter in Sepia or invert matrix in Night mode for authentic E-Ink paper feel
    val colorFilter = remember(theme) {
        when (theme) {
            KindleTheme.PAPER -> null
            KindleTheme.SEPIA -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.96f, 0f, 0f, 0f, 0f,
                        0f, 0.92f, 0f, 0f, 0f,
                        0f, 0f, 0.82f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
            KindleTheme.DARK -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.9f, 0f, 0f, 0f, 238f,
                        0f, -0.9f, 0f, 0f, 238f,
                        0f, 0f, -0.9f, 0f, 238f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }
    }

    Box(
        modifier = modifier
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .background(theme.pageBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Book running header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif),
                    color = theme.mutedText.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (spread && rightPageNum != null) "pp. $leftPageNum–$rightPageNum / $totalPages"
                    else "p. $leftPageNum / $totalPages",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif),
                    color = theme.mutedText.copy(alpha = 0.72f),
                )
            }

            if (spread) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    PageBitmapSurface(
                        bmp = leftBmp,
                        colorFilter = colorFilter,
                        theme = theme,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    // Open-book spine gutter shadow
                    Box(
                        Modifier
                            .width(12.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.16f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    if (rightPageNum != null) {
                        PageBitmapSurface(
                            bmp = rightBmp,
                            colorFilter = colorFilter,
                            theme = theme,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    } else {
                        Box(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else {
                PageBitmapSurface(
                    bmp = leftBmp,
                    colorFilter = colorFilter,
                    theme = theme,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PageBitmapSurface(
    bmp: Bitmap?,
    colorFilter: ColorFilter?,
    theme: KindleTheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.pageBg),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Book page",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                "Turning page…",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                color = theme.mutedText,
            )
        }
    }
}
