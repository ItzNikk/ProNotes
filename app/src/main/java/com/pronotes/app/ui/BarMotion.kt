package com.pronotes.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pronotes.app.R

/** A bundled, decorative Lottie loop. The animation never intercepts bar input. */
@Composable
internal fun BarMotion(modifier: Modifier = Modifier) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    if (LocalReduceMotion.current || LocalReduceEffects.current || LocalClassicAppearance.current ||
        lifecycle != Lifecycle.State.RESUMED) return
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.bar_glow))
    Box(modifier.clearAndSetSemantics {}) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(12.dp).blur(4.dp),
        )
    }
}
