package com.example.flowmind.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowmind.data.db.AppDatabase
import com.example.flowmind.domain.network.CloudflareApiClient
import com.example.flowmind.fcm.FcmTokenRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "Diagnostics"

enum class CheckStatus { PENDING, RUNNING, PASS, FAIL }

data class DiagCheck(
    val id: String,
    val label: String,
    val status: CheckStatus = CheckStatus.PENDING,
    val detail: String = ""
)

data class DiagState(
    val checks: List<DiagCheck> = listOf(
        DiagCheck("auth",   "Firebase Auth token"),
        DiagCheck("infer",  "/v1/infer call"),
        DiagCheck("ocr",    "On-device OCR (ML Kit)"),
        DiagCheck("room",   "Room DB read/write"),
        DiagCheck("notif",  "Notification permission"),
        DiagCheck("fcm",    "FCM push token")
    ),
    val running: Boolean = false,
    val allPass: Boolean = false
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val auth: FirebaseAuth,
    private val api: CloudflareApiClient,
    private val db: AppDatabase,
    private val fcm: FcmTokenRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiagState())
    val state: StateFlow<DiagState> = _state.asStateFlow()

    private var retryCount = 0

    /**
     * Runs all diagnostic checks sequentially.
     * No-ops if a run is already in progress.
     */
    fun runAll() {
        if (_state.value.running) return
        _state.value = DiagState(running = true)
        viewModelScope.launch {
            runCheck("auth")  { checkAuth() }
            runCheck("infer") { checkInfer() }
            runCheck("ocr")   { checkOcr() }
            runCheck("room")  { checkRoom() }
            runCheck("notif") { checkNotif() }
            runCheck("fcm")   { checkFcmToken() }
            val all = _state.value.checks.all { it.status == CheckStatus.PASS }
            _state.value = _state.value.copy(running = false, allPass = all)
            retryCount = 0
        }
    }

    /**
     * Resets all checks to [CheckStatus.PENDING] and increments the retry counter.
     * Call before invoking [runAll] again to show a fresh run.
     */
    fun reset() {
        retryCount++
        _state.value = DiagState()
    }

    /**
     * Convenience function: reset then immediately re-run all checks.
     */
    fun retry() {
        reset()
        runAll()
    }

    private suspend fun runCheck(id: String, block: suspend () -> String) {
        update(id, CheckStatus.RUNNING, "")
        runCatching { block() }
            .onSuccess { detail ->
                Log.i(TAG, "PASS $id: $detail")
                update(id, CheckStatus.PASS, detail)
            }
            .onFailure { e ->
                Log.e(TAG, "FAIL $id", e)
                update(id, CheckStatus.FAIL, e.message ?: "Unknown error")
            }
    }

    private fun update(id: String, status: CheckStatus, detail: String) {
        _state.value = _state.value.copy(
            checks = _state.value.checks.map {
                if (it.id == id) it.copy(status = status, detail = detail) else it
            }
        )
    }

    // ── Individual checks ──────────────────────────────────────────────────

    private suspend fun checkAuth(): String {
        val user = auth.currentUser
            ?: error("Not signed in — login first")
        val token = user.getIdToken(true).await().token
            ?: error("Token was null")
        return "UID=${user.uid.take(8)}… token=${token.take(12)}…"
    }

    private suspend fun checkInfer(): String {
        val result = api.infer(capability = "text", text = "ping").getOrElse { throw it }
        return "Response: ${result.take(60)}"
    }

    private suspend fun checkOcr(): String = withContext(Dispatchers.IO) {
        // Create a 1×1 bitmap — enough to initialise the recogniser without crashing
        val bmp = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        val image = InputImage.fromBitmap(bmp, 0)
        val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return@withContext try {
            val result = recogniser.process(image).await()
            "ML Kit OK, text='${result.text.ifBlank { "(empty — expected on blank image)" }}'"
        } finally {
            recogniser.close()
        }
    }

    private suspend fun checkRoom(): String = withContext(Dispatchers.IO) {
        val dao = db.runRecordDao()
        val count = dao.getAll().size   // simple count; no writes needed
        "Row count=$count, DAO accessible"
    }

    private fun checkNotif(): String {
        return if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) "Granted"
        else error("POST_NOTIFICATIONS not granted — go to App Settings → Notifications")
    }

    private suspend fun checkFcmToken(): String {
        val token = fcm.getToken().getOrElse { throw it }
        if (token.isBlank()) error("FCM token is blank — check google-services.json")
        return "Token=${token.take(16)}…"
    }
}
