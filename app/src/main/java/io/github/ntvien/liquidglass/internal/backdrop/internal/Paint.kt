/*
 * Copied from Kyant0/AndroidLiquidGlass "backdrop" 2.0.1 (Apache License 2.0, https://github.com/Kyant0/AndroidLiquidGlass).
 * Modified for liquid-glass: package renamed, declarations made internal, performance changes listed in NOTICE,
 * deprecated asFrameworkPaint() replaced by nativePaint.
 */
package io.github.ntvien.liquidglass.internal.backdrop.internal

import android.graphics.BlurMaskFilter
import io.github.ntvien.liquidglass.internal.backdrop.asAndroidRuntimeShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativePaint
import io.github.ntvien.liquidglass.internal.backdrop.RuntimeShader

internal fun Paint.blur(radius: Float) {
    this.nativePaint.maskFilter =
        if (radius > 0f) BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        else null
}

internal fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?) {
    this.nativePaint.shader = runtimeShader?.asAndroidRuntimeShader()
}
