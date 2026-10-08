package com.example.flowmind.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// SimpleBarChart
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A lightweight bar-chart composable that renders labelled vertical bars
 * using a vertical gradient fill for a polished look.
 *
 * @param data       Map of label → value pairs. Order is preserved.
 * @param modifier   Modifier applied to the outer [Column].
 * @param barColor   Base colour used for the gradient fill. Defaults to the
 *                   current Material primary colour.
 * @param chartHeight Height allocated to the Canvas drawing area.
 */
@Composable
fun SimpleBarChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    chartHeight: Dp = 200.dp
) {
    if (data.isEmpty()) return
    val maxValue = data.values.maxOrNull() ?: 1f

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / (data.size * 2f)
            val spacing = barWidth

            var startX = spacing / 2f

            data.forEach { (_, value) ->
                val barHeight = (value / maxValue) * canvasHeight
                val topLeft = Offset(startX, canvasHeight - barHeight)

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(barColor.copy(alpha = 0.55f), barColor),
                        startY = topLeft.y,
                        endY = canvasHeight
                    ),
                    topLeft = topLeft,
                    size = Size(barWidth, barHeight)
                )
                startX += barWidth + spacing
            }
        }

        // Label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.keys.forEach { key ->
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SimpleLineChart
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A lightweight line-chart composable that plots a list of float values as a
 * connected series with data-point circles.
 *
 * @param data      Ordered list of float data values.
 * @param modifier  Modifier applied to the drawing [Canvas].
 * @param lineColor Colour used for the line and data-point circles.
 */
@Composable
fun SimpleLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return
    val maxValue = data.maxOrNull() ?: 1f

    Canvas(modifier = modifier.height(200.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val pointSpacing = canvasWidth / (data.size - 1).coerceAtLeast(1)

        var previousOffset: Offset? = null

        data.forEachIndexed { index, value ->
            val x = index * pointSpacing
            val y = canvasHeight - ((value / maxValue) * canvasHeight)
            val currentOffset = Offset(x, y)

            previousOffset?.let { prev ->
                drawLine(
                    color = lineColor,
                    start = prev,
                    end = currentOffset,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                color = lineColor,
                radius = 6.dp.toPx(),
                center = currentOffset
            )
            previousOffset = currentOffset
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SimplePieChart
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A pie chart composable with a colour-keyed legend.
 *
 * @param data    Map of label → value pairs representing each segment.
 * @param modifier Modifier applied to the outer [Column].
 * @param colors  Colours cycled across segments. Falls back cyclically when
 *                [data] has more entries than [colors].
 */
@Composable
fun SimplePieChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF6C63FF),
        Color(0xFF00C853),
        Color(0xFFFF6D00),
        Color(0xFF00B0FF),
        Color(0xFFE91E63)
    )
) {
    if (data.isEmpty()) return
    val total = data.values.sum()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = -90f
            data.values.forEachIndexed { index, value ->
                val sweepAngle = (value / total) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            data.keys.forEachIndexed { index, key ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = colors[index % colors.size])
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val pct = String.format("%.1f", (data[key]!! / total) * 100)
                    Text(
                        text = "$key ($pct%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonutChart  (new)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * An animated donut chart that sweeps in each segment on first composition.
 *
 * Segments are drawn as arcs with a configurable stroke width, leaving the
 * centre transparent so a label can be overlaid via [centerLabel].
 *
 * @param data         Map of label → value pairs representing each segment.
 * @param modifier     Modifier applied to the outer [Box].
 * @param colors       Colours cycled across segments.
 * @param strokeWidth  Thickness of the donut ring in dp.
 * @param centerLabel  Optional composable rendered in the centre of the ring
 *                     (e.g. a total-value [Text]).
 */
@Composable
fun DonutChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF6C63FF),
        Color(0xFF00C853),
        Color(0xFFFF6D00),
        Color(0xFF00B0FF),
        Color(0xFFE91E63)
    ),
    strokeWidth: Dp = 28.dp,
    centerLabel: @Composable (BoxScope.() -> Unit)? = null
) {
    if (data.isEmpty()) return
    val total = data.values.sum()

    // Animate a 0→1 progress on first composition
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "donut_sweep"
    )
    LaunchedEffect(Unit) { animationPlayed = true }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
                var startAngle = -90f

                data.values.forEachIndexed { index, value ->
                    val sweepAngle = (value / total) * 360f * animatedProgress
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = stroke,
                        size = Size(size.width, size.height)
                    )
                    startAngle += (value / total) * 360f
                }
            }
            centerLabel?.invoke(this)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Column(modifier = Modifier.fillMaxWidth()) {
            data.keys.forEachIndexed { index, key ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = colors[index % colors.size])
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val pct = String.format("%.1f", (data[key]!! / total) * 100)
                    Text(
                        text = "$key ($pct%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
