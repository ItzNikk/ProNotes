package com.pronotes.app.support

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pronotes.app.BuildConfig
import com.pronotes.app.ui.GradientTitle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object Support {
    val upi: String get() = BuildConfig.SUPPORT_UPI
    val payee: String get() = BuildConfig.SUPPORT_UPI_NAME
    val enabled: Boolean get() = upi.isNotBlank()
}

/** "Support Pro Notes": pay any amount with a UPI app, then a thank-you animation. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("99") }
    var thanks by remember { mutableStateOf<Boolean?>(null) }   // true = paid, false = not sure
    val pay = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val response = res.data?.getStringExtra("response") ?: res.data?.getStringExtra("Status") ?: ""
        thanks = when {
            response.contains("status=success", ignoreCase = true) || response.equals("success", true) -> true
            res.resultCode == Activity.RESULT_OK -> false
            else -> null
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            Spacer(Modifier.height(24.dp))
            BeatingHeart(Modifier.size(110.dp))
            Spacer(Modifier.height(24.dp))
            GradientTitle("Support Pro Notes", MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Text(
                "Pro Notes is free and made by one developer. If it helps you, a small tip keeps updates coming.",
                Modifier.widthIn(max = 460.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            if (!Support.enabled) {
                Text("Support payments are not set up in this build.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("49", "99", "199", "499").forEach { a ->
                    FilterChip(amount == a, { amount = a }, label = { Text("₹$a") }, shape = CircleShape)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                amount, { v -> amount = v.filter { it.isDigit() }.take(6) }, singleLine = true, shape = CircleShape,
                label = { Text("Amount (₹)") }, modifier = Modifier.widthIn(max = 240.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val uri = Uri.parse("upi://pay").buildUpon()
                        .appendQueryParameter("pa", Support.upi)
                        .appendQueryParameter("pn", Support.payee)
                        .appendQueryParameter("tn", "Support Pro Notes")
                        .appendQueryParameter("am", amount.ifBlank { "99" })
                        .appendQueryParameter("cu", "INR")
                        .build()
                    try {
                        pay.launch(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Pay with"))
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = CircleShape, modifier = Modifier.widthIn(min = 240.dp),
            ) { Text("Pay ₹${amount.ifBlank { "99" }} with UPI") }
            Spacer(Modifier.height(10.dp))
            Text("UPI: ${Support.upi}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        thanks?.let { paid -> ThankYou(paid) { thanks = null } }
    }
}

@Composable
private fun BeatingHeart(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "beat")
    val s by t.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Canvas(modifier.scale(s)) { drawPath(heartPath(size.width / 2, size.height / 2, size.minDimension * 0.46f), Color(0xFFE5484D)) }
}

private fun heartPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0..80) {
        val t = i / 80.0 * 2 * Math.PI
        val x = 16 * sin(t) * sin(t) * sin(t)
        val y = 13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)
        val px = cx + (x / 17.0 * r).toFloat(); val py = cy - (y / 17.0 * r).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

/** Hearts burst out and float up, then a thank-you note. */
@Composable
private fun ThankYou(paid: Boolean, onClose: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(2600, easing = LinearEasing)) }
    val hearts = remember {
        List(36) { Triple(Random.nextFloat() * 360f, 0.4f + Random.nextFloat() * 0.6f, listOf(0xFFE5484D, 0xFFFF7EB6, 0xFF7161E0, 0xFF3987E5, 0xFFFFB224)[it % 5]) }
    }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val p = progress.value
            hearts.forEach { (angle, speed, color) ->
                val a = Math.toRadians(angle.toDouble())
                val dist = size.minDimension * 0.45f * speed * p
                val x = size.width / 2 + (cos(a) * dist).toFloat()
                val y = size.height / 2 + (sin(a) * dist).toFloat() - size.height * 0.15f * p * p
                drawPath(heartPath(x, y, 18f + 14f * speed), Color(color).copy(alpha = (1f - p).coerceIn(0f, 1f)))
            }
            drawCircle(Color(0x22E5484D), size.minDimension * 0.25f * (0.6f + p), Offset(size.width / 2, size.height / 2))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(0.9f + 0.1f * progress.value.coerceAtMost(1f))) {
            BeatingHeart(Modifier.size(90.dp))
            Spacer(Modifier.height(16.dp))
            GradientTitle("Thank you!", MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                if (paid) "Your support means a lot. Enjoy Pro Notes." else "If your payment went through, thank you so much.",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
