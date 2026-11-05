package com.pronotes.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pronotes.app.data.Updater
import kotlinx.coroutines.launch

/** Shows what changed before an update is downloaded. Lives once at the top of the app. */
@Composable
fun UpdateDialogHost() {
    if (!Updater.showDialog) return
    val rel = Updater.release ?: run { Updater.showDialog = false; return }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var autoOpen by remember { mutableStateOf(Updater.autoOpen(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun start() {
        Updater.showDialog = false
        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        Updater.startDownload(context)
    }

    AlertDialog(
        onDismissRequest = { Updater.showDialog = false },
        shape = RoundedCornerShape(28.dp),
        title = { Text("Update to ${rel.tag}") },
        text = {
            Column {
                val mb = if (rel.size > 0) " · %.1f MB".format(rel.size / 1048576.0) else ""
                Text(
                    "You have ${Updater.currentVersion(context)}$mb", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text("What's new", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                    val notes = rel.notes.ifBlank { "Improvements and fixes." }
                    MiniMarkdown(notes)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Open the app after updating")
                        Text(
                            "Needs “Display over other apps”. Otherwise you get a tap-to-open notification.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(autoOpen, { on ->
                        autoOpen = on; Updater.setAutoOpen(context, on)
                        if (on && !Settings.canDrawOverlays(context)) {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    })
                }
            }
        },
        confirmButton = { Button(onClick = { start() }, shape = androidx.compose.foundation.shape.CircleShape) { Text("Update") } },
        dismissButton = { TextButton(onClick = { Updater.showDialog = false }) { Text("Later") } },
    )
}

/** Just enough markdown for release notes: headings, bullets and **bold**. */
@Composable
private fun MiniMarkdown(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        text.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                line.startsWith("#") -> Text(
                    line.trimStart('#').trim().replace("**", ""),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp),
                )
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") ->
                    Row(Modifier.fillMaxWidth()) {
                        Text("•  ", style = MaterialTheme.typography.bodyMedium)
                        Text(bold(line.trimStart().drop(2)), style = MaterialTheme.typography.bodyMedium)
                    }
                else -> Text(bold(line), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun bold(s: String): AnnotatedString = buildAnnotatedString {
    val parts = s.replace("`", "").split("**")
    parts.forEachIndexed { i, p ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(p) } else append(p)
    }
}
