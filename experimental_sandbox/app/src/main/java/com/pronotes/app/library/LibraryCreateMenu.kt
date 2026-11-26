package com.pronotes.app.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.LocalMotionStyle
import com.pronotes.app.ui.LocalPerformanceMode
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProMotion
import com.pronotes.app.ui.appCardShape
import com.pronotes.app.ui.tactilePressScale

private val CreateMenuAccents = listOf(
    Color(0xFF60A5FA), // Create — vibrant sky blue
    Color(0xFFA78BFA), // Explore — luminous violet
    Color(0xFF34D399), // Organize — crisp emerald
    Color(0xFFFBBF24), // Import — warm amber
)

private val CreateMenuBadgeTints = CreateMenuAccents.map { it.copy(alpha = 0.14f) }
private val CreateMenuGroupTitles = listOf("CREATE", "EXPLORE", "ORGANIZE", "IMPORT")
private val CreateMenuRowShape = RoundedCornerShape(12.dp)

private fun groupForIndex(index: Int): Int = when {
    index < 4 -> 0
    index < 9 -> 1
    index < 11 -> 2
    else -> 3
}

private fun isGroupStart(index: Int): Boolean = index == 0 || index == 4 || index == 9 || index == 11

/**
 * Ultra-smooth library creation menu (+ button & spring popover).
 * Uses a fixed-width Popup instead of Material3 DropdownMenu to completely eliminate
 * IntrinsicSize.Max double-measurement passes and per-row CompositionLocal overhead.
 */
@Composable
internal fun LibraryCreateMenu(label: String, items: List<AppMenuItem>) {
    var expanded by remember { mutableStateOf(false) }
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded

    val reduceMotion = LocalReduceMotion.current
    val perfMode = LocalPerformanceMode.current
    val motionStyle = LocalMotionStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val plusRotation = animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = ProMotion.snappySpring(reduceMotion, perfMode, motionStyle),
        label = "createPlusSpin",
    )
    val cardShape = appCardShape()
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val yOffsetPx = remember(density) { with(density) { 54.dp.roundToPx() } }

    Box {
        Surface(
            onClick = { expanded = !expanded },
            interactionSource = interactionSource,
            shape = cardShape,
            color = Color(0xFF254DB5),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color(0xFF749AF0).copy(alpha = 0.55f)),
            shadowElevation = 4.dp,
            modifier = Modifier.tactilePressScale(interactionSource, pressedScale = 0.93f),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { rotationZ = plusRotation.value },
                )
            }
        }

        if (expandedState.currentState || expandedState.targetState) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, yOffsetPx),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = expandedState,
                    enter = if (reduceMotion) fadeIn(snap()) else {
                        fadeIn(tween(110)) + scaleIn(
                            initialScale = 0.91f,
                            transformOrigin = TransformOrigin(0.95f, 0f),
                            animationSpec = ProMotion.snappySpring(reduceMotion, perfMode, motionStyle),
                        )
                    },
                    exit = if (reduceMotion) fadeOut(snap()) else {
                        fadeOut(tween(80)) + scaleOut(
                            targetScale = 0.94f,
                            transformOrigin = TransformOrigin(0.95f, 0f),
                            animationSpec = tween(80),
                        )
                    },
                ) {
                    Surface(
                        shape = cardShape,
                        color = scheme.surfaceContainerHigh,
                        tonalElevation = 0.dp,
                        shadowElevation = 12.dp,
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.42f)),
                        modifier = Modifier
                            .width(292.dp)
                            .heightIn(max = 560.dp),
                    ) {
                        val badgeCornerPx = remember(density) { with(density) { 10.dp.toPx() } }
                        val dividerColor = scheme.onSurface.copy(alpha = 0.08f)
                        val disabledColor = scheme.onSurface.copy(alpha = 0.38f)
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                        ) {
                            Column(
                                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurface,
                                )
                                Text(
                                    "Notebooks, imports and study tools",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            items.forEachIndexed { index, item ->
                                val group = groupForIndex(index)
                                val accent = CreateMenuAccents[group]
                                val badgeTint = CreateMenuBadgeTints[group]
                                if (isGroupStart(index)) {
                                    if (index != 0) {
                                        HorizontalDivider(
                                            Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                            color = dividerColor,
                                        )
                                    }
                                    Text(
                                        CreateMenuGroupTitles[group],
                                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .padding(horizontal = 8.dp, vertical = 1.dp)
                                        .clip(CreateMenuRowShape)
                                        .clickable(enabled = item.enabled) {
                                            expanded = false
                                            item.onClick()
                                        }
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .size(32.dp)
                                            .drawBehind {
                                                drawRoundRect(
                                                    color = badgeTint,
                                                    cornerRadius = CornerRadius(badgeCornerPx, badgeCornerPx),
                                                )
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        val vec = item.iconVector
                                        when {
                                            vec != null -> Icon(
                                                imageVector = vec,
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(19.dp),
                                            )
                                            item.icon != null -> CompositionLocalProvider(LocalContentColor provides accent) {
                                                item.icon.invoke()
                                            }
                                            else -> Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        item.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (item.enabled) scheme.onSurface else disabledColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}
