package com.pronotes.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pronotes.app.data.AppLanguageOption
import com.pronotes.app.data.AppLanguages
import com.pronotes.app.data.LocalAppStrings
import com.pronotes.app.editor.HandwritingLanguage
import com.pronotes.app.editor.HandwritingRecognizer
import kotlinx.coroutines.launch

/**
 * Searchable, region-grouped global picker for Pro Notes' 58+ on-device ML Kit
 * handwriting recognition languages, with live offline model download/removal.
 */
@Composable
fun HandwritingLanguagePickerDialog(
    selectedTag: String,
    onDismiss: () -> Unit,
    onSelect: (HandwritingLanguage) -> Unit,
    showDownloadActions: Boolean = true,
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val downloadedTags by HandwritingRecognizer.downloadedTags.collectAsStateWithLifecycle()
    val downloadingTags by HandwritingRecognizer.downloadingTags.collectAsStateWithLifecycle()
    val activeLang = remember(selectedTag) { HandwritingRecognizer.findLanguage(selectedTag) }

    var query by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("All") }

    val regions = remember {
        listOf("All", "Popular", "South Asia", "East & SE Asia", "Europe & Americas", "Middle East & Africa")
    }

    LaunchedEffect(showDownloadActions) {
        if (showDownloadActions) {
            HandwritingRecognizer.refreshDownloadedModels()
        }
    }

    val filtered = remember(query, selectedRegion) {
        val q = query.trim().lowercase()
        HandwritingRecognizer.supportedLanguages.filter { lang ->
            val regionOk = selectedRegion == "All" || lang.region == selectedRegion
            val queryOk = q.isEmpty() ||
                lang.name.lowercase().contains(q) ||
                lang.nativeName.lowercase().contains(q) ||
                lang.tag.lowercase().contains(q) ||
                lang.region.lowercase().contains(q)
            regionOk && queryOk
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Language,
                        contentDescription = null,
                        tint = ProButtonPalette.ElectricBlue,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.handwritingRecognitionLang,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "${HandwritingRecognizer.supportedLanguages.size} languages · ${downloadedTags.size} ${strings.downloaded.lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = strings.cancel)
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(strings.searchLanguages) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProButtonPalette.ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    regions.forEach { reg ->
                        val active = selectedRegion == reg
                        AppChip(
                            selected = active,
                            onClick = { selectedRegion = reg },
                            label = reg,
                            tint = ProButtonPalette.ElectricBlue,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 240.dp, max = 380.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.tag }) { lang ->
                        val isSelected = lang.tag.equals(activeLang.tag, ignoreCase = true)
                        val isDownloaded = lang.tag in downloadedTags
                        val isDownloading = lang.tag in downloadingTags
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) ProButtonPalette.ElectricBlue.copy(alpha = 0.13f)
                                    else Color.Transparent,
                                )
                                .border(
                                    width = if (isSelected) 1.25.dp else 0.dp,
                                    color = if (isSelected) ProButtonPalette.ElectricBlue.copy(alpha = 0.65f) else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable { onSelect(lang) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        lang.nativeName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (lang.isRtl) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                "RTL",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                        }
                                    }
                                }
                                Text(
                                    if (lang.nativeName == lang.name) "${lang.region} · ${lang.tag}"
                                    else "${lang.name} · ${lang.region}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            if (showDownloadActions) {
                                when {
                                    isDownloading -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = ProButtonPalette.ElectricBlue,
                                        )
                                    }
                                    isDownloaded -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.CloudDone,
                                                contentDescription = strings.downloaded,
                                                tint = ProButtonPalette.Emerald,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            if (!isSelected) {
                                                IconButton(
                                                    onClick = { scope.launch { HandwritingRecognizer.deleteLanguageModel(lang.tag) } },
                                                    modifier = Modifier.size(30.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.DeleteOutline,
                                                        contentDescription = strings.deletePack,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        IconButton(
                                            onClick = { scope.launch { HandwritingRecognizer.downloadLanguage(lang.tag) } },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                Icons.Outlined.CloudDownload,
                                                contentDescription = strings.download,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            if (isSelected) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = ProButtonPalette.ElectricBlue,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.done, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Searchable global picker for Pro Notes' 27+ app UI display languages + System Default.
 * Only English is built into the APK to minimize default app size; all other 25 world
 * languages download optionally on demand (~3 KB JSON packs) and can be removed anytime.
 */
@Composable
fun AppLanguagePickerDialog(
    selectedTag: String,
    onDismiss: () -> Unit,
    onSelect: (AppLanguageOption) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val installedTags by com.pronotes.app.data.LanguagePackManager.installedTags.collectAsStateWithLifecycle()
    val downloadingTags by com.pronotes.app.data.LanguagePackManager.downloadingTags.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        com.pronotes.app.data.LanguagePackManager.refreshInstalled(context)
    }

    val filtered = remember(query) {
        val q = query.trim().lowercase()
        AppLanguages.ALL.filter { opt ->
            q.isEmpty() ||
                opt.englishName.lowercase().contains(q) ||
                opt.nativeName.lowercase().contains(q) ||
                opt.tag.lowercase().contains(q) ||
                opt.region.lowercase().contains(q)
        }
    }

    fun activateOrDownload(opt: AppLanguageOption) {
        errorMsg = null
        if (opt.builtIn || opt.tag.isEmpty() || opt.tag in installedTags) {
            onSelect(opt)
            return
        }
        scope.launch {
            val res = com.pronotes.app.data.LanguagePackManager.downloadPack(context, opt.tag)
            res.onSuccess {
                onSelect(opt)
            }.onFailure { err ->
                errorMsg = "Could not download ${opt.englishName} pack (${err.message ?: "check internet connection"})."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 540.dp)
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Language,
                        contentDescription = null,
                        tint = ProButtonPalette.ElectricBlue,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.appDisplayLanguage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "English built-in · ${installedTags.size} optional packs installed · ~3 KB each",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = strings.cancel)
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(strings.searchLanguages) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProButtonPalette.ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMsg!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 240.dp, max = 380.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.tag.ifEmpty { "system" } }) { opt ->
                        val isSelected = opt.tag.equals(selectedTag, ignoreCase = true)
                        val isInstalled = opt.builtIn || opt.tag.isEmpty() || opt.tag in installedTags
                        val isDownloading = opt.tag in downloadingTags
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) ProButtonPalette.ElectricBlue.copy(alpha = 0.13f)
                                    else Color.Transparent,
                                )
                                .border(
                                    width = if (isSelected) 1.25.dp else 0.dp,
                                    color = if (isSelected) ProButtonPalette.ElectricBlue.copy(alpha = 0.65f) else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable(enabled = !isDownloading) { activateOrDownload(opt) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        if (opt.tag.isEmpty()) strings.systemDefault else opt.nativeName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) ProButtonPalette.ElectricBlue else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (opt.builtIn && opt.tag.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                "Built-in",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    if (opt.isRtl) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                "RTL",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                        }
                                    }
                                }
                                Text(
                                    when {
                                        opt.tag.isEmpty() -> "Matches your device language (uses downloaded pack if installed)"
                                        opt.builtIn -> opt.region
                                        isDownloading -> strings.downloading
                                        isInstalled -> "${opt.englishName} · ${strings.downloaded}"
                                        else -> "${opt.englishName} · Optional download (~${opt.approxKb} KB)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            when {
                                isDownloading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = ProButtonPalette.ElectricBlue,
                                    )
                                }
                                !opt.builtIn && opt.tag.isNotEmpty() && isInstalled -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.CloudDone,
                                            contentDescription = strings.downloaded,
                                            tint = ProButtonPalette.Emerald,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        IconButton(
                                            onClick = {
                                                com.pronotes.app.data.LanguagePackManager.deletePack(context, opt.tag)
                                                if (isSelected) onSelect(AppLanguages.SYSTEM)
                                            },
                                            modifier = Modifier.size(30.dp),
                                        ) {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = strings.deletePack,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                                !opt.builtIn && opt.tag.isNotEmpty() -> {
                                    IconButton(
                                        onClick = { activateOrDownload(opt) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.CloudDownload,
                                            contentDescription = strings.download,
                                            tint = ProButtonPalette.ElectricBlue,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = ProButtonPalette.ElectricBlue,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.done, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
