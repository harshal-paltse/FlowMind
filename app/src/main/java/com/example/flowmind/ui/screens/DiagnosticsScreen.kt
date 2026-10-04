package com.example.flowmind.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    vm: DiagnosticsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    // Auto-run once on first open
    LaunchedEffect(Unit) { vm.runAll() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.runAll() }, enabled = !state.running) {
                        Icon(Icons.Filled.Refresh, "Re-run",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // Overall banner
                val allPass = state.allPass
                val bannerColor = when {
                    state.running -> MaterialTheme.colorScheme.surfaceVariant
                    allPass -> Color(0xFF1B5E20).copy(.12f)
                    else -> MaterialTheme.colorScheme.errorContainer.copy(.5f)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerColor),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        when {
                            state.running -> CircularProgressIndicator(
                                Modifier.size(24.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary)
                            allPass -> Icon(Icons.Filled.CheckCircle, null,
                                tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                            else -> Icon(Icons.Filled.Warning, null,
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(
                                if (state.running) "Running checks…"
                                else if (allPass) "All checks passed — ready to use"
                                else "Some checks failed — fix before using the app",
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                            )
                            if (!state.running && !allPass) {
                                val failed = state.checks.count { it.status == CheckStatus.FAIL }
                                Text("$failed check(s) need attention",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            items(state.checks, key = { it.id }) { check ->
                CheckRow(check)
            }

            // Notification permission request button
            item {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { vm.runAll() }

                AnimatedVisibility(
                    state.checks.firstOrNull { it.id == "notif" }?.status == CheckStatus.FAIL
                ) {
                    Button(
                        onClick = { launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Notifications, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Grant Notification Permission")
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(check: DiagCheck) {
    val spin by rememberInfiniteTransition(label = "spin")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), "spin")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (check.status) {
                CheckStatus.PASS -> Color(0xFF1B5E20).copy(.07f)
                CheckStatus.FAIL -> MaterialTheme.colorScheme.errorContainer.copy(.35f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status icon
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(
                    when (check.status) {
                        CheckStatus.PASS -> Color(0xFF2E7D32).copy(.15f)
                        CheckStatus.FAIL -> MaterialTheme.colorScheme.error.copy(.15f)
                        CheckStatus.RUNNING -> MaterialTheme.colorScheme.primary.copy(.12f)
                        CheckStatus.PENDING -> MaterialTheme.colorScheme.onSurface.copy(.07f)
                    }
                ),
                contentAlignment = Alignment.Center
            ) {
                when (check.status) {
                    CheckStatus.PENDING -> Icon(Icons.Filled.HourglassEmpty, null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(.4f),
                        modifier = Modifier.size(18.dp))
                    CheckStatus.RUNNING -> Icon(Icons.Filled.Sync, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp).rotate(spin))
                    CheckStatus.PASS -> Icon(Icons.Filled.CheckCircle, null,
                        tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                    CheckStatus.FAIL -> Icon(Icons.Filled.Cancel, null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            Column(Modifier.weight(1f)) {
                Text(check.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                if (check.detail.isNotBlank()) {
                    Text(check.detail, fontSize = 12.sp,
                        color = if (check.status == CheckStatus.FAIL)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(.55f),
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }

            // Status chip
            val chipColor = when (check.status) {
                CheckStatus.PASS -> Color(0xFF2E7D32)
                CheckStatus.FAIL -> MaterialTheme.colorScheme.error
                CheckStatus.RUNNING -> MaterialTheme.colorScheme.primary
                CheckStatus.PENDING -> MaterialTheme.colorScheme.onSurface.copy(.35f)
            }
            Surface(shape = RoundedCornerShape(6.dp), color = chipColor.copy(.12f)) {
                Text(check.status.name, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = chipColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
    }
}
