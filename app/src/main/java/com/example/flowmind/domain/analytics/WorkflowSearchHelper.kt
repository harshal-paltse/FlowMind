package com.example.flowmind.domain.analytics

import com.example.flowmind.domain.models.Workflow

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
