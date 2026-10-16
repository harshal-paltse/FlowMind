package com.example.flowmind.domain.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Extension functions on [RunRecord] for analytics, formatting, and display helpers.
 *
 * Keeping these outside the entity class prevents Room from treating them as columns.
 */

/** Returns `"Success"` or `"Failed"` label for display in lists and reports. */
fun RunRecord.statusLabel(): String = if (success) "Success" else "Failed"

/**
 * Returns the latency formatted as a human-readable string.
 * Values under 1 second are shown in milliseconds; longer values in seconds.
 *
 * Examples: `"240 ms"`, `"3.2 s"`
 */
fun RunRecord.latencyFormatted(): String =
    if (latencyMs < 1_000L) "${latencyMs} ms"
    else String.format(Locale.US, "%.1f s", latencyMs / 1_000.0)

/**
 * Returns the run timestamp formatted as `"dd MMM yyyy, HH:mm"`.
 *
 * Example: `"17 Oct 2026, 00:09"`
 */
fun RunRecord.runAtFormatted(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(runAt))
}

/** Returns `"On-Device"` or `"Cloud"` depending on [RunRecord.isLocal]. */
fun RunRecord.inferenceLabel(): String = if (isLocal) "On-Device" else "Cloud"

/**
 * Returns `true` when the run was completed within a "fast" threshold.
 * @param thresholdMs Latency ceiling in milliseconds; defaults to 1 000 ms.
 */
fun RunRecord.isFast(thresholdMs: Long = 1_000L): Boolean = latencyMs <= thresholdMs

/**
 * Returns a one-line summary suitable for share / export, e.g.:
 * `"[17 Oct 2026, 00:09] Invoice Parser – Success – 340 ms (On-Device)"`
 */
fun RunRecord.shareSummary(): String =
    "[${runAtFormatted()}] $workflowName – ${statusLabel()} – ${latencyFormatted()} (${inferenceLabel()})"
