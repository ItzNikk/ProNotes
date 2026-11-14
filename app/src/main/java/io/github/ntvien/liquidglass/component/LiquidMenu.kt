package io.github.ntvien.liquidglass.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.GlassStyle
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.LocalGlassBackdrops
import io.github.ntvien.liquidglass.liquidGlass
import io.github.ntvien.liquidglass.rememberGlassInteraction

/** One entry of [LiquidMenuButton]: a [LiquidMenuItem] or a [LiquidMenuDivider]. */
public sealed interface LiquidMenuEntry

/**
 * A menu action with optional icon, custom leading content, and destructive / accent tinting.
 */
@Stable
public class LiquidMenuItem(
    public val label: String,
    public val icon: ImageVector? = null,
    public val destructive: Boolean = false,
    public val enabled: Boolean = true,
    public val leadingContent: (@Composable () -> Unit)? = null,
    public val onClick: () -> Unit,
) : LiquidMenuEntry {
    public constructor(
        label: String,
        icon: @Composable () -> Unit,
        isDestructive: Boolean = false,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) : this(
        label = label,
        icon = null,
        destructive = isDestructive,
        enabled = enabled,
        leadingContent = icon,
        onClick = onClick,
    )

    public constructor(
        label: String,
        isDestructive: Boolean,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) : this(
        label = label,
        icon = null,
        destructive = isDestructive,
        enabled = enabled,
        leadingContent = null,
        onClick = onClick,
    )
}

/** A thin line between groups of [LiquidMenuItem]s. */
public object LiquidMenuDivider : LiquidMenuEntry

@Composable
public fun LiquidMenuButton(
    contentPadding: PaddingValues,
    buttonContent: @Composable RowScope.() -> Unit,
    items: List<LiquidMenuEntry>,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassDefaults.control(),
    shape: Shape = GlassDefaults.PillShape,
    menuWidth: Dp = 256.dp,
    contentDescription: String = "Menu",
) {
    LiquidMenuButton(
        items = items,
        contentDescription = contentDescription,
        modifier = modifier,
        style = style,
        shape = shape,
        menuWidth = menuWidth,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = buttonContent,
        )
    }
}

@Composable
public fun LiquidMenuButton(
    items: List<LiquidMenuEntry>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassDefaults.control(),
    shape: Shape = GlassDefaults.PillShape,
    menuWidth: Dp = 256.dp,
    textStyle: TextStyle = TextStyle(fontSize = 14.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    content: @Composable BoxScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val interaction = rememberGlassInteraction()
    val colors = LiquidGlassTheme.colors

    Box {
        Box(
            modifier = modifier
                .liquidGlass(style, shape, interaction)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = contentDescription,
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    expanded = true
                }
                .semantics {
                    role = Role.Button
                    this.contentDescription = contentDescription
                },
            contentAlignment = Alignment.Center,
            content = content,
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MENU_SHAPE,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            CompositionLocalProvider(LocalGlassBackdrops provides null) {
                var appeared by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { appeared = true }
                val menuScale by animateFloatAsState(
                    targetValue = if (appeared) 1f else 0.88f,
                    animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
                    label = "menuScale",
                )
                val menuAlpha by animateFloatAsState(
                    targetValue = if (appeared) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "menuAlpha",
                )
                Box(
                    Modifier
                        .width(menuWidth)
                        .heightIn(max = 440.dp)
                        .graphicsLayer {
                            scaleX = menuScale
                            scaleY = menuScale
                            alpha = menuAlpha
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                        .shadow(22.dp, MENU_SHAPE, ambientColor = colors.glassShadow, spotColor = colors.glassShadow)
                        .clip(MENU_SHAPE)
                        .background(colors.glassFillOpaque.copy(alpha = 0.88f))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    colors.glassHighlight.copy(alpha = 0.42f),
                                    colors.accent.copy(alpha = 0.08f),
                                    Color.Transparent,
                                ),
                                center = Offset(40f, 0f),
                                radius = 560f,
                            ),
                        )
                        .padding(vertical = MENU_PADDING, horizontal = 6.dp),
                ) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items.forEachIndexed { index, entry ->
                            when (entry) {
                                is LiquidMenuItem -> {
                                    MenuRowItem(
                                        entry = entry,
                                        index = index,
                                        textStyle = textStyle,
                                        onSelect = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            expanded = false
                                            entry.onClick()
                                        },
                                    )
                                }
                                LiquidMenuDivider -> Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(DIVIDER_HEIGHT)
                                        .padding(horizontal = 10.dp, vertical = (DIVIDER_HEIGHT - 0.8.dp) / 2)
                                        .background(colors.content.copy(alpha = 0.09f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRowItem(
    entry: LiquidMenuItem,
    index: Int,
    textStyle: TextStyle,
    onSelect: () -> Unit,
) {
    val colors = LiquidGlassTheme.colors
    var pressed by remember { mutableStateOf(false) }
    val scaleX by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 460f),
        label = "itemScaleX",
    )
    val scaleY by animateFloatAsState(
        targetValue = if (pressed) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 460f),
        label = "itemScaleY",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(stiffness = 500f),
        label = "itemGlow",
    )

    val palette = MENU_ACCENT_PALETTE[index % MENU_ACCENT_PALETTE.size]
    val badgeTint = when {
        !entry.enabled -> colors.contentMuted.copy(alpha = 0.35f)
        entry.destructive -> colors.badge
        else -> palette
    }
    val labelColor = when {
        !entry.enabled -> colors.contentMuted.copy(alpha = 0.5f)
        entry.destructive -> colors.badge
        else -> colors.content
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT)
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (glowAlpha > 0.01f) {
                    badgeTint.copy(alpha = 0.16f * glowAlpha)
                } else {
                    Color.Transparent
                },
            )
            .pointerInput(entry.enabled) {
                if (!entry.enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        pressed = event.changes.any { it.pressed }
                    }
                }
            }
            .clickable(enabled = entry.enabled, onClick = onSelect)
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
                    GlassIcon(entry.icon, badgeTint, size = 18.dp)
                } else {
                    entry.leadingContent?.invoke()
                }
            }
        }
        BasicText(
            entry.label,
            style = textStyle.copy(color = labelColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private val MENU_ACCENT_PALETTE = listOf(
    Color(0xFF246A5B), // Botanical Emerald
    Color(0xFF3B82F6), // Ocean Blue
    Color(0xFF8B5CF6), // Galaxy Violet
    Color(0xFFD97706), // Warm Amber
    Color(0xFF0EA5E9), // Sky Cyan
    Color(0xFFEC4899), // Rose Pink
    Color(0xFF10B981), // Mint Leaf
)

private val MENU_SHAPE = RoundedCornerShape(24.dp)
private val MENU_PADDING = 8.dp
private val ITEM_HEIGHT = 44.dp
private val DIVIDER_HEIGHT = 9.dp
