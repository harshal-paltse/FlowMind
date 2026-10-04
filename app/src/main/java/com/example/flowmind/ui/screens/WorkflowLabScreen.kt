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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowLabScreen(onBackClicked: () -> Unit) {
    var ramMb    by remember { mutableFloatStateOf(2048f) }   // simulated RAM
    var battery  by remember { mutableFloatStateOf(80f) }
    var isWifi   by remember { mutableStateOf(true) }
    var isOffline by remember { mutableStateOf(false) }
    var privacyShield by remember { mutableStateOf(false) }

    // Derived plan
    val plan = derivePlan(ramMb.toInt(), battery.toInt(), isWifi, isOffline, privacyShield)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workflow Lab", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Simulate device conditions and watch the orchestrator re-plan in real time.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(.6f))

            // ── Sliders ──
            LabCard("RAM Budget") {
                Text("Simulated free RAM: ${ramMb.toInt()} MB", fontSize = 14.sp)
                Slider(value = ramMb, onValueChange = { ramMb = it }, valueRange = 256f..8192f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary))
            }

            LabCard("Battery Level") {
                Text("Battery: ${battery.toInt()}%", fontSize = 14.sp)
                Slider(value = battery, onValueChange = { battery = it }, valueRange = 5f..100f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary))
            }

            // ── Toggles ──
            LabCard("Network & Privacy") {
                LabToggle("Wi-Fi connected", isWifi) { isWifi = it; if (it) isOffline = false }
                LabToggle("Offline mode (queue cloud steps)", isOffline) { isOffline = it; if (it) isWifi = false }
                LabToggle("Privacy Shield (local only — never cloud)", privacyShield) { privacyShield = it }
            }

            // ── Live plan ──
            LabCard("Live Orchestration Plan") {
                plan.steps.forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        val isCloud = step.execution == "Cloud"
                        val tint = if (isCloud) Color(0xFF7C3AED) else MaterialTheme.colorScheme.primary
                        Icon(if (isCloud) Icons.Filled.Cloud else Icons.Filled.PhoneAndroid,
                            contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${i+1}. ${step.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(step.reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = tint.copy(.12f)) {
                            Text(step.execution, fontSize = 10.sp, color = tint, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Est. time: ${plan.estimatedMs}ms  ·  Battery cost: ${plan.batteryPct}%",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(.5f))
            }
        }
    }
}

@Composable
private fun LabCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun LabToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(.3f)))
    }
}

// ── Plan derivation ───────────────────────────────────────────────────────────

data class PlanStep(val name: String, val execution: String, val reason: String)
data class OrchestratorPlan(val steps: List<PlanStep>, val estimatedMs: Int, val batteryPct: Float)

private fun derivePlan(ramMb: Int, battery: Int, wifi: Boolean, offline: Boolean, privacy: Boolean): OrchestratorPlan {
    val steps = mutableListOf<PlanStep>()

    fun exec(needsRam: Int, cloudOk: Boolean): String {
        if (privacy) return "Local"
        if (offline) return if (cloudOk) "Queued" else "Local"
        if (ramMb < needsRam) return if (cloudOk) "Cloud" else "Local"
        if (battery < 20 && needsRam > 512) return if (cloudOk) "Cloud" else "Local"
        return "Local"
    }

    steps += PlanStep("Input capture", "Local", "Always on-device")
    val ocrEx = exec(256, !privacy)
    steps += PlanStep("OCR / Perception", ocrEx,
        if (ocrEx == "Cloud") "Insufficient RAM (${ramMb}MB < 512MB)" else "RAM OK, running on-device")
    val llmEx = exec(1024, !privacy)
    steps += PlanStep("LLM Generate", llmEx,
        when {
            privacy -> "Privacy Shield active"
            offline && llmEx == "Queued" -> "Offline — will resume on Wi-Fi"
            llmEx == "Cloud" -> "LLM > 1GB, offloading to Cloudflare"
            else -> "Fits in ${ramMb}MB RAM"
        })
    steps += PlanStep("Transform / Format", "Local", "CPU-only, always local")
    val actEx = exec(0, !privacy && wifi)
    steps += PlanStep("Action: Save / Notify", if (actEx == "Cloud" || (wifi && !privacy)) "Cloud" else "Local",
        if (!wifi) "No Wi-Fi — saving locally" else "Sending via /v1/notify")

    val ms: Int = steps.map { if (it.execution == "Local") 400 else 900 }.sum()
    val batt = steps.count { it.execution == "Local" } * 0.3f + steps.count { it.execution == "Cloud" } * 0.1f
    return OrchestratorPlan(steps, ms, batt)
}
