package com.example.flowmind.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.data.WorkflowRepository
import com.example.flowmind.domain.models.Workflow
import com.example.flowmind.domain.network.CloudflareApiClient
import com.example.flowmind.fcm.FcmTokenRepository
import com.example.flowmind.worker.WorkflowScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiEvent(val message: String, val isError: Boolean = false)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val workflowRepo: WorkflowRepository,
    private val scheduler: WorkflowScheduler,
    private val api: CloudflareApiClient,
    private val fcmRepo: FcmTokenRepository
) : ViewModel() {

    val workflows: StateFlow<List<Workflow>> = workflowRepo.allWorkflows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _event = MutableSharedFlow<ScheduleUiEvent>(replay = 0, extraBufferCapacity = 1)
    val event: SharedFlow<ScheduleUiEvent> = _event.asSharedFlow()

    fun scheduleOnce(workflow: Workflow, delayMinutes: Long) {
        runCatching { scheduler.scheduleOnce(workflow, delayMinutes) }
            .onSuccess { _event.tryEmit(ScheduleUiEvent("Scheduled '${workflow.name}' in ${delayMinutes}min")) }
            .onFailure { _event.tryEmit(ScheduleUiEvent("Schedule failed: ${it.message}", true)) }
    }

    fun scheduleRepeating(workflow: Workflow, intervalHours: Long) {
        runCatching { scheduler.scheduleRepeating(workflow, intervalHours) }
            .onSuccess { _event.tryEmit(ScheduleUiEvent("Repeating every ${intervalHours}h set for '${workflow.name}'")) }
            .onFailure { _event.tryEmit(ScheduleUiEvent("Repeat failed: ${it.message}", true)) }
    }

    fun cancelSchedule(workflow: Workflow) {
        scheduler.cancel(workflow.id)
        _event.tryEmit(ScheduleUiEvent("Schedule cancelled for '${workflow.name}'"))
    }

    fun sendTestNotification(title: String, body: String) {
        viewModelScope.launch {
            runCatching {
                val token = fcmRepo.getToken()
                api.notify(title = title, body = body, fcmToken = token)
            }.onSuccess { _event.tryEmit(ScheduleUiEvent("Notification sent ✓")) }
             .onFailure { _event.tryEmit(ScheduleUiEvent("Notify failed: ${it.message}", true)) }
        }
    }
}
