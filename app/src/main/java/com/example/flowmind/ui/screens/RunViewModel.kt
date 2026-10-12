package com.example.flowmind.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.data.RunRecordRepository
import com.example.flowmind.domain.models.RunRecord
import com.example.flowmind.domain.models.Workflow
import com.example.flowmind.ml.WorkflowOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One step in the visual timeline. */
data class TimelineStep(
    val name: String,
    val status: StepStatus = StepStatus.Pending,
    val execution: String = "Local",   // "Local" | "Cloud"
    val latencyMs: Long = 0L,
    val output: String = ""
)

enum class StepStatus { Pending, Running, Done, Failed }

sealed class RunUiState {
    object Idle : RunUiState()
    data class Running(
        val steps: List<TimelineStep>,
        val currentStep: Int,
        val workflowName: String
    ) : RunUiState()
    data class Success(
        val steps: List<TimelineStep>,
        val totalMs: Long,
        val workflowName: String,
        val runId: String
    ) : RunUiState()
    data class Failure(val reason: String) : RunUiState()
}

/**
 * ViewModel backing the [ResultScreen].
 *
 * Drives the step-by-step animated workflow timeline by:
 * 1. Parsing node titles from the persisted [Workflow.nodesJson].
 * 2. Executing each step via [WorkflowOrchestrator], emitting [RunUiState.Running]
 *    updates for real-time UI feedback.
 * 3. Persisting the completed [RunRecord] to [RunRecordRepository].
 * 4. Transitioning to [RunUiState.Success] or [RunUiState.Failure].
 */
@HiltViewModel
class RunViewModel @Inject constructor(
    private val orchestrator: WorkflowOrchestrator,
    private val repo: RunRecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RunUiState>(RunUiState.Idle)
    /** Observable execution state; collect in Compose with [collectAsState]. */
    val state: StateFlow<RunUiState> = _state.asStateFlow()

    /**
     * Starts executing [workflow], updating [state] as each step progresses.
     *
     * No-ops if a run is already in progress. On completion, persists a
     * [RunRecord] and transitions to [RunUiState.Success] or [RunUiState.Failure].
     *
     * @param workflow The [Workflow] whose nodes should be executed in sequence.
     */
    fun runWorkflow(workflow: Workflow) {
        if (_state.value is RunUiState.Running) return

        // Parse node titles from JSON (simple bracket extraction)
        val titles = parseNodeTitles(workflow.nodesJson)
        val steps  = titles.map { TimelineStep(it) }

        _state.value = RunUiState.Running(steps, 0, workflow.name)

        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val results = steps.toMutableList()
            var overallSuccess = true

            results.forEachIndexed { i, step ->
                // Mark running
                results[i] = step.copy(status = StepStatus.Running)
                _state.value = RunUiState.Running(results.toList(), i, workflow.name)

                val stepStart = System.currentTimeMillis()
                runCatching {
                    val cap = capabilityForStep(step.name)
                    val isCloud = cap in listOf("text", "ocr", "vision", "audio_transcribe")
                    val output = orchestrator.executeStepCloud(
                        stepId     = "${workflow.id}_$i",
                        capability = cap,
                        text       = "Execute: ${step.name}"
                    )
                    val ms = System.currentTimeMillis() - stepStart
                    results[i] = step.copy(
                        status    = StepStatus.Done,
                        execution = if (isCloud) "Cloud" else "Local",
                        latencyMs = ms,
                        output    = output
                    )
                }.onFailure { e ->
                    overallSuccess = false
                    results[i] = step.copy(status = StepStatus.Failed, output = e.message ?: "Error")
                }

                _state.value = RunUiState.Running(results.toList(), i + 1, workflow.name)
                delay(120) // brief visual pause between steps
            }

            val totalMs = System.currentTimeMillis() - start
            val record = RunRecord(
                workflowId   = workflow.id,
                workflowName = workflow.name,
                success      = overallSuccess,
                latencyMs    = totalMs,
                isLocal      = results.all { it.execution == "Local" }
            )
            runCatching { repo.insert(record) }

            if (overallSuccess) {
                _state.value = RunUiState.Success(results.toList(), totalMs, workflow.name, record.id)
            } else {
                _state.value = RunUiState.Failure(
                    results.firstOrNull { it.status == StepStatus.Failed }?.output ?: "A step failed"
                )
            }
        }
    }

    /** Resets [state] to [RunUiState.Idle], allowing the screen to be re-used. */
    fun reset() { _state.value = RunUiState.Idle }

    // ── helpers ─────────────────────────────────────────────────────────────

    /**
     * Extracts node titles from a JSON array string produced by [BuilderViewModel].
     *
     * Handles both compact (`["A","B"]`) and pretty-printed formats. Falls back
     * to a default three-step list when [json] is blank or unparseable.
     *
     * @param json Raw JSON such as `["Trigger","AI Process","Action"]`.
     * @return Ordered list of non-blank node title strings.
     */
    private fun parseNodeTitles(json: String): List<String> {
        val trimmed = json.trim()
        if (trimmed == "[]" || trimmed.isBlank()) {
            return listOf("Trigger", "AI Process", "Action")
        }
        // Strip outer brackets, then split on comma-quote boundaries to avoid
        // splitting on commas that may appear inside a title string.
        val inner = trimmed.removePrefix("[").removeSuffix("]")
        val titles = inner
            .split(Regex("\",\\s*\""))          // split on "," with optional whitespace
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
        return titles.ifEmpty { listOf("Trigger", "AI Process", "Action") }
    }

    /**
     * Maps a node [name] to the Cloudflare AI capability string it should invoke.
     *
     * Matching is case-insensitive. More-specific keywords (e.g. "STT") are
     * checked before broader ones (e.g. "Audio") to avoid accidental short-
     * circuiting.
     *
     * @param name The [TimelineStep.name] to classify.
     * @return A capability key such as `"ocr"`, `"vision"`, `"audio_transcribe"`,
     *         `"text"`, or `"local"`.
     */
    private fun capabilityForStep(name: String): String = when {
        name.contains("OCR",       ignoreCase = true) -> "ocr"
        name.contains("Vision",    ignoreCase = true) -> "vision"
        name.contains("STT",       ignoreCase = true) -> "audio_transcribe"
        name.contains("Audio",     ignoreCase = true) -> "audio_transcribe"
        name.contains("Transcrib", ignoreCase = true) -> "audio_transcribe"
        name.contains("LLM",       ignoreCase = true) -> "text"
        name.contains("Summari",   ignoreCase = true) -> "text"
        name.contains("Trigger",   ignoreCase = true) -> "local"
        name.contains("Action",    ignoreCase = true) -> "local"
        name.contains("Transform", ignoreCase = true) -> "local"
        else                                           -> "text"
    }
}
