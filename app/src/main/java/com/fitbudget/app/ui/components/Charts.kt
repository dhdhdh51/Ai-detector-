package com.fitbudget.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Minimal line chart drawn with Compose Canvas - no third-party chart dependency, works offline
 * and only ever renders values that were actually logged.
 */
@Composable
fun LineChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.14f),
    targetValue: Double? = null,
    startLabel: String? = null,
    endLabel: String? = null,
    valueFormatter: (Double) -> String = { "%.1f".format(it) }
) {
    if (points.isEmpty()) return

    val values = points.map { it.second }
    val rawMin = minOf(values.min(), targetValue ?: values.min())
    val rawMax = maxOf(values.max(), targetValue ?: values.max())
    val padding = ((rawMax - rawMin) * 0.15).takeIf { it > 0.0001 } ?: 1.0
    val minValue = rawMin - padding
    val maxValue = rawMax + padding
    val span = (maxValue - minValue).takeIf { abs(it) > 0.0001 } ?: 1.0

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val targetColor = MaterialTheme.colorScheme.secondary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .height(height)
                    .padding(end = 6.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    valueFormatter(maxValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    valueFormatter((maxValue + minValue) / 2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    valueFormatter(minValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
            ) {
                val w = size.width
                val h = size.height

                // Horizontal grid
                for (index in 0..2) {
                    val y = h * index / 2f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                fun xFor(index: Int): Float =
                    if (points.size == 1) w / 2f else w * index / (points.size - 1).toFloat()

                fun yFor(value: Double): Float =
                    (h - ((value - minValue) / span * h)).toFloat().coerceIn(0f, h)

                targetValue?.let { target ->
                    val y = yFor(target)
                    drawLine(
                        color = targetColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                }

                if (points.size == 1) {
                    drawCircle(
                        color = lineColor,
                        radius = 6f,
                        center = Offset(xFor(0), yFor(points[0].second))
                    )
                    return@Canvas
                }

                val linePath = Path()
                val fillPath = Path()
                points.forEachIndexed { index, point ->
                    val x = xFor(index)
                    val y = yFor(point.second)
                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(xFor(points.size - 1), h)
                fillPath.close()

                drawPath(fillPath, color = fillColor)
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                points.forEachIndexed { index, point ->
                    drawCircle(
                        color = lineColor,
                        radius = 4f,
                        center = Offset(xFor(index), yFor(point.second))
                    )
                }
            }
        }

        if (startLabel != null || endLabel != null) {
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 52.dp)) {
                Text(
                    startLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    endLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Bar chart for daily spending / steps. [limitValue] draws the budget or goal line.
 */
@Composable
fun BarChart(
    bars: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    overLimitColor: Color = MaterialTheme.colorScheme.error,
    limitValue: Double? = null,
    startLabel: String? = null,
    endLabel: String? = null,
    valueFormatter: (Double) -> String = { "%.0f".format(it) }
) {
    if (bars.isEmpty()) return

    val maxValue = maxOf(bars.maxOf { it.second }, limitValue ?: 0.0).takeIf { it > 0.0 } ?: 1.0
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val limitColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .height(height)
                    .padding(end = 6.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    valueFormatter(maxValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    valueFormatter(maxValue / 2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
            ) {
                val w = size.width
                val h = size.height

                for (index in 0..2) {
                    val y = h * index / 2f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                val slot = w / bars.size
                val barWidth = (slot * 0.62f).coerceAtLeast(2f)

                bars.forEachIndexed { index, (_, value) ->
                    val barHeight = ((value / maxValue) * h).toFloat().coerceIn(0f, h)
                    val left = slot * index + (slot - barWidth) / 2f
                    // Track behind every bar so empty days are visibly empty, not missing.
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, h - 3f),
                        size = Size(barWidth, 3f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                    if (barHeight > 0f) {
                        val overLimit = limitValue != null && value > limitValue
                        drawRoundRect(
                            color = if (overLimit) overLimitColor else barColor,
                            topLeft = Offset(left, h - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                    }
                }

                limitValue?.let { limit ->
                    val y = (h - (limit / maxValue * h)).toFloat().coerceIn(0f, h)
                    drawLine(
                        color = limitColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                }
            }
        }

        if (startLabel != null || endLabel != null) {
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 52.dp)) {
                Text(
                    startLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    endLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Compact seven-day activity strip used on the dashboard. */
@Composable
fun MiniBarRow(
    values: List<Int>,
    goal: Int,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (values.isEmpty()) return
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier.fillMaxWidth().height(44.dp)) {
        val maxValue = maxOf(values.max(), goal, 1)
        val slot = size.width / values.size
        val barWidth = (slot * 0.5f).coerceAtLeast(2f)
        values.forEachIndexed { index, value ->
            val left = slot * index + (slot - barWidth) / 2f
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            val barHeight = (value.toFloat() / maxValue * size.height).coerceIn(0f, size.height)
            if (barHeight > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        }
    }
}
