package com.example.flowmind.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class NodeUI(val id: String, val title: String, var position: Offset)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowBuilderScreen() {
    val nodes = remember {
        mutableStateListOf(
            NodeUI("n1", "Trigger: Camera", Offset(100f, 200f)),
            NodeUI("n2", "AI: OCR", Offset(400f, 200f)),
            NodeUI("n3", "Action: Save Expense", Offset(700f, 200f))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build Workflow", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add node */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (nodes.size >= 2) {
                    for (i in 0 until nodes.size - 1) {
                        val start = nodes[i].position
                        val end = nodes[i + 1].position
                        drawLine(
                            color = Color.Gray,
                            start = Offset(start.x + 250f, start.y + 100f), 
                            end = Offset(end.x, end.y + 100f),
                            strokeWidth = 5f
                        )
                    }
                }
            }

            nodes.forEach { node ->
                DraggableNode(
                    node = node,
                    onPositionChange = { newPos ->
                        val index = nodes.indexOfFirst { it.id == node.id }
                        if (index != -1) {
                            nodes[index] = nodes[index].copy(position = newPos)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DraggableNode(node: NodeUI, onPositionChange: (Offset) -> Unit) {
    var offsetX by remember { mutableFloatStateOf(node.position.x) }
    var offsetY by remember { mutableFloatStateOf(node.position.y) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(width = 180.dp, height = 80.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    onPositionChange(Offset(offsetX, offsetY))
                }
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = node.title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}
