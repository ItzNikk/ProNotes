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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import io.github.ntvien.liquidglass.component.LiquidGlassToggle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pronotes.app.data.Updater

private fun formatSpeed(bps: Long): String = when {
    bps >= 1_048_576L -> "%.1f MB/s".format(bps / 1_048_576.0)
    bps >= 1024L -> "%d KB/s".format(bps / 1024L)
    else -> ""
}

private fun formatEtaLabel(sec: Long?): String = when {
    sec == null || sec < 0L -> ""
    sec >= 60L -> "${sec / 60}m ${sec % 60}s remaining"
    else -> "${sec}s remaining"
}

/** Shows what changed before an update is downloaded, live background download controls, and installer handoff. */
@Composable
fun UpdateDialogHost() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pending = Updater.pendingConfirm
    androidx.compose.runtime.LaunchedEffect(pending, lifecycleOwner) {
        if (pending != null) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                Updater.pendingConfirm?.let { Updater.launchConfirm(context, it) }
            }
        }
    }
    if (!Updater.showDialog) return
    val rel = Updater.release ?: run { Updater.showDialog = false; return }
    var autoOpen by remember { mutableStateOf(Updater.autoOpen(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun startOrResume() {
        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (Updater.phase == Updater.Phase.PAUSED) Updater.resumeDownload(context)
        else Updater.startDownload(context)
    }

    val isDownloading = Updater.phase == Updater.Phase.DOWNLOADING
    val isPaused = Updater.phase == Updater.Phase.PAUSED

    LiquidGlassDialog(
        onDismissRequest = { Updater.showDialog = false },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pro Notes ${rel.tag}", modifier = Modifier.weight(1f))
                if (isPaused) {
                    LiquidGlassPill {
                        Text("Paused", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        text = {
            Column {
                val mb = if (rel.size > 0) " · %.1f MB".format(rel.size / 1048576.0) else ""
                Text(
                    "Installed: ${Updater.currentVersion(context)}$mb · Resumable background updater",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isDownloading || isPaused) {
                    Spacer(Modifier.height(12.dp))
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            val pct = (Updater.progress * 100).toInt().coerceIn(0, 100)
                            val doneMb = "%.1f".format(Updater.downloadedBytes / 1048576.0)
                            val totalMb = if (Updater.totalBytes > 0L) " / %.1f MB".format(Updater.totalBytes / 1048576.0) else " MB"
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    if (isDownloading) "Downloading $pct%" else "Paused at $pct%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text("$doneMb$totalMb", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { Updater.progress },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            )
                            val sp = formatSpeed(Updater.speedBytesPerSec)
                            val eta = formatEtaLabel(Updater.etaSeconds)
                            if (sp.isNotEmpty() || eta.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    listOf(sp, eta).filter { it.isNotEmpty() }.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("What's new", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    val notes = rel.notes.ifBlank { "Improvements, design refinements, and fixes." }
                    MiniMarkdown(notes)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 10.dp)) {
                        Text("Open after updating")
                        Text(
                            "Needs “Display over other apps”. Otherwise you get a tap-to-open notification.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LiquidGlassToggle(
                        checked = autoOpen,
                        onCheckedChange = { on ->
                            autoOpen = on; Updater.setAutoOpen(context, on)
                            if (on && !Settings.canDrawOverlays(context)) {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isDownloading) {
                    OutlinedButton(onClick = { Updater.pauseDownload(context) }, shape = CircleShape) { Text("Pause") }
                    Button(onClick = { Updater.showDialog = false }, shape = CircleShape) { Text("Background") }
                } else {
                    Button(onClick = { startOrResume() }, shape = CircleShape) {
                        Text(if (isPaused) "Resume" else "Download & Update")
                    }
                }
            }
        },
        dismissButton = {
            if (isDownloading || isPaused) {
                TextButton(onClick = { Updater.cancelDownload(context) }) { Text("Cancel download") }
            } else {
                TextButton(onClick = { Updater.showDialog = false }) { Text("Later") }
            }
        },
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
