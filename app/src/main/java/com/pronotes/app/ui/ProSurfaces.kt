package com.pronotes.app.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

/**
 * Clean Material 3 surface roles used across Pro Notes (borderless/hairline tonal architecture).
 */
enum class SurfaceRole {
    Chrome,
    Frost,
    Control,
    Capsule,
    Popover,
    Sheet,
}

@Composable
fun appCardShape(offsetDp: Float = 0f): Shape {
    val base = LocalCornerRoundness.current.value
    val radius = (base + offsetDp).coerceIn(8f, 32f)
    return RoundedCornerShape(radius.dp)
}

@Composable
fun appPillShape(): Shape {
    val base = LocalCornerRoundness.current.value
    val radius = if (base <= 12f) (base * 1.15f).coerceAtMost(14f) else 999f
    return RoundedCornerShape(radius.dp)
}

@Composable
fun appContentBottomPadding(extra: Dp = 16.dp): Dp {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return 96.dp + navBottom + extra
}

/** Applies native Android 12+ blur when active and Classic appearance is off. */
@Composable
fun Modifier.appBackdropBlur(enabled: Boolean, radiusDp: Dp = 14.dp): Modifier {
    val classic = LocalClassicAppearance.current
    if (!enabled || classic || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    val radiusPx = with(LocalDensity.current) { radiusDp.toPx() }.coerceAtLeast(1f)
    return graphicsLayer {
        renderEffect = android.graphics.RenderEffect.createBlurEffect(
            radiusPx,
            radiusPx,
            android.graphics.Shader.TileMode.CLAMP,
        ).asComposeRenderEffect()
    }
}

private fun resolveSurfaceColors(
    role: SurfaceRole,
    scheme: androidx.compose.material3.ColorScheme,
    classicAppearance: Boolean,
    containerColor: Color?,
    contentColor: Color?,
): Pair<Color, Color> {
    val base = containerColor ?: when (role) {
        SurfaceRole.Chrome, SurfaceRole.Popover -> scheme.surfaceContainerHigh
        SurfaceRole.Frost, SurfaceRole.Sheet -> scheme.surface
        SurfaceRole.Control, SurfaceRole.Capsule -> scheme.surfaceVariant
    }
    val bg = when {
        classicAppearance -> base.copy(alpha = 1f)
        role == SurfaceRole.Chrome || role == SurfaceRole.Popover -> {
            val a = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.90f else 0.96f
            if (base.alpha > a) base.copy(alpha = a) else base
        }
        else -> base
    }
    val fg = contentColor ?: if (bg.luminance() < 0.42f) Color(0xFFF1F5F9) else scheme.onSurface
    return bg to fg
}

/**
 * Primary surface wrapper with crisp tonal layering and configurable shadow/blur policies.
 */
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    role: SurfaceRole = SurfaceRole.Frost,
    shape: Shape = appCardShape(),
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    shadowElevation: Dp = 0.dp,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val classic = LocalClassicAppearance.current
    val reduceEffects = LocalReduceEffects.current
    val (bg, fg) = resolveSurfaceColors(role, scheme, classic, containerColor, contentColor)
    val baseElevation = if (shadowElevation > 0.dp) shadowElevation else when (role) {
        SurfaceRole.Popover -> 12.dp
        SurfaceRole.Sheet -> 8.dp
        SurfaceRole.Chrome -> 2.dp
        else -> 0.dp
    }
    val effectiveElevation = if (reduceEffects) 0.dp else baseElevation
    val resolvedBorderColor = when {
        borderColor != null -> borderColor
        reduceEffects && effectiveElevation == 0.dp && (role == SurfaceRole.Chrome || role == SurfaceRole.Popover) ->
            scheme.outlineVariant
        else -> null
    }
    val stroke = resolvedBorderColor?.let { BorderStroke(0.85.dp, it) }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(enabled = enabled, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    } else Modifier

    Surface(
        modifier = modifier
            .then(if (effectiveElevation > 0.dp) Modifier.shadow(effectiveElevation, shape, clip = false) else Modifier)
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = bg,
        contentColor = fg,
        border = stroke,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Box(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = appCardShape(),
    hero: Boolean = false,
    containerColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val reduceEffects = LocalReduceEffects.current
    val bg = containerColor ?: if (hero) scheme.surfaceContainerHigh else scheme.surface
    val fg = if (bg.luminance() < 0.42f) Color(0xFFF3F4F6) else scheme.onSurface
    val cardMod = modifier
        .then(if (hero && !reduceEffects) Modifier.shadow(2.dp, shape, clip = false) else Modifier)
        .clip(shape)
        .then(
            if (onClick != null) {
                Modifier.clickable(role = Role.Button, onClick = onClick)
            } else Modifier,
        )

    Surface(
        modifier = cardMod,
        shape = shape,
        color = bg,
        contentColor = fg,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun AppDialogSurface(
    modifier: Modifier = Modifier,
    shape: Shape = appCardShape(2f),
    content: @Composable BoxScope.() -> Unit,
) {
    val reduceEffects = LocalReduceEffects.current
    AppSurface(
        modifier = modifier,
        role = SurfaceRole.Popover,
        shape = shape,
        shadowElevation = if (reduceEffects) 0.dp else 18.dp,
        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (reduceEffects) 0.18f else 0.08f),
        content = content,
    )
}

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                )
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppDialogSurface(
                modifier = modifier
                    .widthIn(min = 290.dp, max = 520.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (title != null) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                        ) {
                            Box(Modifier.fillMaxWidth()) { title() }
                        }
                    }
                    if (text != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 520.dp),
                        ) {
                            text()
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
    }
}

@Composable
fun AppPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.4f
    val reduceMotion = LocalReduceMotion.current
    val activeTint = tint ?: scheme.primary
    val bg by animateColorAsState(
        targetValue = when {
            selected -> activeTint
            tint != null -> tint.copy(alpha = if (dark) 0.22f else 0.14f)
            else -> scheme.surfaceVariant
        },
        animationSpec = if (reduceMotion) snap() else tween(180),
        label = "pillBg",
    )
    val fg = when {
        selected -> if (contrastRatio(activeTint, Color(0xFF0F172A)) > contrastRatio(activeTint, Color.White)) Color(0xFF0F172A) else Color.White
        tint != null && !dark -> lerp(tint, scheme.onSurface, 0.35f)
        tint != null && dark -> lerp(tint, Color.White, 0.52f)
        else -> scheme.onSurface
    }
    val shape = appPillShape()
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else Modifier,
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            content()
        }
    }
}

@Composable
fun AppChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    tint: Color? = null,
) {
    AppPill(
        modifier = modifier,
        selected = selected,
        tint = tint,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AppToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
        ),
    )
}

@Composable
fun AppSliderRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AppSlider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val track = scheme.surfaceVariant
    val pillBg = scheme.surfaceContainerHigh
    val shape = appPillShape()
    Row(
        modifier = modifier
            .clip(shape)
            .background(track)
            .selectableGroup()
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (sel) pillBg else Color.Transparent)
                    .selectable(selected = sel, role = Role.Tab, onClick = { onSelect(i) })
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (sel) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Immutable
data class AppMenuItem(
    val label: String,
    val selected: Boolean = false,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val icon: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit,
)

@Composable
fun AppContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val menuBg = scheme.surfaceContainerHigh
    MaterialTheme(
        colorScheme = scheme.copy(surface = menuBg, surfaceContainer = menuBg),
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp)),
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
                .widthIn(min = 180.dp, max = 280.dp)
                .heightIn(max = 420.dp)
                .background(menuBg, RoundedCornerShape(16.dp)),
        ) {
            items.forEach { item ->
                val color = when {
                    !item.enabled -> scheme.onSurface.copy(alpha = 0.38f)
                    item.destructive -> scheme.error
                    item.selected -> ProButtonPalette.ElectricBlue
                    else -> scheme.onSurface
                }
                DropdownMenuItem(
                    enabled = item.enabled,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (item.icon != null) {
                                CompositionLocalProvider(LocalContentColor provides color) {
                                    item.icon.invoke()
                                }
                            }
                            Text(
                                item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (item.selected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = ProButtonPalette.ElectricBlue,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    },
                    onClick = {
                        onDismissRequest()
                        item.onClick()
                    },
                )
            }
        }
    }
}

@Composable
fun AppMenuButton(
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    role: SurfaceRole = SurfaceRole.Control,
    shape: Shape = appPillShape(),
    containerColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    buttonContent: @Composable BoxScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AppSurface(
            role = role,
            shape = shape,
            containerColor = containerColor,
            contentPadding = contentPadding,
            onClick = { expanded = true },
            onClickLabel = contentDescription,
            content = buttonContent,
        )
        AppContextMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items = items,
        )
    }
}

@Composable
fun AppChoiceRow(
    title: String,
    value: String,
    options: List<String>,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        AppContextMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items = options.mapIndexed { idx, opt ->
                AppMenuItem(
                    label = opt,
                    selected = opt == value,
                    onClick = { onPick(idx) },
                )
            },
        )
    }
}

@Immutable
data class AppTabItem(
    val label: String,
    val icon: (@Composable () -> Unit)? = null,
)

@Composable
fun AppBottomBar(
    tabs: List<AppTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = navPad),
        contentAlignment = Alignment.Center,
    ) {
        AppSurface(
            role = SurfaceRole.Chrome,
            shape = appPillShape(),
            shadowElevation = 10.dp,
            contentPadding = PaddingValues(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { idx, tab ->
                    AppPill(
                        selected = idx == selectedIndex,
                        onClick = { onSelect(idx) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        if (tab.icon != null) {
                            tab.icon.invoke()
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(tab.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        modifier = modifier,
        content = content,
    )
}

@Immutable
data class FanCardSpec(
    val id: Int,
    val title: String,
    val subtitle: String,
    val primary: Color,
    val secondary: Color,
    val rim: Color,
)

private val DefaultFanCards = listOf(
    FanCardSpec(0, "Due Today", "Active spaced recall queue", Color(0xFF0044EA), Color(0xFF0032B5), Color(0xFF4BA6FF)),
    FanCardSpec(1, "Visual Cutouts", "Image occlusion & diagram cards", Color(0xFF00A3F5), Color(0xFF0077CC), Color(0xFF8BE2FF)),
    FanCardSpec(2, "Quick Quiz", "Prompt & answer flip review", Color(0xFFFF8C00), Color(0xFFD96400), Color(0xFFFFCF6E)),
    FanCardSpec(3, "Mastered", "Scheduled long-term memory", Color(0xFFF01E6B), Color(0xFFC20E4F), Color(0xFFFF86B4)),
)

/**
 * Fanned rounded card deck for Flashcards review decks without glow or stretch effects.
 */
@Composable
fun FannedColorfulCardsDeck(
    modifier: Modifier = Modifier,
    cards: List<FanCardSpec> = DefaultFanCards,
    autoCycle: Boolean = false,
    showLabels: Boolean = true,
    compact: Boolean = false,
    onFrontCardClick: ((Int) -> Unit)? = null,
) {
    if (cards.isEmpty()) return
    var frontIndex by remember(cards.size) { mutableIntStateOf(0) }

    LaunchedEffect(autoCycle, cards.size) {
        if (!autoCycle || cards.size <= 1) return@LaunchedEffect
        while (true) {
            delay(2600L)
            frontIndex = (frontIndex + 1) % cards.size
        }
    }

    val cardW = if (compact) 114.dp else 188.dp
    val cardH = if (compact) 144.dp else 238.dp
    val boxW = if (compact) 210.dp else 330.dp
    val boxH = if (compact) 172.dp else 274.dp

    Box(
        modifier = modifier.size(boxW, boxH),
        contentAlignment = Alignment.Center,
    ) {
        cards.forEachIndexed { cardIdx, spec ->
            val slot = (cardIdx - frontIndex).mod(cards.size)
            val targetAngle = when (slot) {
                0 -> -7.5f
                1 -> 6.5f
                2 -> 17.5f
                else -> 28.5f
            }
            val targetTx = when (slot) {
                0 -> if (compact) -28f else -46f
                1 -> if (compact) 0f else 2f
                2 -> if (compact) 24f else 44f
                else -> if (compact) 46f else 82f
            }
            val targetTy = when (slot) {
                0 -> if (compact) -4f else -8f
                1 -> if (compact) 2f else 4f
                2 -> if (compact) 8f else 14f
                else -> if (compact) 14f else 24f
            }
            val targetScale = when (slot) {
                0 -> 1.0f
                1 -> 0.94f
                2 -> 0.88f
                else -> 0.82f
            }

            val reduceMotion = LocalReduceMotion.current
            val reduceEffects = LocalReduceEffects.current
            val rot by animateFloatAsState(
                targetValue = targetAngle,
                animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.72f, stiffness = 220f),
                label = "fanRot$cardIdx",
            )
            val tx by animateFloatAsState(
                targetValue = targetTx,
                animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.76f, stiffness = 220f),
                label = "fanTx$cardIdx",
            )
            val ty by animateFloatAsState(
                targetValue = targetTy,
                animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.76f, stiffness = 220f),
                label = "fanTy$cardIdx",
            )
            val sc by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.78f, stiffness = 240f),
                label = "fanSc$cardIdx",
            )
            val cornerRadius = if (compact) 20.dp else 28.dp
            val shape = RoundedCornerShape(cornerRadius)

            Box(
                modifier = Modifier
                    .zIndex((cards.size - slot).toFloat())
                    .size(cardW, cardH)
                    .graphicsLayer {
                        translationX = tx.dp.toPx()
                        translationY = ty.dp.toPx()
                        rotationZ = rot
                        scaleX = sc
                        scaleY = sc
                        transformOrigin = TransformOrigin(0.5f, 0.88f)
                    }
                    .then(
                        if (reduceEffects) Modifier
                        else Modifier.shadow(
                            elevation = (12 - slot * 2).coerceAtLeast(2).dp,
                            shape = shape,
                            ambientColor = Color.Black.copy(alpha = 0.18f),
                            spotColor = Color.Black.copy(alpha = 0.24f),
                        ),
                    )
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(spec.primary, spec.secondary),
                            start = Offset(0f, 0f),
                            end = Offset(450f, 620f),
                        ),
                    )
                    .border(1.2.dp, spec.rim.copy(alpha = 0.78f), shape)
                    .clickable {
                        if (slot == 0 && onFrontCardClick != null) {
                            onFrontCardClick(spec.id)
                        } else {
                            frontIndex = (frontIndex + 1) % cards.size
                        }
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.10f),
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(300f, 450f),
                            ),
                        ),
                )
                if (showLabels && slot == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (compact) 12.dp else 18.dp),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "0${spec.id + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                        Text(
                            spec.title,
                            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!compact) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                spec.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.86f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
