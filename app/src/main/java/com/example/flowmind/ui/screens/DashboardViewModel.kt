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

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: WorkflowRepository
) : ViewModel() {

    val workflows: StateFlow<List<Workflow>> = repo.allWorkflows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _journeyStep = MutableStateFlow(0)
    val journeyStep: StateFlow<Int> = _journeyStep.asStateFlow()

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

    fun runWorkflow(workflow: Workflow) {
        // Advance journey on first run
        if (_journeyStep.value < 4) _journeyStep.value = 4
    }

    fun advanceJourney(step: Int) {
        if (step > _journeyStep.value) _journeyStep.value = step
    }
}
