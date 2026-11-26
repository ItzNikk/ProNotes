package com.pronotes.app.editor

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pronotes.app.ui.AppDialog

private data class StickyBoardEntry(
    val pageIndex: Int,
    val sticky: StickyItem,
    val inkCount: Int,
)

/** Notein-style Sticky Notes Manager across the notebook: search, color filter, ink count, fold/pin toggles, and tap-to-jump. */
@Composable
fun StickyBoard(vm: EditorViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var colorFilter by remember { mutableStateOf<Int?>(null) }

    val allEntries = remember(vm.pages.toList()) {
        vm.pages.flatMapIndexed { pageIdx, page ->
            val inks = page.items.filterIsInstance<InkItem>()
            page.items.filterIsInstance<StickyItem>().map { s ->
                val paper = s.paperBounds
                val count = inks.count { ink ->
                    paper.contains(ink.bounds.centerX(), ink.bounds.centerY())
                }
                StickyBoardEntry(pageIdx, s, count)
            }
        }
    }

    val filtered = remember(allEntries, query, colorFilter) {
        val q = query.trim().lowercase()
        allEntries.filter { entry ->
            val colorOk = colorFilter == null || entry.sticky.color == colorFilter
            val queryOk = q.isEmpty() ||
                entry.sticky.title.lowercase().contains(q) ||
                entry.sticky.text.lowercase().contains(q)
            colorOk && queryOk
        }
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sticky notes (${allEntries.size})", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = {
                    val idx = vm.pageIndex
                    val p = vm.pages.getOrNull(idx)
                    if (p != null) {
                        vm.openSticky(idx, p.width * 0.5f, p.height * 0.35f, null)
                    }
                    onDismiss()
                }) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Sticky")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (allEntries.isNotEmpty()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Search sticky notes…") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Square color filter strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (colorFilter == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .border(
                                    width = if (colorFilter == null) 1.5.dp else 0.dp,
                                    color = if (colorFilter == null) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .clickable { colorFilter = null }
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "All",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (colorFilter == null) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                        StickyColors.forEach { c ->
                            val sel = colorFilter == c
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(c))
                                    .border(
                                        width = if (sel) 2.dp else 1.dp,
                                        color = if (sel) MaterialTheme.colorScheme.onSurface else Color.Black.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(5.dp),
                                    )
                                    .clickable { colorFilter = if (sel) null else c },
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (allEntries.isEmpty()) "No sticky notes in this notebook yet."
                            else "No matching sticky notes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.sticky.id }) { entry ->
                            val sticky = entry.sticky
                            val lum = android.graphics.Color.luminance(sticky.color)
                            val fg = if (lum < 0.38f) Color.White else Color(0xFF1F2937)
                            val fgMuted = fg.copy(alpha = 0.65f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(sticky.color))
                                    .border(1.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (sticky.collapsed) vm.toggleStickyCollapse(entry.pageIndex, sticky)
                                        vm.onScrollTo?.invoke(entry.pageIndex)
                                        onDismiss()
                                    }
                                    .padding(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        sticky.title.ifBlank { "Sticky note" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = fg,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { vm.toggleStickyPin(entry.pageIndex, sticky) },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            if (sticky.pinned) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                            contentDescription = if (sticky.pinned) "Unpin" else "Pin",
                                            tint = if (sticky.pinned) fg else fgMuted,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { vm.toggleStickyCollapse(entry.pageIndex, sticky) },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            if (sticky.collapsed) Icons.Outlined.UnfoldMore else Icons.Outlined.UnfoldLess,
                                            contentDescription = if (sticky.collapsed) "Expand" else "Fold",
                                            tint = fg,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    sticky.text.ifBlank {
                                        if (entry.inkCount > 0) "Handwritten sticky" else "Empty note"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (sticky.text.isBlank()) fgMuted else fg,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.heightIn(min = 38.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "p.${entry.pageIndex + 1}" + if (sticky.collapsed) " · Folded" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = fgMuted,
                                    )
                                    if (entry.inkCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(fg.copy(alpha = 0.12f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                "${entry.inkCount} ink",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = fg,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
