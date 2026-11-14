package io.github.ntvien.liquidglass.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import io.github.ntvien.liquidglass.GlassKind
import io.github.ntvien.liquidglass.GlassStyle
import io.github.ntvien.liquidglass.liquidGlass
import io.github.ntvien.liquidglass.rememberGlassInteraction

/**
 * A block of liquid glass: cards, glass buttons, floating actions.
 *
 * With [onClick] it is a button and gets the press effect (swell, tilt, glint). Content color is up to the caller;
 * [GlassStyle.contentColor] of the chosen [style] is the matching color.
 *
 * ```
 * LiquidSurface(onClick = { open() }, onClickLabel = "Open details") {
 *     BasicText("Today", style = TextStyle(color = GlassDefaults.card().contentColor))
 * }
 * ```
 *
 * @param style preset from [GlassDefaults] (default [GlassDefaults.card]).
 * @param shape default [GlassDefaults.ContinuousCardShape] for real glass and [GlassDefaults.CardShape] for frost (lists).
 *   Use a [androidx.compose.foundation.shape.RoundedCornerShape] for repeated items: continuous corners are a path.
 * @param contentPadding padding inside the glass.
 * @param onClick makes the surface a button.
 * @param onClickLabel describes the click for accessibility services.
 */
@Composable
public fun LiquidSurface(
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassDefaults.card(),
    shape: Shape = if (style.kind == GlassKind.Frost) GlassDefaults.CardShape else GlassDefaults.ContinuousCardShape,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = if (onClick != null) rememberGlassInteraction() else null
    Box(
        modifier = modifier
            .liquidGlass(style = style, shape = shape, interaction = interaction)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = null, indication = null, role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content,
    )
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
