package com.example.flowmind.domain.analytics

import com.example.flowmind.domain.models.RunRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RunAnalytics].
 *
 * These are pure JVM tests (no Robolectric, no Android framework) because
 * [RunAnalytics] is entirely stateless and operates on in-memory [RunRecord] lists.
 * The suite executes in milliseconds — ideal for pre-merge CI gates.
 *
 * Coverage targets:
 *  - successRate          → empty, all-pass, all-fail, mixed
 *  - averageLatencyMs     → empty, uniform, mixed
 *  - medianLatencyMs      → empty, odd-size, even-size
 *  - bestLatencyMs        → empty, extremes
 *  - worstLatencyMs       → empty, extremes
 *  - runCountByWorkflow   → grouping + descending sort
 *  - localInferenceRate   → empty, all-local, mixed
 *  - groupByDay           → same-day coalescing, multi-day split
 */
class RunAnalyticsTest {

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Convenience builder for [RunRecord]. Only fields relevant to a specific test
     * need to be supplied; everything else defaults to a safe, deterministic value.
     */
    private fun record(
        workflowName: String = "Test Workflow",
        success: Boolean    = true,
        latencyMs: Long     = 1_000L,
        isLocal: Boolean    = true,
        templateId: String  = "test",
        runAt: Long         = System.currentTimeMillis(),
    ) = RunRecord(
        workflowId   = "wf-test",
        workflowName = workflowName,
        templateId   = templateId,
        success      = success,
        latencyMs    = latencyMs,
        ramUsedMb    = 100,
        isLocal      = isLocal,
        isSample     = false,
        runAt        = runAt,
    )

    // ── successRate ────────────────────────────────────────────────────────────

    @Test
    fun `successRate returns 0 for empty list`() =
        assertEquals(0f, RunAnalytics.successRate(emptyList()), 0.001f)

    @Test
    fun `successRate returns 1_0 when all runs succeed`() {
        val records = List(5) { record(success = true) }
        assertEquals(1.0f, RunAnalytics.successRate(records), 0.001f)
    }

    @Test
    fun `successRate returns 0_0 when all runs fail`() {
        val records = List(4) { record(success = false) }
        assertEquals(0.0f, RunAnalytics.successRate(records), 0.001f)
    }

    @Test
    fun `successRate returns correct fraction for mixed results`() {
        // 3 successes out of 4 → 0.75
        val records = listOf(
            record(success = true),
            record(success = true),
            record(success = true),
            record(success = false),
        )
        assertEquals(0.75f, RunAnalytics.successRate(records), 0.001f)
    }

    // ── averageLatencyMs ───────────────────────────────────────────────────────

    @Test
    fun `averageLatencyMs returns 0 for empty list`() =
        assertEquals(0L, RunAnalytics.averageLatencyMs(emptyList()))

    @Test
    fun `averageLatencyMs computes arithmetic mean correctly`() {
        val records = listOf(
            record(latencyMs = 1_000L),
            record(latencyMs = 2_000L),
            record(latencyMs = 3_000L),
        )
        // (1000 + 2000 + 3000) / 3 = 2000
        assertEquals(2_000L, RunAnalytics.averageLatencyMs(records))
    }

    // ── medianLatencyMs ────────────────────────────────────────────────────────

    @Test
    fun `medianLatencyMs returns 0 for empty list`() =
        assertEquals(0L, RunAnalytics.medianLatencyMs(emptyList()))

    @Test
    fun `medianLatencyMs returns correct value for odd-sized list`() {
        val records = listOf(
            record(latencyMs = 300L),
            record(latencyMs = 100L), // intentionally unsorted
            record(latencyMs = 200L),
        )
        // sorted: [100, 200, 300] → median index 1 = 200
        assertEquals(200L, RunAnalytics.medianLatencyMs(records))
    }

    @Test
    fun `medianLatencyMs picks upper-middle for even-sized list`() {
        val records = listOf(
            record(latencyMs = 100L),
            record(latencyMs = 200L),
            record(latencyMs = 300L),
            record(latencyMs = 400L),
        )
        // sorted: [100,200,300,400] → size/2 = index 2 → 300
        assertEquals(300L, RunAnalytics.medianLatencyMs(records))
    }

    // ── bestLatencyMs / worstLatencyMs ─────────────────────────────────────────

    @Test
    fun `bestLatencyMs returns 0 for empty list`() =
        assertEquals(0L, RunAnalytics.bestLatencyMs(emptyList()))

    @Test
    fun `worstLatencyMs returns 0 for empty list`() =
        assertEquals(0L, RunAnalytics.worstLatencyMs(emptyList()))

    @Test
    fun `best and worst latency return correct extremes`() {
        val records = listOf(
            record(latencyMs = 500L),
            record(latencyMs = 1_200L),
            record(latencyMs = 300L),
            record(latencyMs = 800L),
        )
        assertEquals(300L,   RunAnalytics.bestLatencyMs(records))
        assertEquals(1_200L, RunAnalytics.worstLatencyMs(records))
    }

    // ── runCountByWorkflow ─────────────────────────────────────────────────────

    @Test
    fun `runCountByWorkflow returns empty map for empty input`() =
        assertTrue(RunAnalytics.runCountByWorkflow(emptyList()).isEmpty())

    @Test
    fun `runCountByWorkflow groups correctly and sorts descending by count`() {
        val records = listOf(
            record(workflowName = "Bill OCR"),
            record(workflowName = "Plant Disease"),
            record(workflowName = "Bill OCR"),
            record(workflowName = "Bill OCR"),
            record(workflowName = "Plant Disease"),
        )
        val result = RunAnalytics.runCountByWorkflow(records)
        val keys   = result.keys.toList()

        // Bill OCR (3 runs) should rank above Plant Disease (2 runs)
        assertEquals("Bill OCR",      keys[0])
        assertEquals("Plant Disease", keys[1])
        assertEquals(3, result["Bill OCR"])
        assertEquals(2, result["Plant Disease"])
    }

    // ── localInferenceRate ─────────────────────────────────────────────────────

    @Test
    fun `localInferenceRate returns 0 for empty list`() =
        assertEquals(0f, RunAnalytics.localInferenceRate(emptyList()), 0.001f)

    @Test
    fun `localInferenceRate returns 1_0 when all runs are local`() {
        val records = List(3) { record(isLocal = true) }
        assertEquals(1.0f, RunAnalytics.localInferenceRate(records), 0.001f)
    }

    @Test
    fun `localInferenceRate returns 0_5 for equal local and cloud split`() {
        val records = listOf(
            record(isLocal = true),
            record(isLocal = true),
            record(isLocal = false),
            record(isLocal = false),
        )
        assertEquals(0.5f, RunAnalytics.localInferenceRate(records), 0.001f)
    }

    // ── groupByDay ─────────────────────────────────────────────────────────────

    @Test
    fun `groupByDay returns empty map for empty input`() =
        assertTrue(RunAnalytics.groupByDay(emptyList()).isEmpty())

    @Test
    fun `groupByDay coalesces same-day records into one bucket`() {
        val dayMs  = 86_400_000L
        val today  = (System.currentTimeMillis() / dayMs) * dayMs

        val records = listOf(
            record(runAt = today + 1_000L),   // 00:00:01 — today
            record(runAt = today + 50_000L),  // 00:00:50 — still today
            record(runAt = today - dayMs),    // yesterday
        )

        val grouped = RunAnalytics.groupByDay(records)

        assertEquals(2, grouped.size)
        assertEquals(2, grouped[today / dayMs]?.size)
    }
}
