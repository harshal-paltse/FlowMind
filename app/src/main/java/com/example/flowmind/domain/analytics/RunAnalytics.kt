package com.example.flowmind.domain.analytics

import com.example.flowmind.domain.models.RunRecord

/**
 * Lightweight, pure analytics functions computed over a list of [RunRecord] rows.
 *
 * These helpers are intentionally stateless and work directly on an in-memory snapshot,
 * making them easy to unit-test without a database or coroutines.
 */
object RunAnalytics {

    /**
     * Returns the fraction of successful runs in [records] as a value between 0.0 and 1.0.
     * Returns 0.0 when [records] is empty.
     */
    fun successRate(records: List<RunRecord>): Float {
        if (records.isEmpty()) return 0f
        val successCount = records.count { it.success }
        return successCount.toFloat() / records.size
    }

    /**
     * Returns the arithmetic mean of [RunRecord.latencyMs] across all [records].
     * Returns 0 when [records] is empty.
     */
    fun averageLatencyMs(records: List<RunRecord>): Long {
        if (records.isEmpty()) return 0L
        return records.map { it.latencyMs }.average().toLong()
    }

    /**
     * Returns the median latency across all [records].
     * For an even-sized list the lower of the two middle values is returned.
     * Returns 0 when [records] is empty.
     */
    fun medianLatencyMs(records: List<RunRecord>): Long {
        if (records.isEmpty()) return 0L
        val sorted = records.map { it.latencyMs }.sorted()
        return sorted[sorted.size / 2]
    }

    /**
     * Returns the fastest (minimum) latency observed across all [records].
     * Returns 0 when [records] is empty.
     */
    fun bestLatencyMs(records: List<RunRecord>): Long =
        records.minOfOrNull { it.latencyMs } ?: 0L

    /**
     * Returns the slowest (maximum) latency observed across all [records].
     * Returns 0 when [records] is empty.
     */
    fun worstLatencyMs(records: List<RunRecord>): Long =
        records.maxOfOrNull { it.latencyMs } ?: 0L

    /**
     * Aggregates runs by [RunRecord.workflowName] and returns a map of
     * `workflowName → runCount` sorted descending by count.
     */
    fun runCountByWorkflow(records: List<RunRecord>): Map<String, Int> =
        records.groupingBy { it.workflowName }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }

    /**
     * Returns the percentage of runs that used on-device inference.
     * Returns 0.0 when [records] is empty.
     */
    fun localInferenceRate(records: List<RunRecord>): Float {
        if (records.isEmpty()) return 0f
        return records.count { it.isLocal }.toFloat() / records.size
    }

    /**
     * Splits [records] into daily buckets using [RunRecord.runAt] epoch-millis.
     * Keys are epoch-day integers (ms / 86_400_000); values are the list of records
     * that ran on that day. Useful for populating contribution-style charts.
     */
    fun groupByDay(records: List<RunRecord>): Map<Long, List<RunRecord>> {
        val msPerDay = 86_400_000L
        return records.groupBy { it.runAt / msPerDay }
    }

    // -------------------------------------------------------------------------
    // Percentile latency, failure rate, streak analysis
    // -------------------------------------------------------------------------

    /**
     * Returns the 95th-percentile latency across [records].
     *
     * Uses the nearest-rank method: `ceil(0.95 * n)` where `n` is the number of records.
     * This is the canonical SLO threshold metric — if your p95 is under your SLA budget,
     * the vast majority of user-facing runs will feel fast.
     * Returns 0 when [records] is empty.
     */
    fun p95LatencyMs(records: List<RunRecord>): Long {
        if (records.isEmpty()) return 0L
        val sorted = records.map { it.latencyMs }.sorted()
        val rank = kotlin.math.ceil(0.95 * sorted.size).toInt().coerceAtMost(sorted.size) - 1
        return sorted[rank]
    }

    /**
     * Returns the fraction of **failed** runs in [records] as a value between 0.0 and 1.0.
     *
     * Defined as `1 - successRate(records)` so the two metrics always sum to 1.0.
     * Returns 0.0 when [records] is empty.
     */
    fun failureRate(records: List<RunRecord>): Float = 1f - successRate(records)

    /**
     * Snapshot of the current success / failure streak at the tail of the run list.
     *
     * @property successStreak Length of the most recent unbroken chain of successful runs
     *   (chronological order, most-recent last). 0 when the last run failed.
     * @property failureStreak Length of the most recent unbroken chain of failed runs.
     *   0 when the last run succeeded.
     * @property longestSuccessStreak Historically longest success streak observed in [records].
     */
    data class StreakInfo(
        val successStreak: Int,
        val failureStreak: Int,
        val longestSuccessStreak: Int,
    )

    /**
     * Analyses [records] (assumed chronologically sorted, oldest first) to compute
     * [StreakInfo].
     *
     * A single forward pass tracks the current and best success streaks; the failure streak
     * is computed via a backward pass so the logic stays symmetrical and readable.
     */
    fun streakInfo(records: List<RunRecord>): StreakInfo {
        if (records.isEmpty()) return StreakInfo(0, 0, 0)

        // Forward pass: longest / current success streak
        var currentSuccess = 0
        var longestSuccess = 0
        for (record in records) {
            if (record.success) {
                currentSuccess++
                if (currentSuccess > longestSuccess) longestSuccess = currentSuccess
            } else {
                currentSuccess = 0
            }
        }

        // Backward pass: current failure streak from the tail
        var currentFailure = 0
        for (record in records.reversed()) {
            if (!record.success) currentFailure++ else break
        }

        val successStreak = if (records.last().success) currentSuccess else 0

        return StreakInfo(
            successStreak = successStreak,
            failureStreak = currentFailure,
            longestSuccessStreak = longestSuccess,
        )
    }
}

// ---------------------------------------------------------------------------
// Top-level extension — one-shot summary object for ViewModels
// ---------------------------------------------------------------------------

/**
 * Bundles the most commonly needed analytics fields into a single immutable value object.
 *
 * Produced by [toAnalyticsSummary]. Designed to be a stable type that can be passed
 * directly to Compose UI without requiring repeated individual calls to [RunAnalytics].
 *
 * @property totalRuns       Total number of runs in the snapshot.
 * @property successRate     Fraction of successful runs (0.0–1.0).
 * @property failureRate     Fraction of failed runs (0.0–1.0).
 * @property avgLatencyMs    Arithmetic mean latency.
 * @property medianLatencyMs Median latency.
 * @property p95LatencyMs    95th-percentile latency — primary SLO indicator.
 * @property bestLatencyMs   Minimum observed latency.
 * @property worstLatencyMs  Maximum observed latency.
 * @property localRate       Fraction of on-device inference runs.
 * @property streak          Current and historical streak data.
 */
data class AnalyticsSummary(
    val totalRuns: Int,
    val successRate: Float,
    val failureRate: Float,
    val avgLatencyMs: Long,
    val medianLatencyMs: Long,
    val p95LatencyMs: Long,
    val bestLatencyMs: Long,
    val worstLatencyMs: Long,
    val localRate: Float,
    val streak: RunAnalytics.StreakInfo,
)

/**
 * Computes an [AnalyticsSummary] from this list of [RunRecord]s in a single call.
 *
 * Delegates to the individual [RunAnalytics] helpers so that each metric remains
 * independently unit-testable. The typical usage inside a ViewModel is:
 *
 * ```kotlin
 * val summary = runRecords.stateIn(viewModelScope).map { it.toAnalyticsSummary() }
 * ```
 */
fun List<RunRecord>.toAnalyticsSummary(): AnalyticsSummary = AnalyticsSummary(
    totalRuns       = size,
    successRate     = RunAnalytics.successRate(this),
    failureRate     = RunAnalytics.failureRate(this),
    avgLatencyMs    = RunAnalytics.averageLatencyMs(this),
    medianLatencyMs = RunAnalytics.medianLatencyMs(this),
    p95LatencyMs    = RunAnalytics.p95LatencyMs(this),
    bestLatencyMs   = RunAnalytics.bestLatencyMs(this),
    worstLatencyMs  = RunAnalytics.worstLatencyMs(this),
    localRate       = RunAnalytics.localInferenceRate(this),
    streak          = RunAnalytics.streakInfo(this),
)
