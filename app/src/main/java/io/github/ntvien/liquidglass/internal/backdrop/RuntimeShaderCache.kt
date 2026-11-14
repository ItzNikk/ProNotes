/*
 * Copied from Kyant0/AndroidLiquidGlass "backdrop" 2.0.1 (Apache License 2.0, https://github.com/Kyant0/AndroidLiquidGlass).
 * Modified for liquid-glass: package renamed, declarations made internal, performance changes listed in NOTICE.
 */
package io.github.ntvien.liquidglass.internal.backdrop

import android.os.Build
import androidx.annotation.RequiresApi
import org.intellij.lang.annotations.Language

internal sealed interface RuntimeShaderCache {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun obtainRuntimeShader(key: String, @Language("AGSL") string: String): RuntimeShader
}

/**
 * liquid-glass change: the original compiled the AGSL shaders (lens, highlight) for every glass element when it was
 * attached, ~1 ms per shader on the main thread, so switching to a screen with a dozen glass elements stuttered.
 * Now the whole app shares one shader per key. This is safe because every caller sets the uniforms and immediately
 * creates the RenderEffect / records the draw on the main thread, and Skia copies uniforms when the effect is created
 * (copy-on-write), so a later element changing the uniforms does not affect one already drawn.
 */
internal class RuntimeShaderCacheImpl : RuntimeShaderCache {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
        return sharedShaders.getOrPut(key) { RuntimeShader(string) }
    }

    /** Keeps the shared shaders; there is nothing per element to clear. */
    fun clear() = Unit

    private companion object {
        val sharedShaders = HashMap<String, RuntimeShader>()
    }
}
