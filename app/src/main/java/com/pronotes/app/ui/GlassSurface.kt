package com.pronotes.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pronotes.app.data.InterfaceStyle
import io.github.ntvien.liquidglass.DisableGlassSceneBackdrop
import io.github.ntvien.liquidglass.GlassColors
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.GlassLook
import io.github.ntvien.liquidglass.GlassQualityOverride
import io.github.ntvien.liquidglass.LiquidGlassScene
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.component.LiquidGlassSlider
import io.github.ntvien.liquidglass.component.LiquidGlassToggle
import io.github.ntvien.liquidglass.component.LiquidGradientBackground
import io.github.ntvien.liquidglass.component.LiquidMenuButton
import io.github.ntvien.liquidglass.component.LiquidMenuDivider
import io.github.ntvien.liquidglass.component.LiquidMenuEntry
import io.github.ntvien.liquidglass.component.LiquidMenuItem
import io.github.ntvien.liquidglass.component.LiquidSegmentedControl
import io.github.ntvien.liquidglass.component.LiquidSurface
import io.github.ntvien.liquidglass.component.LiquidTab
import io.github.ntvien.liquidglass.component.LiquidTabBar
import io.github.ntvien.liquidglass.component.LiquidTabLabels

/** Semantic roles for [GlassSurface] that resolve cleanly in both Liquid Glass and Classic modes. */
enum class GlassRole {
    Card,
    HeroCard,
    Control,
    Chrome,
    Accent,
    Frost,
    Tinted,
}

@Composable
fun glassCardShape(): Shape = RoundedCornerShape(LocalCornerRoundness.current.coerceIn(8.dp, 36.dp))

@Composable
fun glassControlShape(): Shape = RoundedCornerShape((LocalCornerRoundness.current * 0.72f).coerceIn(8.dp, 28.dp))

@Composable
fun glassPillShape(): Shape = RoundedCornerShape(LocalCornerRoundness.current.coerceIn(8.dp, 36.dp))

/**
 * Single theme-aware scene wrapper.
 * In [InterfaceStyle.CLASSIC], creates NO backdrop layers and NO glass shaders.
 */
@Composable
fun GlassScene(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        Box(modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), content = background)
            Box(Modifier.fillMaxSize(), content = content)
            Box(Modifier.fillMaxSize(), content = overlay)
        }
    } else {
        LiquidGlassScene(
            modifier = modifier,
            background = background,
            overlay = overlay,
            content = content,
        )
    }
}

/**
 * Botanical background in Liquid Glass mode, clean solid Material 3 background in Classic mode.
 */
@Composable
fun BotanicalGlassBackground(
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.background
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        Box(modifier.fillMaxSize().background(bg))
        return
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val dark = bg.luminance() < 0.5f
    LiquidGradientBackground(
        modifier = modifier,
        dark = dark,
        baseColor = bg,
        blobColors = if (!dark) {
            listOf(
                primary.copy(alpha = 0.14f),
                Color(0xFFD99A4E).copy(alpha = 0.10f),
                secondary.copy(alpha = 0.12f),
                Color(0xFF4BA3C7).copy(alpha = 0.08f),
            )
        } else {
            listOf(
                primary.copy(alpha = 0.16f),
                Color(0xFFD99A4E).copy(alpha = 0.09f),
                secondary.copy(alpha = 0.12f),
                Color(0xFF38BDF8).copy(alpha = 0.09f),
            )
        },
    )
}

/**
 * Scoped light/dark glass override for dark studio modals without leaking outside the dialog.
 */
@Composable
fun GlassThemeOverride(
    dark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isGlass = LocalInterfaceStyle.current == InterfaceStyle.LIQUID_GLASS
    val corner = LocalCornerRoundness.current
    if (!isGlass) {
        content()
        return
    }
    val colors = remember(dark) { if (dark) GlassColors.dark() else GlassColors.light() }
    LiquidGlassTheme(
        colors = colors,
        look = GlassLook.Auto,
        reduceEffects = false,
        qualityOverride = GlassQualityOverride.Auto,
        cornerRoundness = corner,
        overlaySamplesContent = false,
        content = content,
    )
}

/**
 * Universal surface wrapper used everywhere instead of raw `LiquidSurface` or `LiquidGlass*`.
 * In [InterfaceStyle.CLASSIC], renders a flat Material 3 [Surface] with standard surface/onSurface contrast.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    role: GlassRole = GlassRole.Card,
    shape: Shape = when (role) {
        GlassRole.Control, GlassRole.Accent, GlassRole.Tinted -> glassControlShape()
        else -> glassCardShape()
    },
    contentPadding: PaddingValues = PaddingValues(16.dp),
    tint: Color? = null,
    selected: Boolean = false,
    containerColor: Color? = null,
    interactive: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val styleMode = LocalInterfaceStyle.current
    if (styleMode == InterfaceStyle.CLASSIC) {
        val scheme = MaterialTheme.colorScheme
        val resolvedBg = when {
            containerColor != null -> containerColor
            tint != null && selected -> tint
            tint != null -> scheme.surfaceContainerHigh
            selected -> scheme.primary
            role == GlassRole.Accent -> scheme.primary
            role == GlassRole.HeroCard -> scheme.surfaceContainerHigh
            role == GlassRole.Chrome -> scheme.surfaceContainer
            role == GlassRole.Control -> scheme.surfaceContainerHigh
            role == GlassRole.Frost -> scheme.surfaceContainer
            else -> scheme.surface
        }
        val resolvedContent = when {
            (tint != null && selected) -> if (tint.luminance() < 0.45f) Color.White else Color(0xFF1F2421)
            tint != null -> tint
            selected || role == GlassRole.Accent -> scheme.onPrimary
            else -> scheme.onSurface
        }
        val elevation = when (role) {
            GlassRole.HeroCard, GlassRole.Chrome -> 2.dp
            GlassRole.Card -> 1.dp
            else -> 0.dp
        }
        val border = if (role == GlassRole.Card || role == GlassRole.Control) {
            BorderStroke(0.75.dp, scheme.outlineVariant.copy(alpha = 0.7f))
        } else {
            null
        }
        if (onClick != null) {
            Surface(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                color = resolvedBg,
                contentColor = resolvedContent,
                tonalElevation = elevation,
                shadowElevation = elevation,
                border = border,
            ) {
                Box(Modifier.padding(contentPadding), content = content)
            }
        } else {
            Surface(
                modifier = modifier,
                shape = shape,
                color = resolvedBg,
                contentColor = resolvedContent,
                tonalElevation = elevation,
                shadowElevation = elevation,
                border = border,
            ) {
                Box(Modifier.padding(contentPadding), content = content)
            }
        }
        return
    }

    val colors = LiquidGlassTheme.colors
    val effectiveStyle = when {
        containerColor != null -> GlassDefaults.frost().copy(
            surface = containerColor,
            fallbackSurface = containerColor,
            contentColor = if (containerColor.luminance() < 0.45f) Color.White else colors.content,
        )
        tint != null && selected -> GlassDefaults.tinted(
            color = tint,
            onColor = if (tint.luminance() < 0.45f) Color.White else Color(0xFF1F2421),
        )
        tint != null -> GlassDefaults.control().copy(
            surface = tint.copy(alpha = 0.16f),
            fallbackSurface = tint.copy(alpha = 0.20f),
            contentColor = colors.content,
        )
        selected -> GlassDefaults.accent()
        else -> when (role) {
            GlassRole.HeroCard -> GlassDefaults.card()
            GlassRole.Chrome -> GlassDefaults.chrome().copy(chromaticAberration = false)
            GlassRole.Control -> GlassDefaults.control()
            GlassRole.Accent -> GlassDefaults.accent()
            GlassRole.Frost -> GlassDefaults.frost()
            GlassRole.Card, GlassRole.Tinted -> GlassDefaults.frost()
        }
    }
    CompositionLocalProvider(LocalContentColor provides effectiveStyle.contentColor) {
        LiquidSurface(
            modifier = modifier,
            style = effectiveStyle,
            shape = shape,
            contentPadding = contentPadding,
            onClick = onClick,
            onClickLabel = onClickLabel,
            interactive = interactive || onClick != null,
            content = content,
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    shape: Shape = glassCardShape(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    containerColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        role = if (hero) GlassRole.HeroCard else GlassRole.Card,
        shape = shape,
        contentPadding = contentPadding,
        containerColor = containerColor,
        interactive = onClick != null,
        onClick = onClick,
        content = content,
    )
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color? = null,
    shape: Shape = glassPillShape(),
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        role = GlassRole.Control,
        shape = shape,
        tint = tint,
        selected = selected,
        contentPadding = contentPadding,
        interactive = onClick != null,
        onClick = onClick,
        content = content,
    )
}

@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    tint: Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val fg = when {
        selected && tint != null -> if (tint.luminance() < 0.45f) Color.White else Color(0xFF1F2421)
        selected -> scheme.onPrimary
        tint != null -> tint
        else -> scheme.onSurface
    }
    GlassSurface(
        modifier = modifier,
        role = GlassRole.Control,
        shape = glassPillShape(),
        tint = tint,
        selected = selected,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        onClick = onClick,
        interactive = true,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = fg, modifier = Modifier.size(16.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GlassDialogSurface(
    shape: Shape = glassCardShape(),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Box(content = content)
        }
        return
    }
    val colors = LiquidGlassTheme.colors
    DisableGlassSceneBackdrop {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            LiquidSurface(
                modifier = modifier,
                style = GlassDefaults.frost().copy(
                    fallbackSurface = colors.glassFillOpaque,
                    fallbackSheen = 0.42f,
                    highlightWidth = 0.dp,
                    shadowRadius = 22.dp,
                    shadowColor = colors.glassShadow,
                    contentColor = colors.content,
                ),
                shape = shape,
                contentPadding = PaddingValues(0.dp),
                interactive = false,
                content = content,
            )
        }
    }
}

@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    maxWidth: Dp = 520.dp,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
            shape = glassCardShape(),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
        )
        return
    }
    val colors = LiquidGlassTheme.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        DisableGlassSceneBackdrop {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlassDialogSurface(
                    shape = glassCardShape(),
                    modifier = Modifier
                        .widthIn(max = maxWidth)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    CompositionLocalProvider(LocalContentColor provides colors.content) {
                        Column(Modifier.fillMaxWidth().padding(22.dp)) {
                            if (title != null) {
                                CompositionLocalProvider(
                                    androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.content,
                                    ),
                                ) {
                                    Box(Modifier.fillMaxWidth()) { title() }
                                }
                                if (text != null) Spacer(Modifier.height(12.dp))
                            }
                            if (text != null) {
                                CompositionLocalProvider(
                                    androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.content,
                                    ),
                                ) {
                                    Box(Modifier.fillMaxWidth().heightIn(max = 540.dp)) { text() }
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
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
    }
}

@Composable
fun GlassSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val radius = LocalCornerRoundness.current.coerceIn(12.dp, 32.dp)
    val sheetShape = RoundedCornerShape(topStart = radius, topEnd = radius)
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val innerContent: @Composable () -> Unit = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.26f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                GlassDialogSurface(
                    shape = sheetShape,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    val onSurface = MaterialTheme.colorScheme.onSurface
                    CompositionLocalProvider(LocalContentColor provides onSurface) {
                        Column(
                            Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .width(38.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                            )
                            Spacer(Modifier.height(10.dp))
                            content()
                        }
                    }
                }
            }
        }
        if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
            innerContent()
        } else {
            DisableGlassSceneBackdrop { innerContent() }
        }
    }
}

@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    } else {
        LiquidGlassToggle(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    }
}

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = modifier,
        )
    } else {
        LiquidGlassSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = modifier,
            steps = steps,
        )
    }
}

@Composable
fun GlassSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        role = GlassRole.Frost,
        shape = glassControlShape(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        interactive = false,
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(valueText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            GlassSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
            )
        }
    }
}

sealed interface GlassMenuEntry

@Stable
data class GlassMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val leadingContent: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit,
) : GlassMenuEntry

object GlassMenuDivider : GlassMenuEntry

private fun GlassMenuEntry.toLiquidEntry(): LiquidMenuEntry = when (this) {
    is GlassMenuItem -> LiquidMenuItem(
        label = label,
        icon = icon,
        destructive = destructive,
        enabled = enabled,
        leadingContent = leadingContent,
        onClick = onClick,
    )
    GlassMenuDivider -> LiquidMenuDivider
}

@Composable
fun GlassMenuButton(
    contentPadding: PaddingValues,
    buttonContent: @Composable RowScope.() -> Unit,
    items: List<GlassMenuEntry>,
    modifier: Modifier = Modifier,
    role: GlassRole = GlassRole.Control,
    tint: Color? = null,
    selected: Boolean = false,
    shape: Shape = glassPillShape(),
    menuWidth: Dp = 256.dp,
    contentDescription: String = "Menu",
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        var expanded by remember { mutableStateOf(false) }
        val scheme = MaterialTheme.colorScheme
        val containerColor = when {
            tint != null && selected -> tint
            tint != null -> scheme.surfaceContainerHigh
            role == GlassRole.Accent -> scheme.primary
            role == GlassRole.Frost -> scheme.surfaceContainer
            else -> scheme.surfaceContainerHigh
        }
        val contentColor = when {
            tint != null && selected -> if (tint.luminance() < 0.45f) Color.White else Color(0xFF1F2421)
            tint != null -> tint
            role == GlassRole.Accent -> scheme.onPrimary
            else -> scheme.onSurface
        }
        val border = if (role == GlassRole.Accent || (tint != null && selected)) null else BorderStroke(0.75.dp, scheme.outlineVariant)
        Box(modifier) {
            Surface(
                onClick = { expanded = true },
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                border = border,
            ) {
                Row(
                    Modifier.padding(contentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = buttonContent,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(menuWidth),
            ) {
                items.forEach { entry ->
                    when (entry) {
                        is GlassMenuItem -> DropdownMenuItem(
                            text = {
                                Text(
                                    entry.label,
                                    color = if (entry.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = when {
                                entry.icon != null -> {
                                    {
                                        Icon(
                                            entry.icon,
                                            contentDescription = null,
                                            tint = if (entry.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                entry.leadingContent != null -> entry.leadingContent
                                else -> null
                            },
                            enabled = entry.enabled,
                            onClick = {
                                expanded = false
                                entry.onClick()
                            },
                        )
                        GlassMenuDivider -> HorizontalDivider()
                    }
                }
            }
        }
        return
    }

    val liquidEntries = remember(items) { items.map { it.toLiquidEntry() } }
    val colors = LiquidGlassTheme.colors
    val liquidStyle = when {
        tint != null && selected -> GlassDefaults.tinted(
            color = tint,
            onColor = if (tint.luminance() < 0.45f) Color.White else Color(0xFF1F2421),
        )
        tint != null -> GlassDefaults.control().copy(
            surface = tint.copy(alpha = 0.16f),
            fallbackSurface = tint.copy(alpha = 0.20f),
            contentColor = colors.content,
        )
        role == GlassRole.Accent -> GlassDefaults.accent()
        role == GlassRole.Frost -> GlassDefaults.frost()
        else -> GlassDefaults.control()
    }
    LiquidMenuButton(
        contentPadding = contentPadding,
        buttonContent = buttonContent,
        items = liquidEntries,
        modifier = modifier,
        style = liquidStyle,
        shape = shape,
        menuWidth = menuWidth,
        contentDescription = contentDescription,
    )
}

@Composable
fun GlassContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<GlassMenuEntry>,
    menuWidth: Dp = 250.dp,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.width(menuWidth),
        ) {
            items.forEach { entry ->
                when (entry) {
                    is GlassMenuItem -> DropdownMenuItem(
                        text = {
                            Text(
                                entry.label,
                                color = if (entry.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = when {
                            entry.icon != null -> {
                                {
                                    Icon(
                                        entry.icon,
                                        contentDescription = null,
                                        tint = if (entry.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            entry.leadingContent != null -> entry.leadingContent
                            else -> null
                        },
                        enabled = entry.enabled,
                        onClick = {
                            onDismissRequest()
                            entry.onClick()
                        },
                    )
                    GlassMenuDivider -> HorizontalDivider()
                }
            }
        }
        return
    }

    val liquidEntries = remember(items) { items.map { it.toLiquidEntry() } }
    val colors = LiquidGlassTheme.colors
    val palette = remember {
        listOf(
            GlassButtonPalette.Emerald,
            GlassButtonPalette.OceanBlue,
            GlassButtonPalette.GalaxyPurple,
            GlassButtonPalette.WarmAmber,
            GlassButtonPalette.SkyCyan,
            GlassButtonPalette.CoralRose,
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = glassCardShape(),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.width(menuWidth),
    ) {
        DisableGlassSceneBackdrop {
            GlassDialogSurface(shape = glassCardShape(), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp, horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    liquidEntries.forEachIndexed { index, entry ->
                        when (entry) {
                            is LiquidMenuItem -> {
                                val badgeTint = when {
                                    !entry.enabled -> colors.contentMuted.copy(alpha = 0.35f)
                                    entry.destructive -> colors.badge
                                    else -> palette[index % palette.size]
                                }
                                val fg = when {
                                    !entry.enabled -> colors.contentMuted.copy(alpha = 0.5f)
                                    entry.destructive -> colors.badge
                                    else -> colors.content
                                }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(glassControlShape())
                                        .clickable(enabled = entry.enabled) {
                                            onDismissRequest()
                                            entry.onClick()
                                        }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (entry.icon != null || entry.leadingContent != null) {
                                        Box(
                                            Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(badgeTint.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (entry.icon != null) {
                                                Icon(entry.icon, null, tint = badgeTint, modifier = Modifier.size(18.dp))
                                            } else {
                                                entry.leadingContent?.invoke()
                                            }
                                        }
                                    }
                                    Text(
                                        entry.label,
                                        color = fg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            LiquidMenuDivider -> HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = colors.content.copy(alpha = 0.10f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassChoiceRow(
    title: String,
    value: String,
    options: List<String>,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?>? = null,
) {
    val entries = remember(options, icons) {
        options.mapIndexed { idx, label ->
            GlassMenuItem(
                label = label,
                icon = icons?.getOrNull(idx),
                onClick = { onPick(idx) },
            )
        }
    }
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        GlassMenuButton(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            buttonContent = {
                Text(
                    value,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 170.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            },
            items = entries,
            modifier = Modifier.heightIn(min = 38.dp),
            shape = glassPillShape(),
            menuWidth = 240.dp,
            contentDescription = title,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        return
    }
    val colors = LiquidGlassTheme.colors
    LiquidSegmentedControl(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
        indicatorColor = colors.raisedFill,
        selectedTextColor = colors.content,
        textColor = colors.contentMuted,
    )
}

@Immutable
data class GlassTabItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val badge: Boolean = false,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassBottomBar(
    tabs: List<GlassTabItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReselect: (String) -> Unit = {},
    hideWhenImeVisible: Boolean = true,
    onMeasuredHeight: (Dp) -> Unit = {},
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.CLASSIC) {
        if (hideWhenImeVisible && WindowInsets.isImeVisible) return
        val density = LocalDensity.current
        val bottomInsets = WindowInsets.navigationBars.union(
            WindowInsets.displayCutout.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
        )
        NavigationBar(
            modifier = modifier.onSizeChanged { onMeasuredHeight(with(density) { it.height.toDp() }) },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            windowInsets = bottomInsets,
        ) {
            tabs.forEach { tab ->
                val selected = tab.key == selectedKey
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (selected) onReselect(tab.key) else onSelect(tab.key)
                    },
                    icon = {
                        if (tab.badge) {
                            BadgedBox(badge = { Badge() }) {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        } else {
                            Icon(tab.icon, contentDescription = tab.label)
                        }
                    },
                    label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        return
    }

    val liquidTabs = remember(tabs) {
        tabs.map { LiquidTab(key = it.key, label = it.label, icon = it.icon, badge = it.badge) }
    }
    LiquidTabBar(
        tabs = liquidTabs,
        selectedKey = selectedKey,
        onSelect = onSelect,
        onReselect = onReselect,
        labels = LiquidTabLabels.Stacked,
        hideWhenImeVisible = hideWhenImeVisible,
        onMeasuredHeight = onMeasuredHeight,
        modifier = modifier,
    )
}

@Composable
fun GlassContentBottomPadding(measuredBarHeight: Dp): Dp =
    if (measuredBarHeight > 0.dp) measuredBarHeight + 8.dp
    else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
