package com.pronotes.app.ui

import kotlinx.coroutines.launch

import android.os.Build
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
fun appCardShape(offsetDp: Float = 0f): RoundedCornerShape {
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

@androidx.annotation.RequiresApi(31)
private object Api31BlurHelper {
    fun createBlur(radiusPx: Float): androidx.compose.ui.graphics.RenderEffect =
        android.graphics.RenderEffect.createBlurEffect(
            radiusPx,
            radiusPx,
            android.graphics.Shader.TileMode.CLAMP,
        ).asComposeRenderEffect()
}

/** Applies native Android 12+ blur when active and Classic appearance is off. */
@Composable
fun Modifier.appBackdropBlur(enabled: Boolean, radiusDp: Dp = 14.dp): Modifier {
    val classic = LocalClassicAppearance.current
    if (!enabled || !LocalLiveBlur.current || classic || LocalReduceEffects.current || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    val radiusPx = with(LocalDensity.current) { radiusDp.toPx() }.coerceAtLeast(1f)
    val blur = remember(radiusPx) { Api31BlurHelper.createBlur(radiusPx) }
    return graphicsLayer { renderEffect = blur }
}

private fun readableSurfaceText(background: Color, preferred: Color): Color {
    if (contrastRatio(background, preferred) >= 4.5f) return preferred
    return if (contrastRatio(background, Color.White) >= contrastRatio(background, Color.Black)) Color.White else Color.Black
}

private fun resolveSurfaceColors(
    role: SurfaceRole,
    scheme: androidx.compose.material3.ColorScheme,
    classicAppearance: Boolean,
    containerColor: Color?,
    contentColor: Color?,
): Pair<Color, Color> {
    val base = if (role == SurfaceRole.Chrome) Color.Black else containerColor ?: when (role) {
        SurfaceRole.Chrome, SurfaceRole.Popover -> scheme.surfaceContainerHigh
        SurfaceRole.Frost, SurfaceRole.Sheet -> scheme.surface
        SurfaceRole.Control, SurfaceRole.Capsule -> scheme.surfaceVariant
    }
    val bg = when {
        role == SurfaceRole.Chrome -> Color.Black
        classicAppearance -> base.copy(alpha = 1f)
        role == SurfaceRole.Chrome || role == SurfaceRole.Popover -> {
            val a = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.90f else 0.96f
            if (base.alpha > a) base.copy(alpha = a) else base
        }
        else -> base
    }
    val fg = if (role == SurfaceRole.Chrome) Color(0xFFF1F5F9) else contentColor ?: readableSurfaceText(bg, scheme.onSurface)
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
        role == SurfaceRole.Chrome || role == SurfaceRole.Popover || role == SurfaceRole.Sheet -> scheme.outlineVariant
        else -> null
    }
    val stroke = resolvedBorderColor?.let { BorderStroke(0.85.dp, it) }

    val interactionSource = remember { MutableInteractionSource() }
    val pressModifier = if (onClick != null) {
        Modifier.tactilePressScale(interactionSource, enabled = enabled, pressedScale = 0.965f)
    } else Modifier
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick,
        )
    } else Modifier

    Surface(
        modifier = modifier
            .then(pressModifier)
            .then(if (effectiveElevation > 0.dp) Modifier.shadow(effectiveElevation, shape, clip = false) else Modifier)
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = bg,
        contentColor = fg,
        border = stroke,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            Box {
                Box(Modifier.padding(contentPadding), content = content)
            }
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
    val fg = readableSurfaceText(bg, scheme.onSurface)
    val interactionSource = remember { MutableInteractionSource() }
    val cardMod = modifier
        .then(if (onClick != null) Modifier.tactilePressScale(interactionSource, pressedScale = 0.985f) else Modifier)
        .then(if (!reduceEffects) Modifier.shadow(if (hero) 4.dp else 1.dp, shape, clip = false) else Modifier)
        .clip(shape)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                )
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
    val reduceMotion = LocalReduceMotion.current
    val perfMode = LocalPerformanceMode.current
    val motionStyle = LocalMotionStyle.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val enterProgress = remember { androidx.compose.animation.core.Animatable(if (reduceMotion) 1f else 0f) }
        LaunchedEffect(reduceMotion) {
            if (reduceMotion) enterProgress.snapTo(1f)
            else enterProgress.animateTo(1f, ProMotion.sheetSpring(false, perfMode, motionStyle))
        }
        LiveDialogBackdrop()
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val p = enterProgress.value.coerceIn(0f, 1.04f)
                        alpha = p.coerceIn(0f, 1f)
                        val s = 0.94f + 0.06f * p
                        scaleX = s
                        scaleY = s
                        translationY = (1f - p) * 18f
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
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
    val perfMode = LocalPerformanceMode.current
    val motionStyle = LocalMotionStyle.current
    val activeTint = tint ?: scheme.primary
    val bg by animateColorAsState(
        targetValue = when {
            selected -> activeTint
            tint != null -> tint.copy(alpha = if (dark) 0.22f else 0.14f)
            else -> scheme.surfaceVariant
        },
        animationSpec = ProMotion.quickTween(reduceMotion, 150),
        label = "pillBg",
    )
    val fg = when {
        selected -> if (contrastRatio(activeTint, Color(0xFF0F172A)) > contrastRatio(activeTint, Color.White)) Color(0xFF0F172A) else Color.White
        tint != null && !dark -> lerp(tint, scheme.onSurface, 0.35f)
        tint != null && dark -> lerp(tint, Color.White, 0.52f)
        else -> scheme.onSurface
    }
    val shape = appPillShape()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (pressed && onClick != null && !reduceMotion) 0.955f else 1f,
        animationSpec = ProMotion.snappySpring(reduceMotion, perfMode, motionStyle),
        label = "pillScale",
    )
    val borderTint by animateColorAsState(
        targetValue = when {
            selected -> Color.White.copy(alpha = if (dark) 0.24f else 0.28f)
            tint != null -> tint.copy(alpha = if (dark) 0.34f else 0.24f)
            else -> scheme.outlineVariant.copy(alpha = if (dark) 0.55f else 0.72f)
        },
        animationSpec = ProMotion.quickTween(reduceMotion, 150),
        label = "pillBorder",
    )
    Row(
        modifier = modifier
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
            }
            .then(
                if (selected && !LocalReduceEffects.current) {
                    Modifier.shadow(3.dp, shape, ambientColor = activeTint.copy(alpha = 0.28f), spotColor = activeTint.copy(alpha = 0.28f))
                } else Modifier,
            )
            .clip(shape)
            .background(bg)
            .border(0.9.dp, borderTint, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        role = Role.Button,
                        onClick = onClick,
                    )
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

/**
 * High-performance omnidirectional multi-colour gradient text rendered purely in the GPU draw phase
 * (zero per-frame text relayout or Compose recomposition). Colors flow continuously from top, bottom,
 * left, right, and every diagonal via a rotating 2D Lissajous gradient + counter-orbiting color washes.
 * Respects [LocalReduceMotion] and lifecycle state.
 */
@Composable
fun AnimatedGradientText(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
    modifier: Modifier = Modifier,
    colors: List<Color>? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    durationMs: Int = 2200,
    pulseAlpha: Boolean = true,
) {
    val reduceMotion = LocalReduceMotion.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState = lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val animate = !reduceMotion && lifecycleState.value == androidx.lifecycle.Lifecycle.State.RESUMED

    val palette = remember(colors, dark) {
        if (!colors.isNullOrEmpty()) {
            if (colors.size >= 2) colors + colors.first() else listOf(colors.first(), colors.first())
        } else if (dark) {
            listOf(
                Color(0xFFFF4D8D), // Vivid rose-magenta
                Color(0xFFFF8A3D), // Warm tangerine
                Color(0xFFFACC15), // Luminous gold
                Color(0xFF34D399), // Radiant emerald
                Color(0xFF22D3EE), // Bright cyan
                Color(0xFF60A5FA), // Electric sky blue
                Color(0xFFA855F7), // Vivid violet
                Color(0xFFFF4D8D), // Seamless wrap
            )
        } else {
            listOf(
                Color(0xFFE11D48), // Deep crimson-rose
                Color(0xFFEA580C), // Vivid orange
                Color(0xFFD97706), // Rich amber
                Color(0xFF059669), // Rich emerald
                Color(0xFF0891B2), // Vivid cyan-teal
                Color(0xFF2563EB), // Electric cobalt
                Color(0xFF7C3AED), // Deep violet
                Color(0xFFE11D48), // Seamless wrap
            )
        }
    }

    val transition = if (animate) rememberInfiniteTransition(label = "animGradOmni") else null
    val cycleMs = (durationMs * 3).coerceAtLeast(5400)
    // Primary 360-degree directional sweep phase (sweeps left, top, right, bottom & every diagonal).
    val anglePhaseState = transition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "animGradAngle",
    )
    // Secondary incommensurate drift phase so top/bottom & diagonal color washes never feel repetitive.
    val driftPhaseState = transition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (cycleMs * 1.45f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "animGradDrift",
    )
    val pulseState = if (transition != null && pulseAlpha) {
        transition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "animGradPulse",
        )
    } else null

    val solidStyle = remember(style) { style.copy(color = Color.White) }

    Text(
        text = text,
        style = solidStyle,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = pulseState?.value ?: 1f
            }
            .drawWithCache {
                val w = size.width.coerceAtLeast(160f)
                val h = size.height.coerceAtLeast(28f)
                val field = maxOf(w, h) * 1.85f
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                // Pre-cached omnidirectional base field (rotated + 2D-translated purely on GPU).
                val baseBrush = Brush.linearGradient(
                    colors = palette,
                    start = Offset(-field, -field * 0.45f),
                    end = Offset(field, field * 0.45f),
                    tileMode = TileMode.Mirror,
                )
                val orbRadius = maxOf(w * 0.65f, h * 2.4f)
                val topWashColor = palette[palette.size / 3]
                val bottomWashColor = palette[(palette.size * 2) / 3]
                val topOrbBrush = Brush.radialGradient(
                    colors = listOf(topWashColor.copy(alpha = 0.72f), topWashColor.copy(alpha = 0.24f), Color.Transparent),
                    center = Offset.Zero,
                    radius = orbRadius,
                )
                val bottomOrbBrush = Brush.radialGradient(
                    colors = listOf(bottomWashColor.copy(alpha = 0.68f), bottomWashColor.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset.Zero,
                    radius = orbRadius,
                )
                onDrawWithContent {
                    drawContent()
                    val t1 = anglePhaseState?.value ?: 0f
                    val t2 = driftPhaseState?.value ?: 0f
                    val rad1 = (t1 * 2.0 * PI)
                    val rad2 = (t2 * 2.0 * PI)
                    val angleDeg = t1 * 360f
                    val shiftX = (cos(rad1) * w * 0.55f + sin(rad2) * w * 0.35f).toFloat()
                    val shiftY = (sin(rad1) * h * 1.6f + cos(rad2) * h * 1.1f).toFloat()

                    // 1. Rotating & 2D-drifting full-spectrum base fills the text glyphs cleanly.
                    withTransform({
                        rotate(degrees = angleDeg, pivot = Offset(cx, cy))
                        translate(left = shiftX, top = shiftY)
                    }) {
                        drawRect(
                            brush = baseBrush,
                            topLeft = Offset(cx - field, cy - field),
                            size = androidx.compose.ui.geometry.Size(field * 2f, field * 2f),
                            blendMode = BlendMode.SrcIn,
                        )
                    }

                    // 2. Top-to-bottom & corner-sweeping radial color wash (clipped strictly to text glyphs via SrcAtop).
                    val orb1X = cx + (cos(rad2) * w * 0.62f).toFloat()
                    val orb1Y = cy + (sin(rad1 * 2.0) * h * 1.75f).toFloat()
                    withTransform({ translate(left = orb1X, top = orb1Y) }) {
                        drawCircle(
                            brush = topOrbBrush,
                            radius = orbRadius,
                            center = Offset.Zero,
                            blendMode = BlendMode.SrcAtop,
                        )
                    }

                    // 3. Opposing bottom-to-top & reverse-diagonal color wash.
                    val orb2X = cx - (sin(rad1) * w * 0.62f).toFloat()
                    val orb2Y = cy - (cos(rad2 * 2.0) * h * 1.75f).toFloat()
                    withTransform({ translate(left = orb2X, top = orb2Y) }) {
                        drawCircle(
                            brush = bottomOrbBrush,
                            radius = orbRadius,
                            center = Offset.Zero,
                            blendMode = BlendMode.SrcAtop,
                        )
                    }
                }
            },
    )
}

/**
 * Animated sweeping multi-colour gradient placeholder text for search and prompt inputs.
 */
@Composable
fun AnimatedGradientSearchPlaceholder(
    text: String = "What are you looking for?",
    modifier: Modifier = Modifier,
) {
    AnimatedGradientText(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        durationMs = 2200,
        pulseAlpha = true,
    )
}

/**
 * Soft progressive top scroll blur powered by Haze (hardware-accelerated RenderEffect progressive mask)
 * with a gentle, restrained cosine-falloff soft glow beneath the top bar.
 */
@Composable
fun TopScrollCloudyBackdrop(
    scrolled: Boolean,
    enabled: Boolean,
    height: Dp = 76.dp,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    aurora: Boolean = false,
) {
    val classic = LocalClassicAppearance.current
    val reduceEffects = LocalReduceEffects.current
    val liveBlur = LocalLiveBlur.current
    val active = enabled && scrolled && !classic && !reduceEffects && liveBlur
    val blurAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(240),
        label = "hazeTopAlpha",
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (scrolled) (if (active && hazeState != null) 0.22f else 0.88f) else 0f,
        animationSpec = tween(240),
        label = "topScrimAlpha",
    )
    if (scrimAlpha <= 0.01f && blurAlpha <= 0.01f) return
    val surface = MaterialTheme.colorScheme.surface
    val subtleAccent = MaterialTheme.colorScheme.primary
    val subtleGlow = aurora && enabled && scrolled && !classic && !reduceEffects
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (active && hazeState != null) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeMaterials.thin(surface),
                    ) {
                        progressive = HazeProgressive.verticalGradient(
                            startIntensity = 1f,
                            endIntensity = 0f,
                        )
                    }
                } else Modifier,
            )
            .background(
                Brush.verticalGradient(
                    0.0f to surface.copy(alpha = scrimAlpha),
                    0.30f to surface.copy(alpha = scrimAlpha * 0.65f),
                    0.62f to surface.copy(alpha = scrimAlpha * 0.28f),
                    0.85f to surface.copy(alpha = scrimAlpha * 0.08f),
                    1.0f to Color.Transparent,
                ),
            )
            .then(
                if (subtleGlow) {
                    Modifier.background(
                        Brush.verticalGradient(
                            0.0f to subtleAccent.copy(alpha = 0.09f),
                            0.45f to subtleAccent.copy(alpha = 0.035f),
                            1.0f to Color.Transparent,
                        ),
                    )
                } else Modifier,
            ),
    )
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
    if (options.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val track = scheme.surfaceVariant
    val pillBg = scheme.surfaceContainerHigh
    val shape = appPillShape()
    val reduceMotion = LocalReduceMotion.current
    val perfMode = LocalPerformanceMode.current
    val motionStyle = LocalMotionStyle.current
    val clampedIndex = selectedIndex.coerceIn(0, options.lastIndex)
    val animatedIndex by animateFloatAsState(
        targetValue = clampedIndex.toFloat(),
        animationSpec = ProMotion.snappySpring(reduceMotion, perfMode, motionStyle),
        label = "segThumb",
    )
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(track)
            .selectableGroup()
            .padding(3.dp),
    ) {
        val count = options.size
        val gap = 3.dp
        val segWidth = ((maxWidth - gap * (count - 1)) / count).coerceAtLeast(1.dp)
        val density = LocalDensity.current
        // Smooth GPU-sliding active pill indicator
        Box(
            modifier = Modifier
                .width(segWidth)
                .height(44.dp)
                .graphicsLayer {
                    val stepPx = with(density) { (segWidth + gap).toPx() }
                    translationX = animatedIndex * stepPx
                }
                .clip(shape)
                .background(pillBg),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            options.forEachIndexed { i, label ->
                val sel = i == clampedIndex
                val textColor by animateColorAsState(
                    targetValue = if (sel) scheme.onSurface else scheme.onSurfaceVariant,
                    animationSpec = ProMotion.quickTween(reduceMotion, 130),
                    label = "segText$i",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .selectable(selected = sel, role = Role.Tab, onClick = { onSelect(i) })
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
    val iconVector: ImageVector? = null,
    val onClick: () -> Unit,
)

private val ContextMenuShape = RoundedCornerShape(16.dp)
private val ContextMenuRowShape = RoundedCornerShape(10.dp)

/**
 * Zero-jank spring-animated context menu. Uses a fixed-width Popup without Material3's
 * IntrinsicSize.Max double-measurement pass so opening dropdowns is instantaneous at 120Hz.
 */
@Composable
fun AppContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded
    if (!expandedState.currentState && !expandedState.targetState) return

    val scheme = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val perfMode = LocalPerformanceMode.current
    val motionStyle = LocalMotionStyle.current
    val density = LocalDensity.current
    val yOffsetPx = remember(density) { with(density) { 46.dp.roundToPx() } }

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, yOffsetPx),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visibleState = expandedState,
            enter = if (reduceMotion) fadeIn(snap()) else {
                fadeIn(tween(110)) + scaleIn(
                    initialScale = 0.92f,
                    transformOrigin = TransformOrigin(0.95f, 0f),
                    animationSpec = ProMotion.snappySpring(reduceMotion, perfMode, motionStyle),
                )
            },
            exit = if (reduceMotion) fadeOut(snap()) else {
                fadeOut(tween(80)) + scaleOut(
                    targetScale = 0.95f,
                    transformOrigin = TransformOrigin(0.95f, 0f),
                    animationSpec = tween(80),
                )
            },
        ) {
            Surface(
                shape = ContextMenuShape,
                color = scheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.42f)),
                modifier = modifier
                    .width(228.dp)
                    .heightIn(max = 420.dp),
            ) {
                var actionFired by remember(expanded) { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 6.dp, horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items.forEach { item ->
                        val color = when {
                            !item.enabled -> scheme.onSurface.copy(alpha = 0.38f)
                            item.destructive -> scheme.error
                            item.selected -> ProButtonPalette.ElectricBlue
                            else -> scheme.onSurface
                        }
                        val rowInteraction = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp)
                                .tactilePressScale(rowInteraction, enabled = item.enabled, pressedScale = 0.975f)
                                .clip(ContextMenuRowShape)
                                .background(if (item.selected) ProButtonPalette.ElectricBlue.copy(alpha = 0.10f) else Color.Transparent)
                                .clickable(
                                    interactionSource = rowInteraction,
                                    indication = androidx.compose.foundation.LocalIndication.current,
                                    enabled = item.enabled,
                                ) {
                                    if (!actionFired) {
                                        actionFired = true
                                        onDismissRequest()
                                        item.onClick()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            when {
                                item.iconVector != null -> {
                                    Icon(
                                        imageVector = item.iconVector,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                item.icon != null -> {
                                    CompositionLocalProvider(LocalContentColor provides color) {
                                        item.icon.invoke()
                                    }
                                }
                            }
                            Text(
                                item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
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
                    }
                }
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
    var lastDismissMs by remember { mutableStateOf(0L) }
    Box(modifier = modifier) {
        AppSurface(
            role = role,
            shape = shape,
            containerColor = containerColor,
            contentPadding = contentPadding,
            onClick = {
                val now = android.os.SystemClock.uptimeMillis()
                if (expanded) {
                    expanded = false
                    lastDismissMs = now
                } else if (now - lastDismissMs > 180L) {
                    expanded = true
                }
            },
            onClickLabel = contentDescription,
            content = buttonContent,
        )
        AppContextMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                lastDismissMs = android.os.SystemClock.uptimeMillis()
            },
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
fun AppNavigationBar(
    tabs: List<AppTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    applyBottomInset: Boolean = false,
) {
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = if (applyBottomInset) navPad else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppSurface(
            role = SurfaceRole.Chrome,
            shape = appPillShape(),
            shadowElevation = 10.dp,
            contentPadding = PaddingValues(6.dp),
        ) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { idx, tab ->
                    val accent = listOf(ProButtonPalette.ElectricBlue, ProButtonPalette.MintTeal, ProButtonPalette.Violet, ProButtonPalette.WarmAmber, MaterialTheme.colorScheme.error)[idx % 5]
                    AppPill(
                        selected = idx == selectedIndex,
                        tint = accent,
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
