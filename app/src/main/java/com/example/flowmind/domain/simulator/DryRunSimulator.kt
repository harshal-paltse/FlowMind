package com.example.flowmind.domain.simulator

import com.example.flowmind.domain.models.Workflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class SimulationState {
    object Idle : SimulationState()
    data class RunningNode(val nodeName: String, val progress: Int) : SimulationState()
    data class NodeComplete(val nodeName: String, val simulatedOutput: String) : SimulationState()
    data class Success(val totalTimeMs: Long) : SimulationState()
    data class Error(val reason: String) : SimulationState()
}

class DryRunSimulator {
    
    fun simulateWorkflow(workflow: Workflow): Flow<SimulationState> = flow {
        try {
            val startTime = System.currentTimeMillis()
            
            // In a real scenario, this parses workflow.nodesJson
            val mockNodes = listOf("Trigger", "Text Extractor", "Condition", "Action: Save DB")
            
            for (node in mockNodes) {
                // Emit running state
                for (p in 10..100 step 30) {
                    emit(SimulationState.RunningNode(node, p))
                    delay(200)
                }
                
                // Emit completion state for this node
                emit(SimulationState.NodeComplete(node, "Output: [Simulated Data for $node]"))
                delay(300)
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            emit(SimulationState.Success(totalTime))
            
        } catch (e: Exception) {
            emit(SimulationState.Error(e.message ?: "Unknown simulation error"))
        }
    }
}
