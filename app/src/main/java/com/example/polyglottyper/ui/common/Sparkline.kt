package com.example.polyglottyper.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimal line chart with a soft fill — used for the WPM history card. */
@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val stroke = strokeWidth.toPx()
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = (size.width - stroke) / (values.size - 1)

        val line = Path()
        values.forEachIndexed { index, value ->
            val x = stroke / 2 + index * stepX
            val y = stroke / 2 + (1f - (value - min) / range) * (size.height - stroke)
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width - stroke / 2, size.height)
            lineTo(stroke / 2, size.height)
            close()
        }
        drawPath(fill, color.copy(alpha = 0.12f))
        drawPath(line, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}
