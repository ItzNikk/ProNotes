package com.pronotes.app.editor

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pronotes.app.data.ScrollDirection
import com.pronotes.app.library.PdfSkeletonPreviewBox
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppMenuButton
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Writable Split View pane displaying a secondary notebook alongside the active workspace.
 */
@Composable
internal fun SplitPanel(
    noteId: String,
    mainPageIndex: Int,
    scrollDirection: ScrollDirection,
    others: List<Pair<String, String>>,
    onPick: (String) -> Unit,
    onOpen: (String) -> Unit,
    onSwap: () -> Unit,
    onRatioPreset: (Float) -> Unit,
    onClipToSticky: (String, String) -> Unit,
    onInsertToPage: (String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val app = LocalContext.current.applicationContext as Application
    val reduceMotion = LocalReduceMotion.current
    val vm2: EditorViewModel = viewModel(
        key = "split-$noteId",
        factory = viewModelFactory { initializer { EditorViewModel(app, noteId) } },
    )
    var splitZoom by remember(noteId) { mutableFloatStateOf(1f) }
    var splitPan by remember(noteId) { mutableStateOf(Offset.Zero) }
    var showSplitSearch by remember(noteId) { mutableStateOf(false) }
    var syncPage by remember(noteId) { mutableStateOf(false) }
    var writeMode by remember(noteId) { mutableStateOf(false) }
    var inkKind by remember(noteId) { mutableStateOf("pen") }
    var inkColor by remember(noteId) { mutableIntStateOf(0xFF1B1F2A.toInt()) }

    val listState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val visiblePage by remember(listState, vm2.pages.size) {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, (vm2.pages.size - 1).coerceAtLeast(0)) }
    }

    LaunchedEffect(splitZoom) {
        if (splitZoom <= 1.0f) splitPan = Offset.Zero
    }
    LaunchedEffect(mainPageIndex, syncPage, vm2.pages.size) {
        if (syncPage && vm2.pages.isNotEmpty()) {
            val target = mainPageIndex.coerceIn(0, vm2.pages.lastIndex)
            if (reduceMotion) listState.scrollToItem(target) else listState.animateScrollToItem(target)
        }
    }
    LaunchedEffect(vm2.activeHit, vm2.searchHits) {
        val hit = vm2.searchHits.getOrNull(vm2.activeHit) ?: return@LaunchedEffect
        val target = hit.page.coerceIn(0, (vm2.pages.size - 1).coerceAtLeast(0))
        if (reduceMotion) listState.scrollToItem(target) else listState.animateScrollToItem(target)
    }

    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.weight(1f)) {
                AppMenuButton(
                    role = SurfaceRole.Control,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    buttonContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.MenuBook, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(vm2.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(16.dp))
                        }
                    },
                    items = others.map { (id, t) -> AppMenuItem(t) { onPick(id) } },
                )
            }

            Row(
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSwap, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.SwapHoriz, "Swap Panes", Modifier.size(16.dp))
                }
                TextButton(onClick = { onRatioPreset(1f) }, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.height(28.dp)) {
                    Text("1:1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { onRatioPreset(0.43f) }, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.height(28.dp)) {
                    Text("70:30", style = MaterialTheme.typography.labelSmall)
                }
            }

            AppPill(
                selected = writeMode,
                tint = ProButtonPalette.ElectricBlue,
                onClick = { writeMode = !writeMode },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Icon(
                    if (writeMode) Icons.Outlined.Brush else Icons.Outlined.MenuBook,
                    null,
                    Modifier.size(13.dp),
                    tint = if (writeMode) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (writeMode) "Write" else "Read",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (writeMode) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }

            if (writeMode) {
                IconButton(enabled = vm2.canUndo, onClick = vm2::undo, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, "Undo", Modifier.size(17.dp))
                }
            }

            AppMenuButton(
                role = SurfaceRole.Control,
                contentPadding = PaddingValues(6.dp),
                buttonContent = { Icon(Icons.Outlined.MoreHoriz, "Split Options", Modifier.size(18.dp)) },
                items = listOf(
                    AppMenuItem(
                        label = if (showSplitSearch) "Hide Search" else "Search in Note",
                        icon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                    ) {
                        showSplitSearch = !showSplitSearch
                        if (!showSplitSearch) vm2.clearSearch()
                    },
                    AppMenuItem(
                        label = "Insert Page Text to Note",
                        icon = { Icon(Icons.Outlined.PostAdd, null, Modifier.size(18.dp)) },
                    ) {
                        val extracted = vm2.extractPageSummaryText(visiblePage)
                        onInsertToPage(extracted.ifBlank { "Notes from ${vm2.title} (Page ${visiblePage + 1})" }, vm2.title)
                    },
                    AppMenuItem(
                        label = "Clip Page as Sticky",
                        icon = { Icon(Icons.Outlined.PushPin, null, Modifier.size(18.dp)) },
                    ) {
                        val extracted = vm2.extractPageSummaryText(visiblePage)
                        onClipToSticky(extracted.ifBlank { "Reference from ${vm2.title} (Page ${visiblePage + 1})" }, vm2.title)
                    },
                    AppMenuItem(
                        label = if (syncPage) "Sync Page Scrolling: On" else "Sync Page Scrolling: Off",
                        icon = { Icon(Icons.Outlined.SwapVert, null, Modifier.size(18.dp)) },
                    ) { syncPage = !syncPage },
                    AppMenuItem(
                        label = "Open Fullscreen",
                        icon = { Icon(Icons.Outlined.UnfoldMore, null, Modifier.size(18.dp)) },
                    ) { onOpen(noteId) },
                    AppMenuItem(
                        label = "Close Split View",
                        destructive = true,
                        icon = { Icon(Icons.Filled.Close, null, Modifier.size(18.dp)) },
                    ) { onClose() },
                ),
            )
        }

        AnimatedVisibility(writeMode) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("pen" to "Pen", "highlighter" to "Highlight", "eraser" to "Eraser").forEach { (k, label) ->
                    AppChip(selected = inkKind == k, onClick = { inkKind = k }, label = label)
                }
                if (inkKind != "eraser") {
                    Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    listOf(0xFF1B1F2A, 0xFF3D6CF4, 0xFFEF4444, 0xFF10B981, 0xFFFFE066).forEach { c ->
                        val ci = c.toInt()
                        Swatch(ci, inkColor == ci, 16) { inkColor = ci }
                    }
                }
            }
        }

        AnimatedVisibility(showSplitSearch) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = vm2.searchQuery,
                    onValueChange = vm2::search,
                    singleLine = true,
                    placeholder = { com.pronotes.app.ui.AnimatedGradientSearchPlaceholder("Search inside ${vm2.title}…") },
                    shape = CircleShape,
                    modifier = Modifier.weight(1f),
                )
                if (vm2.searchHits.isNotEmpty()) {
                    Text("${vm2.activeHit + 1}/${vm2.searchHits.size}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp))
                    IconButton(onClick = { vm2.stepHit(-1) }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Previous match", Modifier.size(16.dp))
                    }
                    IconButton(onClick = { vm2.stepHit(1) }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Next match", Modifier.size(16.dp))
                    }
                }
            }
        }

        if (vm2.pages.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    enabled = visiblePage > 0,
                    onClick = { scope.launch { listState.animateScrollToItem((visiblePage - 1).coerceAtLeast(0)) } },
                    modifier = Modifier.size(26.dp),
                ) { Icon(Icons.Filled.KeyboardArrowUp, "Previous", Modifier.size(16.dp).graphicsLayer { rotationZ = -90f }) }
                Text("p.${visiblePage + 1}/${vm2.pages.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(
                    enabled = visiblePage < vm2.pages.size - 1,
                    onClick = { scope.launch { listState.animateScrollToItem((visiblePage + 1).coerceAtMost(vm2.pages.size - 1)) } },
                    modifier = Modifier.size(26.dp),
                ) { Icon(Icons.Filled.KeyboardArrowUp, "Next", Modifier.size(16.dp).graphicsLayer { rotationZ = 90f }) }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { splitZoom = (splitZoom - 0.15f).coerceIn(0.35f, 3.0f) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp),
                ) { Text("-", style = MaterialTheme.typography.titleSmall) }
                Text(
                    "${(splitZoom * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable { splitZoom = 1f; splitPan = Offset.Zero },
                )
                TextButton(
                    onClick = { splitZoom = (splitZoom + 0.15f).coerceIn(0.35f, 3.0f) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp),
                ) { Text("+", style = MaterialTheme.typography.titleSmall) }
            }
        }

        val renderW = if (splitZoom > 1.35f) 1400 else 860
        BoxWithConstraints(
            Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val viewW = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(120f)
            val viewH = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(120f)
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(viewW, viewH, scrollDirection, writeMode) {
                        awaitEachGesture {
                            var pinching = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val downCount = event.changes.count { it.pressed }
                                if (downCount == 0) break
                                if (downCount >= 2) {
                                    val z = event.calculateZoom()
                                    val p = event.calculatePan()
                                    if (z != 1f || p != Offset.Zero) {
                                        val nextZoom = (splitZoom * z).coerceIn(0.35f, 3.0f)
                                        val maxPanX = ((nextZoom - 1f).coerceAtLeast(0f) * viewW * 0.55f)
                                        val maxPanY = ((nextZoom - 1f).coerceAtLeast(0f) * viewH * 0.50f)
                                        splitZoom = nextZoom
                                        splitPan = if (nextZoom <= 1.0f) Offset.Zero else Offset(
                                            (splitPan.x + p.x).coerceIn(-maxPanX, maxPanX),
                                            (splitPan.y + p.y).coerceIn(-maxPanY, maxPanY),
                                        )
                                        event.changes.forEach { it.consume() }
                                        pinching = true
                                    }
                                } else if (splitZoom > 1.03f && !writeMode) {
                                    val p = event.calculatePan()
                                    val allowSinglePan = if (scrollDirection == ScrollDirection.HORIZONTAL) {
                                        pinching || abs(p.y) > abs(p.x) * 1.15f
                                    } else {
                                        pinching || abs(p.x) > abs(p.y) * 1.15f
                                    }
                                    if (allowSinglePan && p != Offset.Zero) {
                                        val maxPanX = (splitZoom - 1f) * viewW * 0.55f
                                        val maxPanY = (splitZoom - 1f) * viewH * 0.50f
                                        splitPan = Offset(
                                            (splitPan.x + p.x).coerceIn(-maxPanX, maxPanX),
                                            (splitPan.y + p.y).coerceIn(-maxPanY, maxPanY),
                                        )
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(writeMode) {
                        if (!writeMode) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (abs(splitZoom - 1f) > 0.08f) {
                                        splitZoom = 1f
                                        splitPan = Offset.Zero
                                    } else {
                                        splitZoom = 1.65f
                                    }
                                },
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val transformMod = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = splitZoom
                        scaleY = splitZoom
                        translationX = splitPan.x
                        translationY = splitPan.y
                    }

                @Composable
                fun SplitPageCard(i: Int, page: Page, cardModifier: Modifier) {
                    val bmp by produceState<Bitmap?>(null, i, page, vm2.pages.size, renderW) { value = vm2.thumbnail(i, renderW) }
                    var pageBoxSize by remember { mutableStateOf(IntSize.Zero) }
                    var livePts by remember(i) { mutableStateOf<List<Offset>>(emptyList()) }
                    Box(
                        modifier = cardModifier
                            .shadow(2.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .onSizeChanged { pageBoxSize = it },
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            bmp?.let { Image(it.asImageBitmap(), "Page ${i + 1}", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
                                ?: run {
                                    val shimmer = if (reduceMotion) 0f else {
                                        rememberInfiniteTransition(label = "splitPdfShimmer").animateFloat(
                                            initialValue = 0f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart),
                                            label = "sweep",
                                        ).value
                                    }
                                    PdfSkeletonPreviewBox(
                                        shimmer = shimmer,
                                        modifier = Modifier.fillMaxWidth().aspectRatio((page.width / page.height).coerceIn(0.45f, 1.8f)),
                                    )
                                }

                            if (writeMode) {
                                Box(
                                    Modifier.matchParentSize().pointerInput(i, pageBoxSize, inkKind, inkColor) {
                                        detectDragGestures(
                                            onDragStart = { start -> livePts = listOf(start) },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                livePts = livePts + change.position
                                            },
                                            onDragEnd = {
                                                val bw = pageBoxSize.width.toFloat().coerceAtLeast(1f)
                                                val bh = pageBoxSize.height.toFloat().coerceAtLeast(1f)
                                                val pagePts = livePts.map { pt ->
                                                    (pt.x / bw * page.width) to (pt.y / bh * page.height)
                                                }
                                                livePts = emptyList()
                                                if (pagePts.size >= 2) {
                                                    if (inkKind == "eraser") {
                                                        val hitIds = page.items.filterIsInstance<InkItem>().filter { item ->
                                                            pagePts.any { (px, py) ->
                                                                val b = item.bounds
                                                                px >= b.left - 24f && px <= b.right + 24f && py >= b.top - 24f && py <= b.bottom + 24f
                                                            }
                                                        }.map { it.id }.toSet()
                                                        if (hitIds.isNotEmpty()) {
                                                            vm2.beginEdit(i)
                                                            vm2.applySplitItems(i, page.items.filterNot { it.id in hitIds })
                                                        }
                                                    } else {
                                                        val brush = if (inkKind == "highlighter") {
                                                            Brushes.create(Brushes.HIGHLIGHTER, inkColor, 26f)
                                                        } else {
                                                            Brushes.create(Brushes.PEN, inkColor, 4.5f)
                                                        }
                                                        val stroke = InkCodec.strokeFromPoints(pagePts, brush)
                                                        vm2.beginEdit(i)
                                                        vm2.applySplitItems(i, page.items + InkItem(ItemIds.next(), stroke))
                                                    }
                                                }
                                            },
                                            onDragCancel = { livePts = emptyList() },
                                        )
                                    },
                                )
                                if (livePts.size > 1) {
                                    Canvas(Modifier.matchParentSize()) {
                                        val path = Path().apply {
                                            moveTo(livePts.first().x, livePts.first().y)
                                            for (k in 1 until livePts.size) lineTo(livePts[k].x, livePts[k].y)
                                        }
                                        val col = if (inkKind == "eraser") Color(0x88EF4444) else if (inkKind == "highlighter") Color(inkColor).copy(alpha = 0.42f) else Color(inkColor)
                                        val w = if (inkKind == "highlighter") 18f else if (inkKind == "eraser") 12f else 4f
                                        drawPath(path, col, style = Stroke(width = w, cap = StrokeCap.Round))
                                    }
                                }
                            }
                        }
                    }
                }

                if (scrollDirection == ScrollDirection.HORIZONTAL) {
                    LazyRow(
                        state = listState,
                        flingBehavior = snapBehavior,
                        modifier = transformMod,
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(vm2.pages.size, key = { vm2.pages[it].id }) { i ->
                            SplitPageCard(i, vm2.pages[i], Modifier.fillParentMaxWidth(0.95f))
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = transformMod,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(vm2.pages.size, key = { vm2.pages[it].id }) { i ->
                            SplitPageCard(i, vm2.pages[i], Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
