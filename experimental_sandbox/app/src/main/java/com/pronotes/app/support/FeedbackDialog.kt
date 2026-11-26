package com.pronotes.app.support

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pronotes.app.BuildConfigInfo
import com.pronotes.app.ui.AppChip
import com.pronotes.app.ui.AppDialog
import com.pronotes.app.ui.AppToggle
import com.pronotes.app.ui.GradientTitle
import com.pronotes.app.ui.ProButtonPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 100% free in-app bug report and feedback channel.
 * Delivers reports directly to the developer's free ntfy.sh inbox topic,
 * and also offers one-tap GitHub Issue & Email fallbacks.
 */
object DeveloperFeedbackClient {
    const val DEV_EMAIL = "nikhiltulsani76206@gmail.com"
    const val GITHUB_NEW_ISSUE = "https://github.com/ItzNikk/ProNotes/issues/new"
    private const val FREE_INBOX_TOPIC = "pronotes-dev-feedback-itznik"

    suspend fun sendDirect(
        category: String,
        title: String,
        message: String,
        contact: String,
        deviceInfo: String?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://ntfy.sh/$FREE_INBOX_TOPIC").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
                val cleanTitle = "[$category] ${title.trim().ifBlank { "Pro Notes Feedback" }}".take(120)
                setRequestProperty("Title", cleanTitle)
                setRequestProperty(
                    "Tags",
                    when (category) {
                        "Bug Report" -> "bug,rotating_light"
                        "Feature Idea" -> "sparkles,bulb"
                        else -> "speech_balloon,heart"
                    },
                )
                setRequestProperty("Priority", if (category == "Bug Report") "4" else "3")
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            }
            val body = buildString {
                appendLine(message.trim())
                if (contact.isNotBlank()) {
                    appendLine()
                    appendLine("Reply contact: ${contact.trim()}")
                }
                if (!deviceInfo.isNullOrBlank()) {
                    appendLine()
                    appendLine("--- Diagnostics ---")
                    appendLine(deviceInfo)
                }
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()
            if (code !in 200..299) error("Relay returned HTTP $code")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackDialog(
    initialCategory: String = "Bug Report",
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories = remember { listOf("Bug Report", "Feature Idea", "General Feedback") }
    var category by remember { mutableStateOf(initialCategory) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var includeDevice by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val deviceInfo = remember(context) {
        "App: Pro Notes ${BuildConfigInfo.versionName(context)} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}"
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.BugReport, contentDescription = null, tint = ProButtonPalette.ElectricBlue)
                GradientTitle("Report Bug / Feedback", MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Send bugs, crashes, or feature ideas straight to the developer for free — no account needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { cat ->
                        AppChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = cat,
                            tint = ProButtonPalette.ElectricBlue,
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Short summary") },
                    placeholder = { Text("e.g., Pen pressure lag on tablet") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details / steps to reproduce") },
                    placeholder = { Text("Tell me what happened or what you'd love to see…") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Your email or handle (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Include device & version info", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                        Text(deviceInfo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    AppToggle(checked = includeDevice, onCheckedChange = { includeDevice = it })
                }

                statusText?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (msg.startsWith("Sent")) ProButtonPalette.Emerald else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val encTitle = URLEncoder.encode("[$category] ${title.trim()}", "UTF-8")
                            val encBody = URLEncoder.encode(
                                buildString {
                                    appendLine(details.trim())
                                    if (includeDevice) appendLine("\n---\n$deviceInfo")
                                },
                                "UTF-8",
                            )
                            val url = "${DeveloperFeedbackClient.GITHUB_NEW_ISSUE}?title=$encTitle&body=$encBody"
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("GitHub Issue", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            val subj = Uri.encode("[$category] ${title.trim().ifBlank { "Pro Notes Feedback" }}")
                            val body = Uri.encode(
                                buildString {
                                    appendLine(details.trim())
                                    if (includeDevice) appendLine("\n---\n$deviceInfo")
                                },
                            )
                            val uri = Uri.parse("mailto:${DeveloperFeedbackClient.DEV_EMAIL}?subject=$subj&body=$body")
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }.onFailure {
                                Toast.makeText(context, "Email: ${DeveloperFeedbackClient.DEV_EMAIL}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Email, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Email", maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !sending && details.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = ProButtonPalette.ElectricBlue, contentColor = Color.White),
                onClick = {
                    sending = true
                    statusText = null
                    scope.launch {
                        val res = DeveloperFeedbackClient.sendDirect(
                            category = category,
                            title = title,
                            message = details,
                            contact = contact,
                            deviceInfo = if (includeDevice) deviceInfo else null,
                        )
                        sending = false
                        res.onSuccess {
                            statusText = "Sent! Thank you for helping improve Pro Notes."
                            Toast.makeText(context, "Feedback sent to developer!", Toast.LENGTH_SHORT).show()
                            title = ""
                            details = ""
                        }.onFailure { e ->
                            statusText = "Direct send failed (${e.message}). Try GitHub Issue or Email below."
                        }
                    }
                },
            ) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                } else {
                    Icon(Icons.Outlined.Send, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text("Send Free")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
