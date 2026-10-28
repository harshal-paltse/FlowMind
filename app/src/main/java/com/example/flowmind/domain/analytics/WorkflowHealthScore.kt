package com.example.flowmind.domain.analytics

/**
 * Computes a composite 0–100 **health score** for a workflow from its [AnalyticsSummary].
 *
 * The score is built from three independently-weighted dimensions:
 *
 * | Dimension      | Weight | Signal                                      |
 * |----------------|--------|---------------------------------------------|
 * | Reliability    |  50 %  | Success rate (0–1) scaled to 0–50           |
 * | Speed          |  30 %  | p95 latency vs a 3 000 ms reference budget  |
 * | On-device rate |  20 %  | Local inference fraction scaled to 0–20     |
 *
 * A score of **80+** is considered healthy, **60–79** needs attention,
 * and **below 60** is critical.
 */
object WorkflowHealthScore {

    /** p95 latency (ms) that maps to a full 30-point speed score. */
    const val SPEED_BUDGET_MS = 3_000L

    /**
     * Returns a [HealthResult] for [summary].
     *
     * All intermediate values are clamped to their expected ranges before
     * summation so that outlier data cannot push the score below 0 or above 100.
     */
    fun compute(summary: AnalyticsSummary): HealthResult {
        val reliabilityScore = (summary.successRate * 50f).coerceIn(0f, 50f)

        val speedFraction = if (summary.totalRuns == 0) 1f
        else (1f - (summary.p95LatencyMs.toFloat() / SPEED_BUDGET_MS)).coerceIn(0f, 1f)
        val speedScore = speedFraction * 30f

        val localScore = (summary.localRate * 20f).coerceIn(0f, 20f)

        val total = (reliabilityScore + speedScore + localScore).toInt().coerceIn(0, 100)

        return HealthResult(
            score           = total,
            reliabilityScore = reliabilityScore.toInt(),
            speedScore       = speedScore.toInt(),
            localScore       = localScore.toInt(),
            grade            = grade(total),
        )
    }

    /**
     * Convenience overload that computes a health score directly from a [List] of
     * [com.example.flowmind.domain.models.RunRecord]s, saving the caller a manual
     * call to [toAnalyticsSummary].
     */
    fun compute(records: List<com.example.flowmind.domain.models.RunRecord>): HealthResult =
        compute(records.toAnalyticsSummary())

    // -------------------------------------------------------------------------

    private fun grade(score: Int): Grade = when {
        score >= 80 -> Grade.HEALTHY
        score >= 60 -> Grade.NEEDS_ATTENTION
        else        -> Grade.CRITICAL
    }

    // -------------------------------------------------------------------------
    // Result types
    // -------------------------------------------------------------------------

    /**
     * Immutable result produced by [compute].
     *
     * @property score            Overall health score (0–100).
     * @property reliabilityScore Contribution from the reliability dimension (0–50).
     * @property speedScore       Contribution from the speed dimension (0–30).
     * @property localScore       Contribution from the on-device inference dimension (0–20).
     * @property grade            Categorical classification of the overall score.
     */
    data class HealthResult(
        val score: Int,
        val reliabilityScore: Int,
        val speedScore: Int,
        val localScore: Int,
        val grade: Grade,
    )

    /** Categorical health classification. */
    enum class Grade {
        /** Score ≥ 80. Workflow is performing well across all dimensions. */
        HEALTHY,

        /** Score 60–79. At least one dimension needs improvement. */
        NEEDS_ATTENTION,

        /** Score < 60. Significant reliability or performance issues detected. */
        CRITICAL,
    }
}

/**
 * Computes the [WorkflowHealthScore.HealthResult] for this [AnalyticsSummary].
 *
 * Usage in a ViewModel:
 * ```kotlin
 * val health = runRecords.toAnalyticsSummary().toHealthResult()
 * ```
 */
fun AnalyticsSummary.toHealthResult(): WorkflowHealthScore.HealthResult =
    WorkflowHealthScore.compute(this)
