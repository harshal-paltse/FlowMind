package com.example.flowmind.domain.manager

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.flowmind.domain.models.MLModel

data class DeviceFitScore(
    val model: MLModel,
    val score: Int, // 0 to 100
    val reason: String,
    val isRejected: Boolean
)

class DeviceFitScorer(private val context: Context) {

    fun scoreModels(models: List<MLModel>): List<DeviceFitScore> {
        val availableRamMb = getAvailableRamMb()
        val availableStorageMb = getAvailableStorageMb()
        val batteryPct = getBatteryPercentage()
        val isWifi = isWifiConnected()

        return models.map { model ->
            var score = 100
            val reasons = mutableListOf<String>()
            var isRejected = false

            // Hard constraints
            if (model.sizeMb > availableStorageMb) {
                isRejected = true
                reasons.add("Not enough storage (${availableStorageMb}MB available, needs ${model.sizeMb}MB).")
            }
            if (model.requiredRamMb > availableRamMb) {
                isRejected = true
                reasons.add("Not enough RAM (${availableRamMb}MB available, needs ${model.requiredRamMb}MB).")
            }

            // Soft constraints
            if (batteryPct < 20) {
                score -= 30
                reasons.add("Low battery ($batteryPct%).")
            }
            
            if (model.sizeMb > 500 && !isWifi) {
                score -= 40
                reasons.add("Large download required over Cellular.")
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 && model.supportedAccelerators.contains("NNAPI")) {
                score -= 10
                reasons.add("NNAPI not fully supported on this OS version.")
            }

            if (!isRejected && reasons.isEmpty()) {
                reasons.add("Optimal fit for current device state.")
            } else if (isRejected) {
                score = 0
            }

            DeviceFitScore(
                model = model,
                score = score.coerceIn(0, 100),
                reason = reasons.joinToString(" "),
                isRejected = isRejected
            )
        }.sortedByDescending { it.score }
    }

    private fun getAvailableRamMb(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    private fun getAvailableStorageMb(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        return bytesAvailable / (1024 * 1024)
    }

    private fun getBatteryPercentage(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100 // Assume 100 if we can't read it
        }
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
