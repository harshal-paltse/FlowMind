package com.example.flowmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flowmind.domain.models.TriggerType
import kotlinx.coroutines.launch
import java.util.UUID

data class NodeUI(val id: String = UUID.randomUUID().toString(), val title: String, val type: String, val isPrivate: Boolean = false)

sealed class BuilderUiState {
    object Idle    : BuilderUiState()
    object Loading : BuilderUiState()
    data class Success(val msg: String) : BuilderUiState()
    data class Error(val msg: String)   : BuilderUiState()
}

private data class TemplateCard(
    val id: String, val title: String, val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val nodes: List<NodeUI>, val trigger: TriggerType
)

private val TEMPLATES = listOf(
    TemplateCard("bill", "Bill → Expense Tracker", "OCR receipt → items+prices → donut by category",
        Icons.Filled.Receipt,
        listOf(NodeUI(title="Trigger: Camera", type="Trigger: Camera"), NodeUI(title="AI: OCR", type="AI: OCR"),
               NodeUI(title="Transform: Categorize", type="Transform: Categorize"), NodeUI(title="Action: Save DB", type="Action: Save DB")),
        TriggerType.CAMERA),
    TemplateCard("lecture","Lecture → Notes & Quiz","Transcript → key concepts → 5-question quiz",
        Icons.Filled.School,
        listOf(NodeUI(title="Trigger: Mic", type="Trigger: Mic"), NodeUI(title="AI: STT", type="AI: STT"),
               NodeUI(title="AI: Summarize", type="AI: Summarize"), NodeUI(title="AI: LLM Generate", type="AI: LLM Generate")),
        TriggerType.MIC),
    TemplateCard("plant","Plant Disease → Care Plan","Diagnose disease → 7-day care checklist",
        Icons.Filled.Eco,
        listOf(NodeUI(title="Trigger: Camera", type="Trigger: Camera"), NodeUI(title="AI: Vision Classify", type="AI: Vision Classify"),
               NodeUI(title="AI: LLM Generate", type="AI: LLM Generate"), NodeUI(title="Action: Notify", type="Action: Notify")),
        TriggerType.CAMERA),
    TemplateCard("meeting","Meeting → Action Items","Record → summary → calendar events",
        Icons.Filled.People,
        listOf(NodeUI(title="Trigger: Mic", type="Trigger: Mic"), NodeUI(title="AI: STT", type="AI: STT"),
               NodeUI(title="AI: Summarize", type="AI: Summarize"), NodeUI(title="Action: Calendar", type="Action: Calendar")),
        TriggerType.MIC),
    TemplateCard("medicine","Medicine Label → Reminders","Read dosage → schedule reminders",
        Icons.Filled.MedicalServices,
        listOf(NodeUI(title="Trigger: Camera", type="Trigger: Camera"), NodeUI(title="AI: OCR", type="AI: OCR"),
               NodeUI(title="Transform: Format", type="Transform: Format"), NodeUI(title="Action: Notify", type="Action: Notify")),
        TriggerType.CAMERA)
)

private val PALETTE_SECTIONS = listOf(
    "Triggers"   to listOf("Trigger: Camera","Trigger: Mic","Trigger: Notification","Trigger: Schedule","Trigger: Share"),
    "AI"         to listOf("AI: OCR","AI: LLM Generate","AI: STT","AI: Vision Classify","AI: Summarize"),
    "Transform"  to listOf("Transform: Calculate","Transform: Categorize","Transform: Format"),
    "Actions"    to listOf("Action: Save PDF","Action: Save DB","Action: Notify","Action: Email","Action: Calendar")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowBuilderScreen(
    viewModel: BuilderViewModel = hiltViewModel()
) {
    val nodes = remember { mutableStateListOf<NodeUI>() }
    val saveState by viewModel.saveState.collectAsState()
    val scope = rememberCoroutineScope()
    var showPalette  by remember { mutableStateOf(false) }
    var selectedNode by remember { mutableStateOf<NodeUI?>(null) }
    var workflowName by remember { mutableStateOf("") }
    val paletteSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configSheet  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackHost    = remember { SnackbarHostState() }

    LaunchedEffect(saveState) {
        when (val s = saveState) {
            is BuilderUiState.Success -> { snackHost.showSnackbar(s.msg); viewModel.resetState() }
            is BuilderUiState.Error   -> { snackHost.showSnackbar(s.msg); viewModel.resetState() }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackHost) },
        topBar = {
            TopAppBar(
                title = { Text("Build Workflow", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (saveState == BuilderUiState.Loading) {
                        CircularProgressIndicator(Modifier.size(24.dp).padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = {
                            viewModel.save(nodes.toList(), workflowName,
                                nodes.firstOrNull { it.type.startsWith("Trigger") }
                                    ?.let { mapTrigger(it.type) } ?: TriggerType.NOTIFICATION)
                        }) {
                            Icon(Icons.Filled.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPalette = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Step") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Workflow name field
            item {
                OutlinedTextField(
                    value = workflowName,
                    onValueChange = { workflowName = it },
                    label = { Text("Workflow name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
            }

            // Empty state: template cards
            if (nodes.isEmpty()) {
                item {
                    Text("Start from a template", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(TEMPLATES) { tpl ->
                            TemplateStartCard(tpl) {
                                nodes.addAll(tpl.nodes)
                                workflowName = tpl.title
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    Text("Or build manually with the + button below.", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(.5f))
                    Spacer(Modifier.height(80.dp))
                }
            }

            // Node list
            itemsIndexed(nodes) { index, node ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    NodeRow(node = node, index = index,
                        onDelete = { nodes.remove(node) },
                        onClick  = { selectedNode = node },
                        onMoveUp = { if (index > 0) { val t = nodes[index-1]; nodes[index-1] = nodes[index]; nodes[index] = t } }
                    )
                    if (index < nodes.size - 1) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            if (nodes.isNotEmpty()) { item { Spacer(Modifier.height(80.dp)) } }
        }
    }

    // ── Palette sheet ──
    if (showPalette) {
        ModalBottomSheet(
            onDismissRequest = { showPalette = false },
            sheetState = paletteSheet,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Add Step", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                PALETTE_SECTIONS.forEach { (section, items) ->
                    item { Text(section, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(.5f), modifier = Modifier.padding(top = 12.dp)) }
                    items(items) { opt ->
                        OutlinedButton(
                            onClick = {
                                nodes.add(NodeUI(title = opt, type = opt))
                                scope.launch { paletteSheet.hide() }.invokeOnCompletion { showPalette = false }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(opt, modifier = Modifier.fillMaxWidth()) }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    // ── Config sheet ──
    if (selectedNode != null) {
        val node = selectedNode!!
        ModalBottomSheet(
            onDismissRequest = { selectedNode = null },
            sheetState = configSheet,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            var isPrivate by remember { mutableStateOf(node.isPrivate) }
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Configure Node", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(node.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Privacy Shield", fontWeight = FontWeight.SemiBold)
                        Text("Force local execution — data never leaves device", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(.6f))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    val idx = nodes.indexOfFirst { it.id == node.id }
                    if (idx >= 0) nodes[idx] = node.copy(isPrivate = isPrivate)
                    selectedNode = null
                }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Apply")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeRow(node: NodeUI, index: Int, onDelete: () -> Unit, onClick: () -> Unit, onMoveUp: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled) { onDelete(); true } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
            }
        }
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (node.isPrivate) MaterialTheme.colorScheme.primaryContainer
                                 else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth().height(70.dp).clickable { onClick() }
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp, modifier = Modifier.width(24.dp))
                if (node.isPrivate) Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp))
                Text(node.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up",
                        tint = MaterialTheme.colorScheme.primary.copy(.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TemplateStartCard(tpl: TemplateCard, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.width(180.dp).height(140.dp).clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Icon(tpl.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(tpl.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(tpl.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.7f), maxLines = 3)
        }
    }
}

private fun mapTrigger(type: String): TriggerType = when {
    type.contains("Camera")       -> TriggerType.CAMERA
    type.contains("Mic")          -> TriggerType.MIC
    type.contains("Schedule")     -> TriggerType.SCHEDULE
    type.contains("Share")        -> TriggerType.SHARE
    else                          -> TriggerType.NOTIFICATION
}
