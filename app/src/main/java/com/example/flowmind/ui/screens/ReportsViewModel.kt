package com.example.flowmind.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.data.RunRecordRepository
import com.example.flowmind.domain.models.RunRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Aggregated analytics shown in the Reports screen header.
 *
 * @property successRate  Fraction of successful runs in [0.0, 1.0].
 * @property avgLatencyMs Average run duration across all stored records.
 * @property localCount   Number of on-device (local) runs.
 * @property cloudCount   Number of cloud-inference runs.
 * @property totalRuns    Total number of run records (local + cloud).
 * @property bestLatencyMs Fastest recorded run latency; 0 if no records exist.
 */
data class InsightsData(
    val successRate: Float = 0f,
    val avgLatencyMs: Long = 0L,
    val localCount: Int = 0,
    val cloudCount: Int = 0,
    val totalRuns: Int = 0,
    val bestLatencyMs: Long = 0L
)

/** UI state emitted by [ReportsViewModel]. */
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

    /** Emits a one-shot CSV string when [exportCsv] is called. */
    private val _csvExport = MutableSharedFlow<String>()
    val csvExport: SharedFlow<String> = _csvExport.asSharedFlow()

    init {
        viewModelScope.launch {
            repo.allRuns().collect { runs ->
                if (runs.isEmpty()) {
                    _state.value = ReportsUiState.Empty()
                } else {
                    val (localCount, cloudCount) = repo.localVsCloud()
                    val insights = InsightsData(
                        successRate  = repo.successRate(),
                        avgLatencyMs = repo.avgLatencyMs(),
                        localCount   = localCount,
                        cloudCount   = cloudCount,
                        totalRuns    = runs.size,
                        bestLatencyMs = runs.filter { it.success }
                            .minOfOrNull { it.latencyMs } ?: 0L
                    )
                    _state.value = ReportsUiState.Ready(runs, insights)
                }
            }
        }
    }

    fun loadSamples() = viewModelScope.launch { repo.loadSamples() }

    fun deleteSamples() = viewModelScope.launch { repo.deleteSamples() }

    fun deleteRun(run: RunRecord) = viewModelScope.launch { repo.delete(run) }

    /**
     * Builds a CSV string from all current run records and emits it via [csvExport].
     * The caller is responsible for writing the file to external storage.
     */
    fun exportCsv() = viewModelScope.launch {
        val currentState = _state.value
        if (currentState !is ReportsUiState.Ready) return@launch

        val header = "id,workflowName,success,latencyMs,ramUsedMb,isLocal,runAt\n"
        val rows = currentState.runs.joinToString("\n") { r ->
            "${r.id},${r.workflowName},${r.success},${r.latencyMs},${r.ramUsedMb},${r.isLocal},${r.runAt}"
        }
        _csvExport.emit(header + rows)
    }
}
