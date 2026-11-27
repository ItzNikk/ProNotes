package com.pronotes.app.editor

import android.annotation.SuppressLint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

/** Strict URL allowlist for in-page WebViews: only http and https schemes with a valid host. */
internal fun isAllowedWebPageUrl(raw: String): Boolean {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return false
    val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "https" && scheme != "http") return false
    return !uri.host.isNullOrBlank()
}

internal fun sanitizeWebPageUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    return if (isAllowedWebPageUrl(withScheme)) withScheme else ""
}

/**
 * Interactive, movable, resizable in-page WebView frame for every [WebLinkItem] marked [WebLinkItem.embedded]
 * on the active note page. Users can scroll and interact directly inside the website while dragging the top
 * header bar to reposition it or the bottom-right corner grip to resize it on the page.
 */
@Composable
internal fun EmbeddedWebPagesOverlay(
    vm: EditorViewModel,
    canvas: NoteCanvasView?,
    areaSize: IntSize,
    viewportTick: Int,
) {
    if (canvas == null || areaSize.width <= 0 || areaSize.height <= 0) return
    val pageIndex = vm.pageIndex
    val page = vm.pages.getOrNull(pageIndex) ?: return
    val embeddedItems = remember(page.items) {
        page.items.filterIsInstance<WebLinkItem>().filter { it.embedded && isAllowedWebPageUrl(it.url) }
    }
    if (embeddedItems.isEmpty()) return

    // Read viewportTick so panning/zooming the canvas keeps frames anchored in lockstep.
    @Suppress("UNUSED_VARIABLE")
    val tick = viewportTick
    val density = LocalDensity.current.density

    Box(Modifier.fillMaxSize()) {
        embeddedItems.forEach { item ->
            key(item.id) {
                EmbeddedWebPageCard(
                    item = item,
                    pageIndex = pageIndex,
                    pageWidth = page.width,
                    pageHeight = page.height,
                    canvas = canvas,
                    areaSize = areaSize,
                    density = density,
                    viewportTick = tick,
                    readOnly = vm.readOnly,
                    onMoveOrResize = { nx, ny, nw, nh, commit ->
                        vm.updateWebLinkGeometry(pageIndex, item.id, nx, ny, nw, nh, commit)
                    },
                    onCollapseToCard = { vm.setWebLinkEmbedded(pageIndex, item.id, false) },
                    onOpenExternal = { vm.openExternalUrl(item.url) },
                    onDelete = { vm.removeWebLinkItem(pageIndex, item.id) },
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun EmbeddedWebPageCard(
    item: WebLinkItem,
    pageIndex: Int,
    pageWidth: Float,
    pageHeight: Float,
    canvas: NoteCanvasView,
    areaSize: IntSize,
    density: Float,
    viewportTick: Int,
    readOnly: Boolean,
    onMoveOrResize: (Float, Float, Float, Float, Boolean) -> Unit,
    onCollapseToCard: () -> Unit,
    onOpenExternal: () -> Unit,
    onDelete: () -> Unit,
) {
    var localX by remember(item.id, item.x) { mutableFloatStateOf(item.x) }
    var localY by remember(item.id, item.y) { mutableFloatStateOf(item.y) }
    var localW by remember(item.id, item.w) { mutableFloatStateOf(item.w) }
    var localH by remember(item.id, item.h) { mutableFloatStateOf(item.h) }

    // Map current page-space rect to screen pixels.
    @Suppress("UNUSED_VARIABLE")
    val tick = viewportTick
    val screenRect = remember(localX, localY, localW, localH, pageIndex, tick) {
        canvas.pageToScreen(pageIndex, RectF(localX, localY, localX + localW, localY + localH))
    } ?: return

    // Hide gracefully when scrolled completely off-screen.
    val visible = screenRect.right > -40f &&
        screenRect.left < areaSize.width + 40f &&
        screenRect.bottom > -40f &&
        screenRect.top < areaSize.height + 40f
    if (!visible) return

    val widthDp = (screenRect.width() / density).coerceIn(220f, 1200f).dp
    val heightDp = (screenRect.height() / density).coerceIn(180f, 1200f).dp
    val accentColor = Color(item.color)

    var webViewRef by remember(item.id) { mutableStateOf<WebView?>(null) }
    var canGoBack by remember(item.id) { mutableStateOf(false) }
    var canGoForward by remember(item.id) { mutableStateOf(false) }
    var currentTitle by remember(item.id, item.title) { mutableStateOf(item.title.ifBlank { item.domain }) }
    var loadTick by remember(item.id) { mutableIntStateOf(0) }

    DisposableEffect(item.id) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.25.dp, accentColor.copy(alpha = 0.55f)),
        shadowElevation = 6.dp,
        modifier = Modifier
            .offset { IntOffset(screenRect.left.roundToInt(), screenRect.top.roundToInt()) }
            .width(widthDp)
            .height(heightDp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // Draggable top browser bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(accentColor.copy(alpha = 0.14f))
                        .then(
                            if (!readOnly) {
                                Modifier.pointerInput(item.id, pageWidth, pageHeight) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val invScale = 1f / canvas.scale.coerceAtLeast(0.2f)
                                            localX = (localX + dragAmount.x * invScale).coerceIn(12f, (pageWidth - localW - 12f).coerceAtLeast(12f))
                                            localY = (localY + dragAmount.y * invScale).coerceIn(12f, (pageHeight - localH - 12f).coerceAtLeast(12f))
                                            onMoveOrResize(localX, localY, localW, localH, false)
                                        },
                                        onDragEnd = {
                                            onMoveOrResize(localX, localY, localW, localH, true)
                                        },
                                    )
                                }
                            } else Modifier,
                        )
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragIndicator,
                        contentDescription = "Drag web page",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                    )
                    WebMiniIcon(
                        enabled = canGoBack,
                        onClick = { webViewRef?.goBack() },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.size(15.dp))
                    }
                    WebMiniIcon(
                        enabled = canGoForward,
                        onClick = { webViewRef?.goForward() },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Forward", Modifier.size(15.dp))
                    }
                    WebMiniIcon(onClick = { webViewRef?.reload(); loadTick++ }) {
                        Icon(Icons.Rounded.Refresh, "Reload", Modifier.size(15.dp))
                    }
                    WebMiniIcon(onClick = onOpenExternal) {
                        Icon(Icons.Rounded.OpenInNew, "Open in browser", Modifier.size(14.dp))
                    }
                    if (!readOnly) {
                        WebMiniIcon(onClick = onCollapseToCard) {
                            Icon(Icons.Rounded.UnfoldLess, "Collapse to card", Modifier.size(15.dp))
                        }
                        WebMiniIcon(onClick = onDelete) {
                            Icon(Icons.Rounded.Close, "Remove web page", Modifier.size(15.dp))
                        }
                    }
                }

                // Live interactive WebView body (consumes its own scroll/touch inside the frame)
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                // Strict security hardening: block local file/content access and mixed content
                                allowFileAccess = false
                                allowContentAccess = false
                                @Suppress("DEPRECATION")
                                allowFileAccessFromFileURLs = false
                                @Suppress("DEPRECATION")
                                allowUniversalAccessFromFileURLs = false
                                javaScriptCanOpenWindowsAutomatically = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    safeBrowsingEnabled = true
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val target = request?.url?.toString().orEmpty()
                                    // Block non-http(s) schemes (javascript:, file:, content:, intent:, data:)
                                    return !isAllowedWebPageUrl(target)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                    val pageTitle = view?.title?.trim().orEmpty()
                                    if (pageTitle.isNotBlank() && pageTitle != "about:blank") {
                                        currentTitle = pageTitle
                                    }
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: android.webkit.RenderProcessGoneDetail?,
                                ): Boolean {
                                    webViewRef = null
                                    return true
                                }
                            }
                            // Prevent parent canvas from stealing scroll/tap gestures while interacting with the website
                            setOnTouchListener { v, ev ->
                                if (ev.actionMasked == MotionEvent.ACTION_DOWN || ev.actionMasked == MotionEvent.ACTION_MOVE) {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                false
                            }
                            val safeUrl = sanitizeWebPageUrl(item.url)
                            if (safeUrl.isNotEmpty()) loadUrl(safeUrl)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        @Suppress("UNUSED_VARIABLE")
                        val ignored = loadTick
                        val safeUrl = sanitizeWebPageUrl(item.url)
                        val loaded = view.url
                        if (safeUrl.isNotEmpty() && loaded.isNullOrBlank()) {
                            view.loadUrl(safeUrl)
                        }
                    },
                )
            }

            // Bottom-right resize corner grip
            if (!readOnly) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.85f))
                        .pointerInput(item.id, pageWidth, pageHeight) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val invScale = 1f / canvas.scale.coerceAtLeast(0.2f)
                                    localW = (localW + dragAmount.x * invScale).coerceIn(280f, (pageWidth - localX - 12f).coerceAtLeast(280f))
                                    localH = (localH + dragAmount.y * invScale).coerceIn(220f, (pageHeight - localY - 12f).coerceAtLeast(220f))
                                    onMoveOrResize(localX, localY, localW, localH, false)
                                },
                                onDragEnd = {
                                    onMoveOrResize(localX, localY, localW, localH, true)
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↘", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WebMiniIcon(
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.85f else 0.30f),
        ) {
            content()
        }
    }
}
