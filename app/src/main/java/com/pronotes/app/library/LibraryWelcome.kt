package com.pronotes.app.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.R
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.LocalClassicAppearance
import com.pronotes.app.ui.LocalReduceEffects
import com.pronotes.app.ui.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun LibraryWorkspace(documents: Boolean, drag: DragController, content: @Composable LookaheadScope.() -> Unit) {
    val colors = if (documents) com.pronotes.app.ui.LocalDocumentsColorScheme.current else MaterialTheme.colorScheme
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalDragController provides drag) {
            LookaheadScope { content() }
        }
    }
}

@Composable
internal fun LibraryGradient(modifier: Modifier, paused: Boolean) {
    val effects = !LocalClassicAppearance.current && !LocalReduceEffects.current
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val phase = if (effects && !paused && !LocalReduceMotion.current && lifecycleState.value == Lifecycle.State.RESUMED) {
        rememberInfiniteTransition(label = "Library background").animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
            label = "Gradient orbit",
        )
    } else null
    Box(modifier.graphicsLayer { clip = true }.drawWithCache {
        val radius = (size.minDimension * 0.66f).coerceAtLeast(1f)
        val blue = Color(0xFF385BFF)
        val violet = Color(0xFFB12BE8)
        val primaryGlow = Brush.radialGradient(
            listOf(blue.copy(alpha = 0.86f), blue.copy(alpha = 0.29f), Color.Transparent),
            center = Offset.Zero,
            radius = radius,
        )
        val secondaryGlow = Brush.radialGradient(
            listOf(violet.copy(alpha = 0.74f), violet.copy(alpha = 0.24f), Color.Transparent),
            center = Offset.Zero,
            radius = radius,
        )
        onDrawBehind {
            val angle = phase?.value ?: 0f
            val dx = cos(angle) * size.width * 0.20f
            val dy = sin(angle) * size.height * 0.16f
            val intensity = if (effects) 0.86f + sin(angle) * 0.14f else 0.55f
            translate(size.width * 0.24f + dx, size.height * 0.32f + dy) {
                drawCircle(primaryGlow, radius, center = Offset.Zero, alpha = intensity)
            }
            translate(size.width * 0.76f - dx, size.height * 0.73f - dy) {
                drawCircle(secondaryGlow, radius, center = Offset.Zero, alpha = if (effects) 0.86f - sin(angle) * 0.14f else 0.55f)
            }
        }
    })
}

private val guideSteps = listOf(
    R.string.library_guide_create_title to R.string.library_guide_create_body,
    R.string.library_guide_write_title to R.string.library_guide_write_body,
    R.string.library_guide_read_title to R.string.library_guide_read_body,
)

@Composable
internal fun LibraryGuide(onDismiss: () -> Unit, onCreate: () -> Unit) {
    val step = rememberSaveable { mutableIntStateOf(0) }
    val (title, body) = guideSteps[step.intValue]
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_guide_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (com.pronotes.app.BuildConfig.DEBUG) {
                    Text(
                        stringResource(R.string.library_guide_test_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.library_guide_progress, step.intValue + 1, guideSteps.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_guide_close)) }
        },
        confirmButton = {
            if (step.intValue == guideSteps.lastIndex) {
                Button(onClick = onCreate) { Text(stringResource(R.string.library_guide_create)) }
            } else {
                Button(onClick = { step.intValue++ }) { Text(stringResource(R.string.library_guide_next)) }
            }
        },
    )
}
