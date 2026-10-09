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

/**
 * One-shot UI event emitted by [ScheduleViewModel].
 *
 * @property message  Human-readable message to display in a snackbar.
 * @property isError  `true` when the event represents a failure.
 */
data class ScheduleUiEvent(val message: String, val isError: Boolean = false)

/**
 * ViewModel for the Schedule & Notify screen.
 *
 * Exposes the persisted [workflows] list and provides scheduling helpers that
 * delegate to [WorkflowScheduler] (WorkManager). Push-notification test calls
 * go directly to [CloudflareApiClient].
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val workflowRepo: WorkflowRepository,
    private val scheduler: WorkflowScheduler,
    private val api: CloudflareApiClient,
    private val fcmRepo: FcmTokenRepository
) : ViewModel() {

    /** Live list of all persisted workflows, updated whenever the DB changes. */
    val workflows: StateFlow<List<Workflow>> = workflowRepo.allWorkflows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _event = MutableSharedFlow<ScheduleUiEvent>(replay = 0, extraBufferCapacity = 1)
    /** One-shot events for snackbar feedback; collect with `collectLatest`. */
    val event: SharedFlow<ScheduleUiEvent> = _event.asSharedFlow()

    /**
     * Schedules [workflow] to run once after [delayMinutes] minutes.
     *
     * Emits a [ScheduleUiEvent] with a success or failure message.
     *
     * @param workflow      The workflow to schedule.
     * @param delayMinutes  How many minutes to wait before executing.
     */
    fun scheduleOnce(workflow: Workflow, delayMinutes: Long) {
        runCatching { scheduler.scheduleOnce(workflow, delayMinutes) }
            .onSuccess { _event.tryEmit(ScheduleUiEvent("Scheduled '${workflow.name}' in ${delayMinutes}min")) }
            .onFailure { _event.tryEmit(ScheduleUiEvent("Schedule failed: ${it.message}", true)) }
    }

    /**
     * Schedules [workflow] to repeat every [intervalHours] hours.
     *
     * @param workflow       The workflow to repeat.
     * @param intervalHours  Repeat period in whole hours.
     */
    fun scheduleRepeating(workflow: Workflow, intervalHours: Long) {
        runCatching { scheduler.scheduleRepeating(workflow, intervalHours) }
            .onSuccess { _event.tryEmit(ScheduleUiEvent("Repeating every ${intervalHours}h set for '${workflow.name}'")) }
            .onFailure { _event.tryEmit(ScheduleUiEvent("Repeat failed: ${it.message}", true)) }
    }

    /**
     * Cancels any pending schedule for [workflow].
     *
     * @param workflow The workflow whose schedule should be removed.
     */
    fun cancelSchedule(workflow: Workflow) {
        scheduler.cancel(workflow.id)
        _event.tryEmit(ScheduleUiEvent("Schedule cancelled for '${workflow.name}'"))
    }

    /**
     * Sends a test push notification to the current device via `POST /v1/notify`.
     *
     * Useful for verifying FCM delivery without executing a real workflow.
     *
     * @param title Notification title shown in the system tray.
     * @param body  Notification body text.
     */
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
