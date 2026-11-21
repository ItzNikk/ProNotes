package com.pronotes.app.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pronotes.app.ui.AppDialog

/** Overview board listing all sticky notes across the notebook, with tap-to-jump navigation. */
@Composable
fun StickyBoard(vm: EditorViewModel, onDismiss: () -> Unit) {
    val entries = remember(vm.pages.toList()) {
        vm.pages.flatMapIndexed { pageIdx, page ->
            page.items.filterIsInstance<StickyItem>().map { pageIdx to it }
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
                Text("Sticky board (${entries.size})", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = {
                    val idx = vm.pageIndex
                    val p = vm.pages.getOrNull(idx)
                    if (p != null) {
                        vm.openSticky(idx, p.width * 0.5f, p.height * 0.35f, null)
                    }
                    onDismiss()
                }) {
                    Text("New sticky")
                }
            }
        },
        text = {
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No sticky notes in this notebook yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = { (_, s) -> s.id }) { (pageIdx, sticky) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(sticky.color))
                                .clickable {
                                    vm.onScrollTo?.invoke(pageIdx)
                                    onDismiss()
                                }
                                .padding(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    sticky.title.ifBlank { "Sticky note" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F2937),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "Page ${pageIdx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4B5563),
                                )
                            }
                            if (sticky.text.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    sticky.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1F2937),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
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
