package com.pronotes.app.editor

import android.annotation.SuppressLint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * on the active note page. Reads [viewportTick] locally inside the card so panning/zooming never recomposes
 * the surrounding editor chrome.
 */
@Composable
internal fun EmbeddedWebPagesOverlay(
    vm: EditorViewModel,
    canvas: NoteCanvasView?,
    areaSize: IntSize,
    viewportTick: () -> Int,
) {
    if (canvas == null || areaSize.width <= 0 || areaSize.height <= 0) return
    val pageIndex = vm.pageIndex
    val page = vm.pages.getOrNull(pageIndex) ?: return
    val embeddedItems = remember(page.items) {
        page.items.filterIsInstance<WebLinkItem>().filter { it.embedded && isAllowedWebPageUrl(it.url) }
    }
    if (embeddedItems.isEmpty()) return

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
                    viewportTick = viewportTick,
                    readOnly = vm.readOnly,
                    onMoveOrResize = { nx, ny, nw, nh, commit ->
                        vm.updateWebLinkGeometry(pageIndex, item.id, nx, ny, nw, nh, commit)
                    },
                    onEditUrl = { vm.openWebLink(pageIndex, item.x, item.y, item) },
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
    viewportTick: () -> Int,
    readOnly: Boolean,
    onMoveOrResize: (Float, Float, Float, Float, Boolean) -> Unit,
    onEditUrl: () -> Unit,
    onCollapseToCard: () -> Unit,
    onOpenExternal: () -> Unit,
    onDelete: () -> Unit,
) {
    var dragging by remember(item.id) { mutableStateOf(false) }
    var localX by remember(item.id) { mutableFloatStateOf(item.x) }
    var localY by remember(item.id) { mutableFloatStateOf(item.y) }
    var localW by remember(item.id) { mutableFloatStateOf(item.w) }
    var localH by remember(item.id) { mutableFloatStateOf(item.h) }

    // Keep synced when external updates (undo/redo, lasso resize, edit dialog) change item geometry
    LaunchedEffect(item.x, item.y, item.w, item.h) {
        if (!dragging) {
            localX = item.x
            localY = item.y
            localW = item.w
            localH = item.h
        }
    }

    val currentOnMoveOrResize by rememberUpdatedState(onMoveOrResize)

    // Read viewportTick ONLY inside this composable scope so pan/zoom never invalidates EditorScreen
    val tick = viewportTick()
    val screenRect = remember(localX, localY, localW, localH, pageIndex, tick) {
        canvas.pageToScreen(pageIndex, RectF(localX, localY, localX + localW, localY + localH))
    } ?: return

    val visible = screenRect.right > -48f &&
        screenRect.left < areaSize.width + 48f &&
        screenRect.bottom > -48f &&
        screenRect.top < areaSize.height + 48f
    if (!visible) return

    val widthDp = (screenRect.width() / density).coerceIn(160f, 2400f).dp
    val heightDp = (screenRect.height() / density).coerceIn(140f, 2400f).dp
    val accentColor = Color(item.color)

    var webViewRef by remember(item.id) { mutableStateOf<WebView?>(null) }
    var canGoBack by remember(item.id) { mutableStateOf(false) }
    var canGoForward by remember(item.id) { mutableStateOf(false) }
    var loadProgress by remember(item.id) { mutableIntStateOf(0) }
    var pageError by remember(item.id) { mutableStateOf<String?>(null) }
    var currentTitle by remember(item.id, item.title) { mutableStateOf(item.title.ifBlank { item.domain }) }
    var activePageUrl by remember(item.id, item.url) { mutableStateOf(sanitizeWebPageUrl(item.url)) }
    var reloadNonce by remember(item.id) { mutableIntStateOf(0) }

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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.25.dp, accentColor.copy(alpha = 0.45f)),
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
                        .height(38.dp)
                        .background(accentColor.copy(alpha = 0.13f))
                        .then(
                            if (!readOnly) {
                                Modifier.pointerInput(item.id, pageWidth, pageHeight) {
                                    detectDragGestures(
                                        onDragStart = { dragging = true },
                                        onDragCancel = {
                                            dragging = false
                                            currentOnMoveOrResize(localX, localY, localW, localH, true)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val invScale = 1f / canvas.scale.coerceAtLeast(0.15f)
                                            localX = (localX + dragAmount.x * invScale).coerceIn(8f, (pageWidth - localW - 8f).coerceAtLeast(8f))
                                            localY = (localY + dragAmount.y * invScale).coerceIn(8f, (pageHeight - localH - 8f).coerceAtLeast(8f))
                                            currentOnMoveOrResize(localX, localY, localW, localH, false)
                                        },
                                        onDragEnd = {
                                            dragging = false
                                            currentOnMoveOrResize(localX, localY, localW, localH, true)
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
                            .then(
                                if (!readOnly) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onEditUrl)
                                else Modifier,
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    WebMiniIcon(
                        enabled = canGoBack,
                        onClick = { pageError = null; webViewRef?.goBack() },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.size(15.dp))
                    }
                    WebMiniIcon(
                        enabled = canGoForward,
                        onClick = { pageError = null; webViewRef?.goForward() },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Forward", Modifier.size(15.dp))
                    }
                    WebMiniIcon(onClick = {
                        pageError = null
                        val wv = webViewRef
                        if (wv != null && !wv.url.isNullOrBlank()) {
                            wv.reload()
                        } else {
                            reloadNonce++
                        }
                    }) {
                        Icon(Icons.Rounded.Refresh, "Reload", Modifier.size(15.dp))
                    }
                    if (!readOnly) {
                        WebMiniIcon(onClick = onEditUrl) {
                            Icon(Icons.Rounded.Edit, "Edit URL", Modifier.size(14.dp))
                        }
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

                AnimatedVisibility(
                    visible = loadProgress in 1..99,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    LinearProgressIndicator(
                        progress = { (loadProgress / 100f).coerceIn(0.04f, 1f) },
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                    )
                }

                Box(Modifier.fillMaxWidth().weight(1f)) {
                    // Live interactive WebView body (consumes its own scroll/touch inside the frame)
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
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
                                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        safeBrowsingEnabled = true
                                    }
                                }
                                runCatching {
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadProgress = if (newProgress >= 100) 0 else newProgress
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        val clean = title?.trim().orEmpty()
                                        if (clean.isNotBlank() && clean != "about:blank" && !clean.startsWith("http")) {
                                            currentTitle = clean
                                        }
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val target = request?.url?.toString().orEmpty()
                                        return !isAllowedWebPageUrl(target)
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        if (!url.isNullOrBlank()) activePageUrl = url
                                        pageError = null
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        loadProgress = 0
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                        val pageTitle = view?.title?.trim().orEmpty()
                                        if (pageTitle.isNotBlank() && pageTitle != "about:blank") {
                                            currentTitle = pageTitle
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?,
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            loadProgress = 0
                                            val desc = error?.description?.toString()?.takeIf { it.isNotBlank() } ?: "Page unavailable"
                                            pageError = desc
                                        }
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        errorResponse: WebResourceResponse?,
                                    ) {
                                        if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                                            val code = errorResponse?.statusCode ?: 404
                                            if (code == 403 || code == 404 || code >= 500) {
                                                pageError = "This site blocked embedding (HTTP $code). Open it in your browser instead."
                                            }
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: android.webkit.RenderProcessGoneDetail?,
                                    ): Boolean {
                                        webViewRef = null
                                        pageError = "Web view paused to save memory"
                                        return true
                                    }
                                }
                                setOnTouchListener { v, ev ->
                                    if (ev.actionMasked == MotionEvent.ACTION_DOWN || ev.actionMasked == MotionEvent.ACTION_MOVE) {
                                        v.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    false
                                }
                                val safeUrl = sanitizeWebPageUrl(item.url)
                                if (safeUrl.isNotEmpty()) {
                                    tag = safeUrl
                                    loadUrl(safeUrl)
                                }
                                webViewRef = this
                            }
                        },
                        update = { view ->
                            @Suppress("UNUSED_VARIABLE")
                            val nonce = reloadNonce
                            val safeUrl = sanitizeWebPageUrl(item.url)
                            val prevTarget = view.tag as? String
                            if (safeUrl.isNotEmpty() && (prevTarget != safeUrl || view.url.isNullOrBlank())) {
                                pageError = null
                                view.tag = safeUrl
                                view.loadUrl(safeUrl)
                            }
                        },
                    )

                    pageError?.let { msg ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Public,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(30.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        pageError = null
                                        val wv = webViewRef
                                        val safeUrl = sanitizeWebPageUrl(item.url)
                                        if (wv != null && safeUrl.isNotEmpty()) wv.loadUrl(safeUrl) else reloadNonce++
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text("Retry", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = onOpenExternal,
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text("Open in browser", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom-right resize corner grip with generous touch target
            if (!readOnly) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.88f))
                        .pointerInput(item.id, pageWidth, pageHeight) {
                            detectDragGestures(
                                onDragStart = { dragging = true },
                                onDragCancel = {
                                    dragging = false
                                    currentOnMoveOrResize(localX, localY, localW, localH, true)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val invScale = 1f / canvas.scale.coerceAtLeast(0.15f)
                                    localW = (localW + dragAmount.x * invScale).coerceIn(240f, (pageWidth - localX - 8f).coerceAtLeast(240f))
                                    localH = (localH + dragAmount.y * invScale).coerceIn(200f, (pageHeight - localY - 8f).coerceAtLeast(200f))
                                    currentOnMoveOrResize(localX, localY, localW, localH, false)
                                },
                                onDragEnd = {
                                    dragging = false
                                    currentOnMoveOrResize(localX, localY, localW, localH, true)
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
            .size(28.dp)
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
