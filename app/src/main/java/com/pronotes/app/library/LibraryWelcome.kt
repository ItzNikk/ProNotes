package com.pronotes.app.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pronotes.app.R
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.LocalMotionStyle
import com.pronotes.app.ui.LocalPerformanceMode
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ProMotion

@Composable
internal fun LibraryWorkspace(drag: DragController, content: @Composable LookaheadScope.() -> Unit) {
    CompositionLocalProvider(LocalDragController provides drag) {
        LookaheadScope { content() }
    }
}

private data class GuideStepSpec(
    val titleRes: Int,
    val bodyRes: Int,
    val icon: ImageVector,
    val accentStart: Color,
    val accentEnd: Color,
    val tag: String,
)

private val guideSteps = listOf(
    GuideStepSpec(
        titleRes = R.string.library_guide_create_title,
        bodyRes = R.string.library_guide_create_body,
        icon = Icons.Rounded.AutoStories,
        accentStart = ProButtonPalette.ElectricBlue,
        accentEnd = ProButtonPalette.SkyCyan,
        tag = "Workspace",
    ),
    GuideStepSpec(
        titleRes = R.string.library_guide_write_title,
        bodyRes = R.string.library_guide_write_body,
        icon = Icons.Rounded.Draw,
        accentStart = ProButtonPalette.Violet,
        accentEnd = ProButtonPalette.MagentaPink,
        tag = "Ink & Notes",
    ),
    GuideStepSpec(
        titleRes = R.string.library_guide_read_title,
        bodyRes = R.string.library_guide_read_body,
        icon = Icons.Rounded.MenuBook,
        accentStart = ProButtonPalette.Emerald,
        accentEnd = ProButtonPalette.ElectricBlue,
        tag = "Reading & Study",
    ),
)

@Composable
internal fun LibraryGuide(onDismiss: () -> Unit, onCreate: () -> Unit) {
    val step = rememberSaveable { mutableIntStateOf(0) }
    val reduceMotion = LocalReduceMotion.current
    val perfMode = LocalPerformanceMode.current
    val motionStyle = LocalMotionStyle.current
    val current = guideSteps[step.intValue]
    val accent by animateColorAsState(
        targetValue = current.accentStart,
        animationSpec = ProMotion.quickTween(reduceMotion, 220),
        label = "guideAccent",
    )

    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.library_guide_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f))
                        .border(0.8.dp, accent.copy(alpha = 0.30f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.library_guide_progress, step.intValue + 1, guideSteps.size),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AnimatedContent(
                    targetState = step.intValue,
                    transitionSpec = {
                        val forward = targetState >= initialState
                        val dir = if (forward) 1 else -1
                        if (reduceMotion) {
                            fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                        } else {
                            (slideInHorizontally(tween(240)) { it / 4 * dir } + fadeIn(tween(200))) togetherWith
                                (slideOutHorizontally(tween(190)) { -it / 4 * dir } + fadeOut(tween(150)))
                        }
                    },
                    label = "guideStepContent",
                ) { idx ->
                    val spec = guideSteps[idx]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(spec.accentStart, spec.accentEnd),
                                    ),
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp),
                            )
                        }
                        Text(
                            text = spec.tag.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = spec.accentStart,
                        )
                        Text(
                            text = stringResource(spec.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(spec.bodyRes),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                    }
                }

                // Connected morphing step indicator pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    guideSteps.indices.forEach { i ->
                        val active = i == step.intValue
                        val w by animateDpAsState(
                            targetValue = if (active) 24.dp else 7.dp,
                            animationSpec = ProMotion.snappySpring(reduceMotion, perfMode, motionStyle),
                            label = "guideDot$i",
                        )
                        val col by animateColorAsState(
                            targetValue = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            animationSpec = ProMotion.quickTween(reduceMotion, 160),
                            label = "guideDotCol$i",
                        )
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(w)
                                .clip(CircleShape)
                                .background(col),
                        )
                    }
                }
            }
        },
        dismissButton = {
            if (step.intValue > 0) {
                TextButton(onClick = { step.intValue-- }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Back")
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_guide_close)) }
            }
        },
        confirmButton = {
            if (step.intValue == guideSteps.lastIndex) {
                Button(
                    onClick = onCreate,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(stringResource(R.string.library_guide_create), fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { step.intValue++ },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(stringResource(R.string.library_guide_next), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(16.dp))
                }
            }
        },
    )
}
