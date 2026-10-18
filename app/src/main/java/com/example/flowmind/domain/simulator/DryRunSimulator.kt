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

                emit(SimulationState.NodeComplete(node, simulatedOutputFor(node)))
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
     * Returns a node-type-aware simulated output string for display in the Lab screen.
     *
     * The output is derived from the node's type prefix so the dry-run feels
     * realistic without executing any real ML model.
     */
    private fun simulatedOutputFor(nodeName: String): String = when {
        nodeName.startsWith("Trigger", ignoreCase = true)   ->
            "✅ Trigger fired — input data captured successfully."
        nodeName.startsWith("AI: OCR", ignoreCase = true)    ->
            "📄 OCR extracted 3 text regions with 94% confidence."
        nodeName.startsWith("AI: STT", ignoreCase = true)    ->
            "🎤 Speech recognised: \"Schedule meeting at 3 PM tomorrow.\""
        nodeName.startsWith("AI: LLM", ignoreCase = true)    ->
            "🤖 LLM generated 128 tokens in 340 ms."
        nodeName.startsWith("AI: Vision", ignoreCase = true) ->
            "🌿 Top class: Leaf Blight (82%). Remediation plan generated."
        nodeName.startsWith("AI: Summar", ignoreCase = true) ->
            "📝 Summary: 5 key points extracted from 1 200-word input."
        nodeName.startsWith("Transform", ignoreCase = true)  ->
            "🔄 Transform applied — output rows: 12."
        nodeName.startsWith("Action: Notify", ignoreCase = true) ->
            "🔔 Push notification delivered to device."
        nodeName.startsWith("Action: Save PDF", ignoreCase = true) ->
            "📑 PDF written to Downloads/FlowMind_Report.pdf."
        nodeName.startsWith("Action: Save DB", ignoreCase = true)  ->
            "💾 Record inserted into local Room database."
        nodeName.startsWith("Action: Email", ignoreCase = true)    ->
            "📧 Email dispatched via /v1/notify endpoint."
        else -> "Output: [Simulated data for $nodeName]"
    }

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
        listOf("Trigger: Schedule", "AI: OCR", "Transform: Format", "Action: Save PDF")
}

// ── Top-level helpers ─────────────────────────────────────────────────────────

/**
 * Builds a human-readable summary from a [SimulationState.Success] result.
 *
 * Example: `"✅ Dry-run complete — 4 nodes in 1 842 ms"`
 *
 * Returns `null` for any other [SimulationState].
 */
fun SimulationState.simulationSummary(): String? {
    if (this !is SimulationState.Success) return null
    val seconds = totalTimeMs / 1000.0
    return "✅ Dry-run complete — $nodeCount node(s) in ${"%.2f".format(seconds)} s"
}
