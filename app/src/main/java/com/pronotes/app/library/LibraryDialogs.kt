package com.pronotes.app.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CropLandscape
import androidx.compose.material.icons.outlined.CropPortrait
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pronotes.app.data.CoverColors
import com.pronotes.app.data.CoverPatterns
import com.pronotes.app.data.Folder
import com.pronotes.app.data.FolderColors
import com.pronotes.app.data.NotebookMeta
import com.pronotes.app.data.PagePreset
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageTagsDialog(
    note: NotebookMeta,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var tags by remember(note.id) { mutableStateOf(note.tags) }
    var customTag by remember { mutableStateOf("") }
    val presets = remember(availableTags) {
        (availableTags + listOf("Important", "Study", "Lecture", "Exam", "Work", "Math", "Review", "Project", "Personal", "Summary"))
            .distinctBy { it.lowercase() }
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tags · ${note.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Organise and filter your notebooks with colour-coded tags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it.take(24) },
                        singleLine = true,
                        label = { Text("Add custom tag") },
                        placeholder = { Text("e.g. Semester1, Physics") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        enabled = customTag.trim().removePrefix("#").isNotBlank(),
                        onClick = {
                            val clean = customTag.trim().removePrefix("#").trim()
                            if (clean.isNotBlank() && tags.none { it.equals(clean, ignoreCase = true) }) {
                                tags = tags + clean
                            }
                            customTag = ""
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                    ) {
                        Text("Add")
                    }
                }
                Text("TAP TO TOGGLE TAGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (tags + presets).distinctBy { it.lowercase() }.forEach { t ->
                        val active = tags.any { it.equals(t, ignoreCase = true) }
                        AppChip(
                            selected = active,
                            onClick = {
                                tags = if (active) {
                                    tags.filterNot { it.equals(t, ignoreCase = true) }
                                } else {
                                    tags + t
                                }
                            },
                            label = "#$t",
                            tint = tagTintFor(t),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(tags) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
            ) {
                Text("Save Tags")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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
)

private val PaperColors = listOf(0xFFFFFFFF, 0xFFFBF7EC, 0xFFF1F2F4, 0xFFFFF4E0, 0xFFEAF4FF, 0xFFEFF8EC, 0xFF22252B).map { it.toInt() }
private val PaperColorNames = listOf("White", "Cream", "Cool Grey", "Ivory", "Sky", "Mint", "Dark")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewNotebookDialog(
    defaults: LibraryNewBookPrefs,
    onDismiss: () -> Unit,
    onCreate: (NewBook) -> Unit,
) {
    var name by remember { mutableStateOf("Untitled notebook") }
    val startColor = remember { CoverColors.random() }
    var cover by remember { mutableStateOf(if (defaults.thumbCovers) "thumb" else "solid:$startColor") }
    var coverPage by remember { mutableStateOf(defaults.defaultCoverPage) }
    var paper by remember { mutableStateOf(defaults.defaultPaper) }
    var paperColor by remember { mutableStateOf<Int?>(null) }
    var customPaper by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf(defaults.defaultPreset) }
    var landscape by remember { mutableStateOf(defaults.defaultLandscape) }
    var wMm by remember { mutableStateOf("%.0f".format(defaults.defaultPreset.wMm)) }
    var hMm by remember { mutableStateOf("%.0f".format(defaults.defaultPreset.hMm)) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var tab by remember { mutableStateOf(0) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) { photo = uri; cover = "photo" } }
    val effectiveLandscape = if (preset.isWhiteboard) true else landscape

    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create notebook", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ProButtonPalette.ElectricBlue.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (preset.isWhiteboard) "Whiteboard" else preset.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = ProButtonPalette.ElectricBlue,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Studio Live Preview Stage: realistic cover + peeking paper sheet + inline title & spec pills
                AppCard(
                    hero = false,
                    shape = appCardShape(3f),
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Layered 3D-feel book mockup: peeking paper sheet behind bound front cover
                        Box(
                            modifier = Modifier.size(
                                width = if (effectiveLandscape) 114.dp else 94.dp,
                                height = if (effectiveLandscape) 86.dp else 112.dp,
                            ),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            // Peeking styled paper page offset to the right
                            PaperPreview(
                                style = paper,
                                modifier = Modifier
                                    .offset(x = 12.dp, y = 2.dp)
                                    .size(
                                        width = if (effectiveLandscape) 94.dp else 74.dp,
                                        height = if (effectiveLandscape) 72.dp else 98.dp,
                                    )
                                    .shadow(3.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.75.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                tint = paperColor ?: 0xFFFFFFFF.toInt(),
                                landscape = effectiveLandscape,
                            )
                            // Front bound cover with spine shadow
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = if (effectiveLandscape) 94.dp else 74.dp,
                                        height = if (effectiveLandscape) 74.dp else 102.dp,
                                    )
                                    .shadow(8.dp, NotebookShape)
                                    .clip(NotebookShape),
                            ) {
                                when (cover) {
                                    "photo" -> Box(
                                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                            Text("Photo", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    "thumb" -> PaperPreview(
                                        style = paper,
                                        modifier = Modifier.fillMaxSize(),
                                        tint = paperColor ?: 0xFFFFFFFF.toInt(),
                                        landscape = effectiveLandscape,
                                    )
                                    else -> CoverArt(cover, name, null, 0, Modifier.fillMaxSize())
                                }
                                // Subtle bookbinding spine crease on left edge
                                Box(
                                    Modifier
                                        .align(Alignment.CenterStart)
                                        .width(8.dp)
                                        .height(110.dp)
                                        .background(Color.Black.copy(alpha = 0.14f)),
                                )
                            }
                        }

                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                label = { Text("Notebook title") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                            ) {
                                SpecBadge(paper.label)
                                SpecBadge(if (preset.isWhiteboard) "Infinite" else if (effectiveLandscape) "Landscape" else "Portrait")
                                if (coverPage && cover != "thumb" && !preset.isWhiteboard) SpecBadge("Cover page")
                            }
                        }
                    }
                }

                // Studio navigation segmented control
                AppSegmentedControl(
                    options = listOf("Cover", "Paper", "Layout"),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                Box(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                    when (tab) {
                        0 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CoverGallery(cover, { cover = it; photo = null }) {
                                pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            if (cover != "thumb" && !preset.isWhiteboard) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Use cover as page 1", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("Write titles or stickers directly onto the front cover", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    AppToggle(checked = coverPage, onCheckedChange = { coverPage = it })
                                }
                            }
                        }

                        1 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Paper tint selector bar at top so every template card previews live in the chosen tone
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "PAPER TONE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                ) {
                                    PaperToneChip("Auto", null, paperColor == null) { paperColor = null }
                                    PaperColors.forEachIndexed { idx, c ->
                                        PaperToneChip(
                                            label = PaperColorNames.getOrElse(idx) { "Tone" },
                                            colorInt = c,
                                            selected = c == paperColor,
                                        ) { paperColor = c }
                                    }
                                    Box(
                                        Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                            .clickable { customPaper = true },
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(Icons.Filled.Add, "Custom tone", Modifier.size(16.dp)) }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "TEMPLATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PaperStyle.pickerEntries.forEach { p ->
                                        val sel = p == paper
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (sel) ProButtonPalette.ElectricBlue.copy(alpha = 0.12f) else Color.Transparent)
                                                .clickable { paper = p }
                                                .padding(5.dp),
                                        ) {
                                            Box {
                                                PaperPreview(
                                                    p,
                                                    Modifier
                                                        .size(
                                                            width = if (effectiveLandscape) 86.dp else 66.dp,
                                                            height = if (effectiveLandscape) 64.dp else 88.dp,
                                                        )
                                                        .shadow(if (sel) 4.dp else 1.dp, RoundedCornerShape(8.dp))
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(
                                                            if (sel) 2.2.dp else 0.75.dp,
                                                            if (sel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant,
                                                            RoundedCornerShape(8.dp),
                                                        ),
                                                    tint = paperColor ?: 0xFFFFFFFF.toInt(),
                                                    landscape = effectiveLandscape,
                                                )
                                                if (sel) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .background(ProButtonPalette.ElectricBlue),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                p.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (sel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                "PAGE FORMAT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PagePreset.entries.forEach { p ->
                                    val sel = preset == p
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (sel) ProButtonPalette.ElectricBlue.copy(alpha = 0.13f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                            )
                                            .border(
                                                width = if (sel) 1.6.dp else 1.dp,
                                                color = if (sel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(14.dp),
                                            )
                                            .clickable {
                                                preset = p
                                                if (p != PagePreset.CUSTOM) {
                                                    wMm = "%.0f".format(p.wMm)
                                                    hMm = "%.0f".format(p.hMm)
                                                }
                                                if (p.isWhiteboard && paper == PaperStyle.BLANK) paper = PaperStyle.DOTTED
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(if (sel) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.surface),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (p.isWhiteboard) {
                                                Icon(
                                                    Icons.Outlined.AllInclusive,
                                                    null,
                                                    tint = if (sel) Color.White else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            } else {
                                                val ratio = (p.wMm / p.hMm.coerceAtLeast(1f)).coerceIn(0.6f, 1.4f)
                                                Box(
                                                    Modifier
                                                        .size(width = (16f * ratio).dp, height = 18.dp)
                                                        .border(1.5.dp, if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(3.dp)),
                                                )
                                            }
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(p.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                if (p.recommended) {
                                                    Box(
                                                        Modifier
                                                            .clip(CircleShape)
                                                            .background(ProButtonPalette.Emerald.copy(alpha = 0.16f))
                                                            .padding(horizontal = 7.dp, vertical = 1.dp),
                                                    ) {
                                                        Text("Recommended", style = MaterialTheme.typography.labelSmall, color = ProButtonPalette.Emerald, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                            val subtitle = when {
                                                p.isWhiteboard -> "Unbounded infinite pan & zoom canvas"
                                                p == PagePreset.CUSTOM -> "Set exact width and height in mm"
                                                else -> "%.0f × %.0f mm".format(p.wMm, p.hMm)
                                            }
                                            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (sel) {
                                            Icon(Icons.Filled.Check, null, tint = ProButtonPalette.ElectricBlue, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            if (!preset.isWhiteboard) {
                                Text(
                                    "ORIENTATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    OrientationCard(
                                        label = "Portrait",
                                        icon = Icons.Outlined.CropPortrait,
                                        selected = !landscape,
                                        onClick = { landscape = false },
                                        modifier = Modifier.weight(1f),
                                    )
                                    OrientationCard(
                                        label = "Landscape",
                                        icon = Icons.Outlined.CropLandscape,
                                        selected = landscape,
                                        onClick = { landscape = true },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }

                            if (preset == PagePreset.CUSTOM) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(wMm, { v -> wMm = v.filter { it.isDigit() }.take(4) }, label = { Text("Width mm") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                                    OutlinedTextField(hMm, { v -> hMm = v.filter { it.isDigit() }.take(4) }, label = { Text("Height mm") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val color = cover.substringAfterLast(':').toLongOrNull()?.toInt() ?: startColor
                    var w = (wMm.toFloatOrNull() ?: 210f).coerceIn(40f, 1000f)
                    var h = (hMm.toFloatOrNull() ?: 297f).coerceIn(40f, 1000f)
                    if (!preset.isWhiteboard && landscape != (w > h)) {
                        val t = w; w = h; h = t
                    }
                    val k = PagePreset.UNITS_PER_MM
                    onCreate(
                        NewBook(
                            name.ifBlank { if (preset.isWhiteboard) "Whiteboard" else "Untitled notebook" },
                            if (cover == "photo") "" else cover,
                            color,
                            paper,
                            photo,
                            w * k,
                            h * k,
                            paperColor,
                            coverPage && cover != "thumb" && !preset.isWhiteboard,
                            isWhiteboard = preset.isWhiteboard,
                        ),
                    )
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
            ) { Text("Create notebook") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (customPaper) ColorEditorDialog(paperColor ?: 0xFFFFFFFF.toInt(), PaperColors, emptyList(), onDismiss = { customPaper = false }) { paperColor = it; customPaper = false }
}

@Composable
private fun SpecBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun PaperToneChip(label: String, colorInt: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) ProButtonPalette.ElectricBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (colorInt != null) Color(colorInt) else MaterialTheme.colorScheme.surface)
                .border(0.8.dp, Color.Black.copy(alpha = 0.22f), CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun OrientationCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ProButtonPalette.ElectricBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurface,
        )
    }
}
