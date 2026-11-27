package com.pronotes.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pronotes.app.ui.AppDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CloudStorageProvider(
    val id: String,
    val displayName: String,
    val shortName: String,
    val subtitle: String,
    val accentColor: Long,
    val webUrl: String,
    val allowedHost: String,
    val androidPackage: String,
) {
    GOOGLE_DRIVE(
        id = "gdrive",
        displayName = "Google Drive",
        shortName = "Drive",
        subtitle = "Store note backups and open PDFs or documents from Google Drive",
        accentColor = 0xFF1A73E8L,
        webUrl = "https://drive.google.com/drive/my-drive",
        allowedHost = "drive.google.com",
        androidPackage = "com.google.android.apps.docs",
    ),
    ONEDRIVE(
        id = "onedrive",
        displayName = "Microsoft OneDrive",
        shortName = "OneDrive",
        subtitle = "Sync notebooks and open Office or PDF files from OneDrive",
        accentColor = 0xFF0078D4L,
        webUrl = "https://onedrive.live.com/",
        allowedHost = "onedrive.live.com",
        androidPackage = "com.microsoft.skydrive",
    ),
    ICLOUD_DRIVE(
        id = "icloud",
        displayName = "Apple iCloud Drive",
        shortName = "iCloud",
        subtitle = "Save note archives and access documents stored in iCloud Drive",
        accentColor = 0xFF0A84FFL,
        webUrl = "https://www.icloud.com/iclouddrive",
        allowedHost = "www.icloud.com",
        androidPackage = "",
    ),
}

data class CloudProviderState(
    val provider: CloudStorageProvider,
    val folderUri: String,
    val folderLabel: String,
    val lastSyncedMs: Long,
) {
    val isFolderLinked: Boolean get() = folderUri.isNotBlank()
}

/**
 * Manages linked storage folders, backups, and document access for
 * Google Drive, Microsoft OneDrive, and Apple iCloud Drive via Android's
 * Storage Access Framework (SAF) and verified HTTPS provider portals.
 */
object CloudStorageStore {
    private fun uriKey(p: CloudStorageProvider) = "cloud_provider_uri_${p.id}"
    private fun labelKey(p: CloudStorageProvider) = "cloud_provider_label_${p.id}"
    private fun tsKey(p: CloudStorageProvider) = "cloud_provider_ts_${p.id}"

    fun state(context: Context, provider: CloudStorageProvider): CloudProviderState {
        val store = SettingsStore.get(context)
        return CloudProviderState(
            provider = provider,
            folderUri = store.pref(uriKey(provider), ""),
            folderLabel = store.pref(labelKey(provider), ""),
            lastSyncedMs = store.pref(tsKey(provider), "0").toLongOrNull() ?: 0L,
        )
    }

    fun linkedFolderUris(context: Context): List<String> {
        val store = SettingsStore.get(context)
        return CloudStorageProvider.entries
            .map { store.pref(uriKey(it), "") }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun linkFolder(context: Context, provider: CloudStorageProvider, uri: Uri): String {
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }
        val label = StorageFolderSync.friendlyFolderName(uri)
        val store = SettingsStore.get(context)
        store.setPref(uriKey(provider), uri.toString())
        store.setPref(labelKey(provider), label)
        return label
    }

    fun unlinkFolder(context: Context, provider: CloudStorageProvider) {
        val store = SettingsStore.get(context)
        store.setPref(uriKey(provider), "")
        store.setPref(labelKey(provider), "")
    }

    fun markSynced(context: Context, provider: CloudStorageProvider, timestampMs: Long = System.currentTimeMillis()) {
        SettingsStore.get(context).setPref(tsKey(provider), timestampMs.toString())
    }

    fun suggestedBackupFileName(provider: CloudStorageProvider): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "ProNotes-${provider.shortName}-$date.zip"
    }

    /**
     * Opens the provider's installed Android app if available, otherwise launches
     * the provider's verified HTTPS web portal.
     */
    fun openProvider(context: Context, provider: CloudStorageProvider): Boolean {
        if (provider.androidPackage.isNotBlank()) {
            val appIntent = runCatching {
                context.packageManager.getLaunchIntentForPackage(provider.androidPackage)
            }.getOrNull()
            if (appIntent != null) {
                appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val opened = runCatching { context.startActivity(appIntent); true }.getOrDefault(false)
                if (opened) return true
            }
        }
        val parsed = runCatching { Uri.parse(provider.webUrl) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase(Locale.ROOT)
        val host = parsed.host?.lowercase(Locale.ROOT)
        if (scheme != "https" || host != provider.allowedHost) return false
        val webIntent = Intent(Intent.ACTION_VIEW, parsed).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(webIntent); true }.getOrDefault(false)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloudStorageProvidersPanel(
    repo: Repository,
    onImportDocuments: ((List<Uri>) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshTick by remember { mutableIntStateOf(0) }
    var activeProvider by remember { mutableStateOf(CloudStorageProvider.GOOGLE_DRIVE) }
    var busyProvider by remember { mutableStateOf<CloudStorageProvider?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val linkFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val provider = activeProvider
            val label = CloudStorageStore.linkFolder(context, provider, uri)
            refreshTick++
            busyProvider = provider
            scope.launch {
                val synced = runCatching {
                    StorageFolderSync.syncAllNotebooks(context, uri, repo)
                }.getOrElse { -1 }
                busyProvider = null
                if (synced >= 0) {
                    CloudStorageStore.markSynced(context, provider)
                    refreshTick++
                    statusMessage = "${provider.displayName} linked ($label) · synced $synced notebook(s)."
                } else {
                    statusMessage = "Linked $label for ${provider.displayName}. Tap Sync Folder to retry."
                }
            }
        }
    }

    val saveBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            val provider = activeProvider
            busyProvider = provider
            scope.launch {
                val err = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            repo.backup(out)
                        } ?: error("Unable to open destination file")
                    }.exceptionOrNull()
                }
                busyProvider = null
                if (err == null) {
                    CloudStorageStore.markSynced(context, provider)
                    refreshTick++
                    statusMessage = "Saved full library backup to ${provider.displayName}."
                } else {
                    statusMessage = "Could not save backup to ${provider.displayName}: ${err.message ?: "write error"}"
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val provider = activeProvider
            busyProvider = provider
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            repo.restore(input)
                        } ?: error("Unable to read backup file")
                    }
                }
                busyProvider = null
                result.onSuccess { count ->
                    statusMessage = "Restored $count notebook(s) from ${provider.displayName}."
                }.onFailure { err ->
                    statusMessage = "Could not restore backup: ${err.message ?: "invalid archive"}"
                }
            }
        }
    }

    val openDocsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val provider = activeProvider
            if (onImportDocuments != null) {
                onImportDocuments(uris)
            } else {
                busyProvider = provider
                scope.launch {
                    val result = runCatching { repo.importFiles(uris, null) }
                    busyProvider = null
                    result.onSuccess {
                        statusMessage = "Imported ${uris.size} document(s) from ${provider.displayName} into your Library."
                    }.onFailure { err ->
                        statusMessage = "Could not import document: ${err.message ?: "unsupported or locked file"}"
                    }
                }
            }
        }
    }

    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Store note backups, sync folders, or open PDFs and documents from Google Drive, Microsoft OneDrive, and Apple iCloud Drive. Select the provider in Android's file picker or link a synced cloud folder.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )

        CloudStorageProvider.entries.forEach { provider ->
            val st = remember(provider, refreshTick) { CloudStorageStore.state(context, provider) }
            val accent = Color(provider.accentColor)
            val isBusy = busyProvider == provider

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceContainerLow,
                border = BorderStroke(
                    1.dp,
                    if (st.isFolderLinked) accent.copy(alpha = 0.45f) else scheme.outlineVariant.copy(alpha = 0.45f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (st.isFolderLinked) Icons.Rounded.CloudDone else Icons.Rounded.Cloud,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                provider.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (st.isFolderLinked) {
                                    "Linked folder: ${st.folderLabel.ifBlank { "Connected" }}"
                                } else {
                                    provider.subtitle
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (st.isFolderLinked) accent else scheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (st.lastSyncedMs > 0L) {
                                val whenStr = remember(st.lastSyncedMs) {
                                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                        .format(Date(st.lastSyncedMs))
                                }
                                Text(
                                    "Last saved $whenStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            enabled = !isBusy,
                            onClick = {
                                activeProvider = provider
                                saveBackupLauncher.launch(CloudStorageStore.suggestedBackupFileName(provider))
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = accent.copy(alpha = 0.15f),
                                contentColor = accent,
                            ),
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isBusy) "Working…" else "Save Backup", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            enabled = !isBusy,
                            onClick = {
                                activeProvider = provider
                                openDocsLauncher.launch(arrayOf("*/*"))
                            },
                            shape = CircleShape,
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Docs", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            enabled = !isBusy,
                            onClick = {
                                activeProvider = provider
                                restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            },
                            shape = CircleShape,
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore", style = MaterialTheme.typography.labelMedium)
                        }

                        if (st.isFolderLinked) {
                            OutlinedButton(
                                enabled = !isBusy,
                                onClick = {
                                    busyProvider = provider
                                    scope.launch {
                                        val synced = runCatching {
                                            StorageFolderSync.syncAllNotebooks(context, Uri.parse(st.folderUri), repo)
                                        }.getOrElse { -1 }
                                        busyProvider = null
                                        if (synced >= 0) {
                                            CloudStorageStore.markSynced(context, provider)
                                            refreshTick++
                                            statusMessage = "Synced $synced notebook(s) to ${provider.displayName}."
                                        } else {
                                            statusMessage = "Folder permission expired. Tap Relink Folder."
                                        }
                                    }
                                },
                                shape = CircleShape,
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sync Folder", style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                enabled = !isBusy,
                                onClick = {
                                    CloudStorageStore.unlinkFolder(context, provider)
                                    refreshTick++
                                    statusMessage = "Unlinked ${provider.displayName} folder."
                                },
                            ) {
                                Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Unlink", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            OutlinedButton(
                                enabled = !isBusy,
                                onClick = {
                                    activeProvider = provider
                                    linkFolderLauncher.launch(null)
                                },
                                shape = CircleShape,
                            ) {
                                Icon(Icons.Rounded.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Link Folder", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        TextButton(
                            onClick = {
                                val ok = CloudStorageStore.openProvider(context, provider)
                                if (!ok) {
                                    Toast.makeText(context, "Could not open ${provider.displayName}.", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open ${provider.shortName}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        statusMessage?.let { msg ->
            HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.08f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Backup, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                )
            }
        }
    }
}

@Composable
fun CloudStorageDialog(
    repo: Repository,
    onImportDocuments: (List<Uri>) -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cloud Storage & Documents") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                CloudStorageProvidersPanel(
                    repo = repo,
                    onImportDocuments = { uris ->
                        onDismiss()
                        onImportDocuments(uris)
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
