package com.example.flowmind.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.data.WorkflowRepository
import com.example.flowmind.domain.models.TriggerType
import com.example.flowmind.domain.models.Workflow
import com.example.flowmind.domain.validation.WorkflowValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sealed state for the workflow builder save/update operation. */
sealed class BuilderUiState {
    object Idle : BuilderUiState()
    object Loading : BuilderUiState()
    data class Success(val message: String) : BuilderUiState()
    data class Error(val message: String) : BuilderUiState()
}

/**
 * ViewModel backing [WorkflowBuilderScreen].
 *
 * Handles validation, duplicate-name detection, persistence via
 * [WorkflowRepository], and optional update of an existing workflow.
 */
@HiltViewModel
class BuilderViewModel @Inject constructor(
    private val repo: WorkflowRepository
) : ViewModel() {

    private val _saveState = MutableStateFlow<BuilderUiState>(BuilderUiState.Idle)
    val saveState: StateFlow<BuilderUiState> = _saveState.asStateFlow()

    /**
     * Validates [nodes] and saves a new [Workflow] to the repository.
     *
     * Emits [BuilderUiState.Error] if:
     * - Validation fails (see [WorkflowValidator])
     * - A workflow with the same [name] already exists
     *
     * @param nodes   Current node list from the canvas.
     * @param name    User-supplied workflow name; defaults to "Custom Workflow" if blank.
     * @param trigger How the workflow will be activated.
     */
    fun save(nodes: List<NodeUI>, name: String, trigger: TriggerType) {
        val resolvedName = name.ifBlank { "Custom Workflow" }

        val validator = WorkflowValidator()
        when (val r = validator.validate(nodes)) {
            is WorkflowValidator.ValidationResult.Error -> {
                _saveState.value = BuilderUiState.Error(r.message)
                return
            }
            else -> Unit
        }

        _saveState.value = BuilderUiState.Loading
        viewModelScope.launch {
            runCatching {
                // Guard against duplicate workflow names
                val existing = repo.findByName(resolvedName)
                if (existing != null) {
                    error("A workflow named \"$resolvedName\" already exists. Choose a different name.")
                }

                val nodesJson = nodes.joinToString(",", "[", "]") { "\"${it.title}\"" }
                repo.save(
                    Workflow(
                        name        = resolvedName,
                        description = nodes.joinToString(" → ") { it.title },
                        triggerType = trigger,
                        nodesJson   = nodesJson,
                        edgesJson   = "[]"
                    )
                )
            }
                .onSuccess { _saveState.value = BuilderUiState.Success("Workflow saved!") }
                .onFailure { _saveState.value = BuilderUiState.Error(it.message ?: "Save failed") }
        }
    }

    /**
     * Updates an existing workflow identified by [workflowId] with new node data.
     *
     * @param workflowId ID of the workflow to update.
     * @param nodes      Updated node list from the canvas.
     * @param name       New name for the workflow.
     * @param trigger    New trigger type.
     */
    fun update(workflowId: String, nodes: List<NodeUI>, name: String, trigger: TriggerType) {
        val resolvedName = name.ifBlank { "Custom Workflow" }

        val validator = WorkflowValidator()
        when (val r = validator.validate(nodes)) {
            is WorkflowValidator.ValidationResult.Error -> {
                _saveState.value = BuilderUiState.Error(r.message)
                return
            }
            else -> Unit
        }

        _saveState.value = BuilderUiState.Loading
        viewModelScope.launch {
            runCatching {
                val nodesJson = nodes.joinToString(",", "[", "]") { "\"${it.title}\"" }
                repo.update(
                    Workflow(
                        id          = workflowId,
                        name        = resolvedName,
                        description = nodes.joinToString(" → ") { it.title },
                        triggerType = trigger,
                        nodesJson   = nodesJson,
                        edgesJson   = "[]"
                    )
                )
            }
                .onSuccess { _saveState.value = BuilderUiState.Success("Workflow updated!") }
                .onFailure { _saveState.value = BuilderUiState.Error(it.message ?: "Update failed") }
        }
    }

    /** Resets the save state back to [BuilderUiState.Idle]. */
    fun resetState() { _saveState.value = BuilderUiState.Idle }
}
