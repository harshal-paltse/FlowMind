package com.example.flowmind.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.domain.network.CloudflareApiClient
import com.example.flowmind.fcm.FcmTokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InferUiState(
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null
)

@HiltViewModel
class InferViewModel @Inject constructor(
    private val api: CloudflareApiClient,
    private val fcmTokenRepo: FcmTokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InferUiState())
    val uiState: StateFlow<InferUiState> = _uiState

    /**
     * Calls POST /v1/infer with a plain text capability.
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
     * Sends a push notification to the current device via POST /v1/notify.
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

    fun resetResult() {
        _uiState.value = _uiState.value.copy(result = null)
    }
}
