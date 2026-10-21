package com.example.flowmind.domain.validation

import com.example.flowmind.ui.screens.NodeUI
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [WorkflowValidator].
 *
 * The validator is the last line of defence before a workflow is persisted to Room.
 * These tests cover both the fail-fast [validate] path and the collect-all-errors
 * [validateAll] path, ensuring regressions in either surface immediately.
 *
 * No Android framework or coroutines are needed; every assertion is pure JVM.
 */
class WorkflowValidatorTest {

    private val validator = WorkflowValidator()

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Minimal valid node list: one Trigger + one Action. */
    private fun minimalValidNodes() = listOf(
        NodeUI(type = "Trigger: Camera",  title = "Capture Image"),
        NodeUI(type = "Action: Save PDF", title = "Export Report"),
    )

    private fun node(type: String, title: String) = NodeUI(type = type, title = title)

    // ── validate — happy path ──────────────────────────────────────────────────

    @Test
    fun `validate returns Success for minimal valid workflow`() {
        val result = validator.validate(minimalValidNodes())
        assertTrue(result is WorkflowValidator.ValidationResult.Success)
    }

    @Test
    fun `validate returns Success for workflow at MAX_NODES boundary`() {
        val nodes = mutableListOf(node("Trigger: Schedule", "Start"))
        repeat(WorkflowValidator.MAX_NODES - 2) { i -> nodes += node("AI: OCR", "Step $i") }
        nodes += node("Action: Notify", "Done")
        assertTrue(validator.validate(nodes) is WorkflowValidator.ValidationResult.Success)
    }

    // ── validate — MIN_NODES guard ─────────────────────────────────────────────

    @Test
    fun `validate rejects empty workflow`() =
        assertError(validator.validate(emptyList()), "at least ${WorkflowValidator.MIN_NODES} steps")

    @Test
    fun `validate rejects workflow with only one node`() =
        assertError(validator.validate(listOf(node("Trigger: Camera", "Capture"))),
            "at least ${WorkflowValidator.MIN_NODES} steps")

    // ── validate — MAX_NODES guard ─────────────────────────────────────────────

    @Test
    fun `validate rejects workflow exceeding MAX_NODES`() {
        val nodes = List(WorkflowValidator.MAX_NODES + 1) { i -> node("AI: OCR", "Step $i") }
        assertError(validator.validate(nodes), "Max ${WorkflowValidator.MAX_NODES} steps")
    }

    // ── validate — unknown node type ───────────────────────────────────────────

    @Test
    fun `validate rejects unknown node type`() {
        val nodes = listOf(
            node("Trigger: Camera",       "Start"),
            node("UNKNOWN: DoSomething",  "Mystery Box"),
            node("Action: Save PDF",      "Finish"),
        )
        assertError(validator.validate(nodes), "Unknown step type")
    }

    // ── validate — blank and over-length titles ────────────────────────────────

    @Test
    fun `validate rejects node with blank title`() {
        val nodes = listOf(
            node("Trigger: Schedule", ""),
            node("Action: Notify",    "Send Alert"),
        )
        assertError(validator.validate(nodes), "blank")
    }

    @Test
    fun `validate rejects node with title exceeding 100 characters`() {
        val longTitle = "A".repeat(101)
        val nodes = listOf(
            node("Trigger: Camera",  longTitle),
            node("Action: Save PDF", "Export"),
        )
        assertError(validator.validate(nodes), "too long")
    }

    // ── validate — duplicate title guard ──────────────────────────────────────

    @Test
    fun `validate rejects duplicate step titles (case-insensitive)`() {
        val nodes = listOf(
            node("Trigger: Camera",  "Capture Image"),
            node("AI: OCR",          "capture image"),   // same as above, different case
            node("Action: Save PDF", "Export Report"),
        )
        assertError(validator.validate(nodes), "Duplicate step title")
    }

    // ── validate — missing Trigger / Action ───────────────────────────────────

    @Test
    fun `validate rejects workflow with no Trigger node`() {
        val nodes = listOf(
            node("AI: OCR",          "Run OCR"),
            node("Action: Save PDF", "Export"),
        )
        assertError(validator.validate(nodes), "Trigger")
    }

    @Test
    fun `validate rejects workflow with no Action node`() {
        val nodes = listOf(
            node("Trigger: Camera", "Capture"),
            node("AI: OCR",         "Run OCR"),
        )
        assertError(validator.validate(nodes), "Action")
    }

    // ── validateName ───────────────────────────────────────────────────────────

    @Test
    fun `validateName returns Success for a normal name`() {
        assertTrue(validator.validateName("My Awesome Workflow") is WorkflowValidator.ValidationResult.Success)
    }

    @Test
    fun `validateName rejects blank name`() =
        assertError(validator.validateName("   "), "blank")

    @Test
    fun `validateName rejects name exceeding MAX_NAME_LENGTH`() {
        val longName = "W".repeat(WorkflowValidator.MAX_NAME_LENGTH + 1)
        assertError(validator.validateName(longName), WorkflowValidator.MAX_NAME_LENGTH.toString())
    }

    // ── validateAll — collects every error in one pass ─────────────────────────

    @Test
    fun `validateAll returns empty list for a valid workflow`() {
        val errors = validator.validateAll(minimalValidNodes())
        assertTrue("Expected no errors but got: $errors", errors.isEmpty())
    }

    @Test
    fun `validateAll collects multiple distinct errors simultaneously`() {
        // unknown type + blank title + no Trigger + no Action all in one pass
        val nodes = listOf(
            node("UNKNOWN: Foo",       ""),
            node("AI: LLM Generate",   "LLM Step"),
        )
        val errors = validator.validateAll(nodes)
        assertTrue("Expected >= 4 errors, got ${errors.size}", errors.size >= 4)
        assertTrue(errors.any { it.contains("Unknown",  ignoreCase = true) })
        assertTrue(errors.any { it.contains("blank",    ignoreCase = true) })
        assertTrue(errors.any { it.contains("Trigger",  ignoreCase = true) })
        assertTrue(errors.any { it.contains("Action",   ignoreCase = true) })
    }

    @Test
    fun `validateAll does not short-circuit on first failure`() {
        val nodes = listOf(
            node("Trigger: Camera",  "Capture"),
            node("AI: OCR",          ""),          // blank title
            node("AI: LLM Generate", "Capture"),  // duplicate of node[0]
            node("Action: Notify",   "Alert"),
        )
        val errors = validator.validateAll(nodes)
        assertTrue("Blank-title error missing",     errors.any { it.contains("blank",     ignoreCase = true) })
        assertTrue("Duplicate-title error missing", errors.any { it.contains("Duplicate", ignoreCase = true) })
    }

    // ── Private assertion helper ───────────────────────────────────────────────

    private fun assertError(result: WorkflowValidator.ValidationResult, substring: String) {
        assertTrue(
            "Expected ValidationResult.Error containing \"$substring\" but got: $result",
            result is WorkflowValidator.ValidationResult.Error &&
                result.message.contains(substring, ignoreCase = true)
        )
    }
}
