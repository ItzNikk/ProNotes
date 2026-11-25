package com.pronotes.app.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pronotes.app.ui.AppMenuItem
import com.pronotes.app.ui.LocalReduceMotion

/** The library creation menu has its own hierarchy; ordinary context menus stay compact. */
@Composable
internal fun LibraryCreateMenu(label: String, items: List<AppMenuItem>) {
    var expanded by remember { mutableStateOf(false) }
    val accents = listOf(Color(0xFF8EB9FF), Color(0xFFC0A4FF), Color(0xFF7BE0C6), Color(0xFFFFC38B))
    Box {
        Surface(
            onClick = { expanded = !expanded },
            shape = com.pronotes.app.ui.appCardShape(),
            color = Color(0xFF254DB5),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color(0xFF749AF0).copy(alpha = 0.5f)),
            shadowElevation = 5.dp,
        ) {
            Box(Modifier.size(48.dp).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, null, Modifier.size(25.dp))
            }
        }
        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = com.pronotes.app.ui.appCardShape())) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 260.dp, max = 340.dp).heightIn(max = 560.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Notebooks, imports and study tools", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items.forEachIndexed { index, item ->
                    val group = when {
                        index < 4 -> 0
                        index < 9 -> 1
                        index < 11 -> 2
                        else -> 3
                    }
                    val accent = accents[group]
                    if (index in listOf(0, 4, 9, 11)) {
                        if (index != 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Text(
                            listOf("CREATE", "EXPLORE", "ORGANIZE", "IMPORT")[group],
                            Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent,
                        )
                    }
                    DropdownMenuItem(
                        enabled = item.enabled,
                        leadingIcon = {
                            Box(Modifier.size(34.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                                CompositionLocalProvider(LocalContentColor provides accent) {
                                    item.icon?.invoke() ?: Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp))
                                }
                            }
                        },
                        text = { Text(item.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { expanded = false; item.onClick() },
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
