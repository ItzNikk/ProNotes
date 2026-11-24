package com.pronotes.app.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pronotes.app.data.PaperStyle
import com.pronotes.app.data.Repository
import com.pronotes.app.ui.AppDialog
import kotlinx.coroutines.launch

private data class CatalogTemplate(
    val title: String,
    val subtitle: String,
    val paper: PaperStyle,
    val coverColor: Int,
    val isBook: Boolean,
)

private val BuiltInCatalog = listOf(
    CatalogTemplate("Cornell Study Notebook", "Two-column lecture notes with summary footer", PaperStyle.CORNELL, 0xFF24483F.toInt(), false),
    CatalogTemplate("Engineering Grid Journal", "Precision square grid for math & diagrams", PaperStyle.GRID, 0xFF2E4A7A.toInt(), false),
    CatalogTemplate("Dot-Grid Planner", "Flexible bullet & weekly layout paper", PaperStyle.DOTTED, 0xFF7A2E42.toInt(), false),
    CatalogTemplate("Blank Sketchbook", "Clean distraction-free drawing canvas", PaperStyle.BLANK, 0xFFB98A2E.toInt(), false),
    CatalogTemplate("Reading & Research Workbook", "Ruled notebook tailored for book summaries & quotes", PaperStyle.RULED, 0xFF35655A.toInt(), true),
    CatalogTemplate("Annotated Literature Notes", "Cornell margins for chapter analysis & reflections", PaperStyle.CORNELL, 0xFF475569.toInt(), true),
)

/** Template and study notebook catalog dialog used by LibraryScreen and BookReaderArea. */
@Composable
fun NotebookTemplatesDialog(
    booksOnly: Boolean,
    folderId: String?,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val context = LocalContext.current
    val repo = Repository.get(context)
    val scope = rememberCoroutineScope()
    val items = if (booksOnly) BuiltInCatalog.filter { it.isBook } else BuiltInCatalog

    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (booksOnly) "Reading workbooks" else "Notebook templates",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.title }) { tpl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable {
                                scope.launch {
                                    val id = repo.createNotebook(
                                        title = tpl.title,
                                        folderId = folderId,
                                        cover = tpl.coverColor,
                                        paper = tpl.paper,
                                    )
                                    onDismiss()
                                    onOpen(id)
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tpl.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                tpl.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "Create",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
