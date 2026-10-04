package com.example.flowmind.ui.screens

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.data.RunRecordRepository
import com.example.flowmind.domain.models.RunRecord
import com.example.flowmind.domain.network.CloudflareApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed class LiveInputUiState {
    object Idle      : LiveInputUiState()
    object Recording : LiveInputUiState()
    object Processing: LiveInputUiState()
    data class Result(val text: String, val templateId: String = "") : LiveInputUiState()
    data class Error(val msg: String) : LiveInputUiState()
}

@HiltViewModel
class LiveInputViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: CloudflareApiClient,
    private val runRepo: RunRecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LiveInputUiState>(LiveInputUiState.Idle)
    val state: StateFlow<LiveInputUiState> = _state.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var amplitudeJob: Job? = null

    // ── Text inference ─────────────────────────────────────────────────────────
    fun inferText(prompt: String, templateId: String = "") {
        if (prompt.isBlank()) return
        val start = System.currentTimeMillis()
        _state.value = LiveInputUiState.Processing
        viewModelScope.launch {
            runCatching { api.infer(capability = "text", text = prompt) }
                .onSuccess { r ->
                    val text = r.getOrDefault("(no result)")
                    _state.value = LiveInputUiState.Result(text, templateId)
                    saveRun(templateId, true, System.currentTimeMillis() - start, true)
                }
                .onFailure { e ->
                    _state.value = LiveInputUiState.Error(e.message ?: "Inference failed")
                    saveRun(templateId, false, System.currentTimeMillis() - start, false)
                }
        }
    }

    // ── Image inference ────────────────────────────────────────────────────────
    fun inferImage(uri: Uri, capability: String = "image", templateId: String = "") {
        val start = System.currentTimeMillis()
        _state.value = LiveInputUiState.Processing
        viewModelScope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.readBytes()
                        ?: error("Cannot read image")
                }
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                api.infer(capability = capability, imageBase64 = b64)
            }
                .onSuccess { r ->
                    _state.value = LiveInputUiState.Result(r.getOrDefault("(no result)"), templateId)
                    saveRun(templateId, true, System.currentTimeMillis() - start, true)
                }
                .onFailure { e ->
                    _state.value = LiveInputUiState.Error(e.message ?: "Image inference failed")
                    saveRun(templateId, false, System.currentTimeMillis() - start, false)
                }
        }
    }

    // ── Audio recording ────────────────────────────────────────────────────────
    fun startRecording() {
        if (_state.value == LiveInputUiState.Recording) return
        runCatching {
            val file = File(ctx.cacheDir, "rec_${System.currentTimeMillis()}.m4a").also { recordingFile = it }
            recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            _state.value = LiveInputUiState.Recording
            amplitudeJob = viewModelScope.launch {
                val buf = ArrayDeque<Float>(60)
                while (true) {
                    delay(80)
                    val amp = (recorder?.maxAmplitude ?: 0) / 32768f
                    buf.addLast(amp)
                    if (buf.size > 60) buf.removeFirst()
                    _amplitudes.value = buf.toList()
                }
            }
        }.onFailure { _state.value = LiveInputUiState.Error("Mic error: ${it.message}") }
    }

    fun stopRecordingAndInfer(templateId: String = "") {
        amplitudeJob?.cancel()
        amplitudeJob = null
        runCatching {
            recorder?.apply { stop(); release() }
            recorder = null
        }
        _amplitudes.value = emptyList()
        val file = recordingFile ?: run {
            _state.value = LiveInputUiState.Error("No recording found")
            return
        }
        val start = System.currentTimeMillis()
        _state.value = LiveInputUiState.Processing
        viewModelScope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                api.infer(capability = "audio", audioBase64 = b64)
            }
                .onSuccess { r ->
                    _state.value = LiveInputUiState.Result(r.getOrDefault("(no result)"), templateId)
                    saveRun(templateId, true, System.currentTimeMillis() - start, true)
                    file.delete()
                }
                .onFailure { e ->
                    _state.value = LiveInputUiState.Error(e.message ?: "Audio inference failed")
                    saveRun(templateId, false, System.currentTimeMillis() - start, false)
                }
        }
    }

    fun reset() { _state.value = LiveInputUiState.Idle }

    private suspend fun saveRun(templateId: String, success: Boolean, latencyMs: Long, local: Boolean) {
        runCatching {
            runRepo.insert(RunRecord(
                workflowId   = templateId.ifBlank { "live_input" },
                workflowName = templateIdToName(templateId),
                templateId   = templateId,
                success      = success,
                latencyMs    = latencyMs,
                isLocal      = local
            ))
        }
    }

    private fun templateIdToName(id: String) = when (id) {
        "bill"     -> "Bill → Expense Tracker"
        "lecture"  -> "Lecture → Notes & Quiz"
        "plant"    -> "Plant Disease → Care Plan"
        "meeting"  -> "Meeting → Action Items"
        "medicine" -> "Medicine Label → Reminders"
        else       -> "Live Input"
    }

    override fun onCleared() {
        super.onCleared()
        amplitudeJob?.cancel()
        runCatching { recorder?.apply { stop(); release() } }
    }
}
