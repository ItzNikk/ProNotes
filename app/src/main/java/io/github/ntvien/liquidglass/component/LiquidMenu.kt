package io.github.ntvien.liquidglass.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
 * A menu action.
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
    menuWidth: Dp = 250.dp,
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
    menuWidth: Dp = 250.dp,
    textStyle: TextStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
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
                Box(
                    Modifier
                        .width(menuWidth)
                        .heightIn(max = 420.dp)
                        .shadow(16.dp, MENU_SHAPE, ambientColor = colors.glassShadow, spotColor = colors.glassShadow)
                        .clip(MENU_SHAPE)
                        .background(colors.glassFillOpaque)
                        .background(
                            Brush.verticalGradient(
                                listOf(colors.glassHighlight.copy(alpha = 0.28f), Color.Transparent),
                            ),
                        )
                        .border(1.dp, colors.glassStroke, MENU_SHAPE)
                        .padding(vertical = MENU_PADDING),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        items.forEach { entry ->
                            when (entry) {
                                is LiquidMenuItem -> {
                                    val color = when {
                                        !entry.enabled -> colors.contentMuted.copy(alpha = 0.5f)
                                        entry.destructive -> colors.badge
                                        else -> colors.content
                                    }
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(ITEM_HEIGHT)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = entry.enabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                expanded = false
                                                entry.onClick()
                                            }
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        if (entry.icon != null) {
                                            GlassIcon(entry.icon, color, size = 20.dp)
                                        } else if (entry.leadingContent != null) {
                                            entry.leadingContent.invoke()
                                        }
                                        BasicText(
                                            entry.label,
                                            style = textStyle.copy(color = color),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                LiquidMenuDivider -> Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(DIVIDER_HEIGHT)
                                        .padding(horizontal = 12.dp, vertical = (DIVIDER_HEIGHT - 0.8.dp) / 2)
                                        .background(colors.content.copy(alpha = 0.12f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val MENU_SHAPE = RoundedCornerShape(22.dp)
private val MENU_PADDING = 6.dp
private val ITEM_HEIGHT = 44.dp
private val DIVIDER_HEIGHT = 9.dp
