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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/**
 * Synthesizes realistic physical paper-flip swish & page-settle acoustic waves in real time.
 * Zero external audio files required; works offline with ultra-low latency.
 */
private object BookFlipSoundEngine {
    private const val SAMPLE_RATE = 22050
    private val audioExecutor = Executors.newSingleThreadExecutor()
    @Volatile
    private var lastRustleAt = 0L

    fun playPageTurnSwish(volume: Float = 0.72f, forward: Boolean = true) {
        val vol = volume.coerceIn(0.05f, 1f)
        audioExecutor.execute {
            runCatching {
                val durationSec = 0.25f
                val totalSamples = (SAMPLE_RATE * durationSec).toInt()
                val pcm = ShortArray(totalSamples)
                val rng = Random(System.nanoTime())
                var lp1 = 0f
                var lp2 = 0f
                var prev = 0f

                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / totalSamples
                    // Crisp paper lift -> airy mid-turn swish -> soft page settle thud
                    val liftEnv = exp(-((t - 0.14f) * (t - 0.14f)) / 0.008f) * 0.45f
                    val swishEnv = sin(t * PI).toFloat().let { it * it } * 0.85f
                    val settleEnv = exp(-((t - 0.86f) * (t - 0.86f)) / 0.0018f) * 0.52f

                    val white = rng.nextFloat() * 2f - 1f
                    // Dynamic filter cutoff sweeps higher mid-flip like real paper fibers
                    val cutoff = if (forward) (0.18f + 0.34f * sin(t * PI).toFloat()) else (0.15f + 0.28f * sin(t * PI).toFloat())
                    lp1 += cutoff * (white - lp1)
                    lp2 += cutoff * (lp1 - lp2)
                    val bandNoise = (lp1 - lp2 * 0.55f) + (white - prev) * 0.22f
                    prev = white

                    // Subtle low-frequency air displacement on settle
                    val settleThud = sin(2.0 * PI * (if (forward) 105.0 else 92.0) * (i.toDouble() / SAMPLE_RATE)).toFloat() * settleEnv
                    val sample = ((bandNoise * (liftEnv + swishEnv) + settleThud) * vol * 0.82f).coerceIn(-1f, 1f)
                    pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                playPcm(pcm)
            }
        }
    }

    fun playDragRustleTick(speedPx: Float, volume: Float = 0.45f) {
        val now = System.currentTimeMillis()
        if (now - lastRustleAt < 65L) return
        lastRustleAt = now
        val vol = (volume * (abs(speedPx) / 48f).coerceIn(0.2f, 0.85f)).coerceIn(0.04f, 0.65f)
        audioExecutor.execute {
            runCatching {
                val totalSamples = (SAMPLE_RATE * 0.045f).toInt()
                val pcm = ShortArray(totalSamples)
                val rng = Random(now)
                var lp = 0f
                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / totalSamples
                    val env = sin(t * PI).toFloat()
                    val w = rng.nextFloat() * 2f - 1f
                    lp += 0.28f * (w - lp)
                    val s = (lp * env * vol * 0.55f).coerceIn(-1f, 1f)
                    pcm[i] = (s * Short.MAX_VALUE).toInt().toShort()
                }
                playPcm(pcm)
            }
        }
    }

    private fun playPcm(pcm: ShortArray) {
        val byteCount = pcm.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(byteCount, AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        Thread.sleep((pcm.size * 1000L / SAMPLE_RATE) + 35L)
        runCatching {
            track.stop()
            track.release()
        }
    }
}

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
                    Text("Import Book / Doc", color = Color.White, style = MaterialTheme.typography.labelLarge)
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
    var showChrome by remember { mutableStateOf(true) }
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
    var curlTouchY by remember { mutableFloatStateOf(0.68f) }

    val totalPages = pages.size.coerceAtLeast(1)
    val config = LocalConfiguration.current
    val isWideTablet = config.screenWidthDp >= 760
    val effectiveSpread = isWideTablet && twoPageSpread && totalPages > 1
    val pageStep = if (effectiveSpread) 2 else 1

    fun canGoNext(): Boolean = currentPage + pageStep < totalPages
    fun canGoPrev(): Boolean = currentPage - pageStep >= 0

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

    fun animatePageTurn(direction: Int, normalizedY: Float = 0.72f) {
        if (direction > 0 && !canGoNext()) return
        if (direction < 0 && !canGoPrev()) return
        if (turnProgress.isRunning) return
        if (bookFlipSound) {
            BookFlipSoundEngine.playPageTurnSwish(volume = appSettings.paperVolume, forward = direction > 0)
        }
        if (flipStyle == BookFlipStyle.INSTANT) {
            currentPage = (currentPage + direction * pageStep).coerceIn(0, totalPages - 1)
            return
        }
        scope.launch {
            turnDir = direction
            curlTouchY = normalizedY.coerceIn(0.15f, 0.85f)
            turnProgress.snapTo(0f)
            turnProgress.animateTo(1f, tween(durationMillis = flipSpeed.durationMs, easing = FastOutSlowInEasing))
            currentPage = (currentPage + direction * pageStep).coerceIn(0, totalPages - 1)
            turnDir = 0
            turnProgress.snapTo(0f)
        }
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

    val shimmer by rememberInfiniteTransition(label = "bookLoader").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(theme.deskBg)
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

        // Main Interactive Book Stage
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isWideTablet) (bookMargin.padDp + 12).dp else bookMargin.padDp.dp,
                    vertical = if (showChrome) 74.dp else 14.dp,
                )
                .pointerInput(zoom) {
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
                .pointerInput(currentPage, totalPages, effectiveSpread, zoom) {
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
                .pointerInput(currentPage, totalPages, effectiveSpread, zoom) {
                    if (zoom <= 1.04f) {
                        var totalDx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { start ->
                                totalDx = 0f
                                turnDir = 0
                                curlTouchY = (start.y / size.height.coerceAtLeast(1)).coerceIn(0.15f, 0.85f)
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDx += dragAmount
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                if (totalDx < -10f && canGoNext()) {
                                    turnDir = +1
                                    if (bookFlipSound && abs(dragAmount) > 4f) {
                                        BookFlipSoundEngine.playDragRustleTick(dragAmount, appSettings.paperVolume)
                                    }
                                    scope.launch { turnProgress.snapTo((abs(totalDx) / (w * 0.72f)).coerceIn(0f, 1f)) }
                                } else if (totalDx > 10f && canGoPrev()) {
                                    turnDir = -1
                                    if (bookFlipSound && abs(dragAmount) > 4f) {
                                        BookFlipSoundEngine.playDragRustleTick(dragAmount, appSettings.paperVolume)
                                    }
                                    scope.launch { turnProgress.snapTo((abs(totalDx) / (w * 0.72f)).coerceIn(0f, 1f)) }
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
                                    if (p >= 0.18f && turnDir != 0) {
                                        if (bookFlipSound) {
                                            BookFlipSoundEngine.playPageTurnSwish(volume = appSettings.paperVolume, forward = turnDir > 0)
                                        }
                                        turnProgress.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
                                        currentPage = (currentPage + turnDir * pageStep).coerceIn(0, totalPages - 1)
                                    } else {
                                        turnProgress.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 360f))
                                    }
                                    turnDir = 0
                                    turnProgress.snapTo(0f)
                                }
                            },
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
                val p = turnProgress.value.coerceIn(0f, 1f)
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
                    curlProgress = p,
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
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
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
                    IconButton(onClick = onBack) {
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
                        Text(
                            if (effectiveSpread && currentPage + 1 < totalPages) {
                                "Pages ${currentPage + 1}–${currentPage + 2} of $totalPages"
                            } else {
                                "Page ${currentPage + 1} of $totalPages"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.mutedText,
                        )
                    }
                    IconButton(onClick = { showSearchSheet = true }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search book", tint = theme.inkText)
                    }
                    IconButton(onClick = { showContentsSheet = true }) {
                        Icon(Icons.Outlined.GridView, contentDescription = "Contents & bookmarks", tint = theme.inkText)
                    }
                    IconButton(onClick = { showDisplaySheet = true }) {
                        Icon(Icons.Outlined.TextFields, contentDescription = "Reader theme & layout", tint = theme.inkText)
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
                    IconButton(
                        onClick = {
                            val nextVal = !bookFlipSound
                            store.update { it.copy(bookFlipSound = nextVal) }
                            if (nextVal) BookFlipSoundEngine.playPageTurnSwish(volume = appSettings.paperVolume, forward = true)
                        },
                    ) {
                        Icon(
                            if (bookFlipSound) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                            contentDescription = "Page flip sound",
                            tint = if (bookFlipSound) theme.accent else theme.mutedText,
                        )
                    }
                    val isBookmarked = pages.getOrNull(currentPage)?.bookmarked == true
                    IconButton(onClick = { toggleBookmarkAt(currentPage) }) {
                        Icon(
                            if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark page",
                            tint = if (isBookmarked) theme.accent else theme.mutedText,
                        )
                    }
                }
            }
        }

        // Bottom Page Scrubber Bar
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
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
                    IconButton(enabled = canGoPrev(), onClick = { animatePageTurn(-1) }) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous page",
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
                    IconButton(enabled = canGoNext(), onClick = { animatePageTurn(+1) }) {
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

    // Theme & Reading Comfort Sheet
    if (showDisplaySheet) {
        AppSheet(onDismissRequest = { showDisplaySheet = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
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

/**
 * Interactive 3-layer curved page-peel stage:
 * 1) Target next sheet underneath with crease drop shadow
 * 2) Current sheet clipped dynamically along a curved Bezier diagonal crease
 * 3) Curled backside paper flap with faint bleed-through reflection & cylindrical shading gradient
 */
@Composable
private fun BookCurlStage(
    theme: BookTheme,
    flipStyle: BookFlipStyle,
    spread: Boolean,
    title: String,
    totalPages: Int,
    curPageNum: Int,
    nextPageNum: Int,
    curLeftBmp: Bitmap?,
    curRightBmp: Bitmap?,
    nextLeftBmp: Bitmap?,
    nextRightBmp: Bitmap?,
    turnDir: Int,
    curlProgress: Float,
    curlTouchY: Float,
    colorFilter: ColorFilter?,
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    val activeCurl = turnDir != 0 && curlProgress > 0.005f
    Box(modifier) {
        // Layer 1: Revealed destination sheet underneath during page turn
        if (activeCurl) {
            BookSheetFrame(
                theme = theme,
                spread = spread,
                leftBmp = nextLeftBmp,
                rightBmp = nextRightBmp,
                leftPageNum = nextPageNum,
                rightPageNum = if (spread && nextPageNum < totalPages) nextPageNum + 1 else null,
                totalPages = totalPages,
                title = title,
                colorFilter = colorFilter,
                shimmer = shimmer,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Layer 2 + Layer 3: Turning sheet with selected animation physics
        val cornerRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { clip = true; shape = RoundedCornerShape(16.dp) },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .let { base ->
                        when {
                            !activeCurl -> base
                            flipStyle == BookFlipStyle.REALISTIC_CURL -> base.drawCurvedPageClip(
                                turnDir = turnDir,
                                progress = curlProgress,
                                touchY = curlTouchY,
                            )
                            flipStyle == BookFlipStyle.SPINE_FLIP -> base.graphicsLayer {
                                val p = curlProgress.coerceIn(0f, 1f)
                                cameraDistance = 28f * density
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (turnDir > 0) 0f else 1f, 0.5f)
                                rotationY = if (turnDir > 0) -105f * p else 105f * p
                                alpha = (1f - (p - 0.55f).coerceAtLeast(0f) * 2.2f).coerceIn(0f, 1f)
                            }
                            flipStyle == BookFlipStyle.CRISP_SLIDE -> base.graphicsLayer {
                                val p = curlProgress.coerceIn(0f, 1f)
                                translationX = (if (turnDir > 0) -1f else 1f) * size.width * p
                            }
                            else -> base
                        }
                    },
            ) {
                BookSheetFrame(
                    theme = theme,
                    spread = spread,
                    leftBmp = curLeftBmp,
                    rightBmp = curRightBmp,
                    leftPageNum = curPageNum,
                    rightPageNum = if (spread && curPageNum < totalPages) curPageNum + 1 else null,
                    totalPages = totalPages,
                    title = title,
                    colorFilter = colorFilter,
                    shimmer = shimmer,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Cast shadows + Curled backside paper flap overlay with realistic 3D cylindrical roll
            if (activeCurl && flipStyle == BookFlipStyle.REALISTIC_CURL) {
                val turningBmp = if (spread && turnDir > 0) (curRightBmp ?: curLeftBmp) else curLeftBmp
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val p = curlProgress.coerceIn(0f, 1f)
                    val arcSin = sin(p * PI).toFloat()
                    val bias = (curlTouchY - 0.5f) * 2f // -1f near top, +1f near bottom

                    if (turnDir > 0) {
                        // Peeling right-to-left with natural corner lift & S-curve fold spine
                        val foldX = w * (1f - 1.18f * p)
                        val skew = bias * h * 0.17f * arcSin
                        val topX = foldX + skew
                        val botX = foldX - skew
                        val midX = (topX + botX) * 0.5f + minOf(w * 0.075f, (w - foldX).coerceAtLeast(0f) * 0.22f) * arcSin
                        val flapW = ((w - foldX).coerceAtLeast(0f) * 0.48f).coerceIn(16f, w * 0.48f)

                        // Deep ambient occlusion & cast shadow onto the newly uncovered right page
                        val exposedShadow = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.36f * arcSin),
                                Color.Black.copy(alpha = 0.14f * arcSin),
                                Color.Transparent,
                            ),
                            startX = minOf(topX, botX) - 4f,
                            endX = minOf(maxOf(topX, botX) + flapW * 1.15f + 56f, w),
                        )
                        val shadowPath = Path().apply {
                            moveTo(topX, 0f)
                            quadraticTo(midX, h * 0.5f, botX, h)
                            lineTo(w, h)
                            lineTo(w, 0f)
                            close()
                        }
                        clipPath(shadowPath) {
                            drawRect(exposedShadow)
                        }

                        // Curled paper flap curving over the left side of the crease
                        val flapTopX = (topX - flapW * (0.95f - 0.14f * bias)).coerceAtLeast(-w * 0.25f)
                        val flapBotX = (botX - flapW * (0.95f + 0.14f * bias)).coerceAtLeast(-w * 0.25f)
                        val flapOuterMidX = (flapTopX + flapBotX) * 0.5f - flapW * 0.15f * arcSin
                        val topArchY = (h * 0.025f * arcSin * (1f - bias * 0.4f)).coerceAtLeast(0f)
                        val botArchY = h - (h * 0.025f * arcSin * (1f + bias * 0.4f)).coerceAtLeast(0f)

                        val flapPath = Path().apply {
                            moveTo(topX, 0f)
                            quadraticTo(midX, h * 0.5f, botX, h)
                            lineTo(flapBotX, botArchY)
                            quadraticTo(flapOuterMidX, h * 0.5f, flapTopX, topArchY)
                            close()
                        }

                        // Soft drop shadow cast by the curled flap onto the flat left sheet
                        drawPath(
                            path = flapPath,
                            color = Color.Black.copy(alpha = 0.18f * arcSin),
                            style = Stroke(width = 22f),
                        )
                        drawPath(
                            path = flapPath,
                            color = Color.Black.copy(alpha = 0.09f * arcSin),
                            style = Stroke(width = 38f),
                        )

                        clipPath(flapPath) {
                            // Warm backside paper base
                            drawRect(theme.pageBg)

                            // Faint horizontally mirrored bleed-through from turning page
                            if (turningBmp != null && !turningBmp.isRecycled) {
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    nc.save()
                                    val rectLeft = minOf(flapTopX, flapBotX)
                                    val rectRight = maxOf(topX, botX)
                                    val rectW = (rectRight - rectLeft).coerceAtLeast(1f)
                                    nc.translate(rectRight, 0f)
                                    nc.scale(-1f, 1f)
                                    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                                        alpha = if (theme.isDark) 28 else 38
                                    }
                                    nc.drawBitmap(
                                        turningBmp,
                                        null,
                                        android.graphics.RectF(0f, 0f, rectW * 1.45f, h),
                                        paint,
                                    )
                                    nc.restore()
                                }
                            }

                            // 3D cylindrical curl specular highlight + shadow roll across the flap
                            val gradStart = minOf(flapTopX, flapBotX)
                            val gradEnd = maxOf(topX, botX)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.22f * arcSin),
                                        Color.White.copy(alpha = if (theme.isDark) 0.14f else 0.52f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.12f * arcSin),
                                        Color.White.copy(alpha = (if (theme.isDark) 0.18f else 0.62f) * arcSin),
                                        Color.Black.copy(alpha = 0.28f * arcSin),
                                    ),
                                    startX = gradStart,
                                    endX = gradEnd.coerceAtLeast(gradStart + 4f),
                                ),
                            )
                        }

                        // Crisp paper fold edge highlight + outline
                        drawPath(
                            path = flapPath,
                            color = theme.mutedText.copy(alpha = 0.30f),
                            style = Stroke(width = 1.6f),
                        )
                    } else {
                        // Peeling left-to-right (backward page turn)
                        val foldX = w * 1.18f * p
                        val skew = bias * h * 0.17f * arcSin
                        val topX = foldX - skew
                        val botX = foldX + skew
                        val midX = (topX + botX) * 0.5f - minOf(w * 0.075f, foldX.coerceAtLeast(0f) * 0.22f) * arcSin
                        val flapW = (foldX.coerceAtLeast(0f) * 0.48f).coerceIn(16f, w * 0.48f)

                        // Shadow cast by lifting sheet onto uncovered left page
                        val shadowPath = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(topX, 0f)
                            quadraticTo(midX, h * 0.5f, botX, h)
                            lineTo(0f, h)
                            close()
                        }
                        clipPath(shadowPath) {
                            val endX = maxOf(topX, botX)
                            val startX = (endX - flapW * 1.15f - 56f).coerceAtLeast(0f)
                            drawRect(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.14f * arcSin),
                                        Color.Black.copy(alpha = 0.36f * arcSin),
                                    ),
                                    startX = startX,
                                    endX = endX.coerceAtLeast(startX + 4f),
                                ),
                            )
                        }

                        // Curled paper flap rolling to the right of the crease
                        val flapTopX = (topX + flapW * (0.95f - 0.14f * bias)).coerceAtMost(w * 1.25f)
                        val flapBotX = (botX + flapW * (0.95f + 0.14f * bias)).coerceAtMost(w * 1.25f)
                        val flapOuterMidX = (flapTopX + flapBotX) * 0.5f + flapW * 0.15f * arcSin
                        val topArchY = (h * 0.025f * arcSin * (1f - bias * 0.4f)).coerceAtLeast(0f)
                        val botArchY = h - (h * 0.025f * arcSin * (1f + bias * 0.4f)).coerceAtLeast(0f)

                        val flapPath = Path().apply {
                            moveTo(topX, 0f)
                            quadraticTo(midX, h * 0.5f, botX, h)
                            lineTo(flapBotX, botArchY)
                            quadraticTo(flapOuterMidX, h * 0.5f, flapTopX, topArchY)
                            close()
                        }

                        drawPath(
                            path = flapPath,
                            color = Color.Black.copy(alpha = 0.18f * arcSin),
                            style = Stroke(width = 22f),
                        )
                        drawPath(
                            path = flapPath,
                            color = Color.Black.copy(alpha = 0.09f * arcSin),
                            style = Stroke(width = 38f),
                        )

                        clipPath(flapPath) {
                            drawRect(theme.pageBg)
                            if (turningBmp != null && !turningBmp.isRecycled) {
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    nc.save()
                                    val rectLeft = minOf(topX, botX)
                                    val rectRight = maxOf(flapTopX, flapBotX)
                                    val rectW = (rectRight - rectLeft).coerceAtLeast(1f)
                                    nc.translate(rectLeft + rectW, 0f)
                                    nc.scale(-1f, 1f)
                                    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                                        alpha = if (theme.isDark) 28 else 38
                                    }
                                    nc.drawBitmap(
                                        turningBmp,
                                        null,
                                        android.graphics.RectF(0f, 0f, rectW * 1.45f, h),
                                        paint,
                                    )
                                    nc.restore()
                                }
                            }
                            val gradStart = minOf(topX, botX)
                            val gradEnd = maxOf(flapTopX, flapBotX)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.28f * arcSin),
                                        Color.White.copy(alpha = (if (theme.isDark) 0.18f else 0.62f) * arcSin),
                                        Color.Black.copy(alpha = 0.12f * arcSin),
                                        Color.Transparent,
                                        Color.White.copy(alpha = if (theme.isDark) 0.14f else 0.52f),
                                        Color.Black.copy(alpha = 0.22f * arcSin),
                                    ),
                                    startX = gradStart,
                                    endX = gradEnd.coerceAtLeast(gradStart + 4f),
                                ),
                            )
                        }

                        drawPath(
                            path = flapPath,
                            color = theme.mutedText.copy(alpha = 0.30f),
                            style = Stroke(width = 1.6f),
                        )
                    }
                    @Suppress("UNUSED_VARIABLE")
                    val unusedCorner = cornerRadiusPx
                }
            }
        }
    }
}

private fun Modifier.drawCurvedPageClip(
    turnDir: Int,
    progress: Float,
    touchY: Float,
): Modifier = this.then(
    Modifier.graphicsLayer {
        clip = true
        shape = object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density,
            ): androidx.compose.ui.graphics.Outline {
                val w = size.width
                val h = size.height
                val p = progress.coerceIn(0f, 1f)
                val arcSin = sin(p * PI).toFloat()
                val bias = (touchY - 0.5f) * 2f
                val path = Path()
                if (turnDir > 0) {
                    val foldX = w * (1f - 1.18f * p)
                    val skew = bias * h * 0.17f * arcSin
                    val topX = foldX + skew
                    val botX = foldX - skew
                    val midX = (topX + botX) * 0.5f + minOf(w * 0.075f, (w - foldX).coerceAtLeast(0f) * 0.22f) * arcSin
                    path.moveTo(0f, 0f)
                    path.lineTo(topX, 0f)
                    path.quadraticTo(midX, h * 0.5f, botX, h)
                    path.lineTo(0f, h)
                    path.close()
                } else {
                    val foldX = w * 1.18f * p
                    val skew = bias * h * 0.17f * arcSin
                    val topX = foldX - skew
                    val botX = foldX + skew
                    val midX = (topX + botX) * 0.5f - minOf(w * 0.075f, foldX.coerceAtLeast(0f) * 0.22f) * arcSin
                    path.moveTo(topX, 0f)
                    path.lineTo(w, 0f)
                    path.lineTo(w, h)
                    path.lineTo(botX, h)
                    path.quadraticTo(midX, h * 0.5f, topX, 0f)
                    path.close()
                }
                return androidx.compose.ui.graphics.Outline.Generic(path)
            }
        }
    },
)

@Composable
private fun BookSheetFrame(
    theme: BookTheme,
    spread: Boolean,
    leftBmp: Bitmap?,
    rightBmp: Bitmap?,
    leftPageNum: Int,
    rightPageNum: Int?,
    totalPages: Int,
    title: String,
    colorFilter: ColorFilter?,
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .shadow(10.dp, shape, clip = false)
            .clip(shape)
            .background(theme.pageBg)
            .border(1.dp, theme.mutedText.copy(alpha = 0.14f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Classic Book Running Header
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif),
                    color = theme.mutedText.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (spread && rightPageNum != null) "pp. $leftPageNum–$rightPageNum / $totalPages"
                    else "p. $leftPageNum / $totalPages",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif),
                    color = theme.mutedText.copy(alpha = 0.78f),
                )
            }

            if (spread) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    BookPageSurface(
                        bmp = leftBmp,
                        pageNum = leftPageNum,
                        colorFilter = colorFilter,
                        theme = theme,
                        shimmer = shimmer,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    // Open-book central binding gutter crease
                    Box(
                        Modifier
                            .width(14.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = if (theme.isDark) 0.42f else 0.16f),
                                        Color.Black.copy(alpha = if (theme.isDark) 0.25f else 0.08f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    if (rightPageNum != null) {
                        BookPageSurface(
                            bmp = rightBmp,
                            pageNum = rightPageNum,
                            colorFilter = colorFilter,
                            theme = theme,
                            shimmer = shimmer,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    } else {
                        Box(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else {
                BookPageSurface(
                    bmp = leftBmp,
                    pageNum = leftPageNum,
                    colorFilter = colorFilter,
                    theme = theme,
                    shimmer = shimmer,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BookPageSurface(
    bmp: Bitmap?,
    pageNum: Int,
    colorFilter: ColorFilter?,
    theme: BookTheme,
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.pageBg),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Book page $pageNum",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            BookPageLoaderPreview(
                theme = theme,
                pageNum = pageNum,
                shimmer = shimmer,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Animated book page skeleton wireframe displayed while high-res PDF/note pages render. */
@Composable
private fun BookPageLoaderPreview(
    theme: BookTheme,
    pageNum: Int,
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(theme.pageBg), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padX = w * 0.11f
            val padY = h * 0.10f
            val lineCol = theme.mutedText.copy(alpha = 0.14f)
            val accentCol = theme.accent.copy(alpha = 0.20f)
            val cardCol = theme.mutedText.copy(alpha = 0.08f)

            // Chapter heading wireframe
            drawRoundRect(accentCol, Offset(padX, padY), Size((w - padX * 2f) * 0.45f, h * 0.032f), CornerRadius(8f))
            // Paragraph lines
            val ratios = listOf(0.92f, 0.96f, 0.85f, 0.94f, 0.68f, 0.90f, 0.93f, 0.78f, 0.91f, 0.56f)
            var y = padY + h * 0.08f
            val step = h * 0.055f
            for (r in ratios) {
                if (y > h - padY * 2f) break
                drawRoundRect(lineCol, Offset(padX, y), Size((w - padX * 2f) * r, h * 0.018f), CornerRadius(6f))
                y += step
            }
            // Inset figure skeleton
            drawRoundRect(cardCol, Offset(padX, h * 0.68f), Size(w - padX * 2f, h * 0.18f), CornerRadius(12f))

            // Smooth horizontal shimmer sweep
            val sweepX = -w * 0.3f + w * 1.6f * shimmer
            drawRect(
                color = if (theme.isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.42f),
                topLeft = Offset(sweepX, 0f),
                size = Size(w * 0.25f, h),
            )
        }
        AppSurface(
            role = SurfaceRole.Popover,
            containerColor = theme.chromeBg,
            contentColor = theme.inkText,
            shape = appPillShape(),
            borderColor = theme.mutedText.copy(alpha = 0.22f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                "Loading Page $pageNum…",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Serif),
                color = theme.inkText,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
