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

/** Clean resize-and-rotate selection glyph: framed box with corner nodes, diagonal scale arrow, and curved rotation arc. */
@Composable
fun TransformSelectionIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val thin = Stroke(1.55f, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Clean selection frame on lower-left quadrant (leaves top-right open for rotation arc)
            val frame = Path().apply {
                moveTo(3.5f, 11.5f)
                lineTo(3.5f, 19.5f)
                lineTo(14.5f, 19.5f)
                lineTo(14.5f, 14.5f)
                moveTo(3.5f, 8.5f)
                lineTo(10.5f, 8.5f)
            }
            drawPath(frame, tint, style = thin)

            // Crisp corner resize handle nodes
            drawCircle(tint, radius = 1.65f, center = Offset(3.5f, 8.5f))
            drawCircle(tint, radius = 1.65f, center = Offset(3.5f, 19.5f))
            drawCircle(tint, radius = 1.65f, center = Offset(14.5f, 19.5f))

            // Diagonal resize arrow inside frame
            val resize = Path().apply {
                moveTo(6.6f, 16.4f)
                lineTo(12.2f, 10.8f)
                moveTo(9.4f, 10.8f)
                lineTo(12.2f, 10.8f)
                lineTo(12.2f, 13.6f)
            }
            drawPath(resize, tint, style = stroke)

            // Graceful clockwise rotation arc sweeping cleanly around top-right corner
            drawArc(
                color = tint,
                startAngle = -88f,
                sweepAngle = 112f,
                useCenter = false,
                topLeft = Offset(7.5f, 3.0f),
                size = Size(13.0f, 13.0f),
                style = stroke,
            )
            // Crisp rotation arrowhead at arc end (~24 deg)
            val rotHead = Path().apply {
                moveTo(17.4f, 11.1f)
                lineTo(19.9f, 12.6f)
                lineTo(21.4f, 9.9f)
            }
            drawPath(rotHead, tint, style = stroke)
        }
    }
}
