package com.pronotes.app.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.pronotes.app.R
import com.pronotes.app.ui.AppDialog

@Composable
internal fun LibraryWorkspace(drag: DragController, content: @Composable LookaheadScope.() -> Unit) {
    CompositionLocalProvider(LocalDragController provides drag) {
        LookaheadScope { content() }
    }
}

private val guideSteps = listOf(
    R.string.library_guide_create_title to R.string.library_guide_create_body,
    R.string.library_guide_write_title to R.string.library_guide_write_body,
    R.string.library_guide_read_title to R.string.library_guide_read_body,
)

@Composable
internal fun LibraryGuide(onDismiss: () -> Unit, onCreate: () -> Unit) {
    val step = rememberSaveable { mutableIntStateOf(0) }
    val (title, body) = guideSteps[step.intValue]
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_guide_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.library_guide_progress, step.intValue + 1, guideSteps.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.library_guide_close)) }
        },
        confirmButton = {
            if (step.intValue == guideSteps.lastIndex) {
                Button(onClick = onCreate) { Text(stringResource(R.string.library_guide_create)) }
            } else {
                Button(onClick = { step.intValue++ }) { Text(stringResource(R.string.library_guide_next)) }
            }
        },
    )
}
