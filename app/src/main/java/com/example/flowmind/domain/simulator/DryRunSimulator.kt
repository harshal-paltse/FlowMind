package com.example.flowmind.domain.simulator

import com.example.flowmind.domain.models.Workflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Represents the current state of a dry-run simulation.
 */
sealed class SimulationState {
    /** Simulation has not started yet. */
    object Idle : SimulationState()

    /**
     * A node is currently being processed.
     *
     * @property nodeName  Name of the node being executed.
     * @property progress  Completion percentage [0, 100].
     * @property nodeIndex Zero-based index in the node list.
     * @property totalNodes Total number of nodes in the workflow.
     */
    data class RunningNode(
        val nodeName: String,
        val progress: Int,
        val nodeIndex: Int = 0,
        val totalNodes: Int = 0
    ) : SimulationState()

    /**
     * A node has finished executing.
     *
     * @property nodeName        Name of the completed node.
     * @property simulatedOutput Human-readable output produced by the node.
     */
    data class NodeComplete(
        val nodeName: String,
        val simulatedOutput: String
    ) : SimulationState()

    /**
     * All nodes completed successfully.
     *
     * @property totalTimeMs Wall-clock duration of the full simulation run.
     * @property nodeCount   Number of nodes that were executed.
     */
    data class Success(
        val totalTimeMs: Long,
        val nodeCount: Int = 0
    ) : SimulationState()

    /**
     * The simulation was cancelled by the user.
     *
     * @property completedNodes How many nodes finished before cancellation.
     */
    data class Cancelled(val completedNodes: Int) : SimulationState()

    /**
     * An unrecoverable error occurred during simulation.
     *
     * @property reason Human-readable error description.
     */
    data class Error(val reason: String) : SimulationState()
}

/**
 * Simulates a workflow execution step-by-step without actually invoking ML models.
 *
 * Designed for the "dry run" feature in [WorkflowLabScreen]. The returned [Flow]
 * emits a sequence of [SimulationState] updates and respects coroutine cancellation —
 * it will emit [SimulationState.Cancelled] if the collector's scope is cancelled.
 */
class DryRunSimulator {

    /**
     * Simulates the given [workflow] and emits state updates via a cold [Flow].
     *
     * The flow respects coroutine cancellation; cancel the collecting scope to
     * stop the simulation early.
     */
    fun simulateWorkflow(workflow: Workflow): Flow<SimulationState> = flow {
        try {
            val startTime = System.currentTimeMillis()

            // Parse node names from nodesJson; fall back to a default list for demos
            val mockNodes = parseNodes(workflow.nodesJson)
            val totalNodes = mockNodes.size
            var completedNodes = 0

            for ((index, node) in mockNodes.withIndex()) {

                // Respect cancellation between nodes
                if (!coroutineContext.isActive) {
                    emit(SimulationState.Cancelled(completedNodes))
                    return@flow
                }

                // Emit incremental progress for this node
                for (p in 10..100 step 30) {
                    if (!coroutineContext.isActive) {
                        emit(SimulationState.Cancelled(completedNodes))
                        return@flow
                    }
                    emit(SimulationState.RunningNode(node, p, index, totalNodes))
                    delay(200)
                }

                emit(SimulationState.NodeComplete(node, "Output: [Simulated data for $node]"))
                completedNodes++
                delay(300)
            }

            val totalTime = System.currentTimeMillis() - startTime
            emit(SimulationState.Success(totalTime, completedNodes))

        } catch (e: Exception) {
            emit(SimulationState.Error(e.message ?: "Unknown simulation error"))
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Attempts to extract node names from [nodesJson].
     * Falls back to a default demo pipeline on parse failure.
     */
    private fun parseNodes(nodesJson: String): List<String> {
        return try {
            // Simple extraction: ["Node A","Node B"] → [Node A, Node B]
            nodesJson.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
                .ifEmpty { defaultPipeline() }
        } catch (_: Exception) {
            defaultPipeline()
        }
    }

    private fun defaultPipeline() =
        listOf("Trigger", "Text Extractor", "Condition", "Action: Save DB")
}
