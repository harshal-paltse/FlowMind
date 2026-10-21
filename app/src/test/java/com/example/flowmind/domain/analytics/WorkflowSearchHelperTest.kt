package com.example.flowmind.domain.analytics

import com.example.flowmind.domain.models.Workflow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [WorkflowSearchHelper].
 *
 * Validates ranking tiers, blank-query short-circuit, enabled-only filtering,
 * and template-ID extraction. All tests run on the JVM with zero Android deps.
 */
class WorkflowSearchHelperTest {

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun workflow(
        name: String,
        templateId: String = "",
        isEnabled: Boolean = true,
        nodesJson: String  = "[]",
    ) = Workflow(
        id         = name.hashCode().toString(),
        name       = name,
        templateId = templateId,
        isEnabled  = isEnabled,
        nodesJson  = nodesJson,
    )

    private val sampleWorkflows = listOf(
        workflow("Bill OCR",        templateId = "bill",    nodesJson = "[\"Trigger: Camera\",\"AI: OCR\",\"Action: Save PDF\"]"),
        workflow("Lecture Audio",   templateId = "lecture", nodesJson = "[\"Trigger: Mic\",\"AI: STT\",\"Action: Save File\"]"),
        workflow("Plant Disease",   templateId = "plant",   nodesJson = "[\"Trigger: Camera\",\"AI: Vision Classify\",\"Action: Notify\"]"),
        workflow("Meeting Notes",   templateId = "meeting", nodesJson = "[\"Trigger: Mic\",\"AI: Summarize\",\"Action: Email\"]"),
        workflow("Bill Reminder",   templateId = "bill",    isEnabled = false, nodesJson = "[]"),
    )

    // ── search — blank query ───────────────────────────────────────────────────

    @Test
    fun `search returns original list unchanged for blank query`() {
        val result = WorkflowSearchHelper.search(sampleWorkflows, "   ")
        assertEquals(sampleWorkflows, result)
    }

    @Test
    fun `search returns empty list when input is empty`() {
        val result = WorkflowSearchHelper.search(emptyList(), "bill")
        assertTrue(result.isEmpty())
    }

    // ── search — ranking tiers ─────────────────────────────────────────────────

    @Test
    fun `search ranks exact name match (tier 1) above prefix match (tier 2)`() {
        val workflows = listOf(
            workflow("Bill"),      // tier 1: exact match for "bill"
            workflow("Bill OCR"),  // tier 2: starts with "bill"
        )
        val result = WorkflowSearchHelper.search(workflows, "bill")
        assertEquals("Bill", result.first().name)
    }

    @Test
    fun `search ranks name-starts-with (tier 2) above name-contains (tier 3)`() {
        val workflows = listOf(
            workflow("My Bill Report"),  // tier 3: contains "bill"
            workflow("Bill OCR"),        // tier 2: starts with "bill"
        )
        val result = WorkflowSearchHelper.search(workflows, "bill")
        assertEquals("Bill OCR", result.first().name)
    }

    @Test
    fun `search excludes workflows that have no match at any tier`() {
        val result = WorkflowSearchHelper.search(sampleWorkflows, "zzznomatch")
        assertTrue(result.isEmpty())
    }

    // ── search — case insensitivity ────────────────────────────────────────────

    @Test
    fun `search is case-insensitive`() {
        val result = WorkflowSearchHelper.search(sampleWorkflows, "LECTURE")
        assertEquals(1, result.size)
        assertEquals("Lecture Audio", result.first().name)
    }

    // ── search — node title match (tier 5) ────────────────────────────────────

    @Test
    fun `search matches workflows via node title in nodesJson`() {
        // "STT" only appears in the nodesJson of "Lecture Audio"
        val result = WorkflowSearchHelper.search(sampleWorkflows, "STT")
        assertEquals(1, result.size)
        assertEquals("Lecture Audio", result.first().name)
    }

    // ── searchEnabled ─────────────────────────────────────────────────────────

    @Test
    fun `searchEnabled excludes disabled workflows`() {
        // "bill" matches "Bill OCR" (enabled) and "Bill Reminder" (disabled)
        val result = WorkflowSearchHelper.searchEnabled(sampleWorkflows, "bill")
        assertTrue("Disabled workflow should be excluded",
            result.none { it.name == "Bill Reminder" })
        assertTrue("Enabled workflow should be included",
            result.any { it.name == "Bill OCR" })
    }

    @Test
    fun `searchEnabled returns empty list when all matching workflows are disabled`() {
        val allDisabled = sampleWorkflows.map { it.copy(isEnabled = false) }
        val result = WorkflowSearchHelper.searchEnabled(allDisabled, "bill")
        assertTrue(result.isEmpty())
    }

    // ── matchingTemplates ─────────────────────────────────────────────────────

    @Test
    fun `matchingTemplates returns distinct template IDs for matching workflows`() {
        // "audio" matches "Lecture Audio" → templateId = "lecture"
        val result = WorkflowSearchHelper.matchingTemplates(sampleWorkflows, "audio")
        assertEquals(listOf("lecture"), result)
    }

    @Test
    fun `matchingTemplates deduplicates template IDs`() {
        // "bill" matches both "Bill OCR" and "Bill Reminder", both with templateId "bill"
        val result = WorkflowSearchHelper.matchingTemplates(sampleWorkflows, "bill")
        assertEquals(1, result.size)
        assertEquals("bill", result.first())
    }

    @Test
    fun `matchingTemplates returns empty list when no workflows match`() {
        val result = WorkflowSearchHelper.matchingTemplates(sampleWorkflows, "zzznomatch")
        assertTrue(result.isEmpty())
    }
}
