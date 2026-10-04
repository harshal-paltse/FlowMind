package com.example.flowmind.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowmind.domain.manager.DeviceFitScorer
import com.example.flowmind.domain.models.ModelRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelCandidatesScreen(onBackClicked: () -> Unit) {
    val context = LocalContext.current
    val scorer = remember { DeviceFitScorer(context) }
    val scores = remember { scorer.scoreModels(ModelRegistry.models) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Candidates", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    Button(onClick = onBackClicked, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("<")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(scores) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.model.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Size: ${item.model.sizeMb}MB | RAM req: ${item.model.requiredRamMb}MB", fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Device Fit Score: ${item.score}/100", fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(
                            progress = { item.score / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 4.dp),
                            color = if (item.isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Reason: ${item.reason}", fontSize = 14.sp, color = if (item.isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* Handle download via ModelDownloader */ },
                            enabled = !item.isRejected,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Download Model")
                        }
                    }
                }
            }
        }
    }
}
