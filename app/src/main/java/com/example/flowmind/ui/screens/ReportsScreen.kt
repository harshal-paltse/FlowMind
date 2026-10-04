package com.example.flowmind.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flowmind.domain.models.RunRecord
import com.example.flowmind.domain.pdf.PdfGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBackClicked: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Insights", "Run History")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Reports", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (state is ReportsUiState.Ready) {
                        IconButton(onClick = {
                            scope.launch {
                                val runs = (state as ReportsUiState.Ready).runs
                                val gen = PdfGenerator(context)
                                val text = runs.take(10).joinToString("\n") {
                                    "${it.workflowName}: ${if (it.success) "✓" else "✗"} ${it.latencyMs}ms"
                                }
                                gen.generateWorkflowReport("FlowMind Insights", text, null)
                            }
                        }) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                }
            }

            when (val s = state) {
                is ReportsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReportsUiState.Empty -> {
                    EmptyRunsState(
                        onLoadSamples = { viewModel.loadSamples() }
                    )
                }
                is ReportsUiState.Ready -> {
                    val hasSamples = s.runs.any { it.isSample }
                    if (selectedTab == 0) {
                        InsightsTab(runs = s.runs, insights = s.insights, hasSamples = hasSamples,
                            onDeleteSamples = { viewModel.deleteSamples() })
                    } else {
                        HistoryTab(runs = s.runs, onDelete = { viewModel.deleteRun(it) })
                    }
                }
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyRunsState(onLoadSamples: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.BarChart, contentDescription = null,
            modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = .4f))
        Spacer(Modifier.height(16.dp))
        Text("No runs yet", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Run a workflow to see analytics here.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onLoadSamples, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Load sample data")
        }
    }
}

// ─── Insights tab ─────────────────────────────────────────────────────────────

@Composable
private fun InsightsTab(
    runs: List<RunRecord>,
    insights: InsightsData,
    hasSamples: Boolean,
    onDeleteSamples: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (hasSamples) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Showing sample data", Modifier.weight(1f), fontSize = 13.sp)
                        TextButton(onClick = onDeleteSamples) { Text("Remove") }
                    }
                }
            }
        }

        // KPI row
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Success Rate", "${(insights.successRate * 100).toInt()}%", Modifier.weight(1f))
                KpiCard("Avg Latency", "${insights.avgLatencyMs}ms", Modifier.weight(1f))
                KpiCard("Total Runs", "${insights.localCount + insights.cloudCount}", Modifier.weight(1f))
            }
        }

        // Latency line chart
        item {
            ChartCard("Latency per Run (ms)") {
                val latencies = runs.filter { it.success }.takeLast(20).map { it.latencyMs.toFloat() }
                if (latencies.size >= 2) AnimatedLineChart(latencies)
                else Text("Need ≥2 runs", color = MaterialTheme.colorScheme.onSurface.copy(.5f), modifier = Modifier.padding(16.dp))
            }
        }

        // Bar chart – runs per day local vs cloud
        item {
            ChartCard("Runs per Day (Local vs Cloud)") {
                val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
                val grouped = runs.groupBy { sdf.format(Date(it.runAt)) }
                val localByDay  = grouped.mapValues { (_, v) -> v.count { it.isLocal }.toFloat() }
                val cloudByDay  = grouped.mapValues { (_, v) -> v.count { !it.isLocal }.toFloat() }
                GroupedBarChart(localByDay, cloudByDay)
            }
        }

        // Donut – success / failure
        item {
            ChartCard("Success vs Failure") {
                val s = insights.successRate
                DonutChart(
                    slices = listOf(s, 1f - s),
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.error),
                    labels = listOf("Success ${(s*100).toInt()}%", "Fail ${((1-s)*100).toInt()}%")
                )
            }
        }

        // Local vs Cloud donut
        item {
            val total = (insights.localCount + insights.cloudCount).coerceAtLeast(1).toFloat()
            ChartCard("Local vs Cloud") {
                DonutChart(
                    slices = listOf(insights.localCount / total, insights.cloudCount / total),
                    colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF7C3AED)),
                    labels = listOf("Local ${insights.localCount}", "Cloud ${insights.cloudCount}")
                )
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.7f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ─── Canvas charts ─────────────────────────────────────────────────────────────

@Composable
fun AnimatedLineChart(data: List<Float>, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "line"
    )
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .1f)
    val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)

    var tapIndex by remember { mutableIntStateOf(-1) }

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(180.dp)
        .pointerInput(data) {
            detectTapGestures { offset ->
                val spacing = size.width.toFloat() / (data.size - 1).coerceAtLeast(1)
                tapIndex = (offset.x / spacing).toInt().coerceIn(0, data.size - 1)
            }
        }
    ) {
        val w = size.width; val h = size.height
        val spacing = w / (data.size - 1).coerceAtLeast(1)
        // Gridlines
        for (i in 0..4) {
            val y = h * i / 4f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
        }
        // Y-axis labels drawn via drawContext if needed – omitted for brevity
        // Animated path
        val path = Path()
        data.forEachIndexed { idx, v ->
            val x = idx * spacing
            val y = h - (v / maxVal) * h
            val drawX = (x * progress).coerceAtMost(w)
            if (idx == 0) path.moveTo(drawX, y) else path.lineTo(drawX, y)
        }
        drawPath(path, primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Dots
        data.forEachIndexed { idx, v ->
            val x = idx * spacing
            if (x > w * progress) return@forEachIndexed
            val y = h - (v / maxVal) * h
            drawCircle(primary, radius = 5.dp.toPx(), center = Offset(x, y))
        }
        // Tooltip
        if (tapIndex in data.indices) {
            val tx = tapIndex * spacing
            val ty = h - (data[tapIndex] / maxVal) * h
            drawLine(gridColor, Offset(tx, 0f), Offset(tx, h), strokeWidth = 1.5.dp.toPx())
            drawCircle(primary, 8.dp.toPx(), Offset(tx, ty))
            // White outline
            drawCircle(Color.White, 5.dp.toPx(), Offset(tx, ty))
        }
    }
    if (tapIndex in data.indices) {
        Text("Run ${tapIndex + 1}: ${data[tapIndex].toInt()}ms", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun GroupedBarChart(local: Map<String, Float>, cloud: Map<String, Float>, modifier: Modifier = Modifier) {
    val keys = (local.keys + cloud.keys).distinct().takeLast(7)
    val maxVal = ((local.values + cloud.values).maxOrNull() ?: 1f).coerceAtLeast(1f)
    val primary = MaterialTheme.colorScheme.primary
    val purple = Color(0xFF7C3AED)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val w = size.width; val h = size.height
            val slotW = w / keys.size
            val barW = slotW * 0.3f
            keys.forEachIndexed { i, key ->
                val slotX = i * slotW + slotW * 0.1f
                val lv = local[key] ?: 0f
                val cv = cloud[key] ?: 0f
                // local bar
                val lh = (lv / maxVal) * h
                drawRect(primary, topLeft = Offset(slotX, h - lh), size = Size(barW, lh))
                // cloud bar
                val ch = (cv / maxVal) * h
                drawRect(purple, topLeft = Offset(slotX + barW + 2.dp.toPx(), h - ch), size = Size(barW, ch))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            keys.forEach { Text(it, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(.7f)) }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(start = 4.dp)) {
            LegendDot(primary, "Local"); LegendDot(purple, "Cloud")
        }
    }
}

@Composable
fun DonutChart(slices: List<Float>, colors: List<Color>, labels: List<String>, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(1f, tween(700, easing = EaseOutCubic), label = "donut")
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(160.dp)) {
            val total = slices.sum().coerceAtLeast(0.001f)
            var start = -90f
            slices.forEachIndexed { i, v ->
                val sweep = (v / total) * 360f * progress
                drawArc(colors[i % colors.size], start, sweep, useCenter = false,
                    style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Butt),
                    size = Size(size.width * .75f, size.height * .75f),
                    topLeft = Offset(size.width * .125f, size.height * .125f))
                start += sweep
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            labels.forEachIndexed { i, l -> LegendDot(colors[i % colors.size], l) }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.8f))
    }
}

// ─── History tab ─────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(runs: List<RunRecord>, onDelete: (RunRecord) -> Unit) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(runs, key = { it.id }) { run ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (run.success) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = if (run.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(run.workflowName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("${sdf.format(Date(run.runAt))} · ${run.latencyMs}ms · ${if (run.isLocal) "Local" else "Cloud"}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
                        if (run.isSample) Text("SAMPLE", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary.copy(.7f))
                    }
                    IconButton(onClick = { onDelete(run) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(.7f))
                    }
                }
            }
        }
    }
}
