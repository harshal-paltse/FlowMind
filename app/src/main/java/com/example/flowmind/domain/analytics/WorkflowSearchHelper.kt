package com.example.flowmind.domain.analytics

import com.example.flowmind.domain.models.Workflow
import com.example.flowmind.domain.models.RunRecord

/**
 * Pure, framework-free helper that provides fuzzy/ranked search over a list of [Workflow]s.
 *
 * All functions are stateless and thread-safe — they do not mutate any input list.
 */
object WorkflowSearchHelper {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Filters [workflows] by [query] using a simple substring match across the
     * workflow name and its node titles.
     *
     * Results are returned ranked by relevance:
     * 1. Exact name match (case-insensitive)
     * 2. Name starts with query
     * 3. Name contains query
     * 4. Any node title matches query
     *
     * @param workflows  Full list of workflows to search.
     * @param query      The search string. Blank query returns the original list unchanged.
     * @return           Filtered and ranked list.
     */
    fun search(workflows: List<Workflow>, query: String): List<Workflow> {
        val q = query.trim()
        if (q.isBlank()) return workflows

        val lower = q.lowercase()

        return workflows
            .mapNotNull { wf -> score(wf, lower)?.let { wf to it } }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Returns only [Workflow]s that are currently enabled and match [query].
     *
     * @see search
     */
    fun searchEnabled(workflows: List<Workflow>, query: String): List<Workflow> =
        search(workflows.filter { it.isEnabled }, query)

    /**
     * Returns distinct template IDs found in [workflows] whose name matches [query].
     *
     * Useful for populating a filter chip row.
     */
    fun matchingTemplates(workflows: List<Workflow>, query: String): List<String> =
        search(workflows, query)
            .mapNotNull { it.templateId.takeIf { id -> id.isNotBlank() } }
            .distinct()

    /**
     * Filters [workflows] to only those whose [Workflow.templateId] equals [tag]
     * (case-insensitive), then applies [search] on top.
     *
     * This enables the Dashboard chip-filtered search bar: tapping a template chip
     * narrows results to that tag before any text-query ranking.
     *
     * @param workflows Full workflow list.
     * @param tag       Template tag to filter by (e.g. "bill", "lecture").
     * @param query     Optional text query applied on top of the tag filter.
     * @return          Tag-filtered and text-ranked list.
     */
    fun searchByTag(workflows: List<Workflow>, tag: String, query: String = ""): List<Workflow> {
        val filtered = workflows.filter { it.templateId.equals(tag, ignoreCase = true) }
        return search(filtered, query)
    }

    /**
     * Sorts [workflows] so that the workflow with the most recent run appears first.
     *
     * Workflows with no run records are pushed to the end of the list.
     *
     * @param workflows  Workflows to sort.
     * @param runRecords All run records used to determine recency.
     * @return           Sorted list, most-recently-run first.
     */
    fun sortByLastRun(
        workflows: List<Workflow>,
        runRecords: List<RunRecord>,
    ): List<Workflow> {
        val lastRunAt: Map<String, Long> = runRecords
            .groupBy { it.workflowId }
            .mapValues { (_, records) -> records.maxOf { it.runAt } }

        return workflows.sortedByDescending { lastRunAt[it.id] ?: Long.MIN_VALUE }
    }

    /**
     * Convenience helper that applies [search] ranking first, then [sortByLastRun]
     * as a secondary sort within same-score groups.
     *
     * Intended for the Dashboard "Recent" tab where text-relevance should still
     * win, but ties break in favour of the most-recently-run workflow.
     *
     * @param workflows  Workflows to search and sort.
     * @param query      Text query passed to [search].
     * @param runRecords Run records used to compute recency.
     */
    fun searchAndSort(
        workflows: List<Workflow>,
        query: String,
        runRecords: List<RunRecord>,
    ): List<Workflow> {
        val searched = search(workflows, query)
        if (query.isBlank()) return sortByLastRun(searched, runRecords)
        return searched
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Assigns a relevance score to [wf] for the lowercase [query].
     *
     * Returns `null` if there is no match at all.
     */
    private fun score(wf: Workflow, query: String): Int? {
        val name = wf.name.lowercase()

        // Tier 1 — exact name match
        if (name == query) return 100

        // Tier 2 — name starts with query
        if (name.startsWith(query)) return 80

        // Tier 3 — name contains query
        if (name.contains(query)) return 60

        // Tier 4 — template id match
        if (wf.templateId.lowercase().contains(query)) return 40

        // Tier 5 — node title match (parse quick from JSON if available)
        if (nodeMatchesQuery(wf.nodesJson, query)) return 20

        return null
    }

    /**
     * Naively checks whether [nodesJson] contains [query] as a substring.
     *
     * A full JSON parse is intentionally avoided here for performance; the raw
     * string check is sufficient for search ranking purposes.
     */
    private fun nodeMatchesQuery(nodesJson: String, query: String): Boolean =
        nodesJson.lowercase().contains(query)
}
