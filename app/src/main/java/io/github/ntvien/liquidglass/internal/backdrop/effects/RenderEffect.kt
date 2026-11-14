/*
 * Copied from Kyant0/AndroidLiquidGlass "backdrop" 2.0.1 (Apache License 2.0, https://github.com/Kyant0/AndroidLiquidGlass).
 * Modified for liquid-glass: package renamed, declarations made internal, performance changes listed in NOTICE.
 */
package io.github.ntvien.liquidglass.internal.backdrop.effects

import androidx.compose.ui.graphics.RenderEffect
import io.github.ntvien.liquidglass.internal.backdrop.BackdropEffectScope
import io.github.ntvien.liquidglass.internal.backdrop.RuntimeShader
import io.github.ntvien.liquidglass.internal.backdrop.internal.RuntimeShaderEffect
import io.github.ntvien.liquidglass.internal.backdrop.internal.chain
import io.github.ntvien.liquidglass.internal.backdrop.isRenderEffectSupported
import io.github.ntvien.liquidglass.internal.backdrop.isRuntimeShaderSupported
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts

internal fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (!isRenderEffectSupported()) return

    renderEffect = renderEffect.chain(effect)
}

@OptIn(ExperimentalContracts::class)
internal fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    @Language("AGSL") shaderString: String,
    uniformShaderName: String,
    block: RuntimeShader.() -> Unit
) {
    if (!isRuntimeShaderSupported()) return

    val effect =
        RuntimeShaderEffect(
            runtimeShader = obtainRuntimeShader(key, shaderString).apply(block),
            uniformShaderName = uniformShaderName
        )
    renderEffect = renderEffect.chain(effect)
}
