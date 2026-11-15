package io.github.ntvien.liquidglass.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ntvien.liquidglass.GlassDefaults
import io.github.ntvien.liquidglass.GlassQuality
import io.github.ntvien.liquidglass.GlassStyle
import io.github.ntvien.liquidglass.LiquidGlassTheme
import io.github.ntvien.liquidglass.LocalInsideGlass
import io.github.ntvien.liquidglass.liquidGlass
import io.github.ntvien.liquidglass.rememberGlassInteraction

/**
 * A block of liquid glass: cards, glass buttons, floating actions.
 * Enforces one glass layer per stacked surface via [LocalInsideGlass].
 */
@Composable
public fun LiquidSurface(
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassDefaults.card(),
    shape: Shape = GlassDefaults.cardShape(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    interactive: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val quality = LiquidGlassTheme.quality
    val alreadyInsideGlass = LocalInsideGlass.current
    val interaction = if ((interactive || onClick != null) && quality != GlassQuality.Flat) rememberGlassInteraction() else null
    Box(
        modifier = modifier
            .liquidGlass(style = style, shape = shape, interaction = interaction)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = onClickLabel,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
    ) {
        if (alreadyInsideGlass || quality == GlassQuality.Flat) {
            content()
        } else {
            CompositionLocalProvider(LocalInsideGlass provides true) {
                content()
            }
        }
    }
}

/** Tinted vector icon without depending on Material. */
@Composable
internal fun GlassIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
