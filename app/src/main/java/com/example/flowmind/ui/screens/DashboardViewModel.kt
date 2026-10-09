package com.example.flowmind.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.data.WorkflowRepository
import com.example.flowmind.domain.models.TriggerType
import com.example.flowmind.domain.models.Workflow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 *
 * Exposes the full list of saved [Workflow]s as a [StateFlow] and provides
 * helpers for creating AI-generated workflows, deleting existing ones, and
 * managing the onboarding journey step.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: WorkflowRepository
) : ViewModel() {

    /** Live list of all persisted workflows, updated whenever the DB changes. */
    val workflows: StateFlow<List<Workflow>> = repo.allWorkflows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _journeyStep = MutableStateFlow(0)
    /** The current onboarding journey step (0 = start, 4 = complete). */
    val journeyStep: StateFlow<Int> = _journeyStep.asStateFlow()

    // ---------------------------------------------------------------------------
    // Public actions
    // ---------------------------------------------------------------------------

    /**
     * Persists a new [Workflow] created from an AI inference [aiResult] string.
     *
     * @param aiResult Raw text returned by the AI endpoint; the first 120 chars
     *   are used as the workflow description.
     */
    fun addAiWorkflow(aiResult: String) {
        viewModelScope.launch {
            runCatching {
                repo.save(
                    Workflow(
                        name = "AI Generated Workflow",
                        description = aiResult.take(120),
                        triggerType = TriggerType.NOTIFICATION,
                        nodesJson = "[]",
                        edgesJson = "[]"
                    )
                )
            }
        }
    }

    /**
     * Deletes the given [workflow] from persistent storage.
     *
     * The operation is fire-and-forget; errors are silently swallowed because
     * the repository layer already logs them.
     */
    fun deleteWorkflow(workflow: Workflow) {
        viewModelScope.launch {
            runCatching { repo.delete(workflow) }
        }
    }

    /**
     * Marks the given [workflow] as executed and advances the journey step to
     * at least 4 (the "first run" milestone).
     */
    fun runWorkflow(workflow: Workflow) {
        // Advance journey on first run
        if (_journeyStep.value < 4) _journeyStep.value = 4
    }

    /**
     * Advances the journey to [step] if [step] is greater than the current value.
     *
     * @param step The target step index (must be in range 0–4).
     */
    fun advanceJourney(step: Int) {
        if (step > _journeyStep.value) _journeyStep.value = step
    }
}
