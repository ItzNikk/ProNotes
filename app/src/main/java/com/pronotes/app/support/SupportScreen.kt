package com.pronotes.app.support

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pronotes.app.BuildConfig
import com.pronotes.app.ui.AppCard
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppSurface
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.GradientTitle
import com.pronotes.app.ui.LocalLiveBlur
import com.pronotes.app.ui.LocalReduceEffects
import com.pronotes.app.ui.LocalReduceMotion
import com.pronotes.app.ui.ProButtonPalette
import com.pronotes.app.ui.ScaleInAnimatedText
import com.pronotes.app.ui.SurfaceRole
import com.pronotes.app.ui.appPillShape
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object Support {
    val upi: String get() = BuildConfig.SUPPORT_UPI
    val payee: String get() = BuildConfig.SUPPORT_UPI_NAME
    const val messagesEnabled: Boolean = true
    val upiEnabled: Boolean get() = upi.isNotBlank()
    val enabled: Boolean get() = messagesEnabled
}

/** "Support Pro Notes": appreciation, feedback & sharing, plus optional UPI contribution. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    // Some UPI apps lock a pre-filled amount; this lets people type it inside their own UPI app instead.
    var chooseInApp by remember { mutableStateOf(false) }
    val amountValue = amount.toLongOrNull() ?: 0L
    val canPay = chooseInApp || amountValue in 1..100_000
    var thanks by remember { mutableStateOf<Boolean?>(null) }   // true = paid/sent, false = not sure
    var supportNote by remember { mutableStateOf("") }
    var showBugDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pay = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val response = res.data?.getStringExtra("response") ?: res.data?.getStringExtra("Status") ?: ""
        thanks = when {
            response.contains("status=success", ignoreCase = true) || response.equals("success", true) -> true
            res.resultCode == Activity.RESULT_OK -> false
            else -> null
        }
    }

    val supportHazeState = remember { HazeState() }
    val supportScheme = MaterialTheme.colorScheme
    val supportLiveBlur = LocalLiveBlur.current && !LocalReduceEffects.current

    Box(Modifier.fillMaxSize().background(supportScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .hazeSource(supportHazeState)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 74.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BeatingHeart(Modifier.size(60.dp))
            Spacer(Modifier.height(18.dp))
            ScaleInAnimatedText(
                "Support Pro Notes",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Pro Notes is free, ad-free, and built by one independent developer. If it helps your studies or work, sharing it or sending a note keeps updates shipping.",
                Modifier.widthIn(max = 500.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            AppCard(
                modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Send a cheer or feature idea", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = supportNote,
                        onValueChange = { supportNote = it },
                        minLines = 2,
                        maxLines = 4,
                        placeholder = { Text("Tell us what you love or what we should build next...") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = {
                                val text = supportNote.trim()
                                if (text.isNotEmpty()) {
                                    supportNote = ""
                                    thanks = true
                                    scope.launch {
                                        DeveloperFeedbackClient.sendDirect(
                                            category = "General Feedback",
                                            title = "Appreciation / Feature Idea",
                                            message = text,
                                            contact = "",
                                            deviceInfo = null,
                                        )
                                    }
                                    Toast.makeText(context, "Sent directly to the developer — thank you!", Toast.LENGTH_SHORT).show()
                                } else {
                                    thanks = true
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                        ) { Text(if (supportNote.isBlank()) "Send Applause" else "Send Free") }
                        OutlinedButton(
                            onClick = { showBugDialog = true },
                            shape = CircleShape,
                        ) { Text("Report Bug") }
                        OutlinedButton(
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_SUBJECT, "Pro Notes")
                                    .putExtra(Intent.EXTRA_TEXT, "Check out Pro Notes — study notebooks, handwritten diagrams, and writable split view.")
                                context.startActivity(Intent.createChooser(send, "Share Pro Notes"))
                            },
                            shape = CircleShape,
                        ) { Text("Share") }
                    }
                }
            }
            if (showBugDialog) {
                FeedbackDialog(initialCategory = "Bug Report", onDismiss = { showBugDialog = false })
            }

            if (Support.upiEnabled) {
                Spacer(Modifier.height(22.dp))
                Text("Optional developer tip (UPI)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    amount, { v -> amount = v.filter { it.isDigit() }.take(6) }, singleLine = true, shape = CircleShape,
                    enabled = !chooseInApp,
                    prefix = { Text("₹ ", style = MaterialTheme.typography.titleMedium) },
                    placeholder = { Text("Enter any amount") },
                    textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                    listOf("49", "99", "199", "499", "999").forEach { a ->
                        AppChip(
                            selected = amount == a && !chooseInApp,
                            onClick = { amount = a; chooseInApp = false },
                            label = "₹$a",
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Type the amount in my UPI app", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    AppToggle(checked = chooseInApp, onCheckedChange = { chooseInApp = it })
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    enabled = canPay,
                    onClick = {
                        val uri = Uri.parse("upi://pay").buildUpon()
                            .appendQueryParameter("pa", Support.upi)
                            .appendQueryParameter("pn", Support.payee)
                            .appendQueryParameter("tn", "Support Pro Notes")
                            .apply { if (!chooseInApp) appendQueryParameter("am", amountValue.toString()) }
                            .appendQueryParameter("cu", "INR")
                            .build()
                        try {
                            pay.launch(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Pay with"))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = CircleShape, modifier = Modifier.widthIn(min = 240.dp).height(52.dp),
                ) { Text(if (chooseInApp) "Continue to UPI" else if (amountValue > 0) "Pay ₹$amountValue with UPI" else "Enter an amount") }
            }
            Spacer(Modifier.height(32.dp))
        }

        // Soft progressive top Haze backdrop + floating back control
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .height(84.dp)
                .then(
                    if (supportLiveBlur) {
                        Modifier.hazeEffect(
                            state = supportHazeState,
                            style = HazeMaterials.ultraThin(supportScheme.background),
                        ) {
                            progressive = HazeProgressive.verticalGradient(
                                startIntensity = 0.85f,
                                endIntensity = 0f,
                            )
                        }
                    } else Modifier,
                )
                .background(
                    Brush.verticalGradient(
                        0f to supportScheme.background.copy(alpha = if (supportLiveBlur) 0.44f else 0.86f),
                        0.50f to supportScheme.background.copy(alpha = if (supportLiveBlur) 0.24f else 0.56f),
                        0.85f to supportScheme.background.copy(alpha = if (supportLiveBlur) 0.08f else 0.20f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSurface(
                role = SurfaceRole.Control,
                containerColor = supportScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                contentColor = supportScheme.onSurface,
                borderColor = supportScheme.outlineVariant.copy(alpha = 0.28f),
                shape = appPillShape(),
                contentPadding = PaddingValues(10.dp),
                onClick = onBack,
                onClickLabel = "Back",
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = supportScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
        }

        // Soft progressive bottom Haze backdrop
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(78.dp)
                .then(
                    if (supportLiveBlur) {
                        Modifier.hazeEffect(
                            state = supportHazeState,
                            style = HazeMaterials.ultraThin(supportScheme.background),
                        ) {
                            progressive = HazeProgressive.verticalGradient(
                                startIntensity = 0f,
                                endIntensity = 0.85f,
                            )
                        }
                    } else Modifier,
                )
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.40f to supportScheme.background.copy(alpha = if (supportLiveBlur) 0.08f else 0.24f),
                        0.76f to supportScheme.background.copy(alpha = if (supportLiveBlur) 0.22f else 0.52f),
                        1f to supportScheme.background.copy(alpha = if (supportLiveBlur) 0.40f else 0.78f),
                    ),
                ),
        )
        thanks?.let { paid -> ThankYou(paid) { thanks = null } }
    }
}

@Composable
private fun BeatingHeart(modifier: Modifier) {
    Canvas(modifier) { drawPath(heartPath(size.width / 2, size.height / 2, size.minDimension * 0.46f), Color(0xFFE5484D)) }
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
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) progress.snapTo(1f)
        else progress.animateTo(1f, tween(2600, easing = LinearEasing))
    }
    val hearts = remember {
        List(36) { Triple(Random.nextFloat() * 360f, 0.4f + Random.nextFloat() * 0.6f, listOf(0xFFE5484D, 0xFFFF7EB6, 0xFF7161E0, 0xFF3987E5, 0xFFFFB224)[it % 5]) }
    }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        if (!reduceMotion) {
            Canvas(Modifier.fillMaxSize()) {
                val p = progress.value
                hearts.forEach { (angle, speed, color) ->
                    val a = Math.toRadians(angle.toDouble())
                    val dist = size.minDimension * 0.45f * speed * p
                    val x = size.width / 2 + (cos(a) * dist).toFloat()
                    val y = size.height / 2 + (sin(a) * dist).toFloat() - size.height * 0.15f * p * p
                    drawPath(heartPath(x, y, 18f + 14f * speed), Color(color).copy(alpha = (1f - p).coerceIn(0f, 1f)))
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BeatingHeart(Modifier.size(90.dp))
            Spacer(Modifier.height(16.dp))
            GradientTitle("Thank you", MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                if (paid) "Your support means a lot. Enjoy Pro Notes." else "If your payment went through, thank you.",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
