package com.pronotes.app.ui

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

@Composable
internal fun LiveDialogBackdrop() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val blur = LocalLiveBlur.current && !LocalClassicAppearance.current && !LocalReduceEffects.current
    val radius = with(LocalDensity.current) { 18.dp.roundToPx() }
    DisposableEffect(window, blur, radius) {
        val original = window.attributes
        val originalDim = original.dimAmount
        val originalFlags = original.flags
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(.38f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blur) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { setBlurBehindRadius(radius) }
        }
        onDispose {
            window.setDimAmount(originalDim)
            val effects = WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) WindowManager.LayoutParams.FLAG_BLUR_BEHIND else 0)
            window.setFlags(originalFlags, effects)
        }
    }
}
