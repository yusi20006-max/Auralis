package com.auralis.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Simple outline icons drawn with Canvas — no icon library dependency. */

@Composable
fun LibraryNavIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
        // Book shape
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val top = size.height * 0.15f
        val bottom = size.height * 0.85f
        val mid = size.width * 0.5f
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(size.minDimension * 0.06f),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(mid, top),
            end = Offset(mid, bottom),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun ExploreNavIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.28f
        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = stroke)
        // Handle
        drawLine(
            color = color,
            start = Offset(cx + r * 0.7f, cy + r * 0.7f),
            end = Offset(cx + r * 1.45f, cy + r * 1.45f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun ProfileNavIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
        val cx = size.width / 2f
        // Head
        drawCircle(
            color = color,
            radius = size.minDimension * 0.16f,
            center = Offset(cx, size.height * 0.32f),
            style = stroke,
        )
        // Shoulders
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.82f)
            quadraticBezierTo(
                cx, size.height * 0.52f,
                size.width * 0.8f, size.height * 0.82f,
            )
        }
        drawPath(path = path, color = color, style = stroke)
    }
}
