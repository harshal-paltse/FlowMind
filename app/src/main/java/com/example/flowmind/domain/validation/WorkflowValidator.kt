package com.example.flowmind.domain.validation

import com.example.flowmind.ui.screens.NodeUI

/**
 * Validates a list of [NodeUI] steps before a workflow is persisted.
 *
 * Validation is intentionally strict so that corrupt or incomplete workflows
 * never reach the execution engine.
 */
class WorkflowValidator {

    companion object {
        /** Maximum number of steps a single workflow may contain. */
        const val MAX_NODES = 30

        /** Minimum number of steps a workflow must contain. */
        const val MIN_NODES = 2

        /** Maximum allowed length for a workflow name. */
        const val MAX_NAME_LENGTH = 60

        val ALLOWED_NODE_TYPES = setOf(
            "Trigger: Camera", "Trigger: Mic", "Trigger: Notification", "Trigger: Schedule", "Trigger: Share",
            "AI: OCR", "AI: LLM Generate", "AI: STT", "AI: Vision Classify", "AI: Summarize",
            "Transform: Calculate", "Transform: Categorize", "Transform: Format",
            "Action: Save PDF", "Action: Save DB", "Action: Save File",
            "Action: Notify", "Action: Email", "Action: Calendar"
        )
    }

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    /**
     * Validates the list of [nodes] for structural correctness.
     *
     * Checks performed (in order):
     * 1. Minimum step count ([MIN_NODES]).
     * 2. Maximum step count ([MAX_NODES]).
     * 3. Unknown node types.
     * 4. Step titles that are blank or too long (> 100 chars).
     * 5. Duplicate step titles.
     * 6. At least one Trigger node must be present.
     * 7. At least one Action node must be present.
     */
    fun validate(nodes: List<NodeUI>): ValidationResult {
        if (nodes.size < MIN_NODES)
            return ValidationResult.Error("Workflow needs at least $MIN_NODES steps.")
        if (nodes.size > MAX_NODES)
            return ValidationResult.Error("Max $MAX_NODES steps allowed.")

        for (node in nodes) {
            if (node.type !in ALLOWED_NODE_TYPES)
                return ValidationResult.Error("Unknown step type: ${node.type}")
            if (node.title.isBlank())
                return ValidationResult.Error("Step title must not be blank.")
            if (node.title.length > 100)
                return ValidationResult.Error("Step title too long: \"${node.title}\"")
        }

        // Duplicate title guard
        val seen = mutableSetOf<String>()
        for (node in nodes) {
            if (!seen.add(node.title.trim().lowercase())) {
                return ValidationResult.Error("Duplicate step title: \"${node.title}\"")
            }
        }

        val hasTrigger = nodes.any { it.type.startsWith("Trigger:") }
        if (!hasTrigger)
            return ValidationResult.Error("Workflow needs at least one Trigger step.")

        val hasAction = nodes.any { it.type.startsWith("Action:") }
        if (!hasAction)
            return ValidationResult.Error("Workflow needs at least one Action step (e.g. Notify, Email, Save PDF).")

        return ValidationResult.Success
    }

    /**
     * Validates a proposed workflow [name] string on its own.
     * Used by the builder UI before calling [validate] on the full node list.
     */
    fun validateName(name: String): ValidationResult {
        if (name.isBlank()) return ValidationResult.Error("Workflow name must not be blank.")
        if (name.length > MAX_NAME_LENGTH)
            return ValidationResult.Error("Workflow name must be $MAX_NAME_LENGTH characters or fewer.")
        return ValidationResult.Success
    }
}
