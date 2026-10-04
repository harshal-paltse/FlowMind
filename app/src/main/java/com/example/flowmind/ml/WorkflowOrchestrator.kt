package com.example.flowmind.ml

import android.util.Log
import com.example.flowmind.domain.network.CloudflareApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WorkflowOrchestrator"

@Singleton
class WorkflowOrchestrator @Inject constructor(
    private val modelSelector: ModelSelector,
    private val api: CloudflareApiClient
) {
    private val loadedModels = LinkedHashMap<String, Any>(4, 0.75f, true)
    private val MAX_RAM_BUDGET_MB = 1000L
    private var currentRamUsedMb = 0L
    private val resultCache = mutableMapOf<String, Any>()

    // -----------------------------------------------------------
    //  Sync (local-only) path — kept for on-device tasks
    // -----------------------------------------------------------

    fun executeStep(
        stepId: String,
        taskType: String,
        inputData: Any,
        isPrivateData: Boolean,
        hasNetwork: Boolean,
        batteryLow: Boolean
    ): Any {
        if (resultCache.containsKey(stepId)) {
            return resultCache[stepId]!!
        }

        val runLocal = isPrivateData || !hasNetwork || batteryLow

        val candidates = if (runLocal) {
            listOf("local_gemini_nano", "local_mobilebert")
        } else {
            listOf("cloud_gemini_pro", "local_gemini_nano")
        }

        var result: Any? = null
        for (modelId in candidates) {
            try {
                result = executeWithModel(modelId, taskType, inputData)
                break
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }

        if (result == null) {
            throw Exception("All models failed for step $stepId")
        }

        resultCache[stepId] = result
        return result
    }

    private fun executeWithModel(modelId: String, taskType: String, inputData: Any): Any {
        loadModel(modelId)
        return "Simulated Result for $taskType using $modelId"
    }

    private fun loadModel(modelId: String): Any {
        if (loadedModels.containsKey(modelId)) return loadedModels[modelId]!!
        val modelSizeMb = 200L
        while (currentRamUsedMb + modelSizeMb > MAX_RAM_BUDGET_MB && loadedModels.isNotEmpty()) {
            val lruKey = loadedModels.keys.first()
            unloadModel(lruKey, 200L)
        }
        val dummyModel = Any()
        loadedModels[modelId] = dummyModel
        currentRamUsedMb += modelSizeMb
        return dummyModel
    }

    private fun unloadModel(modelId: String, sizeMb: Long) {
        val model = loadedModels.remove(modelId)
        (model as? AutoCloseable)?.close()
        currentRamUsedMb -= sizeMb
    }

    // -----------------------------------------------------------
    //  Async path — calls Cloudflare Worker when conditions allow
    // -----------------------------------------------------------

    /**
     * Executes a single step, routing to the Cloudflare Worker for cloud capable tasks
     * and falling back to the on-device simulator when the device is offline, battery is
     * low, or the data is marked private.
     *
     * @param capability  Matches the `capability` field expected by POST /v1/infer
     *                    (e.g. "text", "ocr", "vision", "audio_transcribe").
     * @param text        Optional text prompt / document to analyse.
     * @param imageBase64 Optional base-64 image payload.
     * @param audioBase64 Optional base-64 audio payload.
     */
    suspend fun executeStepCloud(
        stepId: String,
        capability: String,
        text: String? = null,
        imageBase64: String? = null,
        audioBase64: String? = null,
        isPrivateData: Boolean = false,
        hasNetwork: Boolean = true,
        batteryLow: Boolean = false,
        timeoutMs: Long = 30_000L
    ): String {
        resultCache[stepId]?.let { return it as String }

        val runLocal = isPrivateData || !hasNetwork || batteryLow

        return if (runLocal) {
            Log.d(TAG, "[$stepId] Running locally (private=$isPrivateData, net=$hasNetwork, bat=$batteryLow)")
            val fallback = "Local inference result for capability=$capability"
            resultCache[stepId] = fallback
            fallback
        } else {
            withTimeout(timeoutMs) {
                Log.d(TAG, "[$stepId] Calling Cloudflare Worker: capability=$capability")
                val result = api.infer(
                    capability = capability,
                    text = text,
                    imageBase64 = imageBase64,
                    audioBase64 = audioBase64
                ).getOrElse { e ->
                    Log.w(TAG, "[$stepId] Cloud infer failed, falling back to local", e)
                    "Local inference fallback for capability=$capability"
                }
                resultCache[stepId] = result
                result
            }
        }
    }

    /** Convenience wrapper that keeps the old timeout API working. */
    suspend fun executeStepWithTimeout(
        stepId: String,
        taskType: String,
        inputData: Any,
        isPrivateData: Boolean,
        hasNetwork: Boolean,
        batteryLow: Boolean,
        timeoutMs: Long = 5000L
    ): Any {
        return try {
            withTimeout(timeoutMs) {
                executeStep(stepId, taskType, inputData, isPrivateData, hasNetwork, batteryLow)
            }
        } catch (e: TimeoutCancellationException) {
            throw Exception("Step $stepId timed out")
        }
    }
}
