package com.pronotes.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

/** Rounded workspace navigation icons; drawing-tool glyphs keep their original renderer. */
@Composable
fun WorkspaceIcon(icon: ProIcon, tint: Color, modifier: Modifier = Modifier) {
    val vector = when (icon) {
        ProIcon.SETTINGS -> Icons.Rounded.Tune
        ProIcon.PEN -> Icons.Rounded.Draw
        ProIcon.LASSO -> Icons.Rounded.Gesture
        ProIcon.DOCUMENT -> Icons.Rounded.Article
        ProIcon.TOOLBAR -> Icons.Rounded.DashboardCustomize
        ProIcon.PALETTE -> Icons.Rounded.Palette
        ProIcon.FOLDER -> Icons.Rounded.FolderOpen
        ProIcon.INFO -> Icons.Rounded.Info
        ProIcon.HEART -> Icons.Rounded.FavoriteBorder
        ProIcon.STAR -> Icons.Rounded.StarOutline
        ProIcon.DOWNLOAD -> Icons.Rounded.Download
        ProIcon.RESTORE -> Icons.Rounded.History
        ProIcon.HOME -> Icons.Rounded.Home
        ProIcon.BOOK -> Icons.AutoMirrored.Rounded.MenuBook
        ProIcon.BOOKMARK -> Icons.Rounded.BookmarkBorder
        ProIcon.ADD -> Icons.Rounded.Add
        ProIcon.SHARE -> Icons.Rounded.Share
        ProIcon.MORE -> Icons.Rounded.MoreHoriz
        ProIcon.CLOSE -> Icons.Rounded.Close
        else -> null
    }
    if (vector != null) Icon(vector, null, modifier, tint = tint)
    else ProIconView(icon, tint, modifier)
}

/** A resize corner and rotation arrow explain both operations in one compact glyph. */
@Composable
fun TransformSelectionIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val corners = Path().apply {
                moveTo(3f, 10f); lineTo(3f, 19f); lineTo(12f, 19f)
                moveTo(8f, 14f); lineTo(16f, 6f)
                moveTo(11f, 6f); lineTo(16f, 6f); lineTo(16f, 11f)
            }
            drawPath(corners, tint, style = stroke)
            drawArc(tint, -30f, 180f, false, Offset(9f, 11f), Size(12f, 12f), style = stroke)
            drawPath(Path().apply { moveTo(8f, 17f); lineTo(10f, 21f); lineTo(14f, 19f) }, tint, style = stroke)
        }
    }
}
