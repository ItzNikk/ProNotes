# Glass engine (copied from Kyant backdrop 2.0.1)

Source: [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass), library
`io.github.kyant0:backdrop:2.0.1`, Copyright Kyant, Apache License 2.0 (full text in the repository `LICENSE`).
Package renamed `com.kyant.backdrop` → `io.github.ntvien.liquidglass.internal.backdrop`, expect/actual merged (Android
only), every declaration made `internal`: apps use the public API in `io.github.ntvien.liquidglass`, never this package.

## Changes from the original (measured on a Redmi Note 11 Pro 5G / Redmi Note 13, full glass)

1. `DrawBackdropModifier.kt`: the original kept the coordinates in a `neverEqualPolicy` state, so every relayout
   (screen transition, parent animation) re-recorded the backdrop of every glass element even when it had not moved.
   Now it redraws only when the position in the window (`positionInWindow`) changes.
2. `shadow/ShadowModifier.kt`, `highlight/HighlightModifier.kt`: the original re-recorded the shadow layer (blurred) and
   the highlight on every draw. Now they record only when size, shape or parameters change (`internal/RecordKey.kt`).
3. `RuntimeShaderCache.kt`: the original compiled the AGSL shaders (lens, highlight) for every glass element when it was
   attached. Now the whole app shares one shader per key. Safe because uniforms are set and used immediately on the main
   thread, and Skia copies uniforms when the effect is created (copy-on-write).

Upgrading the engine: copy the new release over this folder, rename the package, make declarations internal again, then
re-apply the three changes above (search for "liquid-glass change" and for usages of `RecordKey`).
