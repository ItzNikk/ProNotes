package com.pronotes.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

/** Vibrant multi-color palette for interactive buttons, action pills, and highlights across the UI. */
object ProButtonPalette {
    val ElectricBlue = Color(0xFF1A84FF)
    val SkyCyan = Color(0xFF00A3F5)
    val RoyalBlue = Color(0xFF0044EA)
    val VibrantOrange = Color(0xFFFF8C00)
    val MagentaPink = Color(0xFFF01E6B)
    val Emerald = Color(0xFF10B981)
    val RoyalIndigo = Color(0xFF4F46E5)
    val WarmAmber = Color(0xFFD97706)
    val CoralRose = Color(0xFFE11D48)
    val Violet = Color(0xFF7C3AED)
    val MintTeal = Color(0xFF0D9488)
    val SlateNeutral = Color(0xFF475569)
}

/** A crisp colour dot with an adaptive contrasting border ring so both light and dark swatches stay readable on any toolbar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Swatch(color: Int, selected: Boolean, size: Int = 26, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val dotColor = Color(color)
    val lum = dotColor.luminance()
    val innerRimColor = when {
        dotColor.alpha < 0.05f -> MaterialTheme.colorScheme.outline
        lum > 0.72f -> Color(0xFF14171F).copy(alpha = 0.65f)
        lum < 0.25f -> Color(0xFFF8FAFC).copy(alpha = 0.72f)
        else -> Color.White.copy(alpha = 0.32f)
    }
    val outerRingColor = if (selected) {
        when {
            lum > 0.75f -> Color(0xFF1A84FF)
            lum < 0.22f -> Color(0xFF60A5FA)
            else -> MaterialTheme.colorScheme.primary
        }
    } else Color.Transparent

    Box(
        Modifier
            .size((size + 8).dp)
            .border(
                width = if (selected) 2.2.dp else 0.dp,
                color = outerRingColor,
                shape = CircleShape,
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(if (dotColor.alpha < 0.05f) MaterialTheme.colorScheme.surfaceVariant else dotColor)
            .border(
                width = 1.25.dp,
                color = innerRimColor,
                shape = CircleShape,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorEditorDialog(
    initial: Int,
    presets: List<Int>,
    recent: List<Int>,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    var current by remember { mutableStateOf(initial or (0xFF shl 24)) }
    var hex by remember { mutableStateOf(String.format("%06X", initial and 0xFFFFFF)) }
    val controller = rememberColorPickerController()
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("Colour", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HsvColorPicker(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    controller = controller,
                    initialColor = Color(current),
                    onColorChanged = { env ->
                        if (env.fromUser) {
                            current = env.color.toArgb() or (0xFF shl 24)
                            hex = env.hexCode.takeLast(6).uppercase()
                        }
                    },
                )
                BrightnessSlider(modifier = Modifier.fillMaxWidth().height(28.dp), controller = controller)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(current)))
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { v ->
                            val clean = v.uppercase().filter { it.isDigit() || it in 'A'..'F' }.take(6)
                            hex = clean
                            if (clean.length == 6) current = clean.toLong(16).toInt() or (0xFF shl 24)
                        },
                        prefix = { Text("#") }, singleLine = true, label = { Text("Hex") },
                    )
                }
                Text("Presets", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    presets.forEach { c -> Swatch(c, c == current, 22) { current = c; hex = String.format("%06X", c and 0xFFFFFF) } }
                }
                if (recent.isNotEmpty()) {
                    Text("Recent", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        recent.forEach { c -> Swatch(c, c == current, 22) { current = c; hex = String.format("%06X", c and 0xFFFFFF) } }
                    }
                }
            }
        },
        confirmButton = {
            AppPill(
                selected = true,
                tint = ProButtonPalette.Emerald,
                onClick = { onPick(current) },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text("Use colour", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            AppPill(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

val StickerSet = listOf(
    "Star", "Check", "Cross", "Alert", "Question", "Idea", "Pin", "Clip",
    "Focus", "Heart", "Note", "Goal", "Target", "Launch", "Date", "Clock",
    "Book", "Draft", "Concept", "Quote", "Trophy", "Milestone", "Diagram", "Summary",
)

