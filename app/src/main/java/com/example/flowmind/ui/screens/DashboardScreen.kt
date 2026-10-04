package com.example.flowmind.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flowmind.domain.models.Workflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToBuilder: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToLiveInput: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToLab: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onRunWorkflow: (String) -> Unit = {},
    inferVM: InferViewModel = hiltViewModel(),
    dashVM: DashboardViewModel = hiltViewModel()
) {
    var promptText by remember { mutableStateOf("") }
    val uiState by inferVM.uiState.collectAsState()
    val workflows by dashVM.workflows.collectAsState()
    val journeyStep by dashVM.journeyStep.collectAsState()

    LaunchedEffect(uiState.result) {
        val result = uiState.result ?: return@LaunchedEffect
        if (result.isNotBlank()) {
            dashVM.addAiWorkflow(result)
            inferVM.resetResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FlowMind", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToReports) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Reports", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onNavigateToLiveInput) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Live Input", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onNavigateToModels) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Models", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onNavigateToSchedule) {
                        Icon(Icons.Filled.Schedule, contentDescription = "Schedule", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onNavigateToLab) {
                        Icon(Icons.Filled.Science, contentDescription = "Lab", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToBuilder,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Visual Builder") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── Journey card ──
            item {
                JourneyCard(step = journeyStep, modifier = Modifier.padding(16.dp))
            }

            // ── AI Generate box ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Workflow via AI", fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = promptText, onValueChange = { promptText = it },
                            placeholder = { Text("e.g. Take a photo of a plant and explain symptoms...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), maxLines = 3
                        )
                        if (uiState.error != null) {
                            Text("⚠ ${uiState.error}", color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (promptText.isNotBlank()) {
                                    inferVM.infer(capability = "text", prompt = promptText)
                                    promptText = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Orchestrating...")
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Generate")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── My Automations header ──
            item {
                Text("My Automations", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            }

            // ── Workflow list from Room ──
            if (workflows.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null,
                            modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(.35f))
                        Spacer(Modifier.height(12.dp))
                        Text("No automations yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(.5f))
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onNavigateToBuilder) { Text("Create your first workflow →") }
                    }
                }
            } else {
                items(workflows, key = { it.id }) { wf ->
                    WorkflowCard(
                        workflow = wf,
                        onRunClicked = { onRunWorkflow(wf.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ── Journey card ──────────────────────────────────────────────────────────────

private val JOURNEY_STEPS = listOf(
    "Pick a template", "Provide input", "Dry-run preview",
    "Run workflow", "Review results", "Export & notify"
)

@Composable
fun JourneyCard(step: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Your Journey", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            JOURNEY_STEPS.forEachIndexed { i, label ->
                val done = i < step
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Icon(
                        if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(.3f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 13.sp,
                        color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(.5f),
                        fontWeight = if (i == step) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { step.toFloat() / JOURNEY_STEPS.size },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(.15f)
            )
        }
    }
}

// ── Workflow card ─────────────────────────────────────────────────────────────

@Composable
fun WorkflowCard(workflow: Workflow, onRunClicked: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(workflow.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(workflow.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(.7f))
            }
            IconButton(onClick = onRunClicked,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(.1f))) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Run", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
