package com.example.flowmind.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SimpleBarChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return
    val maxValue = data.values.maxOrNull() ?: 1f

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / (data.size * 2f)
            val spacing = barWidth

            var startX = spacing / 2f

            data.forEach { (_, value) ->
                val barHeight = (value / maxValue) * canvasHeight
                drawRect(
                    color = barColor,
                    topLeft = Offset(startX, canvasHeight - barHeight),
                    size = Size(barWidth, barHeight)
                )
                startX += barWidth + spacing
            }
        }
        
        // Legend
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            data.keys.forEach { key ->
                Text(text = key, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

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

            if (previousOffset != null) {
                drawLine(
                    color = lineColor,
                    start = previousOffset!!,
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

@Composable
fun SimplePieChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan)
) {
    if (data.isEmpty()) return
    val total = data.values.sum()

    Column(modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
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
        
        // Legend
        Column(modifier = Modifier.fillMaxWidth()) {
            data.keys.forEachIndexed { index, key ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = colors[index % colors.size])
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "$key (${String.format("%.1f", (data[key]!! / total) * 100)}%)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
