package com.pronotes.app.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.pronotes.app.ui.LocalReduceMotion
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
private val CreateMenuBadgeShape = RoundedCornerShape(10.dp)

private fun groupForIndex(index: Int): Int = when {
    index < 4 -> 0
    index < 9 -> 1
    index < 11 -> 2
    else -> 3
}

private fun isGroupStart(index: Int): Boolean = index == 0 || index == 4 || index == 9 || index == 11

/**
 * Silky 120Hz library creation menu (+ button & connected spring popover).
 * Uses a top-right connector caret aligned straight to the rotating + button,
 * hardware-accelerated scale + slide + fade spring motion, and lightweight row rendering
 * so opening and closing feels instant and physically connected.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryCreateMenu(
    label: String,
    items: List<AppMenuItem>,
    onLongPress: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded

    val reduceMotion = LocalReduceMotion.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val plusRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.78f, stiffness = 540f),
        label = "createPlusSpin",
    )
    val btnScale by animateFloatAsState(
        targetValue = if (expanded) 0.94f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.80f, stiffness = 540f),
        label = "createBtnScale",
    )
    val btnBg by animateColorAsState(
        targetValue = if (expanded) Color(0xFF3464E0) else Color(0xFF254DB5),
        animationSpec = if (reduceMotion) snap() else tween(140),
        label = "createBtnBg",
    )
    val cardShape = appCardShape()
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val yOffsetPx = remember(density) { with(density) { 46.dp.roundToPx() } }

    Box {
        Surface(
            shape = cardShape,
            color = btnBg,
            contentColor = Color.White,
            border = BorderStroke(
                1.dp,
                if (expanded) Color(0xFFA5C2FF).copy(alpha = 0.85f) else Color(0xFF749AF0).copy(alpha = 0.55f),
            ),
            shadowElevation = if (expanded) 6.dp else 4.dp,
            modifier = Modifier
                .graphicsLayer { scaleX = btnScale; scaleY = btnScale }
                .tactilePressScale(interactionSource, pressedScale = 0.93f)
                .clip(cardShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { expanded = !expanded },
                    onLongClick = if (onLongPress != null) {
                        {
                            expanded = false
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                    } else null,
                ),
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
                        .graphicsLayer { rotationZ = plusRotation },
                )
            }
        }

        if (expandedState.currentState || expandedState.targetState) {
            val surfaceBg = scheme.surfaceContainerHigh
            val borderCol = scheme.outlineVariant.copy(alpha = 0.44f)
            val dividerColor = scheme.onSurface.copy(alpha = 0.08f)
            val disabledColor = scheme.onSurface.copy(alpha = 0.38f)

            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, yOffsetPx),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = expandedState,
                    enter = if (reduceMotion) fadeIn(snap()) else {
                        fadeIn(tween(130)) +
                            slideInVertically(
                                initialOffsetY = { -18 },
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 500f),
                            ) +
                            scaleIn(
                                initialScale = 0.86f,
                                transformOrigin = TransformOrigin(0.92f, 0f),
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 500f),
                            )
                    },
                    exit = if (reduceMotion) fadeOut(snap()) else {
                        fadeOut(tween(100)) +
                            slideOutVertically(
                                targetOffsetY = { -12 },
                                animationSpec = tween(100),
                            ) +
                            scaleOut(
                                targetScale = 0.92f,
                                transformOrigin = TransformOrigin(0.92f, 0f),
                                animationSpec = tween(100),
                            )
                    },
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.width(292.dp),
                    ) {
                        // Connected caret aligned directly under the center of the 48.dp + button
                        Canvas(
                            modifier = Modifier
                                .padding(end = 15.dp)
                                .size(width = 18.dp, height = 8.dp),
                        ) {
                            val tri = Path().apply {
                                moveTo(0f, size.height + 1f)
                                lineTo(size.width * 0.5f, 0f)
                                lineTo(size.width, size.height + 1f)
                                close()
                            }
                            drawPath(tri, surfaceBg)
                            val strokePath = Path().apply {
                                moveTo(0.5f, size.height)
                                lineTo(size.width * 0.5f, 0.5f)
                                lineTo(size.width - 0.5f, size.height)
                            }
                            drawPath(
                                strokePath,
                                borderCol,
                                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                        Surface(
                            shape = cardShape,
                            color = surfaceBg,
                            tonalElevation = 0.dp,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 540.dp),
                        ) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .verticalScroll(scrollState)
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
                                                .background(badgeTint, CreateMenuBadgeShape),
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
}
