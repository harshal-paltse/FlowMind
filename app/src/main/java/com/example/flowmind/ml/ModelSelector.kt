package com.example.flowmind.ml

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.flowmind.domain.models.ModelCandidate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelSelector @Inject constructor(@ApplicationContext private val context: Context) {

    private fun getAvailableRamMb(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return (memoryInfo.availMem / (1024 * 1024)).toInt()
    }

    private fun getBatteryLevel(): Float {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return level * 100f / scale
    }

    fun selectBestModel(candidates: List<ModelCandidate>, isCloudPermitted: Boolean, isPrivateData: Boolean): ModelCandidate? {
        val availableRam = getAvailableRamMb()
        val batteryLevel = getBatteryLevel()

        val viableCandidates = candidates.filter { model ->
            model.requiredRamMb < (availableRam * 0.8)
        }

        if (viableCandidates.isEmpty()) {
            if (isCloudPermitted && !isPrivateData) {
                return ModelCandidate(
                    id = "cloud_fallback", name = "Cloud API", taskType = candidates.firstOrNull()?.taskType ?: "UNKNOWN",
                    sizeMb = 0, requiredRamMb = 0, latencyScore = 90, accuracyScore = 99,
                    accelerator = "CLOUD", downloadUrl = "", sha256 = "", isDownloaded = true
                )
            }
            return null
        }

        return if (batteryLevel > 20f) {
            viableCandidates.maxByOrNull { it.accuracyScore }
        } else {
            viableCandidates.maxByOrNull { it.latencyScore }
        }
    }
}
