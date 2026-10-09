package com.example.flowmind.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.domain.network.CloudflareApiClient
import com.example.flowmind.fcm.FcmTokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Infer screen.
 *
 * @property isLoading `true` while a network request is in-flight.
 * @property result    The raw text returned by the AI endpoint on success.
 * @property error     A human-readable error message when the request fails.
 */
data class InferUiState(
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null
)

/**
 * ViewModel for the Infer screen.
 *
 * Wraps [CloudflareApiClient] calls so the UI only needs to observe [uiState]
 * and call the simple action methods below.
 */
@HiltViewModel
class InferViewModel @Inject constructor(
    private val api: CloudflareApiClient,
    private val fcmTokenRepo: FcmTokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InferUiState())

    /** Observable UI state; collect this in Compose with [collectAsState]. */
    val uiState: StateFlow<InferUiState> = _uiState.asStateFlow()

    // ---------------------------------------------------------------------------
    // Public actions
    // ---------------------------------------------------------------------------

    /**
     * Calls `POST /v1/infer` with the given [capability] and [prompt].
     *
     * On success, [InferUiState.result] is populated.
     * On failure, [InferUiState.error] is populated with the exception message.
     *
     * @param capability The model capability key (e.g. `"text"`, `"image"`).
     * @param prompt     The user prompt or input payload.
     */
    fun infer(capability: String = "text", prompt: String) {
        viewModelScope.launch {
            _uiState.value = InferUiState(isLoading = true)
            val result = api.infer(capability = capability, text = prompt)
            _uiState.value = result.fold(
                onSuccess = { InferUiState(result = it) },
                onFailure = { InferUiState(error = it.message ?: "Unknown error") }
            )
        }
    }

    /**
     * Sends a push notification to the current device via `POST /v1/notify`.
     *
     * This is fire-and-forget; any network errors are suppressed because
     * in-app feedback is out of scope for self-notifications.
     *
     * @param title Notification title shown in the system tray.
     * @param body  Notification body text.
     */
    fun notifySelf(title: String, body: String) {
        viewModelScope.launch {
            try {
                val token = fcmTokenRepo.getToken()
                api.notify(title = title, body = body, fcmToken = token)
            } catch (e: Exception) {
                // Fire-and-forget; surfacing errors is out of scope here.
            }
        }
    }

    /**
     * Clears the current [InferUiState.result] without affecting [InferUiState.error].
     *
     * Useful after the UI has consumed the result (e.g. copied it to clipboard).
     */
    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    /**
     * Clears the current [InferUiState.error] so the error banner is dismissed.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Keep old name as a convenience alias for call-sites not yet migrated.
    /** @see clearResult */
    fun resetResult() = clearResult()
}
