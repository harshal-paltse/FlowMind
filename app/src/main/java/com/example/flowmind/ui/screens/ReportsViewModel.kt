package com.example.flowmind.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.data.RunRecordRepository
import com.example.flowmind.domain.models.RunRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsData(
    val successRate: Float = 0f,
    val avgLatencyMs: Long = 0L,
    val localCount: Int = 0,
    val cloudCount: Int = 0
)

sealed class ReportsUiState {
    object Loading : ReportsUiState()
    data class Ready(val runs: List<RunRecord>, val insights: InsightsData) : ReportsUiState()
    data class Empty(val hasSamples: Boolean = false) : ReportsUiState()
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repo: RunRecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ReportsUiState>(ReportsUiState.Loading)
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.allRuns().collect { runs ->
                if (runs.isEmpty()) {
                    _state.value = ReportsUiState.Empty()
                } else {
                    val insights = InsightsData(
                        successRate  = repo.successRate(),
                        avgLatencyMs = repo.avgLatencyMs(),
                        localCount   = repo.localVsCloud().first,
                        cloudCount   = repo.localVsCloud().second
                    )
                    _state.value = ReportsUiState.Ready(runs, insights)
                }
            }
        }
    }

    fun loadSamples() = viewModelScope.launch {
        repo.loadSamples()
    }

    fun deleteSamples() = viewModelScope.launch {
        repo.deleteSamples()
    }

    fun deleteRun(run: RunRecord) = viewModelScope.launch {
        repo.delete(run)
    }
}
