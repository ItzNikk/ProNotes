package com.pronotes.app.reader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.R
import com.pronotes.app.data.NotebookMeta
import com.pronotes.app.data.Repository
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.editor.Page
import com.pronotes.app.editor.PageRenderer
import com.pronotes.app.editor.PdfPages
import com.pronotes.app.editor.PdfTextIndex
import com.pronotes.app.editor.SearchHit
import com.pronotes.app.editor.StickyItem
import com.pronotes.app.editor.TextItem
import com.pronotes.app.library.CoverArt
import com.pronotes.app.library.PdfImportLoaderOverlay
import com.pronotes.app.ui.AppBottomBar
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSheet
import com.pronotes.app.ui.AppSliderRow
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppTabItem
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.appCardShape
import com.pronotes.app.ui.appContentBottomPadding
import com.pronotes.app.ui.appPillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// BookFlipSoundEngine extracted to BookReaderCanvas.kt

enum class BookTheme(
    val label: String,
    val deskBg: Color,
    val pageBg: Color,
    val chromeBg: Color,
    val inkText: Color,
    val mutedText: Color,
    val accent: Color,
    val isDark: Boolean,
) {
    PAPER(
        label = "Paper",
        deskBg = Color(0xFFE6E2D8),
        pageBg = Color(0xFFFDFBF6),
        chromeBg = Color(0xF5F6F3EC),
        inkText = Color(0xFF1B1F1D),
        mutedText = Color(0xFF5A615B),
        accent = Color(0xFF1A84FF),
        isDark = false,
    ),
    WARM_IVORY(
        label = "Ivory",
        deskBg = Color(0xFFE4DDD0),
        pageBg = Color(0xFFF8F2E4),
        chromeBg = Color(0xF5F3ECD8),
        inkText = Color(0xFF2B241A),
        mutedText = Color(0xFF665A48),
        accent = Color(0xFF24483F),
        isDark = false,
    ),
    SEPIA(
        label = "Sepia",
        deskBg = Color(0xFFDCCCAE),
        pageBg = Color(0xFFF3E6CE),
        chromeBg = Color(0xF5ECE0C6),
        inkText = Color(0xFF392B18),
        mutedText = Color(0xFF6B563E),
        accent = Color(0xFFB45309),
        isDark = false,
    ),
    FOREST_SAGE(
        label = "Sage",
        deskBg = Color(0xFFCED8CF),
        pageBg = Color(0xFFEAF1EB),
        chromeBg = Color(0xF5E3ECE4),
        inkText = Color(0xFF1A2920),
        mutedText = Color(0xFF4B6353),
        accent = Color(0xFF0D9488),
        isDark = false,
    ),
    NIGHT(
        label = "Night",
        deskBg = Color(0xFF0A0B0E),
        pageBg = Color(0xFF14161B),
        chromeBg = Color(0xF51B1E26),
        inkText = Color(0xFFE5E7EB),
        mutedText = Color(0xFF9CA3AF),
        accent = Color(0xFF4FB0F5),
        isDark = true,
    ),
    AMOLED_BLACK(
        label = "OLED",
        deskBg = Color(0xFF000000),
        pageBg = Color(0xFF050608),
        chromeBg = Color(0xF50E1015),
        inkText = Color(0xFFD8DEE9),
        mutedText = Color(0xFF7C8698),
        accent = Color(0xFF38BDF8),
        isDark = true,
    ),
}

enum class BookFlipStyle(val label: String) {
    REALISTIC_CURL("3D Curl"),
    SPINE_FLIP("3D Flip"),
    CRISP_SLIDE("Slide"),
    INSTANT("Instant"),
}

enum class BookFlipSpeed(val label: String, val durationMs: Int) {
    FAST("Fast", 280),
    NATURAL("Natural", 440),
    RELAXED("Relaxed", 620),
}

enum class BookMargin(val label: String, val padDp: Int) {
    COMPACT("Compact", 6),
    CLASSIC("Classic", 16),
    WIDE("Wide", 30),
}

/**
 * Dedicated standalone Book Reading Room separated from the regular notebook library & editor.
 */
@Composable
fun BookReaderHubScreen(
    onOpenBook: (String) -> Unit,
    onBackToLibrary: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val store = remember { SettingsStore.get(context) }
    val library by repo.library.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var filterTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = PDF Books, 2 = Notebooks
    var query by remember { mutableStateOf("") }
    var pdfImportProgress by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }

    val importBookLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
                val id = if (mime.contains("pdf")) {
                    repo.importPdf(uri, null) { title, page, total ->
                        pdfImportProgress = Triple(title, page, total)
                    }
                } else {
                    repo.importFiles(listOf(uri), null) { title, page, total ->
                        pdfImportProgress = Triple(title, page, total)
                    }
                }
                pdfImportProgress = null
                if (id != null) onOpenBook(id)
            }
        }
    }

    val allActive = remember(library.notebooks) {
        library.notebooks
            .filter { it.deletedAt == null }
            .sortedWith(compareByDescending<NotebookMeta> { it.fromPdf }.thenByDescending { it.opened })
    }
    val pdfBooks = remember(allActive) { allActive.filter { it.fromPdf } }
    val noteBooks = remember(allActive) { allActive.filter { !it.fromPdf } }

    val displayed = remember(allActive, pdfBooks, noteBooks, filterTab, query) {
        val base = when (filterTab) {
            1 -> pdfBooks
            2 -> noteBooks
            else -> allActive
        }
        val q = query.trim()
        if (q.isEmpty()) base else base.filter { it.title.contains(q, ignoreCase = true) }
    }

    val lastBookId = remember(library) { store.lastReaderBookId() }
    val continueBook = remember(allActive, lastBookId) {
        allActive.firstOrNull { it.id == lastBookId } ?: allActive.firstOrNull { it.fromPdf }
    }

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    val warmDeskBg = if (dark) Color(0xFF121316) else Color(0xFFF3EFE6)

    Box(
        Modifier
            .fillMaxSize()
            .background(warmDeskBg),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Reading Room Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(onClick = onBackToLibrary) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to library")
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF24483F)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFFF4EEDF),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Reader Mode",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${pdfBooks.size} PDF books · ${noteBooks.size} notebooks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppPill(
                    selected = true,
                    tint = Color(0xFF24483F),
                    onClick = { importBookLauncher.launch(arrayOf("*/*")) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Icon(Icons.Outlined.UploadFile, null, Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Import", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                }
            }

            // Search & Filter controls
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search your reading shelf…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, "Clear")
                            }
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppChip(selected = filterTab == 0, onClick = { filterTab = 0 }, label = "All (${allActive.size})")
                    AppChip(selected = filterTab == 1, onClick = { filterTab = 1 }, label = "PDF Books (${pdfBooks.size})")
                    AppChip(selected = filterTab == 2, onClick = { filterTab = 2 }, label = "Notebooks (${noteBooks.size})")
                }
            }

            Spacer(Modifier.height(12.dp))

            val bottomPad = appContentBottomPadding(24.dp)
            if (displayed.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = bottomPad, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AppCard(
                        hero = true,
                        shape = appCardShape(),
                        contentPadding = PaddingValues(28.dp),
                        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = null,
                                tint = ProButtonPalette.ElectricBlue,
                                modifier = Modifier.size(44.dp),
                            )
                            Text(
                                "Your reading shelf is ready",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Import any PDF book, EPUB, Word document, PowerPoint deck, or study sheet to read it with realistic 3D curved paper-flip animations, page-turn sound effects, and warm paper themes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Button(
                                onClick = { importBookLauncher.launch(arrayOf("*/*")) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24483F), contentColor = Color.White),
                            ) {
                                Icon(Icons.Outlined.UploadFile, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Import Book (PDF, EPUB, Word, PPT)")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPad),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (continueBook != null && query.isBlank()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }, key = "continue") {
                            ContinueReadingCard(
                                book = continueBook,
                                savedPage = store.readerPage(continueBook.id).coerceIn(0, (continueBook.pageCount - 1).coerceAtLeast(0)),
                                repo = repo,
                                onOpen = { onOpenBook(continueBook.id) },
                            )
                        }
                    }
                    items(displayed, key = { it.id }) { book ->
                        val savedPage = store.readerPage(book.id).coerceIn(0, (book.pageCount - 1).coerceAtLeast(0))
                        BookShelfTile(
                            book = book,
                            savedPage = savedPage,
                            repo = repo,
                            onClick = { onOpenBook(book.id) },
                        )
                    }
                }
            }
        }

        // Bottom tab bar switching cleanly between Library and Reader Mode
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            AppBottomBar(
                tabs = listOf(
                    AppTabItem("Library") { Icon(Icons.Outlined.Description, null, Modifier.size(18.dp)) },
                    AppTabItem("Books") { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp)) },
                ),
                selectedIndex = 1,
                onSelect = { idx -> if (idx == 0) onBackToLibrary() },
            )
        }

        pdfImportProgress?.let { (title, page, total) ->
            PdfImportLoaderOverlay(title = title, page = page, total = total)
        }
    }
}

@Composable
private fun ContinueReadingCard(
    book: NotebookMeta,
    savedPage: Int,
    repo: Repository,
    onOpen: () -> Unit,
) {
    val total = book.pageCount.coerceAtLeast(1)
    val frac = ((savedPage + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val pct = (frac * 100f).roundToInt().coerceIn(1, 100)

    AppCard(
        hero = true,
        shape = appCardShape(2f),
        onClick = onOpen,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CoverArt(
                spec = book.coverSpec,
                title = book.title,
                noteDir = repo.noteDir(book.id),
                cacheKey = book.modified,
                showTitle = false,
                modifier = Modifier
                    .width(74.dp)
                    .aspectRatio(0.72f),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "CONTINUE READING",
                    style = MaterialTheme.typography.labelSmall,
                    color = ProButtonPalette.ElectricBlue,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Page ${savedPage + 1} of $total · $pct% complete",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { frac },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = Color(0xFF24483F),
                )
            }
            AppPill(
                selected = true,
                tint = Color(0xFF24483F),
                onClick = onOpen,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text("Read", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BookShelfTile(
    book: NotebookMeta,
    savedPage: Int,
    repo: Repository,
    onClick: () -> Unit,
) {
    val total = book.pageCount.coerceAtLeast(1)
    val frac = ((savedPage + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val pct = (frac * 100f).roundToInt().coerceIn(0, 100)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f),
        ) {
            CoverArt(
                spec = book.coverSpec,
                title = book.title,
                noteDir = repo.noteDir(book.id),
                cacheKey = book.modified,
                modifier = Modifier.fillMaxSize(),
            )
            if (book.fromPdf) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xD916181D))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "PDF",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (savedPage > 0) {
                LinearProgressIndicator(
                    progress = { frac },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = Color(0xFF24483F),
                    trackColor = Color.White.copy(alpha = 0.6f),
                )
            }
        }
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(
                book.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (savedPage > 0) "p.${savedPage + 1}/$total · $pct%" else "$total pages",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Full-screen dedicated Reader Mode with real curved page-peel physics, tablet 2-page open book
 * spread, warm paper themes, PDF skeleton page loader previews, bookmarks, contents drawer,
 * text search, and persisted reading position.
 */
@Composable
fun BookReaderScreen(
    bookId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val store = remember { SettingsStore.get(context) }
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current

    val bookMeta = remember(bookId) { repo.meta(bookId) }
    val bookTitle = bookMeta?.title ?: "Book"

    var pages by remember(bookId) { mutableStateOf<List<Page>>(emptyList()) }
    var loaded by remember(bookId) { mutableStateOf(false) }
    var currentPage by remember(bookId) { mutableIntStateOf(store.readerPage(bookId)) }
    var theme by remember { mutableStateOf(BookTheme.WARM_IVORY) }
    var flipStyle by remember { mutableStateOf(BookFlipStyle.REALISTIC_CURL) }
    var flipSpeed by remember { mutableStateOf(BookFlipSpeed.NATURAL) }
    var bookMargin by remember { mutableStateOf(BookMargin.CLASSIC) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1.05f) }
    var twoPageSpread by remember { mutableStateOf(true) }
    var showChrome by remember(bookId) { mutableStateOf(false) }
    var showDisplaySheet by remember { mutableStateOf(false) }
    var showContentsSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var zoom by remember(bookId) { mutableFloatStateOf(1f) }
    var pan by remember(bookId) { mutableStateOf(Offset.Zero) }

    val pdfPages = remember(bookId) {
        PdfPages(repo.pdfFile(bookId))
    }
    val pageRenderer = remember(bookId) {
        PageRenderer(repo.noteDir(bookId))
    }
    val bitmapCache = remember(bookId) {
        val maxBytes = (Runtime.getRuntime().maxMemory() / 10L).coerceIn(24L * 1024 * 1024, 96L * 1024 * 1024).toInt()
        object : LruCache<String, Bitmap>(maxBytes) {
            override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
        }
    }
    val readyTrigger = remember { mutableStateMapOf<String, Int>() }

    DisposableEffect(bookId) {
        onDispose {
            pdfPages.close()
            bitmapCache.evictAll()
            pageRenderer.evictCaches()
        }
    }

    LaunchedEffect(bookId) {
        repo.markOpened(bookId)
        val loadedPages = repo.loadPages(bookId)
        pages = loadedPages
        currentPage = store.readerPage(bookId).coerceIn(0, (loadedPages.size - 1).coerceAtLeast(0))
        loaded = true
    }

    LaunchedEffect(currentPage, loaded) {
        if (loaded && pages.isNotEmpty()) {
            store.setReaderPage(bookId, currentPage)
        }
    }

    BackHandler {
        when {
            showSearchSheet -> showSearchSheet = false
            showContentsSheet -> showContentsSheet = false
            showDisplaySheet -> showDisplaySheet = false
            zoom > 1.04f -> { zoom = 1f; pan = Offset.Zero }
            else -> onBack()
        }
    }

    // Real page-curl animation state
    val turnProgress = remember { Animatable(0f) }
    var turnDir by remember { mutableIntStateOf(0) } // +1 forward (right->left), -1 backward (left->right)
    var curlTouchY by remember { mutableFloatStateOf(0.86f) }

    val totalPages = pages.size.coerceAtLeast(1)
    val config = LocalConfiguration.current
    val isWideTablet = config.screenWidthDp >= 760
    val effectiveSpread = isWideTablet && twoPageSpread && totalPages > 1
    val pageStep = if (effectiveSpread) 2 else 1

    fun canGoNext(): Boolean = currentPage + pageStep < totalPages
    fun canGoPrev(): Boolean = currentPage > 0

    fun toggleBookmarkAt(idx: Int) {
        if (idx !in pages.indices) return
        val updated = pages[idx].copy(bookmarked = !pages[idx].bookmarked)
        val nextList = pages.toMutableList().also { it[idx] = updated }
        pages = nextList
        scope.launch {
            repo.savePages(bookId, nextList, dirtyPageIds = setOf(updated.id), structureChanged = false)
        }
    }

    val appSettings by store.settings.collectAsStateWithLifecycle()
    val bookFlipSound = appSettings.bookFlipSound

    var dragProgress by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf(false) }

    fun settleTurn(commit: Boolean) {
        if (settling || turnDir == 0) return
        val origin = currentPage
        val destination = (origin + turnDir * pageStep).coerceIn(0, totalPages - 1)
        val start = dragProgress
        if (commit && bookFlipSound) {
            BookFlipSoundEngine.playPageTurnSwish(volume = appSettings.paperVolume, forward = turnDir > 0)
        }
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try {
                turnProgress.snapTo(start)
                settling = true
                if (!reduceMotion && flipStyle != BookFlipStyle.INSTANT) {
                    val end = if (commit) 1f else 0f
                    val duration = (flipSpeed.durationMs * abs(end - start)).roundToInt().coerceAtLeast(1)
                    turnProgress.animateTo(end, tween(duration, easing = if (start == 0f) FastOutSlowInEasing else androidx.compose.animation.core.LinearOutSlowInEasing))
                }
                if (commit && currentPage == origin) currentPage = destination
            } finally {
                turnDir = 0
                dragProgress = 0f
                settling = false
            }
        }
    }

    fun animatePageTurn(direction: Int, normalizedY: Float = 0.86f) {
        if (settling || turnDir != 0) return
        if (direction > 0 && !canGoNext() || direction < 0 && !canGoPrev()) return
        turnDir = direction
        curlTouchY = normalizedY.coerceIn(0.24f, 0.96f)
        settleTurn(true)
    }
    // Helper to render/fetch bitmap into cache
    suspend fun ensureRendered(pageIdx: Int, widthPx: Int, nightMode: Boolean): Bitmap? {
        if (pageIdx !in pages.indices) return null
        val key = "$pageIdx@$widthPx@$nightMode"
        bitmapCache.get(key)?.let { return it }
        return withContext(Dispatchers.Default) {
            bitmapCache.get(key)?.let { return@withContext it }
            val page = pages[pageIdx]
            val scale = (widthPx.toFloat() / page.width.coerceAtLeast(1f)).coerceIn(0.25f, 2.5f)
            val w = (page.width * scale).roundToInt().coerceAtLeast(1)
            val h = (page.height * scale).roundToInt().coerceAtLeast(1)
            val pdfBmp = if (page.pdfPage >= 0 && pdfPages.exists) {
                pdfPages.render(page.pdfPage, w, h, useCache = true)
            } else null
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            out.eraseColor(android.graphics.Color.WHITE)
            synchronized(pageRenderer) {
                pageRenderer.paperColor = android.graphics.Color.WHITE
                pageRenderer.darkPdf = false
                pageRenderer.renderPage(Canvas(out), page, scale, pdfBmp)
            }
            bitmapCache.put(key, out)
            withContext(Dispatchers.Main) {
                readyTrigger[key] = (readyTrigger[key] ?: 0) + 1
            }
            out
        }
    }

    val colorFilter = remember(theme, brightness, contrast) {
        val b = brightness.coerceIn(0.45f, 1.0f)
        val c = contrast.coerceIn(0.85f, 1.35f)
        val t = (1f - c) * 128f
        when (theme) {
            BookTheme.PAPER -> if (b >= 0.99f && kotlin.math.abs(c - 1f) < 0.02f) null else ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        b * c, 0f, 0f, 0f, t * b,
                        0f, b * c, 0f, 0f, t * b,
                        0f, 0f, b * c, 0f, t * b,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
            BookTheme.WARM_IVORY -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.98f * b * c, 0f, 0f, 0f, t * b,
                        0f, 0.95f * b * c, 0f, 0f, t * b,
                        0f, 0f, 0.88f * b * c, 0f, t * b,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
            BookTheme.SEPIA -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.95f * b * c, 0f, 0f, 0f, t * b,
                        0f, 0.89f * b * c, 0f, 0f, t * b,
                        0f, 0f, 0.78f * b * c, 0f, t * b,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
            BookTheme.FOREST_SAGE -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.91f * b * c, 0f, 0f, 0f, t * b,
                        0f, 0.95f * b * c, 0f, 0f, t * b,
                        0f, 0f, 0.92f * b * c, 0f, t * b,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
            BookTheme.NIGHT -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.88f * b * c, 0f, 0f, 0f, 236f * b,
                        0f, -0.88f * b * c, 0f, 0f, 236f * b,
                        0f, 0f, -0.88f * b * c, 0f, 236f * b,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
            BookTheme.AMOLED_BLACK -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.95f * b * c, 0f, 0f, 0f, 244f * b,
                        0f, -0.95f * b * c, 0f, 0f, 244f * b,
                        0f, 0f, -0.95f * b * c, 0f, 244f * b,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(theme.pageBg)
            .safeDrawingPadding(),
    ) {
        val density = LocalDensity.current
        val renderWidthPx = with(density) {
            val perPageDp = if (effectiveSpread) (maxWidth / 2f).coerceAtMost(580.dp) else maxWidth.coerceAtMost(840.dp)
            (perPageDp.toPx() * (if (zoom > 1.3f) 1.75f else 1.25f)).roundToInt().coerceIn(720, 1640)
        }

        val targetPageIdx = when {
            turnDir > 0 -> (currentPage + pageStep).coerceAtMost(totalPages - 1)
            turnDir < 0 -> (currentPage - pageStep).coerceAtLeast(0)
            else -> currentPage
        }

        // Prefetch current and adjacent pages whenever position changes
        LaunchedEffect(loaded, currentPage, effectiveSpread, renderWidthPx, theme.isDark) {
            if (!loaded || pages.isEmpty()) return@LaunchedEffect
            val order = buildList {
                add(currentPage)
                if (effectiveSpread && currentPage + 1 < totalPages) add(currentPage + 1)
                add(currentPage + pageStep)
                add(currentPage - pageStep)
                if (effectiveSpread) {
                    add(currentPage + pageStep + 1)
                    add(currentPage - pageStep + 1)
                }
                add(currentPage + pageStep * 2)
            }.distinct().filter { it in pages.indices }

            for (idx in order) {
                launch { ensureRendered(idx, renderWidthPx, theme.isDark) }
            }
        }

        fun cachedPageBmp(idx: Int): Bitmap? {
            if (idx !in pages.indices) return null
            val key = "$idx@$renderWidthPx@${theme.isDark}"
            // Observing readyTrigger[key] ensures Compose recomposes once background render finishes
            @Suppress("UNUSED_VARIABLE")
            val v = readyTrigger[key]
            return bitmapCache.get(key)
        }

        val curLeftBmp = cachedPageBmp(currentPage)
        val curRightBmp = if (effectiveSpread && currentPage + 1 < totalPages) cachedPageBmp(currentPage + 1) else null
        val nextLeftBmp = if (turnDir != 0) cachedPageBmp(targetPageIdx) else null
        val nextRightBmp = if (turnDir != 0 && effectiveSpread && targetPageIdx + 1 < totalPages) cachedPageBmp(targetPageIdx + 1) else null

        val shimmer = if (reduceMotion || (curLeftBmp != null && (!effectiveSpread || currentPage + 1 >= totalPages || curRightBmp != null))) {
            0f
        } else {
            val animatedShimmer by rememberInfiniteTransition(label = "bookLoader").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart),
                label = "sweep",
            )
            animatedShimmer
        }

        // Main Interactive Book Stage
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isWideTablet) (bookMargin.padDp + 12).dp else bookMargin.padDp.dp,
                    vertical = 6.dp,
                )
                .pointerInput(bookId) {
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val downCount = event.changes.count { it.pressed }
                            if (downCount == 0) break
                            if (downCount >= 2) {
                                val z = event.calculateZoom()
                                val p = event.calculatePan()
                                if (z != 1f || p != Offset.Zero) {
                                    val nextZoom = (zoom * z).coerceIn(1f, 2.8f)
                                    val maxPanX = (nextZoom - 1f) * size.width * 0.48f
                                    val maxPanY = (nextZoom - 1f) * size.height * 0.48f
                                    zoom = nextZoom
                                    pan = if (nextZoom <= 1.02f) Offset.Zero else Offset(
                                        (pan.x + p.x).coerceIn(-maxPanX, maxPanX),
                                        (pan.y + p.y).coerceIn(-maxPanY, maxPanY),
                                    )
                                    event.changes.forEach { it.consume() }
                                }
                            } else if (zoom > 1.04f) {
                                val p = event.calculatePan()
                                if (p != Offset.Zero) {
                                    val maxPanX = (zoom - 1f) * size.width * 0.48f
                                    val maxPanY = (zoom - 1f) * size.height * 0.48f
                                    pan = Offset(
                                        (pan.x + p.x).coerceIn(-maxPanX, maxPanX),
                                        (pan.y + p.y).coerceIn(-maxPanY, maxPanY),
                                    )
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
                .pointerInput(currentPage, totalPages, effectiveSpread, zoom, flipStyle, flipSpeed, reduceMotion, bookFlipSound, appSettings.paperVolume) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoom > 1.08f) {
                                zoom = 1f
                                pan = Offset.Zero
                            } else {
                                zoom = 1.75f
                            }
                        },
                        onTap = { pos ->
                            if (zoom <= 1.04f) {
                                val normY = (pos.y / size.height.coerceAtLeast(1)).coerceIn(0.15f, 0.85f)
                                when {
                                    pos.x < size.width * 0.24f -> animatePageTurn(-1, normY)
                                    pos.x > size.width * 0.76f -> animatePageTurn(+1, normY)
                                    else -> showChrome = !showChrome
                                }
                            } else {
                                showChrome = !showChrome
                            }
                        },
                    )
                }
                .pointerInput(currentPage, totalPages, effectiveSpread, zoom, flipStyle, flipSpeed, reduceMotion, bookFlipSound, appSettings.paperVolume) {
                    if (zoom <= 1.04f) {
                        var totalDx = 0f
                        var tracking = false
                        detectDragGestures(
                            onDragStart = {
                                totalDx = 0f
                                tracking = !settling
                            },
                            onDrag = { change, dragAmount ->
                                if (tracking) {
                                    change.consume()
                                    totalDx += dragAmount.x
                                    turnDir = when {
                                        totalDx < 0f && canGoNext() -> 1
                                        totalDx > 0f && canGoPrev() -> -1
                                        else -> 0
                                    }
                                    dragProgress = if (turnDir == 0) 0f else
                                        (abs(totalDx) / (size.width.coerceAtLeast(1) * 0.72f)).coerceIn(0f, 1f)
                                    curlTouchY = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0.24f, 0.96f)
                                    if (turnDir != 0 && bookFlipSound && abs(dragAmount.x) > 4f) {
                                        BookFlipSoundEngine.playDragRustleTick(dragAmount.x, appSettings.paperVolume)
                                    }
                                }
                            },
                            onDragCancel = { if (tracking) settleTurn(false) },
                            onDragEnd = { if (tracking) settleTurn(dragProgress >= 0.18f) },
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (!loaded) {
                BookSheetFrame(
                    theme = theme,
                    spread = effectiveSpread,
                    leftBmp = null,
                    rightBmp = null,
                    leftPageNum = 1,
                    rightPageNum = null,
                    totalPages = 1,
                    title = bookTitle,
                    colorFilter = colorFilter,
                    shimmer = shimmer,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val p = if (settling) turnProgress.value else dragProgress
                BookCurlStage(
                    theme = theme,
                    flipStyle = flipStyle,
                    spread = effectiveSpread,
                    title = bookTitle,
                    totalPages = totalPages,
                    curPageNum = currentPage + 1,
                    nextPageNum = targetPageIdx + 1,
                    curLeftBmp = curLeftBmp,
                    curRightBmp = curRightBmp,
                    nextLeftBmp = nextLeftBmp,
                    nextRightBmp = nextRightBmp,
                    turnDir = turnDir,
                    curlProgress = if (reduceMotion || flipStyle == BookFlipStyle.INSTANT) 0f else p,
                    curlTouchY = curlTouchY,
                    colorFilter = colorFilter,
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = pan.x
                            translationY = pan.y
                        },
                )
            }
        }

        // Top Reader Bar
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(if (reduceMotion) snap() else tween(180)),
            exit = fadeOut(if (reduceMotion) snap() else tween(180)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            AppSurface(
                role = SurfaceRole.Chrome,
                containerColor = theme.chromeBg,
                contentColor = theme.inkText,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 6.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to shelf", tint = theme.inkText)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            bookTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = theme.inkText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = { showSearchSheet = true },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.reader_search), tint = theme.inkText)
                    }
                    IconButton(
                        onClick = { showContentsSheet = true },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(Icons.Outlined.GridView, contentDescription = stringResource(R.string.reader_contents), tint = theme.inkText)
                    }
                    IconButton(
                        onClick = { showDisplaySheet = true },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(Icons.Outlined.TextFields, contentDescription = stringResource(R.string.reader_display_settings), tint = theme.inkText)
                    }
                    val isBookmarked = pages.getOrNull(currentPage)?.bookmarked == true
                    IconButton(
                        onClick = { toggleBookmarkAt(currentPage) },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(
                                if (isBookmarked) R.string.reader_bookmark_remove else R.string.reader_bookmark_add,
                            ),
                            tint = if (isBookmarked) theme.accent else theme.mutedText,
                        )
                    }
                }
            }
        }

        // Bottom Page Scrubber Bar
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(if (reduceMotion) snap() else tween(180)),
            exit = fadeOut(if (reduceMotion) snap() else tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        enabled = canGoPrev(),
                        onClick = { animatePageTurn(-1) },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = stringResource(R.string.reader_previous_page),
                            tint = if (canGoPrev()) theme.inkText else theme.mutedText.copy(alpha = 0.35f),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        val pct = (((currentPage + 1).toFloat() / totalPages) * 100f).roundToInt().coerceIn(1, 100)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Page ${currentPage + 1} of $totalPages",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.inkText,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (zoom > 1.04f) "${(zoom * 100).roundToInt()}% · $pct%" else "$pct% read",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.mutedText,
                            )
                        }
                        if (totalPages > 1) {
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { v ->
                                    val target = v.roundToInt().coerceIn(0, totalPages - 1)
                                    if (target != currentPage) currentPage = target
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
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = stringResource(R.string.reader_next_page),
                            tint = if (canGoNext()) theme.inkText else theme.mutedText.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        }
    }

    // Theme & Reading Comfort Sheet
    if (showDisplaySheet) {
        AppSheet(onDismissRequest = { showDisplaySheet = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Reading Display & Animation",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    fontWeight = FontWeight.Bold,
                )
                Text("PAPER THEME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BookTheme.entries.forEach { t ->
                        val selected = t == theme
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(t.pageBg)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) ProButtonPalette.ElectricBlue else Color.Gray.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { theme = t }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text("Aa", color = t.inkText, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                            Text(t.label, style = MaterialTheme.typography.labelSmall, color = t.inkText, maxLines = 1)
                        }
                    }
                }
                Text("PAGE TURN ANIMATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppSegmentedControl(
                    options = BookFlipStyle.entries.map { it.label },
                    selectedIndex = flipStyle.ordinal,
                    onSelect = { flipStyle = BookFlipStyle.entries[it] },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ANIMATION SPEED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AppSegmentedControl(
                            options = BookFlipSpeed.entries.map { it.label },
                            selectedIndex = flipSpeed.ordinal,
                            onSelect = { flipSpeed = BookFlipSpeed.entries[it] },
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("PAGE MARGIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AppSegmentedControl(
                            options = BookMargin.entries.map { it.label },
                            selectedIndex = bookMargin.ordinal,
                            onSelect = { bookMargin = BookMargin.entries[it] },
                        )
                    }
                }
                AppSliderRow(
                    title = "PAGE BRIGHTNESS",
                    valueText = "${(brightness * 100).roundToInt()}%",
                    value = brightness,
                    valueRange = 0.45f..1.0f,
                    onValueChange = { brightness = it },
                )
                AppSliderRow(
                    title = "INK CONTRAST",
                    valueText = "${(contrast * 100).roundToInt()}%",
                    value = contrast,
                    valueRange = 0.85f..1.35f,
                    onValueChange = { contrast = it },
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Realistic page-flip paper sound", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    AppChip(
                        selected = bookFlipSound,
                        onClick = {
                            val next = !bookFlipSound
                            store.update { it.copy(bookFlipSound = next) }
                            if (next) BookFlipSoundEngine.playPageTurnSwish(volume = appSettings.paperVolume, forward = true)
                        },
                        label = if (bookFlipSound) "Sound On" else "Muted",
                    )
                }
                if (isWideTablet) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Two-page open book layout", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        AppChip(
                            selected = twoPageSpread,
                            onClick = { twoPageSpread = !twoPageSpread },
                            label = if (twoPageSpread) "2 Pages" else "1 Page",
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    // Contents & Bookmarks Sheet
    if (showContentsSheet) {
        var tab by remember { mutableIntStateOf(0) }
        val bookmarkedIndices = remember(pages) { pages.indices.filter { pages[it].bookmarked } }
        AppSheet(onDismissRequest = { showContentsSheet = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pages & Bookmarks",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showContentsSheet = false }) { Icon(Icons.Filled.Close, "Close") }
                }
                AppSegmentedControl(
                    options = listOf("All Pages (${pages.size})", "Bookmarks (${bookmarkedIndices.size})"),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                )
                if (tab == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(76.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        items(pages.size) { idx ->
                            val active = idx == currentPage
                            val marked = pages[idx].bookmarked
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        currentPage = idx
                                        showContentsSheet = false
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (marked) "★ ${idx + 1}" else "${idx + 1}",
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                } else if (bookmarkedIndices.isEmpty()) {
                    Text(
                        "Tap the bookmark icon while reading any page to save it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        items(bookmarkedIndices) { idx ->
                            AppCard(
                                onClick = {
                                    currentPage = idx
                                    showContentsSheet = false
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Bookmark, null, tint = theme.accent, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Page ${idx + 1}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { toggleBookmarkAt(idx) }) { Text("Remove") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // In-Book Search Sheet
    if (showSearchSheet) {
        var searchQuery by remember { mutableStateOf("") }
        var searchHits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
        var indexing by remember { mutableStateOf(false) }

        LaunchedEffect(searchQuery, bookId, pages.size) {
            val q = searchQuery.trim()
            if (q.length < 2) {
                indexing = false
                searchHits = emptyList()
                return@LaunchedEffect
            }
            indexing = true
            val hits = withContext(Dispatchers.IO) {
                val out = ArrayList<SearchHit>()
                if (pdfPages.exists) {
                    val idx = runCatching { PdfTextIndex.build(context, repo.pdfFile(bookId)) }.getOrNull()
                    if (idx != null) {
                        out += idx.search(q) { pdfIdx ->
                            pages.indexOfFirst { it.pdfPage == pdfIdx }.takeIf { it >= 0 }
                        }
                    }
                }
                pages.forEachIndexed { pageIdx, pg ->
                    pg.items.forEach { item ->
                        val text = when (item) {
                            is TextItem -> item.text
                            is StickyItem -> "${item.title} ${item.text}".trim()
                            else -> ""
                        }
                        if (text.contains(q, ignoreCase = true)) {
                            out += SearchHit(pageIdx, emptyList(), text.replace('\n', ' ').take(90))
                        }
                    }
                }
                out
            }
            searchHits = hits
            indexing = false
        }

        AppSheet(onDismissRequest = { showSearchSheet = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Search Inside Book",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showSearchSheet = false }) { Icon(Icons.Filled.Close, "Close") }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = { Text("Search keywords or phrases…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (indexing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    items(searchHits) { hit ->
                        AppCard(
                            onClick = {
                                currentPage = hit.page.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                                showSearchSheet = false
                            },
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "p.${hit.page + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ProButtonPalette.ElectricBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(46.dp),
                                )
                                Text(hit.snippet, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

// BookCurlStage, BookSheetFrame, and page loader preview extracted to BookReaderCanvas.kt

