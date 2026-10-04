package com.example.flowmind.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flowmind.domain.models.Workflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    workflow: Workflow,
    onBack: () -> Unit,
    viewModel: RunViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Auto-start the run when this screen opens
    LaunchedEffect(workflow.id) {
        if (state is RunUiState.Idle) viewModel.runWorkflow(workflow)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workflow.name, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { viewModel.reset(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when (val s = state) {
                is RunUiState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is RunUiState.Running -> {
                    RunningTimeline(
                        steps       = s.steps,
                        currentStep = s.currentStep,
                        wfName      = s.workflowName
                    )
                }

                is RunUiState.Success -> {
                    SuccessView(s = s, onReset = { viewModel.reset(); onBack() })
                }

                is RunUiState.Failure -> {
                    FailureView(reason = s.reason, onRetry = { viewModel.runWorkflow(workflow) })
                }
            }
        }
    }
}

// ── Running timeline ──────────────────────────────────────────────────────────

@Composable
private fun RunningTimeline(
    steps: List<TimelineStep>,
    currentStep: Int,
    wfName: String
) {


    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Text("Running…", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text(wfName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(.5f))
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (currentStep.toFloat() / steps.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color    = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(.15f)
            )
            Spacer(Modifier.height(24.dp))
        }

        items(steps) { step ->
            TimelineRow(step = step)
            // Connector line (not after last)
            if (steps.indexOf(step) < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .padding(start = 19.dp)
                        .width(2.dp)
                        .height(20.dp)
                        .background(
                            if (step.status == StepStatus.Done) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground.copy(.15f)
                        )
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(step: TimelineStep) {


    val tint = when (step.status) {
        StepStatus.Done    -> MaterialTheme.colorScheme.primary
        StepStatus.Failed  -> MaterialTheme.colorScheme.error
        StepStatus.Running -> MaterialTheme.colorScheme.primary.copy(.6f)
        StepStatus.Pending -> MaterialTheme.colorScheme.onBackground.copy(.25f)
    }

    AnimatedVisibility(true, enter = fadeIn() + slideInVertically()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(.12f)),
                contentAlignment = Alignment.Center
            ) {
                when (step.status) {
                    StepStatus.Pending -> Icon(Icons.Filled.RadioButtonUnchecked, null,
                        tint = tint, modifier = Modifier.size(20.dp))
                    StepStatus.Running -> CircularProgressIndicator(
                        Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    StepStatus.Done    -> Icon(Icons.Filled.CheckCircle, null,
                        tint = tint, modifier = Modifier.size(20.dp))
                    StepStatus.Failed  -> Icon(Icons.Filled.Cancel, null,
                        tint = tint, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(step.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                if (step.status != StepStatus.Pending) {
                    Text(
                        buildString {
                            if (step.execution.isNotBlank()) append("${step.execution}  ")
                            if (step.latencyMs > 0) append("${step.latencyMs}ms")
                        },
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.5f)
                    )
                }
            }

            // Execution badge
            if (step.status != StepStatus.Pending) {
                val badgeColor = if (step.execution == "Cloud") Color(0xFF7C3AED) else MaterialTheme.colorScheme.primary
                Surface(shape = RoundedCornerShape(6.dp), color = badgeColor.copy(.12f)) {
                    Text(step.execution, fontSize = 10.sp, color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ── Success view ──────────────────────────────────────────────────────────────

@Composable
private fun SuccessView(s: RunUiState.Success, onReset: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Workflow complete!", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text("${s.totalMs}ms total", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(.5f))
                }
            }
        }

        // Step summary cards
        items(s.steps) { step ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (step.status == StepStatus.Done)
                        MaterialTheme.colorScheme.primaryContainer.copy(.5f)
                    else MaterialTheme.colorScheme.errorContainer.copy(.4f)
                )
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        if (step.status == StepStatus.Done) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        null,
                        tint = if (step.status == StepStatus.Done) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(step.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (step.output.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(step.output.take(200), fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(.7f))
                        }
                    }
                    Text("${step.latencyMs}ms", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(.4f))
                }
            }
        }

        item {
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Home, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Back to Dashboard")
            }
        }
    }
}

// ── Failure view ──────────────────────────────────────────────────────────────

@Composable
private fun FailureView(reason: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Filled.ErrorOutline, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp))
            Text("Run Failed", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(reason, color = MaterialTheme.colorScheme.onBackground.copy(.6f),
                fontSize = 14.sp)
            Button(onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}
