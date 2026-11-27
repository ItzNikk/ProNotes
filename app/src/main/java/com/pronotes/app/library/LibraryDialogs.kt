package com.pronotes.app.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CropLandscape
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.pronotes.app.data.CoverColors
import com.pronotes.app.data.CoverPatterns
import com.pronotes.app.data.Folder
import com.pronotes.app.data.FolderColors
import com.pronotes.app.data.NotebookMeta
import com.pronotes.app.data.PagePreset
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppContextMenu
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.Swatch
import com.pronotes.app.ui.appCardShape

/**
 * Animated PDF skeleton loader card displayed while importing a PDF document.
 */
@Composable
fun PdfImportLoaderOverlay(
    title: String,
    page: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val shimmer = if (reduceMotion) {
        0f
    } else {
        val animatedShimmer by rememberInfiniteTransition(label = "pdfImportShimmer").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart),
            label = "sweep",
        )
        animatedShimmer
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        AppCard(
            hero = true,
            shape = appCardShape(4f),
            contentPadding = PaddingValues(20.dp),
            modifier = modifier.padding(24.dp).widthIn(min = 260.dp, max = 380.dp).fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PdfSkeletonPreviewBox(
                    shimmer = shimmer,
                    modifier = Modifier.size(width = 72.dp, height = 98.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Loading PDF",
                        style = MaterialTheme.typography.labelSmall,
                        color = ProButtonPalette.ElectricBlue,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        title.ifBlank { "Document" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (total > 0) "Indexing page $page of $total" else "Preparing PDF pages…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (total > 0) {
                        LinearProgressIndicator(
                            progress = { (page.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                            color = ProButtonPalette.ElectricBlue,
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                            color = ProButtonPalette.ElectricBlue,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfSkeletonPreviewBox(
    shimmer: Float,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val accentLine = ProButtonPalette.ElectricBlue.copy(alpha = 0.24f)
    Canvas(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
    ) {
        val w = size.width
        val h = size.height
        val pad = w * 0.13f
        drawRoundRect(accentLine, Offset(pad, pad), Size(w * 0.56f, h * 0.09f), CornerRadius(4f))
        drawRoundRect(lineColor.copy(alpha = 0.10f), Offset(pad, h * 0.26f), Size(w - pad * 2f, h * 0.24f), CornerRadius(6f))
        val lineRatios = listOf(0.84f, 0.92f, 0.72f, 0.88f, 0.55f)
        lineRatios.forEachIndexed { idx, ratio ->
            val y = h * (0.58f + idx * 0.075f)
            drawRoundRect(lineColor, Offset(pad, y), Size((w - pad * 2f) * ratio, h * 0.036f), CornerRadius(3f))
        }
        val sweepX = -w * 0.3f + (w * 1.6f) * shimmer
        drawRect(
            color = Color.White.copy(alpha = 0.22f),
            topLeft = Offset(sweepX, 0f),
            size = Size(w * 0.26f, h),
        )
    }
}

internal fun tagTintFor(tag: String): Color {
    val palette = listOf(
        ProButtonPalette.ElectricBlue,
        ProButtonPalette.Emerald,
        ProButtonPalette.VibrantOrange,
        ProButtonPalette.Violet,
        ProButtonPalette.MagentaPink,
        ProButtonPalette.MintTeal,
        ProButtonPalette.SkyCyan,
    )
    val idx = (tag.lowercase().hashCode() and 0x7FFFFFFF) % palette.size
    return palette[idx]
}

@Composable
internal fun TagBadgePill(tag: String, onClick: () -> Unit) {
    val tint = tagTintFor(tag)
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f))
            .border(1.dp, tint.copy(alpha = 0.48f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = "#$tag",
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun MoveRow(label: String, folder: Folder?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (folder != null) FolderArt(folder.color, "", Modifier.size(28.dp)) else Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

@Composable
internal fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val FolderTags = listOf("", "Study", "Work", "Lab", "Art", "Math", "Bio", "Code", "Plan", "Read", "Idea", "Star")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FolderDialog(existing: Folder?, onDismiss: () -> Unit, onSave: (String, Int, String) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "New folder") }
    var color by remember { mutableStateOf(existing?.color ?: FolderColors.first()) }
    var tag by remember { mutableStateOf(existing?.emoji ?: "") }
    var custom by remember { mutableStateOf(false) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New folder" else "Edit folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                FolderArt(color, tag, Modifier.size(width = 120.dp, height = 104.dp).align(Alignment.CenterHorizontally))
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") })
                Text("Colour", style = MaterialTheme.typography.labelSmall)
                FlowRow {
                    FolderColors.forEach { c -> Swatch(c, c == color) { color = c } }
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { custom = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, "Custom colour", Modifier.size(16.dp)) }
                }
                Text("Label", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FolderTags.forEach { t ->
                        AppChip(selected = t == tag, onClick = { tag = t }, label = t.ifEmpty { "None" })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), color, tag) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (custom) ColorEditorDialog(color, FolderColors, emptyList(), onDismiss = { custom = false }) { color = it; custom = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverGallery(selected: String, onSelect: (String) -> Unit, onPhoto: () -> Unit) {
    val solidSpecs = remember { CoverColors.map { "solid:$it" } }
    val patternSpecs = remember {
        CoverPatterns.flatMap { p ->
            val tones = when (p) {
                "leather" -> listOf(0xFF5D3A23, 0xFF1C1C1E, 0xFF0E2A47, 0xFF6B1E2A)
                "kraft" -> listOf(0xFFC49A6C, 0xFFA77B4F)
                "marble" -> listOf(0xFF8E9AAF, 0xFF2E3440, 0xFFB5838D)
                "terrazzo" -> listOf(0xFFE9E4DA, 0xFF6C757D)
                "linen" -> listOf(0xFF9DB4C0, 0xFFB5A397, 0xFF5C6B73)
                "gradient" -> listOf(0xFF3D6CF4, 0xFF9B51E0, 0xFFEB5757, 0xFF2BB673)
                else -> listOf(CoverColors[0], CoverColors[3], CoverColors[1], CoverColors[9]).map { it.toLong() and 0xFFFFFFFFL }
            }
            tones.map { "pattern:$p:${it.toInt()}" }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Quick special covers: Page 1 & Photo
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            val thumbSel = selected == "thumb"
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (thumbSel) ProButtonPalette.ElectricBlue.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    )
                    .border(
                        width = if (thumbSel) 1.6.dp else 1.dp,
                        color = if (thumbSel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect("thumb") }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = if (thumbSel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Column {
                    Text("Page 1 cover", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text("Shows first page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val photoSel = selected == "photo"
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (photoSel) ProButtonPalette.ElectricBlue.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    )
                    .border(
                        width = if (photoSel) 1.6.dp else 1.dp,
                        color = if (photoSel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable(onClick = onPhoto)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = if (photoSel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Column {
                    Text("Custom photo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text("From gallery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "CLASSIC MATTE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                solidSpecs.forEach { spec ->
                    CoverTile(spec = spec, selected = spec == selected, onClick = { onSelect(spec) })
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "TEXTURED & STUDIO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                patternSpecs.forEach { spec ->
                    CoverTile(spec = spec, selected = spec == selected, onClick = { onSelect(spec) })
                }
            }
        }
    }
}

@Composable
private fun CoverTile(spec: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 56.dp, height = 74.dp)
            .shadow(if (selected) 5.dp else 1.dp, NotebookShape)
            .border(
                width = if (selected) 2.5.dp else 0.75.dp,
                color = if (selected) ProButtonPalette.ElectricBlue else Color.Black.copy(alpha = 0.14f),
                shape = NotebookShape,
            )
            .clickable(onClick = onClick),
    ) {
        CoverArt(spec, "", null, 0, Modifier.fillMaxSize(), showTitle = false)
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(ProButtonPalette.ElectricBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
internal fun CoverDialog(current: String, onDismiss: () -> Unit, onPick: (String, Uri?) -> Unit) {
    var spec by remember { mutableStateOf(current) }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPick(spec, uri) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cover") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { CoverGallery(spec, { spec = it }) { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } } },
        confirmButton = { TextButton(onClick = { onPick(spec, null) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

data class NewBook(
    val name: String,
    val cover: String,
    val coverColor: Int,
    val paper: PaperStyle,
    val photo: Uri?,
    val width: Float,
    val height: Float,
    val paperColor: Int?,
    val coverPage: Boolean,
    val isWhiteboard: Boolean = false,
    val languageTag: String? = null,
)

private val PaperColors = listOf(
    0xFFFFFFFF, // White Paper
    0xFFFAF6E6, // Yellow Paper
    0xFFFBF7EC, // Cream Paper
    0xFFF1F2F4, // Cool Grey
    0xFFEAF4FF, // Sky Paper
    0xFFEFF8EC, // Mint Paper
    0xFF22252B, // Dark Paper
).map { it.toInt() }

private val PaperColorNames = listOf(
    "White Paper",
    "Yellow Paper",
    "Cream Paper",
    "Cool Grey",
    "Sky Paper",
    "Mint Paper",
    "Dark Paper",
)

private val SolidCoverNames = listOf(
    "Forest",
    "Navy",
    "Burgundy",
    "Ochre",
    "Charcoal",
    "Sage",
    "Simple",
    "Cobalt",
    "Emerald",
    "Amber",
    "Coral",
    "Violet",
)

private enum class NotebookCreatorPane { COVER, PAPER }

private val CreatorPaperSections: List<Pair<String, List<PaperStyle>>> = listOf(
    "Essentials" to listOf(
        PaperStyle.BLANK,
        PaperStyle.DOTTED,
        PaperStyle.NARROW,
        PaperStyle.WIDE,
        PaperStyle.RULED,
        PaperStyle.GRID,
    ),
    "Writing papers" to listOf(
        PaperStyle.CORNELL,
        PaperStyle.LEGAL,
        PaperStyle.COLLEGE,
        PaperStyle.TWO_COLUMN,
        PaperStyle.HANDWRITING,
        PaperStyle.SEYES,
        PaperStyle.CALLIGRAPHY,
        PaperStyle.CORNELL_GRID,
        PaperStyle.DOT_CORNELL,
        PaperStyle.STUDY,
    ),
    "Planner" to listOf(
        PaperStyle.PLANNER,
        PaperStyle.DAILY,
        PaperStyle.MONTHLY,
        PaperStyle.DAY_TIMEBLOCK,
        PaperStyle.TODO,
        PaperStyle.CHECKLIST,
        PaperStyle.MEETING,
        PaperStyle.HABIT_TRACKER,
    ),
    "Grid & Technical" to listOf(
        PaperStyle.SMALL_GRID,
        PaperStyle.GRAPH,
        PaperStyle.ENGINEERING,
        PaperStyle.ISOMETRIC,
        PaperStyle.HEX,
    ),
    "Creative & Music" to listOf(
        PaperStyle.STORYBOARD,
        PaperStyle.COMIC_PANELS,
        PaperStyle.MUSIC,
        PaperStyle.TABLATURE,
    ),
)

private val CreatorSolidCoverSpecs: List<Pair<String, String>> = CoverColors.mapIndexed { idx, argb ->
    "solid:$argb" to SolidCoverNames.getOrElse(idx) { "Simple" }
}

private val CreatorPatternCoverSpecs: List<Pair<String, String>> = CoverPatterns.flatMap { p ->
    val tones = when (p) {
        "leather" -> listOf(0xFF5D3A23, 0xFF1C1C1E, 0xFF0E2A47, 0xFF6B1E2A)
        "kraft" -> listOf(0xFFC49A6C, 0xFFA77B4F)
        "marble" -> listOf(0xFF8E9AAF, 0xFF2E3440, 0xFFB5838D)
        "terrazzo" -> listOf(0xFFE9E4DA, 0xFF6C757D)
        "linen" -> listOf(0xFF9DB4C0, 0xFFB5A397, 0xFF5C6B73)
        "gradient" -> listOf(0xFF3D6CF4, 0xFF9B51E0, 0xFFEB5757, 0xFF2BB673)
        else -> listOf(CoverColors[0], CoverColors[3], CoverColors[1], CoverColors[9]).map { it.toLong() and 0xFFFFFFFFL }
    }
    val label = p.replaceFirstChar { it.uppercase() }
    tones.map { "pattern:$p:${it.toInt()}" to label }
}

private fun coverFriendlyName(spec: String, photo: Uri?): String = when {
    photo != null || spec == "photo" -> "Custom Photo"
    spec == "thumb" -> "No Cover"
    spec.startsWith("solid:") -> {
        val argb = spec.removePrefix("solid:").toLongOrNull()?.toInt()
        val idx = CoverColors.indexOfFirst { it == argb }
        SolidCoverNames.getOrElse(idx) { "Simple" }
    }
    spec.startsWith("pattern:") -> {
        val pat = spec.split(':').getOrNull(1).orEmpty()
        pat.replaceFirstChar { it.uppercase() }.ifBlank { "Textured" }
    }
    else -> "Simple"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewNotebookDialog(
    defaults: LibraryNewBookPrefs,
    onDismiss: () -> Unit,
    onCreate: (NewBook) -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val accentBlue = Color(0xFF0A84FF)

    val nameState = remember { mutableStateOf("") }
    // Default to warm kraft-taupe "Simple" cover unless user prefers Page 1 thumbnails
    val startColor = remember { CoverColors.getOrElse(6) { CoverColors.first() } }
    var cover by remember { mutableStateOf(if (defaults.thumbCovers) "thumb" else "solid:$startColor") }
    var coverPage by remember { mutableStateOf(defaults.defaultCoverPage) }
    var paper by remember { mutableStateOf(defaults.defaultPaper) }
    var paperColor by remember { mutableStateOf<Int?>(null) }
    var customPaper by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf(defaults.defaultPreset) }
    var landscape by remember { mutableStateOf(defaults.defaultLandscape) }
    var wMm by remember { mutableStateOf("%.0f".format(defaults.defaultPreset.wMm)) }
    var hMm by remember { mutableStateOf("%.0f".format(defaults.defaultPreset.hMm)) }
    var languageTag by remember { mutableStateOf(defaults.handwritingLanguage.ifBlank { "en-US" }) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var activePane by remember { mutableStateOf(NotebookCreatorPane.PAPER) }

    val collapsedSections = remember { mutableStateMapOf<String, Boolean>() }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photo = uri
            cover = "photo"
        }
    }

    val effectiveLandscape = if (preset.isWhiteboard) true else landscape
    val previewPaperColor = paperColor ?: 0xFFFFFFFF.toInt()
    val selectedPaperColorLabel = remember(paperColor) {
        if (paperColor == null) "White Paper"
        else {
            val idx = PaperColors.indexOfFirst { it == paperColor }
            if (idx >= 0) PaperColorNames[idx] else "Custom Paper"
        }
    }
    val strings = com.pronotes.app.data.LocalAppStrings.current
    var langPickerOpen by remember { mutableStateOf(false) }
    val selectedLanguageLabel = remember(languageTag) {
        val lang = com.pronotes.app.editor.HandwritingRecognizer.findLanguage(languageTag)
        if (lang.nativeName == lang.name) lang.name else lang.nativeName
    }

    val railBg = if (dark) Color(0xFF1A1C1E) else MaterialTheme.colorScheme.surfaceContainerLow
    val galleryBg = if (dark) Color(0xFF232528) else MaterialTheme.colorScheme.surface
    val footerBg = if (dark) Color(0xFF1E2023) else MaterialTheme.colorScheme.surfaceContainer
    val dividerColor = if (dark) Color(0xFF33363B) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    val pillBg = if (dark) Color(0xFF2C2F34) else MaterialTheme.colorScheme.surfaceContainerHigh
    val pillBorder = if (dark) Color(0xFF3B3F46) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val primaryText = if (dark) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.onSurface
    val mutedText = if (dark) Color(0xFF9CA3AF) else MaterialTheme.colorScheme.onSurfaceVariant

    fun submitCreate() {
        val color = cover.substringAfterLast(':').toLongOrNull()?.toInt() ?: startColor
        var w = (wMm.toFloatOrNull() ?: 210f).coerceIn(40f, 1000f)
        var h = (hMm.toFloatOrNull() ?: 297f).coerceIn(40f, 1000f)
        if (!preset.isWhiteboard && landscape != (w > h)) {
            val t = w
            w = h
            h = t
        }
        val k = PagePreset.UNITS_PER_MM
        onCreate(
            NewBook(
                name = nameState.value.trim().ifBlank { if (preset.isWhiteboard) "Whiteboard" else "Untitled Notebook" },
                cover = if (cover == "photo") "" else cover,
                coverColor = color,
                paper = paper,
                photo = photo,
                width = w * k,
                height = h * k,
                paperColor = paperColor,
                coverPage = coverPage && cover != "thumb" && !preset.isWhiteboard,
                isWhiteboard = preset.isWhiteboard,
                languageTag = languageTag,
            ),
        )
    }

    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val wide = screenWidthDp >= 590
    val perfMode = com.pronotes.app.ui.LocalPerformanceMode.current
    val motionStyle = com.pronotes.app.ui.LocalMotionStyle.current

    val onSelectPaneCb = remember { { p: NotebookCreatorPane -> activePane = p } }
    val onSelectPresetCb = remember {
        { p: PagePreset ->
            preset = p
            if (p != PagePreset.CUSTOM) {
                wMm = "%.0f".format(p.wMm)
                hMm = "%.0f".format(p.hMm)
            }
            if (p.isWhiteboard && paper == PaperStyle.BLANK) paper = PaperStyle.DOTTED
        }
    }
    val onWMmChangeCb = remember { { v: String -> wMm = v.filter { it.isDigit() }.take(4) } }
    val onHMmChangeCb = remember { { v: String -> hMm = v.filter { it.isDigit() }.take(4) } }
    val onSelectPaperColorCb = remember { { c: Int? -> paperColor = c } }
    val onOpenCustomPaperColorCb = remember { { customPaper = true } }
    val onOpenLanguagePickerCb = remember { { langPickerOpen = true } }
    val onLandscapeChangeCb = remember { { l: Boolean -> landscape = l } }
    val onSelectPaperCb = remember { { p: PaperStyle -> paper = p } }
    val onCoverPageChangeCb = remember { { cp: Boolean -> coverPage = cp } }
    val onSelectCoverCb = remember { { spec: String -> cover = spec; photo = null } }
    val onPickPhotoCb = remember(pick) {
        { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
    val onToggleSectionCb = remember(collapsedSections) {
        { key: String -> collapsedSections[key] = collapsedSections[key] != true }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val enterAnim = remember { androidx.compose.animation.core.Animatable(if (reduceMotion) 1f else 0f) }
        LaunchedEffect(reduceMotion) {
            if (reduceMotion) enterAnim.snapTo(1f)
            else enterAnim.animateTo(1f, com.pronotes.app.ui.ProMotion.sheetSpring(false, perfMode, motionStyle))
        }
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = galleryBg,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth()
                .widthIn(max = 920.dp)
                .heightIn(min = 420.dp, max = 690.dp)
                .graphicsLayer {
                    val p = enterAnim.value.coerceIn(0f, 1.03f)
                    alpha = p.coerceIn(0f, 1f)
                    val sc = 0.95f + 0.05f * p
                    scaleX = sc
                    scaleY = sc
                    translationY = (1f - p) * 18f
                }
                .border(1.dp, dividerColor, RoundedCornerShape(22.dp)),
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    val inspectorMod = if (wide) Modifier.width(244.dp).fillMaxHeight() else Modifier.fillMaxWidth().heightIn(max = 225.dp)
                    val browserMod = if (wide) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()

                    if (wide) {
                        Row(Modifier.fillMaxSize()) {
                            CreatorInspectorPane(
                                modifier = inspectorMod,
                                wide = true,
                                dark = dark,
                                railBg = railBg,
                                pillBorder = pillBorder,
                                primaryText = primaryText,
                                mutedText = mutedText,
                                accentBlue = accentBlue,
                                nameState = nameState,
                                isWhiteboard = preset.isWhiteboard,
                                cover = cover,
                                photo = photo,
                                paper = paper,
                                previewPaperColor = previewPaperColor,
                                effectiveLandscape = effectiveLandscape,
                                activePane = activePane,
                                onSelectPane = onSelectPaneCb,
                                strings = strings,
                            )
                            VerticalDivider(thickness = 1.dp, color = dividerColor)
                            CreatorBrowserPane(
                                modifier = browserMod.weight(1f),
                                dark = dark,
                                galleryBg = galleryBg,
                                pillBg = pillBg,
                                pillBorder = pillBorder,
                                primaryText = primaryText,
                                mutedText = mutedText,
                                accentBlue = accentBlue,
                                activePane = activePane,
                                preset = preset,
                                onSelectPreset = onSelectPresetCb,
                                wMm = wMm,
                                hMm = hMm,
                                onWMmChange = onWMmChangeCb,
                                onHMmChange = onHMmChangeCb,
                                paperColor = paperColor,
                                selectedPaperColorLabel = selectedPaperColorLabel,
                                onSelectPaperColor = onSelectPaperColorCb,
                                onOpenCustomPaperColor = onOpenCustomPaperColorCb,
                                selectedLanguageLabel = selectedLanguageLabel,
                                onOpenLanguagePicker = onOpenLanguagePickerCb,
                                landscape = landscape,
                                effectiveLandscape = effectiveLandscape,
                                onLandscapeChange = onLandscapeChangeCb,
                                paper = paper,
                                previewPaperColor = previewPaperColor,
                                onSelectPaper = onSelectPaperCb,
                                cover = cover,
                                photo = photo,
                                coverPage = coverPage,
                                onCoverPageChange = onCoverPageChangeCb,
                                onSelectCover = onSelectCoverCb,
                                onPickPhoto = onPickPhotoCb,
                                isSectionCollapsed = { collapsedSections[it] == true },
                                onToggleSection = onToggleSectionCb,
                                strings = strings,
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            CreatorInspectorPane(
                                modifier = inspectorMod,
                                wide = false,
                                dark = dark,
                                railBg = railBg,
                                pillBorder = pillBorder,
                                primaryText = primaryText,
                                mutedText = mutedText,
                                accentBlue = accentBlue,
                                nameState = nameState,
                                isWhiteboard = preset.isWhiteboard,
                                cover = cover,
                                photo = photo,
                                paper = paper,
                                previewPaperColor = previewPaperColor,
                                effectiveLandscape = effectiveLandscape,
                                activePane = activePane,
                                onSelectPane = onSelectPaneCb,
                                strings = strings,
                            )
                            HorizontalDivider(thickness = 1.dp, color = dividerColor)
                            CreatorBrowserPane(
                                modifier = browserMod.weight(1f),
                                dark = dark,
                                galleryBg = galleryBg,
                                pillBg = pillBg,
                                pillBorder = pillBorder,
                                primaryText = primaryText,
                                mutedText = mutedText,
                                accentBlue = accentBlue,
                                activePane = activePane,
                                preset = preset,
                                onSelectPreset = onSelectPresetCb,
                                wMm = wMm,
                                hMm = hMm,
                                onWMmChange = onWMmChangeCb,
                                onHMmChange = onHMmChangeCb,
                                paperColor = paperColor,
                                selectedPaperColorLabel = selectedPaperColorLabel,
                                onSelectPaperColor = onSelectPaperColorCb,
                                onOpenCustomPaperColor = onOpenCustomPaperColorCb,
                                selectedLanguageLabel = selectedLanguageLabel,
                                onOpenLanguagePicker = onOpenLanguagePickerCb,
                                landscape = landscape,
                                effectiveLandscape = effectiveLandscape,
                                onLandscapeChange = onLandscapeChangeCb,
                                paper = paper,
                                previewPaperColor = previewPaperColor,
                                onSelectPaper = onSelectPaperCb,
                                cover = cover,
                                photo = photo,
                                coverPage = coverPage,
                                onCoverPageChange = onCoverPageChangeCb,
                                onSelectCover = onSelectCoverCb,
                                onPickPhoto = onPickPhotoCb,
                                isSectionCollapsed = { collapsedSections[it] == true },
                                onToggleSection = onToggleSectionCb,
                                strings = strings,
                            )
                        }
                    }
                }

                // Bottom Footer Action Bar
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(footerBg)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = pillBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, pillBorder),
                    ) {
                        Text(
                            text = strings.cancel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = primaryText,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                    Button(
                        onClick = { submitCreate() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentBlue,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = if (preset.isWhiteboard) "Create Whiteboard" else strings.createNotebook,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    if (langPickerOpen) {
        com.pronotes.app.ui.HandwritingLanguagePickerDialog(
            selectedTag = languageTag,
            onDismiss = { langPickerOpen = false },
            onSelect = { lang ->
                languageTag = lang.tag
                langPickerOpen = false
            },
        )
    }

    if (customPaper) {
        ColorEditorDialog(
            paperColor ?: 0xFFFFFFFF.toInt(),
            PaperColors,
            emptyList(),
            onDismiss = { customPaper = false },
        ) {
            paperColor = it
            customPaper = false
        }
    }
}

@Composable
private fun CreatorTitleField(
    nameState: androidx.compose.runtime.MutableState<String>,
    isWhiteboard: Boolean,
    dark: Boolean,
    pillBorder: Color,
    primaryText: Color,
    mutedText: Color,
    accentBlue: Color,
    strings: com.pronotes.app.data.AppStrings,
) {
    val name = nameState.value
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = strings.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = primaryText,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (dark) Color(0xFF282A2E) else MaterialTheme.colorScheme.surface)
                .border(1.dp, pillBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            if (name.isEmpty()) {
                Text(
                    text = if (isWhiteboard) "Untitled Whiteboard" else strings.untitledNotebook,
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = name,
                onValueChange = { nameState.value = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = primaryText),
                cursorBrush = SolidColor(accentBlue),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CreatorInspectorPane(
    modifier: Modifier,
    wide: Boolean,
    dark: Boolean,
    railBg: Color,
    pillBorder: Color,
    primaryText: Color,
    mutedText: Color,
    accentBlue: Color,
    nameState: androidx.compose.runtime.MutableState<String>,
    isWhiteboard: Boolean,
    cover: String,
    photo: Uri?,
    paper: PaperStyle,
    previewPaperColor: Int,
    effectiveLandscape: Boolean,
    activePane: NotebookCreatorPane,
    onSelectPane: (NotebookCreatorPane) -> Unit,
    strings: com.pronotes.app.data.AppStrings,
) {
    Column(
        modifier = modifier
            .background(railBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Title input completely isolated in its own recomposition scope so typing never recomposes any preview or gallery tile.
        CreatorTitleField(
            nameState = nameState,
            isWhiteboard = isWhiteboard,
            dark = dark,
            pillBorder = pillBorder,
            primaryText = primaryText,
            mutedText = mutedText,
            accentBlue = accentBlue,
            strings = strings,
        )

        val wDp = if (wide) (if (effectiveLandscape) 128.dp else 92.dp) else (if (effectiveLandscape) 78.dp else 56.dp)
        val hDp = if (wide) (if (effectiveLandscape) 90.dp else 122.dp) else (if (effectiveLandscape) 56.dp else 76.dp)

        if (wide) {
            InspectorPreviewCard(
                heading = strings.cover,
                label = coverFriendlyName(cover, photo),
                selected = activePane == NotebookCreatorPane.COVER,
                accentColor = accentBlue,
                dark = dark,
                primaryText = primaryText,
                mutedText = mutedText,
                onClick = { onSelectPane(NotebookCreatorPane.COVER) },
            ) {
                CoverMiniThumbnail(
                    spec = cover,
                    photo = photo,
                    landscape = effectiveLandscape,
                    dark = dark,
                    widthDp = wDp.value,
                    heightDp = hDp.value,
                    drawShadow = true,
                )
            }

            InspectorPreviewCard(
                heading = strings.paper,
                label = paper.label,
                selected = activePane == NotebookCreatorPane.PAPER,
                accentColor = accentBlue,
                dark = dark,
                primaryText = primaryText,
                mutedText = mutedText,
                onClick = { onSelectPane(NotebookCreatorPane.PAPER) },
            ) {
                FastPaperThumb(
                    style = paper,
                    tint = previewPaperColor,
                    landscape = effectiveLandscape,
                    modifier = Modifier
                        .size(width = wDp, height = hDp)
                        .shadow(4.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .border(0.6.dp, Color.Black.copy(alpha = 0.14f), RoundedCornerShape(4.dp)),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InspectorPreviewCard(
                    heading = strings.cover,
                    label = coverFriendlyName(cover, photo),
                    selected = activePane == NotebookCreatorPane.COVER,
                    accentColor = accentBlue,
                    dark = dark,
                    primaryText = primaryText,
                    mutedText = mutedText,
                    onClick = { onSelectPane(NotebookCreatorPane.COVER) },
                    modifier = Modifier.weight(1f),
                    compact = true,
                ) {
                    CoverMiniThumbnail(
                        spec = cover,
                        photo = photo,
                        landscape = effectiveLandscape,
                        dark = dark,
                        widthDp = wDp.value,
                        heightDp = hDp.value,
                        drawShadow = false,
                    )
                }
                InspectorPreviewCard(
                    heading = strings.paper,
                    label = paper.label,
                    selected = activePane == NotebookCreatorPane.PAPER,
                    accentColor = accentBlue,
                    dark = dark,
                    primaryText = primaryText,
                    mutedText = mutedText,
                    onClick = { onSelectPane(NotebookCreatorPane.PAPER) },
                    modifier = Modifier.weight(1f),
                    compact = true,
                ) {
                    FastPaperThumb(
                        style = paper,
                        tint = previewPaperColor,
                        landscape = effectiveLandscape,
                        modifier = Modifier
                            .size(width = wDp, height = hDp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.6.dp, Color.Black.copy(alpha = 0.14f), RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatorBrowserPane(
    modifier: Modifier,
    dark: Boolean,
    galleryBg: Color,
    pillBg: Color,
    pillBorder: Color,
    primaryText: Color,
    mutedText: Color,
    accentBlue: Color,
    activePane: NotebookCreatorPane,
    preset: PagePreset,
    onSelectPreset: (PagePreset) -> Unit,
    wMm: String,
    hMm: String,
    onWMmChange: (String) -> Unit,
    onHMmChange: (String) -> Unit,
    paperColor: Int?,
    selectedPaperColorLabel: String,
    onSelectPaperColor: (Int?) -> Unit,
    onOpenCustomPaperColor: () -> Unit,
    selectedLanguageLabel: String,
    onOpenLanguagePicker: () -> Unit,
    landscape: Boolean,
    effectiveLandscape: Boolean,
    onLandscapeChange: (Boolean) -> Unit,
    paper: PaperStyle,
    previewPaperColor: Int,
    onSelectPaper: (PaperStyle) -> Unit,
    cover: String,
    photo: Uri?,
    coverPage: Boolean,
    onCoverPageChange: (Boolean) -> Unit,
    onSelectCover: (String) -> Unit,
    onPickPhoto: () -> Unit,
    isSectionCollapsed: (String) -> Boolean,
    onToggleSection: (String) -> Unit,
    strings: com.pronotes.app.data.AppStrings,
) {
    val reduceMotion = LocalReduceMotion.current
    val perfMode = com.pronotes.app.ui.LocalPerformanceMode.current
    val motionStyle = com.pronotes.app.ui.LocalMotionStyle.current
    Column(
        modifier = modifier
            .background(galleryBg)
            .fillMaxSize(),
    ) {
        // Top filter pill bar: Size | Paper Color | Language | Orientation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. Page Size fast popover pill
            var sizeMenuOpen by remember { mutableStateOf(false) }
            val sizeMenuItems = remember(preset) {
                PagePreset.entries.map { p ->
                    val dims = if (!p.isWhiteboard && p != PagePreset.CUSTOM) " (${"%.0f×%.0f".format(p.wMm, p.hMm)})" else ""
                    AppMenuItem(
                        label = "${p.label}$dims",
                        selected = p == preset,
                        onClick = { onSelectPreset(p) },
                    )
                }
            }
            Box {
                StudioFilterPill(
                    label = preset.label,
                    leadingIcon = {
                        Icon(
                            if (preset.isWhiteboard) Icons.Outlined.AllInclusive else Icons.Outlined.Description,
                            contentDescription = null,
                            tint = primaryText.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    bg = pillBg,
                    border = pillBorder,
                    fg = primaryText,
                    onClick = { sizeMenuOpen = true },
                )
                AppContextMenu(
                    expanded = sizeMenuOpen,
                    onDismissRequest = { sizeMenuOpen = false },
                    items = sizeMenuItems,
                )
            }

            // 2. Paper Color fast popover pill
            var colorMenuOpen by remember { mutableStateOf(false) }
            val colorMenuItems = remember(paperColor, strings) {
                PaperColors.mapIndexed { idx, argb ->
                    val sel = (paperColor ?: 0xFFFFFFFF.toInt()) == argb
                    AppMenuItem(
                        label = PaperColorNames[idx],
                        selected = sel,
                        icon = {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.45f), CircleShape),
                            )
                        },
                        onClick = { onSelectPaperColor(if (argb == 0xFFFFFFFF.toInt()) null else argb) },
                    )
                } + AppMenuItem(
                    label = strings.customPaperColor,
                    iconVector = Icons.Filled.Add,
                    onClick = onOpenCustomPaperColor,
                )
            }
            Box {
                StudioFilterPill(
                    label = selectedPaperColorLabel,
                    bg = pillBg,
                    border = pillBorder,
                    fg = primaryText,
                    onClick = { colorMenuOpen = true },
                )
                AppContextMenu(
                    expanded = colorMenuOpen,
                    onDismissRequest = { colorMenuOpen = false },
                    items = colorMenuItems,
                )
            }

            // 3. Global Language picker pill (58+ world handwriting languages)
            StudioFilterPill(
                label = selectedLanguageLabel,
                bg = pillBg,
                border = pillBorder,
                fg = primaryText,
                onClick = onOpenLanguagePicker,
            )

            // 4. Orientation segmented toggle pill (Portrait | Landscape)
            if (!preset.isWhiteboard) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pillBg)
                        .border(1.dp, pillBorder, CircleShape)
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OrientationSegmentIcon(
                        icon = Icons.Outlined.CropPortrait,
                        contentDescription = "Portrait",
                        selected = !landscape,
                        accent = accentBlue,
                        dark = dark,
                        onClick = { onLandscapeChange(false) },
                    )
                    OrientationSegmentIcon(
                        icon = Icons.Outlined.CropLandscape,
                        contentDescription = "Landscape",
                        selected = landscape,
                        accent = accentBlue,
                        dark = dark,
                        onClick = { onLandscapeChange(true) },
                    )
                }
            }
        }

        // Inline custom page size inputs when Custom preset is selected
        AnimatedVisibility(visible = preset == PagePreset.CUSTOM) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = wMm,
                    onValueChange = onWMmChange,
                    label = { Text("Width (mm)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = hMm,
                    onValueChange = onHMmChange,
                    label = { Text("Height (mm)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val tileW = if (effectiveLandscape) 86.dp else 66.dp
        val tileH = if (effectiveLandscape) 60.dp else 88.dp

        // Smooth connected swap between Paper & Cover galleries; each gallery is virtualized in LazyVerticalGrid
        androidx.compose.animation.AnimatedContent(
            targetState = activePane,
            transitionSpec = {
                com.pronotes.app.ui.ProMotion.contentSwap(reduceMotion, perfMode, motionStyle, 0.04f)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "creatorPaneSwap",
        ) { pane ->
            when (pane) {
                NotebookCreatorPane.PAPER -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 102.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CreatorPaperSections.forEach { (sectionTitle, templates) ->
                            val isCollapsed = isSectionCollapsed("p:$sectionTitle")
                            val localizedSecTitle = when (sectionTitle) {
                                "Essentials" -> strings.sectionEssentials
                                "Writing papers" -> strings.sectionWritingPapers
                                "Planner" -> strings.sectionPlanner
                                "Grid & Technical" -> strings.sectionGridTechnical
                                "Creative & Music" -> strings.sectionCreativeMusic
                                else -> sectionTitle
                            }
                            item(
                                key = "p_hdr_$sectionTitle",
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = "section_header",
                            ) {
                                CollapsibleSectionHeader(
                                    title = localizedSecTitle,
                                    collapsed = isCollapsed,
                                    fg = primaryText,
                                    onClick = { onToggleSection("p:$sectionTitle") },
                                )
                            }
                            if (!isCollapsed) {
                                items(
                                    items = templates,
                                    key = { "paper_${it.name}" },
                                    contentType = { "paper_tile" },
                                ) { style ->
                                    val sel = style == paper
                                    CreatorTemplateTile(
                                        label = style.label,
                                        selected = sel,
                                        accentBlue = accentBlue,
                                        mutedText = mutedText,
                                        onClick = { onSelectPaper(style) },
                                    ) {
                                        FastPaperThumb(
                                            style = style,
                                            tint = previewPaperColor,
                                            landscape = effectiveLandscape,
                                            modifier = Modifier
                                                .size(width = tileW, height = tileH)
                                                .clip(RoundedCornerShape(4.dp))
                                                .border(0.6.dp, Color.Black.copy(alpha = 0.14f), RoundedCornerShape(4.dp)),
                                        )
                                    }
                                }
                            }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "spacer") {
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                NotebookCreatorPane.COVER -> {
                    val quickCollapsed = isSectionCollapsed("c:Quick")
                    val simpleCollapsed = isSectionCollapsed("c:Simple")
                    val texCollapsed = isSectionCollapsed("c:Textured")

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 102.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (cover != "thumb" && !preset.isWhiteboard) {
                            item(
                                key = "c_cover_page_banner",
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = "banner",
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(pillBg.copy(alpha = 0.72f))
                                        .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(strings.includeCoverPage, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = primaryText)
                                        Text("Write titles or add stickers onto the front cover", style = MaterialTheme.typography.labelSmall, color = mutedText)
                                    }
                                    AppToggle(checked = coverPage, onCheckedChange = onCoverPageChange)
                                }
                            }
                        }

                        // Special covers
                        item(key = "c_hdr_quick", span = { GridItemSpan(maxLineSpan) }, contentType = "section_header") {
                            CollapsibleSectionHeader(
                                title = "Special",
                                collapsed = quickCollapsed,
                                fg = primaryText,
                                onClick = { onToggleSection("c:Quick") },
                            )
                        }
                        if (!quickCollapsed) {
                            item(key = "c_no_cover", contentType = "special_tile") {
                                CreatorTemplateTile(
                                    label = "No Cover",
                                    selected = cover == "thumb",
                                    accentBlue = accentBlue,
                                    mutedText = mutedText,
                                    onClick = { onSelectCover("thumb") },
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = tileW, height = tileH)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(pillBg)
                                            .border(1.dp, pillBorder, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Outlined.Description, null, tint = mutedText, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            item(key = "c_photo_cover", contentType = "special_tile") {
                                CreatorTemplateTile(
                                    label = "Photo Cover",
                                    selected = cover == "photo",
                                    accentBlue = accentBlue,
                                    mutedText = mutedText,
                                    onClick = onPickPhoto,
                                ) {
                                    if (photo != null) {
                                        AsyncImage(
                                            model = photo,
                                            contentDescription = "Photo cover",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = tileW, height = tileH)
                                                .clip(NotebookShape),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(width = tileW, height = tileH)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(pillBg)
                                                .border(1.dp, pillBorder, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Outlined.Image, null, tint = accentBlue, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Simple Matte Covers
                        item(key = "c_hdr_simple", span = { GridItemSpan(maxLineSpan) }, contentType = "section_header") {
                            CollapsibleSectionHeader(
                                title = "Simple",
                                collapsed = simpleCollapsed,
                                fg = primaryText,
                                onClick = { onToggleSection("c:Simple") },
                            )
                        }
                        if (!simpleCollapsed) {
                            items(
                                items = CreatorSolidCoverSpecs,
                                key = { it.first },
                                contentType = { "cover_tile" },
                            ) { (spec, label) ->
                                CreatorTemplateTile(
                                    label = label,
                                    selected = cover == spec,
                                    accentBlue = accentBlue,
                                    mutedText = mutedText,
                                    onClick = { onSelectCover(spec) },
                                ) {
                                    CoverMiniThumbnail(
                                        spec = spec,
                                        photo = null,
                                        landscape = effectiveLandscape,
                                        dark = dark,
                                        widthDp = tileW.value,
                                        heightDp = tileH.value,
                                        drawShadow = false,
                                    )
                                }
                            }
                        }

                        // Textured & Studio Covers
                        item(key = "c_hdr_tex", span = { GridItemSpan(maxLineSpan) }, contentType = "section_header") {
                            CollapsibleSectionHeader(
                                title = "Textured & Studio",
                                collapsed = texCollapsed,
                                fg = primaryText,
                                onClick = { onToggleSection("c:Textured") },
                            )
                        }
                        if (!texCollapsed) {
                            items(
                                items = CreatorPatternCoverSpecs,
                                key = { it.first },
                                contentType = { "cover_tile" },
                            ) { (spec, label) ->
                                CreatorTemplateTile(
                                    label = label,
                                    selected = cover == spec,
                                    accentBlue = accentBlue,
                                    mutedText = mutedText,
                                    onClick = { onSelectCover(spec) },
                                ) {
                                    CoverMiniThumbnail(
                                        spec = spec,
                                        photo = null,
                                        landscape = effectiveLandscape,
                                        dark = dark,
                                        widthDp = tileW.value,
                                        heightDp = tileH.value,
                                        drawShadow = false,
                                    )
                                }
                            }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "spacer") {
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorPreviewCard(
    heading: String,
    label: String,
    selected: Boolean,
    accentColor: Color,
    dark: Boolean,
    primaryText: Color,
    mutedText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    preview: @Composable () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else Color.Transparent,
        animationSpec = if (reduceMotion) snap() else tween(150),
        label = "inspBorder",
    )
    val cardBg by animateColorAsState(
        targetValue = if (selected) {
            if (dark) Color(0xFF1F2733) else accentColor.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = if (reduceMotion) snap() else tween(150),
        label = "inspBg",
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = primaryText,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(width = if (selected) 1.8.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = if (compact) 10.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        ) {
            preview()
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) primaryText else mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CoverMiniThumbnail(
    spec: String,
    photo: Uri?,
    landscape: Boolean,
    dark: Boolean,
    widthDp: Float,
    heightDp: Float,
    drawShadow: Boolean = false,
) {
    val w = widthDp.dp
    val h = heightDp.dp
    val mod = Modifier.size(width = w, height = h)
    when {
        photo != null && spec == "photo" -> {
            AsyncImage(
                model = photo,
                contentDescription = "Selected cover",
                contentScale = ContentScale.Crop,
                modifier = if (drawShadow) mod.shadow(4.dp, NotebookShape).clip(NotebookShape) else mod.clip(NotebookShape),
            )
        }
        spec == "thumb" -> {
            Box(
                modifier = mod
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (dark) Color(0xFF2B2E33) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, Color.Gray.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
            }
        }
        else -> {
            // Pure GPU zero-allocation vector cover preview — zero bitmaps, zero worker threads, locked 120fps scrolling!
            FastCoverThumb(
                spec = spec,
                modifier = if (drawShadow) mod.shadow(4.dp, NotebookShape).clip(NotebookShape) else mod.clip(NotebookShape),
            )
        }
    }
}

@Composable
private fun FastCoverThumb(
    spec: String,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(spec) {
        val parts = spec.split(':')
        val pattern = if (parts.firstOrNull() == "pattern" && parts.size >= 3) parts[1] else ""
        val col = Color((parts.lastOrNull()?.toLongOrNull() ?: 0xFF3D6CF4).toInt())
        pattern to col
    }
    val pattern = parsed.first
    val baseColor = parsed.second
    Spacer(
        modifier = modifier
            .background(baseColor)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val light = Color.White.copy(alpha = 0.24f)
                val softDark = Color.Black.copy(alpha = 0.14f)
                val spineBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.26f), Color.White.copy(alpha = 0.08f), Color.Transparent),
                    startX = 0f,
                    endX = w * 0.11f,
                )
                onDrawBehind {
                    if (w <= 2f || h <= 2f) return@onDrawBehind
                    when (pattern) {
                        "dots" -> {
                            val step = (w / 6f).coerceAtLeast(6f)
                            var y = step * 0.6f
                            var row = 0
                            while (y < h) {
                                var x = if (row % 2 == 0) step * 0.5f else step
                                while (x < w) {
                                    drawCircle(light, step * 0.16f, androidx.compose.ui.geometry.Offset(x, y))
                                    x += step
                                }
                                y += step; row++
                            }
                        }
                        "grid" -> {
                            val step = (w / 6f).coerceAtLeast(6f)
                            var x = step
                            while (x < w) { drawLine(light, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), 1f); x += step }
                            var y = step
                            while (y < h) { drawLine(light, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), 1f); y += step }
                        }
                        "stripes" -> {
                            val step = (w / 4.5f).coerceAtLeast(8f)
                            var x = -h
                            while (x < w + h) {
                                drawLine(light, androidx.compose.ui.geometry.Offset(x, h), androidx.compose.ui.geometry.Offset(x + h * 0.85f, 0f), step * 0.28f)
                                x += step
                            }
                        }
                        "waves" -> {
                            val step = (h / 7f).coerceAtLeast(7f)
                            var y = step * 0.6f
                            while (y < h) {
                                drawLine(light, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w * 0.5f, y - step * 0.25f), 1.6f)
                                drawLine(light, androidx.compose.ui.geometry.Offset(w * 0.5f, y - step * 0.25f), androidx.compose.ui.geometry.Offset(w, y), 1.6f)
                                y += step
                            }
                        }
                        "linen" -> {
                            var y = 3f
                            while (y < h) { drawLine(Color.White.copy(alpha = 0.08f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y), 1f); y += 4.5f }
                            var x = 3f
                            while (x < w) { drawLine(Color.Black.copy(alpha = 0.07f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), 1f); x += 4.5f }
                        }
                        "scales" -> {
                            val r = (w / 4.5f).coerceAtLeast(8f)
                            var y = r * 0.4f
                            var row = 0
                            while (y < h + r) {
                                var x = if (row % 2 == 0) 0f else r
                                while (x < w + r) {
                                    drawCircle(light, r * 0.85f, androidx.compose.ui.geometry.Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f))
                                    x += r * 1.8f
                                }
                                y += r * 0.85f; row++
                            }
                        }
                        else -> {
                            // Subtle vertical elastic closure band on classic matte covers
                            drawRect(
                                color = softDark,
                                topLeft = androidx.compose.ui.geometry.Offset(w * 0.82f, 0f),
                                size = androidx.compose.ui.geometry.Size(w * 0.055f, h),
                            )
                        }
                    }
                    // Subtle top-to-bottom paper sheen
                    drawRect(
                        color = Color.White.copy(alpha = 0.06f),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(w, h * 0.35f),
                    )
                    // Spine shadow & crease
                    drawRect(
                        brush = spineBrush,
                        topLeft = androidx.compose.ui.geometry.Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(w * 0.11f, h),
                    )
                }
            },
    )
}

@Composable
private fun StudioFilterPill(
    label: String,
    bg: Color,
    border: Color,
    fg: Color,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leadingIcon?.invoke()
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
        )
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = fg.copy(alpha = 0.72f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun OrientationSegmentIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    accent: Color,
    dark: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) {
                    if (dark) Color(0xFF3E434C) else accent.copy(alpha = 0.16f)
                } else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    collapsed: Boolean,
    fg: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (collapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = fg.copy(alpha = 0.78f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

@Composable
private fun CreatorTemplateTile(
    label: String,
    selected: Boolean,
    accentBlue: Color,
    mutedText: Color,
    onClick: () -> Unit,
    thumbnail: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(114.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) accentBlue.copy(alpha = 0.10f) else Color.Transparent)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) accentBlue else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            thumbnail()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accentBlue else mutedText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/**
 * Ultra-fast zero-allocation GPU vector thumbnail for paper templates inside the Notebook Creator.
 * Draws directly in a single Canvas pass with zero bitmap creation or background thread hops.
 */
@Composable
private fun FastPaperThumb(
    style: PaperStyle,
    tint: Int,
    landscape: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = Color(tint)
    val darkPaper = bg.luminance() < 0.35f
    val lineColor = if (darkPaper) Color.White.copy(alpha = 0.22f) else Color(0xFF94A3B8).copy(alpha = 0.55f)
    val strongLine = if (darkPaper) Color.White.copy(alpha = 0.36f) else Color(0xFF64748B).copy(alpha = 0.65f)
    val marginColor = if (darkPaper) Color(0xFFFB7185).copy(alpha = 0.55f) else Color(0xFFEF4444).copy(alpha = 0.55f)
    val accentBox = if (darkPaper) Color.White.copy(alpha = 0.14f) else Color(0xFF3B82F6).copy(alpha = 0.14f)

    Spacer(
        modifier = modifier
            .background(bg)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val pad = minOf(w, h) * 0.08f
                val stroke = (minOf(w, h) * 0.012f).coerceIn(0.6f, 1.4f)
                onDrawBehind {
                    if (w <= 2f || h <= 2f) return@onDrawBehind

        fun hLines(startY: Float, endY: Float, count: Int, x0: Float = pad, x1: Float = w - pad, col: Color = lineColor) {
            if (count <= 0) return
            val step = (endY - startY) / count
            for (i in 0..count) {
                val y = startY + i * step
                drawLine(col, androidx.compose.ui.geometry.Offset(x0, y), androidx.compose.ui.geometry.Offset(x1, y), stroke)
            }
        }

        fun grid(cols: Int, rows: Int, col: Color = lineColor) {
            val sx = (w - pad * 2f) / cols
            val sy = (h - pad * 2f) / rows
            for (c in 0..cols) {
                val x = pad + c * sx
                drawLine(col, androidx.compose.ui.geometry.Offset(x, pad), androidx.compose.ui.geometry.Offset(x, h - pad), stroke)
            }
            for (r in 0..rows) {
                val y = pad + r * sy
                drawLine(col, androidx.compose.ui.geometry.Offset(pad, y), androidx.compose.ui.geometry.Offset(w - pad, y), stroke)
            }
        }

        fun dots(cols: Int, rows: Int) {
            val sx = (w - pad * 2f) / cols
            val sy = (h - pad * 2f) / rows
            val rDot = (minOf(w, h) * 0.014f).coerceIn(0.8f, 1.8f)
            for (r in 0..rows) {
                val y = pad + r * sy
                for (c in 0..cols) {
                    drawCircle(strongLine, rDot, androidx.compose.ui.geometry.Offset(pad + c * sx, y))
                }
            }
        }

        when (style) {
            PaperStyle.BLANK -> Unit
            PaperStyle.DOTTED -> dots(if (landscape) 9 else 6, if (landscape) 6 else 9)
            PaperStyle.NARROW -> hLines(pad * 1.4f, h - pad, if (landscape) 9 else 13)
            PaperStyle.WIDE -> hLines(pad * 1.6f, h - pad, if (landscape) 6 else 8)
            PaperStyle.RULED -> hLines(pad * 1.5f, h - pad, if (landscape) 7 else 10)
            PaperStyle.COLLEGE, PaperStyle.LEGAL -> {
                hLines(pad * 1.5f, h - pad, if (landscape) 8 else 11)
                val mx = w * 0.18f
                drawLine(marginColor, androidx.compose.ui.geometry.Offset(mx, 0f), androidx.compose.ui.geometry.Offset(mx, h), stroke * 1.2f)
            }
            PaperStyle.CORNELL, PaperStyle.STUDY -> {
                val topH = h * 0.16f
                val botY = h * 0.80f
                val cueX = w * 0.30f
                drawLine(strongLine, androidx.compose.ui.geometry.Offset(pad, topH), androidx.compose.ui.geometry.Offset(w - pad, topH), stroke * 1.2f)
                drawLine(marginColor, androidx.compose.ui.geometry.Offset(cueX, topH), androidx.compose.ui.geometry.Offset(cueX, botY), stroke * 1.2f)
                drawLine(strongLine, androidx.compose.ui.geometry.Offset(pad, botY), androidx.compose.ui.geometry.Offset(w - pad, botY), stroke * 1.2f)
                hLines(topH + pad, botY - pad * 0.5f, 6, x0 = cueX + pad * 0.5f, x1 = w - pad)
            }
            PaperStyle.CORNELL_GRID -> {
                val topH = h * 0.16f
                val botY = h * 0.80f
                val cueX = w * 0.30f
                grid(if (landscape) 9 else 6, if (landscape) 6 else 9, lineColor.copy(alpha = lineColor.alpha * 0.7f))
                drawLine(marginColor, androidx.compose.ui.geometry.Offset(cueX, topH), androidx.compose.ui.geometry.Offset(cueX, botY), stroke * 1.2f)
                drawLine(strongLine, androidx.compose.ui.geometry.Offset(pad, topH), androidx.compose.ui.geometry.Offset(w - pad, topH), stroke * 1.2f)
                drawLine(strongLine, androidx.compose.ui.geometry.Offset(pad, botY), androidx.compose.ui.geometry.Offset(w - pad, botY), stroke * 1.2f)
            }
            PaperStyle.DOT_CORNELL -> {
                dots(if (landscape) 8 else 6, if (landscape) 5 else 8)
                val topH = h * 0.16f
                val cueX = w * 0.30f
                drawLine(marginColor, androidx.compose.ui.geometry.Offset(cueX, topH), androidx.compose.ui.geometry.Offset(cueX, h * 0.82f), stroke * 1.2f)
                drawLine(strongLine, androidx.compose.ui.geometry.Offset(pad, topH), androidx.compose.ui.geometry.Offset(w - pad, topH), stroke * 1.2f)
            }
            PaperStyle.TWO_COLUMN -> {
                val mid = w * 0.5f
                drawLine(strongLine, androidx.compose.ui.geometry.Offset(mid, pad), androidx.compose.ui.geometry.Offset(mid, h - pad), stroke * 1.2f)
                hLines(pad * 1.5f, h - pad, 9, pad, mid - pad * 0.6f)
                hLines(pad * 1.5f, h - pad, 9, mid + pad * 0.6f, w - pad)
            }
            PaperStyle.GRID -> grid(if (landscape) 8 else 6, if (landscape) 6 else 8)
            PaperStyle.SMALL_GRID, PaperStyle.GRAPH -> grid(if (landscape) 12 else 9, if (landscape) 9 else 12)
            PaperStyle.ENGINEERING -> {
                grid(if (landscape) 10 else 8, if (landscape) 8 else 10, lineColor.copy(alpha = lineColor.alpha * 0.75f))
                drawRect(strongLine, androidx.compose.ui.geometry.Offset(pad, pad), androidx.compose.ui.geometry.Size(w - pad * 2f, h - pad * 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 1.3f))
            }
            PaperStyle.ISOMETRIC, PaperStyle.HEX -> {
                val step = minOf(w, h) / 5.5f
                var x = -h
                while (x < w + h) {
                    drawLine(lineColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x + h * 0.58f, h), stroke)
                    drawLine(lineColor, androidx.compose.ui.geometry.Offset(x + h * 0.58f, 0f), androidx.compose.ui.geometry.Offset(x, h), stroke)
                    x += step
                }
            }
            PaperStyle.HANDWRITING, PaperStyle.SEYES, PaperStyle.CALLIGRAPHY -> {
                val rows = if (landscape) 4 else 6
                val band = (h - pad * 2f) / rows
                for (i in 0 until rows) {
                    val y0 = pad + i * band
                    drawLine(strongLine, androidx.compose.ui.geometry.Offset(pad, y0 + band * 0.78f), androidx.compose.ui.geometry.Offset(w - pad, y0 + band * 0.78f), stroke)
                    drawLine(lineColor, androidx.compose.ui.geometry.Offset(pad, y0 + band * 0.36f), androidx.compose.ui.geometry.Offset(w - pad, y0 + band * 0.36f), stroke * 0.8f)
                }
            }
            PaperStyle.PLANNER, PaperStyle.MONTHLY, PaperStyle.HABIT_TRACKER -> {
                drawRect(accentBox, androidx.compose.ui.geometry.Offset(pad, pad), androidx.compose.ui.geometry.Size(w - pad * 2f, h * 0.14f))
                val top = pad + h * 0.18f
                val rows = 4
                val cols = if (style == PaperStyle.MONTHLY) 5 else 3
                val cellW = (w - pad * 2f) / cols
                val cellH = (h - top - pad) / rows
                for (r in 0..rows) drawLine(lineColor, androidx.compose.ui.geometry.Offset(pad, top + r * cellH), androidx.compose.ui.geometry.Offset(w - pad, top + r * cellH), stroke)
                for (c in 0..cols) drawLine(lineColor, androidx.compose.ui.geometry.Offset(pad + c * cellW, top), androidx.compose.ui.geometry.Offset(pad + c * cellW, h - pad), stroke)
            }
            PaperStyle.DAILY, PaperStyle.DAY_TIMEBLOCK, PaperStyle.MEETING -> {
                drawRect(accentBox, androidx.compose.ui.geometry.Offset(pad, pad), androidx.compose.ui.geometry.Size(w - pad * 2f, h * 0.14f))
                val splitX = w * 0.42f
                drawLine(strongLine, androidx.compose.ui.geometry.Offset(splitX, pad + h * 0.18f), androidx.compose.ui.geometry.Offset(splitX, h - pad), stroke)
                hLines(pad + h * 0.22f, h - pad, 7, pad, w - pad)
            }
            PaperStyle.TODO, PaperStyle.CHECKLIST -> {
                val rows = if (landscape) 6 else 8
                val step = (h - pad * 2.4f) / rows
                val boxS = (step * 0.48f).coerceIn(2.5f, 6f)
                for (i in 0 until rows) {
                    val y = pad * 1.4f + i * step
                    drawRect(strongLine, androidx.compose.ui.geometry.Offset(pad, y), androidx.compose.ui.geometry.Size(boxS, boxS), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                    drawLine(lineColor, androidx.compose.ui.geometry.Offset(pad + boxS + pad * 0.5f, y + boxS * 0.65f), androidx.compose.ui.geometry.Offset(w - pad, y + boxS * 0.65f), stroke)
                }
            }
            PaperStyle.STORYBOARD, PaperStyle.COMIC_PANELS -> {
                val cols = 2
                val rows = 3
                val gap = pad * 0.7f
                val cw = (w - pad * 2f - gap) / cols
                val ch = (h - pad * 2f - gap * (rows - 1)) / rows
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        drawRect(
                            strongLine,
                            androidx.compose.ui.geometry.Offset(pad + c * (cw + gap), pad + r * (ch + gap)),
                            androidx.compose.ui.geometry.Size(cw, ch),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 1.1f),
                        )
                    }
                }
            }
            PaperStyle.MUSIC, PaperStyle.TABLATURE -> {
                val staves = if (landscape) 3 else 4
                val band = (h - pad * 2f) / staves
                val linesPerStaff = if (style == PaperStyle.TABLATURE) 6 else 5
                for (s in 0 until staves) {
                    val top = pad + s * band + band * 0.16f
                    val gap = (band * 0.56f) / (linesPerStaff - 1)
                    for (l in 0 until linesPerStaff) {
                        val y = top + l * gap
                        drawLine(strongLine, androidx.compose.ui.geometry.Offset(pad, y), androidx.compose.ui.geometry.Offset(w - pad, y), stroke * 0.85f)
                    }
                }
            }
            else -> hLines(pad * 1.5f, h - pad, 9)
        }
                }
            },
    )
}


