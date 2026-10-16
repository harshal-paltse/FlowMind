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
}
