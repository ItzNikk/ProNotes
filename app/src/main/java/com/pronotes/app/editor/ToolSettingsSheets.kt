package com.pronotes.app.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.R
import com.pronotes.app.data.ColorPalettePreset
import com.pronotes.app.data.EraserFilter
import com.pronotes.app.data.EraserMode
import com.pronotes.app.data.HighlighterMode
import com.pronotes.app.data.LassoFilter
import com.pronotes.app.data.LassoMode
import com.pronotes.app.data.PenPreset
import com.pronotes.app.data.ScribbleSensitivity
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.SideButton
import com.pronotes.app.data.ToolDefaults
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppContextMenu
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.AppPill
import com.pronotes.app.ui.AppSegmentedControl
import com.pronotes.app.ui.AppSliderRow
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.ColorEditorDialog
import com.pronotes.app.ui.LocalReduceEffects
import com.pronotes.app.ui.PenGlyph
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProIcon
import com.pronotes.app.ui.ProIconView
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.ShapeGlyph
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.Swatch
import kotlin.math.roundToInt

private val PopoverDarkBg = Color(0xFF1C1C1C)
private val PopoverLightBg = Color(0xFFFAF8F3)

/**
 * Anchored settings sheet for every tool preset in the toolbar (size, opacity, colour set,
 * and tool-specific behaviour).
 */
@Composable
fun ToolPopoverCard(
    tool: Tool,
    vm: EditorViewModel,
    darkUi: Boolean,
    caretSide: PopoverCaretSide = PopoverCaretSide.TOP,
    onBrowseStickers: () -> Unit,
    onPickImage: () -> Unit,
    onSelectionConverted: (Long) -> Unit = {},
    visible: Boolean = true,
    onDismiss: () -> Unit,
) {
    var entered by remember(tool) { mutableStateOf(false) }
    LaunchedEffect(tool) { entered = true }
    val progress by animateFloatAsState(
        targetValue = if (entered && visible) 1f else 0f,
        animationSpec = if (com.pronotes.app.ui.LocalReduceMotion.current) snap() else spring(dampingRatio = 0.80f, stiffness = 540f),
        label = "Tool menu",
    )
    val pickTape = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.useTapeImage(uri)
    }
    val cardBg = if (darkUi) PopoverDarkBg else PopoverLightBg
    val cardFg = if (darkUi) Color.White else Color(0xFF181A1F)
    val borderTint = if (darkUi) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)
    val reduceEffects = LocalReduceEffects.current
    val slideShiftPx = with(androidx.compose.ui.platform.LocalDensity.current) { 10.dp.toPx() }
    val origin = if (caretSide == PopoverCaretSide.BOTTOM) TransformOrigin(0.5f, 1f) else TransformOrigin(0.5f, 0f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 260.dp, max = 360.dp)
            .graphicsLayer {
                val p = progress.coerceIn(0f, 1.05f)
                alpha = progress.coerceIn(0f, 1f)
                transformOrigin = origin
                scaleX = 0.88f + p * 0.12f
                scaleY = 0.88f + p * 0.12f
                translationY = (1f - progress) * (if (caretSide == PopoverCaretSide.BOTTOM) slideShiftPx else -slideShiftPx)
            }
            .onPreviewKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
                    onDismiss()
                    true
                } else false
            },
    ) {
        if (caretSide == PopoverCaretSide.TOP) {
            Canvas(Modifier.size(width = 22.dp, height = 9.dp)) {
                val tri = Path().apply {
                    moveTo(0f, size.height + 1f)
                    lineTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height + 1f)
                    close()
                }
                drawPath(tri, cardBg)
                val outline = Path().apply {
                    moveTo(0.5f, size.height)
                    lineTo(size.width / 2f, 0.5f)
                    lineTo(size.width - 0.5f, size.height)
                }
                drawPath(outline, borderTint, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        AppSurface(
            role = SurfaceRole.Popover,
            containerColor = cardBg,
            contentColor = cardFg,
            shape = RoundedCornerShape(16.dp),
            borderColor = borderTint,
            shadowElevation = if (reduceEffects) 0.dp else 16.dp,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CompositionLocalProvider(LocalContentColor provides cardFg) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ProButtonPalette.ElectricBlue.copy(alpha = if (darkUi) 0.22f else 0.12f))
                                .border(1.dp, ProButtonPalette.ElectricBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            ProIconView(
                                com.pronotes.app.ui.iconFor(tool),
                                ProButtonPalette.ElectricBlue,
                                Modifier.size(18.dp),
                                ink = Color(vm.styleFor(tool)?.color ?: vm.penStyle.color),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        ScaleInAnimatedText(
                            text = if (tool == Tool.PEN) vm.penType.label else toolDisplayName(tool),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = cardFg,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                .clip(CircleShape)
                                .background(cardFg.copy(alpha = 0.06f))
                                .focusable()
                                .clickable(role = Role.Button, onClickLabel = "Close settings sheet", onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = cardFg.copy(alpha = 0.78f), modifier = Modifier.size(18.dp))
                        }
                    }

                    when (tool) {
                        Tool.PEN -> PenCardContent(vm, darkUi, cardFg)
                        Tool.CRAYON -> CrayonCardContent(vm, darkUi, cardFg)
                        Tool.HIGHLIGHTER -> HighlighterCardContent(vm, darkUi, cardFg)
                        Tool.SHAPE -> ShapeCardContent(vm)
                        Tool.ERASER -> EraserCardContent(vm)
                        Tool.LASSO -> LassoCardContent(vm, onSelectionConverted)
                        Tool.LASER -> LaserCardContent(vm)
                        Tool.TEXT -> TextCardContent(vm)
                        Tool.AUTO_TEXT -> AutoTextCardContent(vm, cardFg)
                        Tool.STICKER -> StickerCardContent(vm, onBrowseStickers = {
                            onDismiss()
                            onBrowseStickers()
                        })
                        Tool.IMAGE -> ImageCardContent(vm, onPickImage = {
                            onDismiss()
                            onPickImage()
                        })
                        Tool.TAPE -> {
                            SheetSegments(listOf("Straight", "Freehand"), if (vm.tapeFreehand) 1 else 0) { vm.tapeFreehand = it == 1 }
                            Row(verticalAlignment = Alignment.CenterVertically) { CustomSwatches(TapeColors, vm.tapeColor, vm) { vm.tapeColor = it } }
                            TapePatternPicker(vm) {
                                pickTape.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                        Tool.STICKY -> {
                            SheetSegments(listOf("Type Text", "Handwrite"), vm.stickyInputMode) { vm.updateStickyInputMode(it) }
                            Text(
                                if (vm.stickyInputMode == 0) "Tap the page to place a sticky note and type right away."
                                else "Tap the page to place a sticky note and handwrite directly on it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cardFg.copy(alpha = 0.68f),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CustomSwatches(StickyColors, vm.stickyColor, vm) { vm.stickyColor = it }
                            }
                        }
                        Tool.WEBLINK -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CustomSwatches(WebLinkColors, vm.webLinkColor, vm) { vm.webLinkColor = it }
                        }
                        Tool.MINDMAP -> MindMapCardContent(vm)
                    }
                }
            }
        }
        if (caretSide == PopoverCaretSide.BOTTOM) {
            Canvas(Modifier.size(width = 22.dp, height = 9.dp)) {
                val tri = Path().apply {
                    moveTo(0f, -1f)
                    lineTo(size.width / 2f, size.height)
                    lineTo(size.width, -1f)
                    close()
                }
                drawPath(tri, cardBg)
                val outline = Path().apply {
                    moveTo(0.5f, 0f)
                    lineTo(size.width / 2f, size.height - 0.5f)
                    lineTo(size.width - 0.5f, 0f)
                }
                drawPath(outline, borderTint, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
private fun SheetSegments(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    AppSegmentedControl(
        options = options,
        selectedIndex = selected.coerceIn(0, (options.size - 1).coerceAtLeast(0)),
        onSelect = onPick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StrokePreview(color: Color, width: Float, taper: Boolean, darkUi: Boolean) {
    val boxBg = if (darkUi) Color(0xFF242424) else Color(0xFFECE9E0)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(boxBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.08f, size.height * 0.62f)
                cubicTo(size.width * 0.3f, size.height * 1.05f, size.width * 0.45f, size.height * 0.05f, size.width * 0.92f, size.height * 0.45f)
            }
            if (taper) {
                for (k in 0..3) {
                    drawPath(path, color, style = Stroke(width * (0.4f + k * 0.2f), cap = StrokeCap.Round), alpha = (0.35f * color.alpha).coerceIn(0.05f, 0.35f))
                }
            }
            drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}

@Immutable
private data class PenCardSettings(
    val penName: String,
    val paperSound: Boolean,
    val paperVolume: Float,
    val paperHaptics: Boolean,
    val touchSensitivity: Float,
    val stylusOnly: Boolean,
    val palmRejection: Boolean,
    val hoverCursor: Boolean,
    val sideButton: SideButton,
    val toolDefaults: ToolDefaults,
)

@Immutable
private data class HighlighterCardSettings(
    val highlighterOpacity: Float,
    val highlighterMode: HighlighterMode,
    val highlighterBehind: Boolean,
)

@Immutable
private data class EraserCardSettings(
    val eraserMode: EraserMode,
    val scribbleSensitivity: ScribbleSensitivity,
    val eraseHighlighterOnly: Boolean,
    val eraserFilter: EraserFilter,
    val eraserAutoSwitch: Boolean,
    val scribbleToErase: Boolean,
)

@Immutable
private data class LassoCardSettings(
    val lassoMode: LassoMode,
    val lassoFilter: LassoFilter,
    val lassoTouching: Boolean,
    val circleToLasso: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PenCardContent(vm: EditorViewModel, darkUi: Boolean, fgColor: Color) {
    val style = vm.penStyle
    val presets by vm.presets.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val store = vm.notebookSettings
    val s by remember(store) {
        store.select {
            PenCardSettings(
                it.penName,
                it.paperSound,
                it.paperVolume,
                it.paperHaptics,
                it.touchSensitivity,
                it.stylusOnly,
                it.palmRejection,
                it.hoverCursor,
                it.sideButton,
                it.toolDefaults,
            )
        }
    }.collectAsStateWithLifecycle(
        initialValue = remember(store) {
            store.settings.value.let {
                PenCardSettings(
                    it.penName,
                    it.paperSound,
                    it.paperVolume,
                    it.paperHaptics,
                    it.touchSensitivity,
                    it.stylusOnly,
                    it.palmRejection,
                    it.hoverCursor,
                    it.sideButton,
                    it.toolDefaults,
                )
            }
        },
    )
    var editingPenName by remember { mutableStateOf(false) }
    var savingPreset by remember { mutableStateOf(false) }
    var renamingPreset by remember { mutableStateOf<PenPreset?>(null) }

    var section by remember { mutableStateOf(0) }
    SheetSegments(listOf("Brush", "Writing", "Presets", "Stylus"), section) { section = it }
    val tileBg = if (darkUi) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.primaryContainer
    val tileIdleBg = if (darkUi) Color(0xFF232326) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val mutedFg = fgColor.copy(alpha = 0.68f)

    if (section == 3) {
        AppCard(
            onClick = { editingPenName = true },
            containerColor = tileIdleBg,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ProButtonPalette.ElectricBlue.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit stylus name", Modifier.size(16.dp), tint = ProButtonPalette.ElectricBlue)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("ACTIVE STYLUS", style = MaterialTheme.typography.labelSmall, color = mutedFg, letterSpacing = 0.6.sp)
                    Text(s.penName.ifBlank { "Pro Stylus" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = fgColor)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ProButtonPalette.ElectricBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text("Rename", style = MaterialTheme.typography.labelSmall, color = ProButtonPalette.ElectricBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Quick Side-Button Action selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(tileIdleBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("HOLD SIDE BUTTON", style = MaterialTheme.typography.labelSmall, color = mutedFg, letterSpacing = 0.7.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SideButton.entries.forEach { btn ->
                    AppChip(
                        selected = s.sideButton == btn,
                        onClick = { store.update { it.copy(sideButton = btn) } },
                        label = btn.label,
                        tint = ProButtonPalette.ElectricBlue,
                    )
                }
            }
        }

        // Input & Palm Rejection card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(tileIdleBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("INPUT & PALM PROTECTION", style = MaterialTheme.typography.labelSmall, color = mutedFg, letterSpacing = 0.7.sp)
            SheetToggleLine("Stylus-only writing", s.stylusOnly, fgColor) { enabled ->
                store.update { it.copy(stylusOnly = enabled) }
            }
            SheetToggleLine("Palm rejection", s.palmRejection, fgColor) { enabled ->
                store.update { it.copy(palmRejection = enabled) }
            }
            SheetToggleLine("Hover cursor preview", s.hoverCursor, fgColor) { enabled ->
                store.update { it.copy(hoverCursor = enabled) }
            }
        }
    }

    if (section == 1) {
        SheetToggleLine("Automatic handwriting", s.toolDefaults.autoHandwriting, fgColor, onChange = vm::setAutoHandwriting)
        SheetToggleLine("Align to nearby paper rules", s.toolDefaults.alignHandwriting, fgColor) { enabled ->
            store.update { it.copy(toolDefaults = it.toolDefaults.copy(alignHandwriting = enabled)) }
        }
        SheetToggleLine("Show writing guide line", s.toolDefaults.alignGuideLine, fgColor) { enabled ->
            store.update { it.copy(toolDefaults = it.toolDefaults.copy(alignGuideLine = enabled)) }
        }
        SheetToggleLine("Dynamic bold (large strokes → bold)", s.toolDefaults.dynamicBoldHandwriting, fgColor) { enabled ->
            store.update { it.copy(toolDefaults = it.toolDefaults.copy(dynamicBoldHandwriting = enabled)) }
        }
        SheetToggleLine("Auto spell check", s.toolDefaults.autoSpellCheck, fgColor) { enabled ->
            store.update { it.copy(toolDefaults = it.toolDefaults.copy(autoSpellCheck = enabled)) }
        }
        Text("Write with the pen and pause to convert handwriting to editable text.", style = MaterialTheme.typography.bodySmall, color = mutedFg)
        Text("Recognition language", style = MaterialTheme.typography.titleSmall)
        var showPenLangPicker by remember { mutableStateOf(false) }
        val activePenLang = remember(s.toolDefaults.handwritingLanguage) {
            HandwritingRecognizer.findLanguage(s.toolDefaults.handwritingLanguage)
        }
        val quickPenLangs = remember(activePenLang.tag) {
            val base = HandwritingRecognizer.quickLanguages
            if (base.any { it.tag.equals(activePenLang.tag, ignoreCase = true) }) base else listOf(activePenLang) + base.take(9)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            quickPenLangs.forEach { lang ->
                AppChip(activePenLang.tag.equals(lang.tag, ignoreCase = true), {
                    store.update { it.copy(toolDefaults = it.toolDefaults.copy(handwritingLanguage = lang.tag)) }
                }, label = lang.nativeName)
            }
            AppChip(false, { showPenLangPicker = true }, label = "All ${HandwritingRecognizer.supportedLanguages.size}+ languages…")
        }
        if (showPenLangPicker) {
            com.pronotes.app.ui.HandwritingLanguagePickerDialog(
                selectedTag = s.toolDefaults.handwritingLanguage,
                onDismiss = { showPenLangPicker = false },
                onSelect = { lang ->
                    store.update { it.copy(toolDefaults = it.toolDefaults.copy(handwritingLanguage = lang.tag)) }
                    showPenLangPicker = false
                },
            )
        }
        val model by HandwritingRecognizer.modelState.collectAsStateWithLifecycle()
        if (model == HandwritingModelState.DOWNLOADING) Text("Downloading handwriting model…", style = MaterialTheme.typography.bodySmall)
        Text("Writing font", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextFont.entries.forEach { font ->
                AppChip(s.toolDefaults.customHandwritingFontPath.isBlank() && s.toolDefaults.autoScript == font.name,
                    { store.update { it.copy(toolDefaults = it.toolDefaults.copy(autoScript = font.name, customHandwritingFontPath = "")) } }, label = font.label)
            }
        }
        CustomHandwritingFontPicker(
            selectedPath = s.toolDefaults.customHandwritingFontPath,
            fgColor = fgColor,
            onSelect = { path ->
                store.update { it.copy(toolDefaults = it.toolDefaults.copy(customHandwritingFontPath = path)) }
            },
        )
    }
    if (section == 3) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(tileIdleBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("FEEL & RESPONSE", style = MaterialTheme.typography.labelSmall, color = mutedFg, letterSpacing = 0.7.sp)
            SheetToggleLine("Stylus pressure", s.toolDefaults.pressureEnabled, fgColor) { enabled ->
                store.update { it.copy(toolDefaults = it.toolDefaults.copy(pressureEnabled = enabled)) }
            }
            if (s.toolDefaults.pressureEnabled) {
                SheetSliderRow(
                    "PRESSURE RESPONSE",
                    "${(s.toolDefaults.pressureSensitivity * 100).roundToInt()}%",
                    s.toolDefaults.pressureSensitivity,
                    0.3f..2.0f,
                ) { v ->
                    store.update { it.copy(toolDefaults = it.toolDefaults.copy(pressureSensitivity = v)) }
                }
            }
            SheetSliderRow(
                "TOUCH SENSITIVITY",
                "${(s.touchSensitivity * 100).roundToInt()}%",
                s.touchSensitivity,
                0.4f..2.0f,
            ) { v ->
                store.update { it.copy(touchSensitivity = v) }
            }
            SheetToggleLine("Tilt shading", s.toolDefaults.penTiltShading, fgColor) { enabled ->
                store.update { it.copy(toolDefaults = it.toolDefaults.copy(penTiltShading = enabled)) }
            }
            SheetToggleLine("Tap objects with pen to select", s.toolDefaults.tapSelect, fgColor) { enabled ->
                store.update { it.copy(toolDefaults = it.toolDefaults.copy(tapSelect = enabled)) }
            }
        }
    }
    if (section == 0) {
    StrokePreview(
        Color(style.color).copy(alpha = s.toolDefaults.penOpacity),
        style.size * vm.penType.sizeFactor * 2.2f,
        vm.penType != PenType.BALL,
        darkUi,
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PenType.entries.forEach { type ->
            val sel = vm.penType == type
            Column(
                Modifier
                    .width(76.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (sel) tileBg else tileIdleBg)
                    .border(if (sel) 1.5.dp else 0.dp, if (sel) ProButtonPalette.ElectricBlue else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable(role = Role.RadioButton, onClickLabel = type.label) { vm.penType = type }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PenGlyph(type, fgColor, Color(style.color), Modifier.size(28.dp))
                Spacer(Modifier.height(4.dp))
                Text(type.label, style = MaterialTheme.typography.labelSmall, color = fgColor, maxLines = 1)
            }
        }
    }

    Text(stringResource(R.string.sheet_colour_set), style = MaterialTheme.typography.labelSmall, color = mutedFg)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StyleOptions(vm, Tool.PEN, style, fgColor, darkUi, vertical = false)
    }
    ColorPalettePresetsStrip(vm, Tool.PEN, style, fgColor, darkUi)

    HorizontalDivider(color = fgColor.copy(alpha = 0.08f))
    SheetSliderRow(stringResource(R.string.sheet_thickness), "%.1f pt".format(style.size), style.size, 0.5f..20f) { v ->
        val sizes = style.sizes.map { if (it == style.size) v else it }
        vm.updateStyle(Tool.PEN, style.copy(size = v, sizes = sizes))
    }
    SheetSliderRow(stringResource(R.string.sheet_opacity), "%.0f%%".format(s.toolDefaults.penOpacity * 100), s.toolDefaults.penOpacity, 0.15f..1f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(penOpacity = v)) }
    }
    Text("STROKE STYLE", style = MaterialTheme.typography.labelSmall, color = mutedFg)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("solid" to "Solid", "dashed" to "Dashed", "dotted" to "Dotted").forEach { (key, label) ->
            AppChip(
                selected = s.toolDefaults.penStrokeStyle == key,
                onClick = { store.update { it.copy(toolDefaults = it.toolDefaults.copy(penStrokeStyle = key)) } },
                label = label,
                tint = ProButtonPalette.ElectricBlue,
            )
        }
    }
    SheetSliderRow("NIB ROUNDNESS", "${(s.toolDefaults.penNibRoundness * 100).roundToInt()}%", s.toolDefaults.penNibRoundness, 0.1f..1f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(penNibRoundness = v)) }
    }
    SheetSliderRow("NIB ANGLE", "${s.toolDefaults.penNibAngleDeg.roundToInt()}°", s.toolDefaults.penNibAngleDeg, 0f..180f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(penNibAngleDeg = v)) }
    }
    SheetSliderRow("STABILIZATION", "${(s.toolDefaults.penStabilization * 100).roundToInt()}%", s.toolDefaults.penStabilization, 0f..1f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(penStabilization = v)) }
    }
    SheetSliderRow("TIP TAPER", "${(s.toolDefaults.penTaper * 100).roundToInt()}%", s.toolDefaults.penTaper, 0f..1f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(penTaper = v)) }
    }

    }
    if (section == 2) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("FAVOURITE PENS", style = MaterialTheme.typography.labelSmall, color = mutedFg, modifier = Modifier.weight(1f))
        TextButton(onClick = { savingPreset = true }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
            Icon(Icons.Outlined.StarBorder, contentDescription = null, Modifier.size(16.dp), tint = ProButtonPalette.ElectricBlue)
            Spacer(Modifier.width(4.dp))
            Text("Save pen", color = ProButtonPalette.ElectricBlue)
        }
    }
    if (presets.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { p ->
                PresetChip(
                    p = p,
                    fgColor = fgColor,
                    onClick = { vm.applyPreset(p) },
                    onRename = { renamingPreset = p },
                    onRemove = { vm.removePreset(p) },
                )
            }
        }
    }

    HorizontalDivider(color = fgColor.copy(alpha = 0.08f))
    ColorPalettePresetsStrip(vm, Tool.PEN, style, fgColor, darkUi)
    }
    if (section == 3) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(tileIdleBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("PAPER FEEDBACK", style = MaterialTheme.typography.labelSmall, color = mutedFg, letterSpacing = 0.7.sp)
            SheetToggleLine("Writing sound", s.paperSound, fgColor) { v -> store.update { it.copy(paperSound = v) } }
            if (s.paperSound) {
                SheetSliderRow("VOLUME", "%.0f%%".format(s.paperVolume * 100), s.paperVolume, 0.05f..1f) { v ->
                    store.update { it.copy(paperVolume = v) }
                }
            }
            SheetToggleLine("Paper haptics", s.paperHaptics, fgColor) { v -> store.update { it.copy(paperHaptics = v) } }
        }
    }
    if (editingPenName) {
        var name by remember(s.penName) { mutableStateOf(s.penName) }
        AppDialog(
            onDismissRequest = { editingPenName = false },
            title = { Text("Stylus name") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, shape = CircleShape, label = { Text("Pen display name") }) },
            confirmButton = { TextButton(onClick = { vm.setPenName(name); editingPenName = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingPenName = false }) { Text("Cancel") } },
        )
    }

    if (savingPreset) {
        var label by remember { mutableStateOf("${vm.penType.label} ${style.size.roundToInt()}") }
        AppDialog(
            onDismissRequest = { savingPreset = false },
            title = { Text("Name this pen preset") },
            text = { OutlinedTextField(label, { label = it }, singleLine = true, shape = CircleShape) },
            confirmButton = { TextButton(onClick = { vm.savePreset(label); savingPreset = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { savingPreset = false }) { Text("Cancel") } },
        )
    }

    renamingPreset?.let { p ->
        var label by remember(p) { mutableStateOf(p.name) }
        AppDialog(
            onDismissRequest = { renamingPreset = null },
            title = { Text("Rename preset") },
            text = { OutlinedTextField(label, { label = it }, singleLine = true, shape = CircleShape) },
            confirmButton = { TextButton(onClick = { vm.renamePreset(p, label); renamingPreset = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renamingPreset = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(p: PenPreset, fgColor: Color, onClick: () -> Unit, onRename: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .defaultMinSize(minHeight = 40.dp)
                .clip(CircleShape)
                .border(1.dp, fgColor.copy(alpha = 0.22f), CircleShape)
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PenGlyph(runCatching { PenType.valueOf(p.type) }.getOrDefault(PenType.FOUNTAIN), fgColor, Color(p.color), Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(width = 16.dp, height = (p.size / 2f).coerceIn(2f, 8f).dp).clip(CircleShape).background(Color(p.color)))
            if (p.name.isNotBlank()) {
                Spacer(Modifier.width(5.dp))
                Text(p.name, style = MaterialTheme.typography.labelSmall, color = fgColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AppContextMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            items = listOf(
                AppMenuItem("Rename") { onRename() },
                AppMenuItem("Remove", destructive = true) { onRemove() },
            ),
        )
    }
}

@Composable
private fun HighlighterCardContent(vm: EditorViewModel, darkUi: Boolean, fgColor: Color) {
    val style = vm.highlighterStyle
    val context = LocalContext.current
    val store = vm.notebookSettings
    val s by remember(store) {
        store.select { HighlighterCardSettings(it.highlighterOpacity, it.highlighterMode, it.highlighterBehind) }
    }.collectAsStateWithLifecycle(
        initialValue = remember(store) {
            store.settings.value.let { HighlighterCardSettings(it.highlighterOpacity, it.highlighterMode, it.highlighterBehind) }
        },
    )
    StrokePreview(Color(style.color).copy(alpha = s.highlighterOpacity), style.size * 1.2f, false, darkUi)
    Text(stringResource(R.string.sheet_colour_set), style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StyleOptions(vm, Tool.HIGHLIGHTER, style, fgColor, darkUi, vertical = false)
    }
    ColorPalettePresetsStrip(vm, Tool.HIGHLIGHTER, style, fgColor, darkUi)
    SheetSliderRow(stringResource(R.string.sheet_thickness), "%.0f pt".format(style.size), style.size, 6f..70f) { v ->
        val sizes = style.sizes.map { if (it == style.size) v else it }
        vm.updateStyle(Tool.HIGHLIGHTER, style.copy(size = v, sizes = sizes))
    }
    SheetSliderRow(stringResource(R.string.sheet_opacity), "%.0f%%".format(s.highlighterOpacity * 100), s.highlighterOpacity, 0.3f..0.9f) { v ->
        store.update { it.copy(highlighterOpacity = v) }
    }
    Text("LINE", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    SheetSegments(HighlighterMode.entries.map { it.label }, s.highlighterMode.ordinal) { i ->
        store.update { it.copy(highlighterMode = HighlighterMode.entries[i]) }
    }
    SheetToggleLine("Draw behind ink", s.highlighterBehind, fgColor) { v -> store.update { it.copy(highlighterBehind = v) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EraserCardContent(vm: EditorViewModel) {
    val context = LocalContext.current
    val store = vm.notebookSettings
    val fgColor = LocalContentColor.current
    val s by remember(store) {
        store.select {
            EraserCardSettings(
                it.eraserMode,
                it.scribbleSensitivity,
                it.eraseHighlighterOnly,
                it.eraserFilter,
                it.eraserAutoSwitch,
                it.scribbleToErase,
            )
        }
    }.collectAsStateWithLifecycle(
        initialValue = remember(store) {
            store.settings.value.let {
                EraserCardSettings(
                    it.eraserMode,
                    it.scribbleSensitivity,
                    it.eraseHighlighterOnly,
                    it.eraserFilter,
                    it.eraserAutoSwitch,
                    it.scribbleToErase,
                )
            }
        },
    )
    SheetSegments(EraserMode.entries.map { it.label }, s.eraserMode.ordinal) { i ->
        store.update { it.copy(eraserMode = EraserMode.entries[i]) }
    }
    val presets = listOf(8f, 16f, 30f)
    val currentDp = vm.eraserCustomDp ?: presets[vm.eraserSize.coerceIn(0, 2)]
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        SizeDots(presets, if (vm.eraserCustomDp == null) presets[vm.eraserSize.coerceIn(0, 2)] else -1f, ring = true, fg = fgColor) { v ->
            vm.eraserCustomDp = null
            vm.eraserSize = presets.indexOf(v).coerceIn(0, 2)
        }
    }
    SheetSliderRow(stringResource(R.string.sheet_default_size), "%.0f dp".format(currentDp), currentDp, 4f..64f) { v ->
        vm.eraserCustomDp = v
    }
    Text("WHAT ERASER ERASES", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    val ef = s.eraserFilter
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppChip(ef.ink && !s.eraseHighlighterOnly, {
            store.update { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(ink = !ef.ink)) }
        }, label = "Pen ink")
        AppChip(ef.highlighter, {
            store.update { it.copy(eraserFilter = ef.copy(highlighter = !ef.highlighter)) }
        }, label = "Highlighter")
        AppChip(ef.shapes && !s.eraseHighlighterOnly, {
            store.update { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(shapes = !ef.shapes)) }
        }, label = "Shapes and fills")
        AppChip(ef.text && !s.eraseHighlighterOnly, {
            store.update { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(text = !ef.text)) }
        }, label = "Text and Write Boxes")
        AppChip(ef.stickyAndTape && !s.eraseHighlighterOnly, {
            store.update { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(stickyAndTape = !ef.stickyAndTape)) }
        }, label = "Sticky notes and tape")
        AppChip(ef.images && !s.eraseHighlighterOnly, {
            store.update { it.copy(eraseHighlighterOnly = false, eraserFilter = ef.copy(images = !ef.images)) }
        }, label = "Images")
    }
    Text("SCRIBBLE SENSITIVITY", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    SheetSegments(ScribbleSensitivity.entries.map { it.label }, s.scribbleSensitivity.ordinal) { i ->
        store.update { it.copy(scribbleSensitivity = ScribbleSensitivity.entries[i]) }
    }
    SheetToggleLine("Highlighter only quick-lock", s.eraseHighlighterOnly, fgColor) { v -> store.update { it.copy(eraseHighlighterOnly = v) } }
    SheetToggleLine("Switch back after erasing", s.eraserAutoSwitch, fgColor) { v -> store.update { it.copy(eraserAutoSwitch = v) } }
    SheetToggleLine("Scribble to erase with the pen", s.scribbleToErase, fgColor) { v -> store.update { it.copy(scribbleToErase = v) } }
    HorizontalDivider(color = fgColor.copy(alpha = 0.08f))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = "Clear ink") { vm.clearPageInk() }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CleaningServices, contentDescription = null, Modifier.size(16.dp), tint = Color(0xFFF28B82))
            Spacer(Modifier.width(6.dp))
            Text("Clear ink", style = MaterialTheme.typography.labelMedium, color = Color(0xFFF28B82))
        }
        Row(
            Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = "Clear all") { vm.clearPageAll() }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, Modifier.size(16.dp), tint = Color(0xFFF28B82))
            Spacer(Modifier.width(6.dp))
            Text("Clear all", style = MaterialTheme.typography.labelMedium, color = Color(0xFFF28B82))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LassoCardContent(
    vm: EditorViewModel,
    onSelectionConverted: (Long) -> Unit,
) {
    val context = LocalContext.current
    val store = vm.notebookSettings
    val fgColor = LocalContentColor.current
    val s by remember(store) {
        store.select { LassoCardSettings(it.lassoMode, it.lassoFilter, it.lassoTouching, it.circleToLasso) }
    }.collectAsStateWithLifecycle(
        initialValue = remember(store) {
            store.settings.value.let { LassoCardSettings(it.lassoMode, it.lassoFilter, it.lassoTouching, it.circleToLasso) }
        },
    )
    SheetSegments(LassoMode.entries.map { it.label }, s.lassoMode.ordinal) { i -> store.update { it.copy(lassoMode = LassoMode.entries[i]) } }
    Text("SELECT", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    val f = s.lassoFilter
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppChip(f.ink, { store.update { it.copy(lassoFilter = f.copy(ink = !f.ink)) } }, label = "Ink")
        AppChip(f.highlighter, { store.update { it.copy(lassoFilter = f.copy(highlighter = !f.highlighter)) } }, label = "Highlighter")
        AppChip(f.text, { store.update { it.copy(lassoFilter = f.copy(text = !f.text)) } }, label = "Text")
        AppChip(f.images, { store.update { it.copy(lassoFilter = f.copy(images = !f.images)) } }, label = "Images and stickers")
        AppChip(f.other, { store.update { it.copy(lassoFilter = f.copy(other = !f.other)) } }, label = "Notes and tape")
    }
    SheetToggleLine("Select anything the lasso touches", s.lassoTouching, fgColor) { v -> store.update { it.copy(lassoTouching = v) } }
    SheetToggleLine("Hold circle dot after looping to select", s.circleToLasso, fgColor) { v -> store.update { it.copy(circleToLasso = v) } }
    if (vm.selection.isNotEmpty()) {
        var script by remember { mutableStateOf(TextFont.CURSIVE) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { vm.copySelection() }) { Text("Copy") }
            TextButton(onClick = { vm.rotateSelection(90f) }) { Text("Rotate 90°") }
            TextButton(onClick = { vm.flipSelection() }) { Text("Flip") }
        }
        Text("Beautify handwriting")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(TextFont.CURSIVE, TextFont.SCRIPT, TextFont.HAND, TextFont.BOOK).forEach {
                AppChip(script == it, { script = it }, label = it.label)
            }
        }
        TextButton(onClick = { vm.convertSelectionToText(onSelectionConverted, script) }) { Text("Recognize and restyle ink") }
        AppPill(
            selected = true,
            tint = ProButtonPalette.ElectricBlue,
            onClick = { vm.convertSelectionToText(onSelectionConverted) },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Icon(Icons.Outlined.TextFields, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.sheet_lasso_convert), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LaserCardContent(vm: EditorViewModel) {
    val context = LocalContext.current
    val store = vm.notebookSettings
    val laserSeconds by remember(store) {
        store.select { it.laserSeconds }
    }.collectAsStateWithLifecycle(initialValue = remember(store) { store.settings.value.laserSeconds })
    SheetSliderRow("STAYS FOR", "%.0f s".format(laserSeconds), laserSeconds, 1f..8f) { v ->
        store.update { it.copy(laserSeconds = v) }
    }
    Text(
        "Keep the pen or a finger on the screen to hold the laser pointer active; trails fade smoothly after release.",
        style = MaterialTheme.typography.labelSmall,
        color = LocalContentColor.current.copy(alpha = 0.72f),
    )
}

@Composable
private fun TextCardContent(vm: EditorViewModel) {
    val context = LocalContext.current
    val fgColor = LocalContentColor.current
    val store = vm.notebookSettings
    val settings by vm.settingsFlow.collectAsStateWithLifecycle()
    val td = settings.toolDefaults
    val actionFg = if (Color(vm.textColor ?: vm.penStyle.color).luminance() > 0.45f) Color.Black else Color.White

    Text("Text & keyboard", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AppPill(
            selected = true,
            tint = Color(vm.textColor ?: vm.penStyle.color),
            onClick = { vm.openInlineTextBox(vm.pageIndex, 100f, 160f) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
        ) {
            ProIconView(ProIcon.TEXT, actionFg, Modifier.size(16.dp), ink = actionFg)
            Spacer(Modifier.width(5.dp))
            Text("Text box", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = actionFg, maxLines = 1)
        }
        AppPill(
            selected = vm.keyboardMode,
            tint = Color(vm.textColor ?: vm.penStyle.color),
            onClick = { vm.toggleKeyboardMode() },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
        ) {
            ProIconView(ProIcon.KEYBOARD, if (vm.keyboardMode) actionFg else fgColor, Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("Keyboard", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (vm.keyboardMode) actionFg else fgColor, maxLines = 1)
        }
    }
    Text(stringResource(R.string.sheet_colour_set), style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CustomSwatches(PenPalette.take(8), vm.textColor ?: vm.penStyle.color, vm) { vm.textColor = it; vm.pushRecent(it) }
    }
    SheetSliderRow("TEXT SIZE", "%.0f pt".format(vm.textSize), vm.textSize, 16f..96f) { vm.textSize = it }
    SheetSliderRow(stringResource(R.string.sheet_opacity), "%.0f%%".format(td.textOpacity * 100), td.textOpacity, 0.2f..1f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(textOpacity = v)) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextFont.entries.forEach { font ->
            AppChip(vm.textStyle.customFontPath.isBlank() && vm.textStyle.font == font, { vm.textStyle = vm.textStyle.copy(font = font, customFontPath = "") }, label = font.label)
        }
    }
    CustomHandwritingFontPicker(
        selectedPath = vm.textStyle.customFontPath,
        fgColor = fgColor,
        onSelect = { path -> vm.textStyle = vm.textStyle.copy(customFontPath = path) },
    )
    SheetToggleLine("Auto spell check while typing", td.autoSpellCheck, fgColor) { enabled ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(autoSpellCheck = enabled)) }
    }
    val hwStore = remember(context) { HandwritingFontStore.get(context) }
    val hwProfile by hwStore.profile.collectAsStateWithLifecycle()
    var showHwStudio by remember { mutableStateOf(false) }

    AppPill(
        selected = hwProfile.trainedCharCount() > 0,
        tint = Color(vm.textColor ?: vm.penStyle.color),
        onClick = { showHwStudio = true },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 46.dp),
    ) {
        Icon(
            Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (hwProfile.trainedCharCount() > 0) actionFg else fgColor,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "My Handwriting Style (${hwProfile.trainedCharCount()}/${HandwritingFontChars.ALL.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (hwProfile.trainedCharCount() > 0) actionFg else fgColor,
        )
    }
    Text(
        "Tap or drag anywhere on the page with the Text tool to place a transparent, resizable text box. Type with your keyboard, write with a stylus, or tap My Ink to convert typed text into your own handwriting.",
        style = MaterialTheme.typography.labelSmall,
        color = fgColor.copy(alpha = 0.68f),
    )

    if (showHwStudio) {
        HandwritingStudioDialog(onDismiss = { showHwStudio = false })
    }
}

@Composable
private fun StickerCardContent(vm: EditorViewModel, onBrowseStickers: () -> Unit) {
    val context = LocalContext.current
    val store = vm.notebookSettings
    val fgColor = LocalContentColor.current
    val td by remember(store) {
        store.select { it.toolDefaults }
    }.collectAsStateWithLifecycle(initialValue = store.settings.value.toolDefaults)

    AppPill(
        selected = true,
        tint = ProButtonPalette.ElectricBlue,
        onClick = onBrowseStickers,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
    ) {
        ProIconView(ProIcon.STICKER, Color.White, Modifier.size(18.dp), ink = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.sheet_browse_stickers), style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
    SheetSliderRow(stringResource(R.string.sheet_default_size), "%.0f%%".format(td.stickerScale * 100), td.stickerScale, 0.4f..2f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(stickerScale = v)) }
    }
    SheetSliderRow(stringResource(R.string.sheet_opacity), "%.0f%%".format(td.stickerOpacity * 100), td.stickerOpacity, 0.2f..1f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(stickerOpacity = v)) }
    }
    SheetToggleLine(stringResource(R.string.sheet_lock_aspect), td.stickerLockAspect, fgColor) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(stickerLockAspect = v)) }
    }
    Text(
        stringResource(R.string.sheet_sticker_hint),
        style = MaterialTheme.typography.labelSmall,
        color = fgColor.copy(alpha = 0.68f),
    )
}

@Composable
private fun ImageCardContent(vm: EditorViewModel, onPickImage: () -> Unit) {
    val context = LocalContext.current
    val store = vm.notebookSettings
    val fgColor = LocalContentColor.current
    val td by remember(store) {
        store.select { it.toolDefaults }
    }.collectAsStateWithLifecycle(initialValue = store.settings.value.toolDefaults)

    AppPill(
        selected = true,
        tint = ProButtonPalette.ElectricBlue,
        onClick = onPickImage,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
    ) {
        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.sheet_choose_image), style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
    SheetSliderRow(stringResource(R.string.sheet_default_width), "%.0f%% of page".format(td.imageWidthRatio * 100), td.imageWidthRatio, 0.2f..0.95f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(imageWidthRatio = v)) }
    }
    SheetSliderRow(stringResource(R.string.sheet_opacity), "%.0f%%".format(td.imageOpacity * 100), td.imageOpacity, 0.2f..1f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(imageOpacity = v)) }
    }
    SheetToggleLine(stringResource(R.string.sheet_lock_aspect), td.imageLockAspect, fgColor) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(imageLockAspect = v)) }
    }
    SheetToggleLine(stringResource(R.string.sheet_image_frame), td.imageBorder, fgColor) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(imageBorder = v)) }
    }
    Text(
        stringResource(R.string.sheet_image_hint),
        style = MaterialTheme.typography.labelSmall,
        color = fgColor.copy(alpha = 0.68f),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MindMapCardContent(vm: EditorViewModel) {
    val fgColor = LocalContentColor.current
    val shapes = listOf("pill" to "Pill", "rect" to "Card", "diamond" to "Diamond", "circle" to "Oval")
    Text("NODE SHAPE", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    SheetSegments(shapes.map { it.second }, shapes.indexOfFirst { it.first == vm.mindShape }.coerceAtLeast(0)) { i ->
        vm.mindShape = shapes[i].first
    }
    Text("NODE COLOUR", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MindPalette.forEach { c ->
            Swatch(c, c == vm.mindColor, 20) { vm.recolorMindNode(color = c) }
        }
    }
    SheetToggleLine("Move child branch with node", vm.moveMindSubtree, fgColor) { vm.moveMindSubtree = it }
    Text("LAYOUT", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        EditorViewModel.MindLayout.entries.forEach { layout ->
            TextButton(onClick = { vm.autoLayoutMindMap(layout) }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
                Text(layout.label, maxLines = 1)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { vm.exportMindMapOutlineToPage() }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
            Text("Outline to page", maxLines = 1)
        }
        TextButton(onClick = { vm.flashcardsFromMindMap() }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
            Text("Flashcards", maxLines = 1)
        }
    }
    AppPill(
        selected = true,
        tint = ProButtonPalette.ElectricBlue,
        onClick = { vm.buildMindMapFromNotes() },
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
    ) {
        Icon(Icons.Outlined.Hub, contentDescription = null, Modifier.size(16.dp), tint = Color.White)
        Spacer(Modifier.width(6.dp))
        Text("Build concept map from notes", style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
    Text(
        "Tap the canvas to add nodes, drag nodes to arrange, and double-tap a node to edit.",
        style = MaterialTheme.typography.labelSmall,
        color = fgColor.copy(alpha = 0.68f),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapeCardContent(vm: EditorViewModel) {
    var fillEdit by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val fgColor = LocalContentColor.current
    val store = vm.notebookSettings
    val settings by vm.settingsFlow.collectAsStateWithLifecycle()
    val darkUi = MaterialTheme.colorScheme.background.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.4f }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ShapeKind.entries.forEach { k ->
            val sel = vm.shapeKind == k
            Column(
                Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) ProButtonPalette.ElectricBlue.copy(alpha = 0.2f) else fgColor.copy(alpha = 0.08f))
                    .border(if (sel) 1.5.dp else 0.dp, if (sel) ProButtonPalette.ElectricBlue else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable(role = Role.RadioButton, onClickLabel = k.label) { vm.shapeKind = k },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ShapeGlyph(
                    k,
                    if (sel) ProButtonPalette.ElectricBlue else fgColor,
                    Modifier.size(26.dp),
                    fill = if (vm.shapeFillOn) Color(vm.shapeFillColor) else null,
                )
                Text(k.label, style = MaterialTheme.typography.labelSmall, color = fgColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Text(stringResource(R.string.sheet_colour_set), style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StyleOptions(vm, Tool.SHAPE, vm.shapeStyle, fgColor, darkUi, vertical = false)
    }
    ColorPalettePresetsStrip(vm, Tool.SHAPE, vm.shapeStyle, fgColor, darkUi)
    SheetSliderRow(stringResource(R.string.sheet_thickness), "%.1f pt".format(vm.shapeStyle.size), vm.shapeStyle.size, 0.5f..20f) { v ->
        val sizes = vm.shapeStyle.sizes.map { if (it == vm.shapeStyle.size) v else it }
        vm.updateStyle(Tool.SHAPE, vm.shapeStyle.copy(size = v, sizes = sizes))
    }
    SheetSliderRow(stringResource(R.string.sheet_opacity), "%.0f%%".format(settings.toolDefaults.shapeOpacity * 100), settings.toolDefaults.shapeOpacity, 0.15f..1f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(shapeOpacity = v)) }
    }
    SheetToggleLine("Dashed line", vm.shapeDashed, fgColor) { vm.shapeDashed = it }
    SheetToggleLine("Fill", vm.shapeFillOn, fgColor) { vm.shapeFillOn = it }
    AnimatedVisibility(vm.shapeFillOn) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(0xFFCDE2FB, 0xFFCAEAC7, 0xFFFAD6D6, 0xFFF9DCA4, 0xFFDFDBFD, 0x661B1F2A).map { it.toInt() }.forEach { c ->
                    Swatch(c, c == vm.shapeFillColor, 20) { vm.shapeFillColor = c }
                }
                AddSwatchButton { fillEdit = true }
            }
            SheetSliderRow(stringResource(R.string.sheet_opacity), "${((vm.shapeFillColor ushr 24) * 100 / 255)}%", (vm.shapeFillColor ushr 24) / 255f, 0.1f..1f) { a ->
                vm.shapeFillColor = (vm.shapeFillColor and 0x00FFFFFF) or ((a * 255).toInt() shl 24)
            }
        }
    }
    if (fillEdit) {
        ColorEditorDialog(vm.shapeFillColor, PenPalette, vm.recentColors(), onDismiss = { fillEdit = false }) { c ->
            vm.shapeFillColor = (c and 0x00FFFFFF) or (vm.shapeFillColor and 0xFF000000.toInt())
            fillEdit = false
        }
    }
}

@Composable
private fun SheetToggleLine(label: String, value: Boolean, fgColor: Color = LocalContentColor.current, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Switch, onClickLabel = label) { onChange(!value) },
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = fgColor)
        AppToggle(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun SheetSliderRow(label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    AppSliderRow(
        title = label,
        valueText = value,
        value = v,
        valueRange = range,
        onValueChange = onChange,
    )
}

@Composable
private fun CrayonCardContent(vm: EditorViewModel, darkUi: Boolean, fgColor: Color) {
    val style = vm.crayonStyle
    val store = vm.notebookSettings
    val settings by vm.settingsFlow.collectAsStateWithLifecycle()
    val td = settings.toolDefaults

    StrokePreview(
        Color(style.color).copy(alpha = td.crayonOpacity),
        style.size * 1.45f,
        taper = false,
        darkUi = darkUi,
    )
    Text("CRAYON COLOURS", style = MaterialTheme.typography.labelSmall, color = fgColor.copy(alpha = 0.68f))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StyleOptions(vm, Tool.CRAYON, style, fgColor, darkUi, vertical = false)
    }
    ColorPalettePresetsStrip(vm, Tool.CRAYON, style, fgColor, darkUi)
    HorizontalDivider(color = fgColor.copy(alpha = 0.08f))
    SheetSliderRow("WAX TIP SIZE", "%.1f pt".format(style.size), style.size, 4f..40f) { v ->
        val sizes = style.sizes.map { if (it == style.size) v else it }
        vm.updateStyle(Tool.CRAYON, style.copy(size = v, sizes = sizes))
    }
    SheetSliderRow("WAX GRAIN TEXTURE", "${(td.crayonGrain * 100).roundToInt()}%", td.crayonGrain, 0.15f..1.0f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(crayonGrain = v)) }
    }
    SheetSliderRow("WAX OPACITY", "${(td.crayonOpacity * 100).roundToInt()}%", td.crayonOpacity, 0.35f..1.0f) { v ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(crayonOpacity = v)) }
    }
    Text(
        "Textured dry-wax crayon with paper grain modulation. Great for sketching, shading, and diagrams.",
        style = MaterialTheme.typography.labelSmall,
        color = fgColor.copy(alpha = 0.68f),
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ColorPalettePresetsStrip(
    vm: EditorViewModel,
    tool: Tool,
    style: ToolStyle,
    fgColor: Color,
    darkUi: Boolean,
) {
    val settings by vm.settingsFlow.collectAsStateWithLifecycle()
    val presets = remember(settings.customColorPresetsJson) {
        vm.colorPalettePresets()
    }
    var savingDialog by remember { mutableStateOf(false) }
    var menuForId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "COLOUR PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = fgColor.copy(alpha = 0.68f),
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { savingDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("+ Save current", style = MaterialTheme.typography.labelSmall, color = ProButtonPalette.ElectricBlue, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                val isActive = style.colors == preset.colors
                val cardBg = when {
                    isActive -> ProButtonPalette.ElectricBlue.copy(alpha = if (darkUi) 0.22f else 0.12f)
                    darkUi -> Color(0xFF232326)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                }
                val cardBorder = if (isActive) ProButtonPalette.ElectricBlue else fgColor.copy(alpha = 0.14f)
                Box {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(if (isActive) 1.4.dp else 1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { vm.applyColorPalettePreset(tool, preset) },
                                onLongClick = if (!preset.builtIn) ({ menuForId = preset.id }) else null,
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            preset.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = fgColor,
                            maxLines = 1,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            preset.colors.take(6).forEach { col ->
                                Box(
                                    Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(Color(col))
                                        .border(0.7.dp, Color.Black.copy(alpha = 0.22f), CircleShape),
                                )
                            }
                        }
                    }
                    if (!preset.builtIn) {
                        AppContextMenu(
                            expanded = menuForId == preset.id,
                            onDismissRequest = { menuForId = null },
                            items = listOf(
                                AppMenuItem("Delete preset", destructive = true) {
                                    vm.deleteColorPalettePreset(preset.id)
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    if (savingDialog) {
        var name by remember { mutableStateOf("") }
        AppDialog(
            onDismissRequest = { savingDialog = false },
            title = { Text("Save colour preset") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    label = { Text("Preset name") },
                    placeholder = { Text("e.g. Biology, Pastel Ink") },
                    singleLine = true,
                    shape = CircleShape,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.saveCurrentColorPalettePreset(tool, name)
                    savingDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { savingDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomHandwritingFontPicker(
    selectedPath: String,
    fgColor: Color,
    onSelect: (String) -> Unit,
) {
    val context = LocalContext.current
    var fonts by remember { mutableStateOf(CustomFontRegistry.listFonts(context)) }
    var status by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val imported = CustomFontRegistry.importFont(context, uri)
            if (imported != null) {
                fonts = CustomFontRegistry.listFonts(context)
                onSelect(imported.path)
                status = "Imported ${imported.displayName}"
            } else {
                status = "Could not read font file (.ttf / .otf)"
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "CUSTOM FONTS (.TTF / .OTF)",
                style = MaterialTheme.typography.labelSmall,
                color = fgColor.copy(alpha = 0.68f),
                modifier = Modifier.weight(1f),
            )
            AppPill(
                selected = false,
                tint = ProButtonPalette.ElectricBlue,
                onClick = {
                    importLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream", "*/*"))
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("+ Import font", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        if (fonts.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppChip(
                    selected = selectedPath.isBlank(),
                    onClick = { onSelect("") },
                    label = "Built-in",
                    tint = ProButtonPalette.ElectricBlue,
                )
                fonts.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppChip(
                            selected = selectedPath == entry.path,
                            onClick = { onSelect(entry.path) },
                            label = entry.displayName,
                            tint = ProButtonPalette.Violet,
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    CustomFontRegistry.deleteFont(entry.path)
                                    if (selectedPath == entry.path) onSelect("")
                                    fonts = CustomFontRegistry.listFonts(context)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove font", tint = fgColor.copy(alpha = 0.55f), modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
        status?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = ProButtonPalette.Emerald)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutoTextCardContent(vm: EditorViewModel, fgColor: Color) {
    val store = vm.notebookSettings
    val settings by vm.settingsFlow.collectAsStateWithLifecycle()
    val td = settings.toolDefaults
    Text(
        "Write naturally above the guide line — pauses automatically convert your handwriting into editable text with spell check and dynamic bold.",
        style = MaterialTheme.typography.bodySmall,
        color = fgColor.copy(alpha = 0.78f),
    )
    SheetToggleLine("Align converted text to paper lines", td.alignHandwriting, fgColor) { enabled ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(alignHandwriting = enabled)) }
    }
    SheetToggleLine("Show writing guide line under text", td.alignGuideLine, fgColor) { enabled ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(alignGuideLine = enabled)) }
    }
    SheetToggleLine("Dynamic bold (auto-bold large writing)", td.dynamicBoldHandwriting, fgColor) { enabled ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(dynamicBoldHandwriting = enabled)) }
    }
    SheetToggleLine("Auto spell check", td.autoSpellCheck, fgColor) { enabled ->
        store.update { it.copy(toolDefaults = it.toolDefaults.copy(autoSpellCheck = enabled)) }
    }
    Text("Recognition language", style = MaterialTheme.typography.titleSmall, color = fgColor)
    var showAutoLangPicker by remember { mutableStateOf(false) }
    val activeAutoLang = remember(td.handwritingLanguage) {
        HandwritingRecognizer.findLanguage(td.handwritingLanguage)
    }
    val quickAutoLangs = remember(activeAutoLang.tag) {
        val base = HandwritingRecognizer.quickLanguages
        if (base.any { it.tag.equals(activeAutoLang.tag, ignoreCase = true) }) base else listOf(activeAutoLang) + base.take(9)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        quickAutoLangs.forEach { lang ->
            AppChip(
                selected = activeAutoLang.tag.equals(lang.tag, ignoreCase = true),
                onClick = { store.update { it.copy(toolDefaults = it.toolDefaults.copy(handwritingLanguage = lang.tag)) } },
                label = lang.nativeName,
                tint = ProButtonPalette.ElectricBlue,
            )
        }
        AppChip(
            selected = false,
            onClick = { showAutoLangPicker = true },
            label = "All ${HandwritingRecognizer.supportedLanguages.size}+ languages…",
            tint = ProButtonPalette.Violet,
        )
    }
    if (showAutoLangPicker) {
        com.pronotes.app.ui.HandwritingLanguagePickerDialog(
            selectedTag = td.handwritingLanguage,
            onDismiss = { showAutoLangPicker = false },
            onSelect = { lang ->
                store.update { it.copy(toolDefaults = it.toolDefaults.copy(handwritingLanguage = lang.tag)) }
                showAutoLangPicker = false
            },
        )
    }
    Text("Converted text font", style = MaterialTheme.typography.titleSmall, color = fgColor)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextFont.entries.forEach { font ->
            AppChip(
                selected = td.customHandwritingFontPath.isBlank() && td.autoScript == font.name,
                onClick = { store.update { it.copy(toolDefaults = it.toolDefaults.copy(autoScript = font.name, customHandwritingFontPath = "")) } },
                label = font.label,
                tint = ProButtonPalette.ElectricBlue,
            )
        }
    }
    CustomHandwritingFontPicker(
        selectedPath = td.customHandwritingFontPath,
        fgColor = fgColor,
        onSelect = { path ->
            store.update { it.copy(toolDefaults = it.toolDefaults.copy(customHandwritingFontPath = path)) }
        },
    )
}
