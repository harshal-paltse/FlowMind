package com.example.flowmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onBackClicked: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val workflows by viewModel.workflows.collectAsState()
    val snackHost = remember { SnackbarHostState() }

    // Snackbar feedback
    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { ev ->
            snackHost.showSnackbar(ev.message,
                withDismissAction = true,
                duration = SnackbarDuration.Short)
        }
    }

    // Local state
    var selectedWf     by remember { mutableStateOf(workflows.firstOrNull()) }
    var delayMinutes   by remember { mutableFloatStateOf(30f) }
    var intervalHours  by remember { mutableFloatStateOf(6f) }
    var notifTitle     by remember { mutableStateOf("FlowMind") }
    var notifBody      by remember { mutableStateOf("Your workflow is ready!") }
    var tabIndex       by remember { mutableIntStateOf(0) }

    LaunchedEffect(workflows) {
        if (selectedWf == null) selectedWf = workflows.firstOrNull()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackHost) },
        topBar = {
            TopAppBar(
                title = { Text("Schedule & Notify", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 },
                    text = { Text("Schedule") }, icon = { Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp)) })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 },
                    text = { Text("Notify") }, icon = { Icon(Icons.Filled.Notifications, null, Modifier.size(18.dp)) })
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (tabIndex == 0) {
                    // ── Schedule tab ──────────────────────────────────────────
                    ScheduleCard(
                        workflows   = workflows,
                        selectedWf  = selectedWf,
                        onSelectWf  = { selectedWf = it },
                        delayMin    = delayMinutes,
                        onDelayChange = { delayMinutes = it },
                        intervalHrs = intervalHours,
                        onIntervalChange = { intervalHours = it },
                        onScheduleOnce = {
                            selectedWf?.let { viewModel.scheduleOnce(it, delayMinutes.toLong()) }
                        },
                        onScheduleRepeat = {
                            selectedWf?.let { viewModel.scheduleRepeating(it, intervalHours.toLong()) }
                        },
                        onCancel = {
                            selectedWf?.let { viewModel.cancelSchedule(it) }
                        }
                    )
                } else {
                    // ── Notify tab ────────────────────────────────────────────
                    NotifyCard(
                        title = notifTitle,
                        body  = notifBody,
                        onTitleChange = { notifTitle = it },
                        onBodyChange  = { notifBody = it },
                        onSend = { viewModel.sendTestNotification(notifTitle, notifBody) }
                    )
                }
            }
        }
    }
}

// ── Schedule card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleCard(
    workflows: List<com.example.flowmind.domain.models.Workflow>,
    selectedWf: com.example.flowmind.domain.models.Workflow?,
    onSelectWf: (com.example.flowmind.domain.models.Workflow) -> Unit,
    delayMin: Float, onDelayChange: (Float) -> Unit,
    intervalHrs: Float, onIntervalChange: (Float) -> Unit,
    onScheduleOnce: () -> Unit,
    onScheduleRepeat: () -> Unit,
    onCancel: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Workflow picker
    SectionCard("Select Workflow") {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedWf?.name ?: "No workflows yet",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                workflows.forEach { wf ->
                    DropdownMenuItem(
                        text = { Text(wf.name) },
                        onClick = { onSelectWf(wf); expanded = false }
                    )
                }
                if (workflows.isEmpty()) {
                    DropdownMenuItem(text = { Text("No workflows — build one first") }, onClick = {})
                }
            }
        }
    }

    // One-time delay
    SectionCard("Run Once After") {
        Text("Delay: ${delayMin.toInt()} minutes", fontSize = 14.sp)
        Slider(value = delayMin, onValueChange = onDelayChange, valueRange = 1f..1440f,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary))
        Button(onClick = onScheduleOnce, modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = selectedWf != null) {
            Icon(Icons.Filled.AlarmAdd, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Schedule Once")
        }
    }

    // Repeating
    SectionCard("Repeat Every") {
        Text("Interval: ${intervalHrs.toInt()} hours", fontSize = 14.sp)
        Slider(value = intervalHrs, onValueChange = onIntervalChange, valueRange = 1f..168f,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onScheduleRepeat, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = selectedWf != null) {
                Icon(Icons.Filled.Repeat, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Set Repeat")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f),
                enabled = selectedWf != null) {
                Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cancel")
            }
        }
    }
}

// ── Notify card ───────────────────────────────────────────────────────────────

@Composable
private fun NotifyCard(
    title: String, body: String,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSend: () -> Unit
) {
    SectionCard("Push Notification via /v1/notify") {
        OutlinedTextField(
            value = title, onValueChange = onTitleChange,
            label = { Text("Title") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp), singleLine = true
        )
        OutlinedTextField(
            value = body, onValueChange = onBodyChange,
            label = { Text("Body") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp), maxLines = 3
        )
        Button(
            onClick = onSend,
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Send Notification")
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
