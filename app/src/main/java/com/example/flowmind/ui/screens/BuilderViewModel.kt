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

@HiltViewModel
class BuilderViewModel @Inject constructor(
    private val repo: WorkflowRepository
) : ViewModel() {

    private val _saveState = MutableStateFlow<BuilderUiState>(BuilderUiState.Idle)
    val saveState: StateFlow<BuilderUiState> = _saveState.asStateFlow()

    fun save(nodes: List<NodeUI>, name: String, trigger: TriggerType) {
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
                repo.save(
                    Workflow(
                        name = name.ifBlank { "Custom Workflow" },
                        description = nodes.joinToString(" → ") { it.title },
                        triggerType = trigger,
                        nodesJson = nodesJson,
                        edgesJson = "[]"
                    )
                )
            }.onSuccess { _saveState.value = BuilderUiState.Success("Saved!") }
             .onFailure { _saveState.value = BuilderUiState.Error(it.message ?: "Save failed") }
        }
    }

    fun resetState() { _saveState.value = BuilderUiState.Idle }
}
