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

        /**
         * Maximum allowed length for an optional workflow description.
         * Descriptions are stored in the database and surfaced on the Dashboard detail card.
         */
        const val MAX_DESCRIPTION_LENGTH = 250

        /** Maximum characters allowed in a single step title. */
        const val MAX_TITLE_LENGTH = 100

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
     * Aggregated result produced by [validateSummary].
     *
     * Carries both the pass/fail flag and the full list of error messages so callers can
     * render a single inline error list rather than calling [validateAll] and then checking
     * `isEmpty()` separately.
     *
     * @property isValid  `true` when [errors] is empty.
     * @property errors   Human-readable error messages in the order they were detected.
     * @property warnings Advisory messages that do not prevent saving but should be surfaced
     *                    to the user (e.g., no Transform node, very long workflow).
     */
    data class ValidationSummary(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
    )

    /**
     * Validates the list of [nodes] for structural correctness.
     *
     * Checks performed (in order):
     * 1. Minimum step count ([MIN_NODES]).
     * 2. Maximum step count ([MAX_NODES]).
     * 3. Unknown node types.
     * 4. Step titles that are blank or too long (> [MAX_TITLE_LENGTH] chars).
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
            if (node.title.length > MAX_TITLE_LENGTH)
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

    /**
     * Validates an optional workflow [description] string.
     *
     * Unlike [validateName], a blank description is **allowed** — it simply means
     * the user has not filled in the optional field. Only non-blank values that
     * exceed [MAX_DESCRIPTION_LENGTH] are rejected.
     */
    fun validateDescription(description: String): ValidationResult {
        if (description.isNotBlank() && description.length > MAX_DESCRIPTION_LENGTH)
            return ValidationResult.Error(
                "Description must be $MAX_DESCRIPTION_LENGTH characters or fewer (got ${description.length})."
            )
        return ValidationResult.Success
    }

    /**
     * Runs all validation checks on [nodes] and **collects every error** rather
     * than returning on the first failure.
     *
     * This is preferred for builder UIs that want to show all issues at once
     * so the user can fix them in a single pass.
     *
     * @param nodes The list of [NodeUI] steps to validate.
     * @return A list of human-readable error messages. Empty list means valid.
     */
    fun validateAll(nodes: List<NodeUI>): List<String> {
        val errors = mutableListOf<String>()

        if (nodes.size < MIN_NODES)
            errors += "Workflow needs at least $MIN_NODES steps."
        if (nodes.size > MAX_NODES)
            errors += "Max $MAX_NODES steps allowed."

        val seen = mutableSetOf<String>()
        for (node in nodes) {
            if (node.type !in ALLOWED_NODE_TYPES)
                errors += "Unknown step type: \"${node.type}\""
            if (node.title.isBlank())
                errors += "A step title must not be blank."
            else if (node.title.length > MAX_TITLE_LENGTH)
                errors += "Step title too long: \"${node.title}\""
            if (!seen.add(node.title.trim().lowercase()))
                errors += "Duplicate step title: \"${node.title}\""
        }

        if (nodes.none { it.type.startsWith("Trigger:") })
            errors += "Workflow needs at least one Trigger step."
        if (nodes.none { it.type.startsWith("Action:") })
            errors += "Workflow needs at least one Action step (e.g. Notify, Email, Save PDF)."

        return errors
    }

    /**
     * Runs all validation checks and additionally produces **advisory warnings** for
     * patterns that are technically valid but may indicate an incomplete workflow.
     *
     * Current warnings:
     * - No Transform or AI node: the workflow goes straight from Trigger to Action,
     *   which is uncommon and may be unintentional.
     * - High node count (> 20 nodes): may indicate the workflow should be split.
     *
     * @param nodes     The list of [NodeUI] steps to validate.
     * @param name      The workflow name to include in name-level validation.
     * @param description Optional workflow description (may be blank).
     * @return [ValidationSummary] with errors and warnings populated.
     */
    fun validateSummary(
        nodes: List<NodeUI>,
        name: String,
        description: String = "",
    ): ValidationSummary {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Name validation
        when (val result = validateName(name)) {
            is ValidationResult.Error -> errors += result.message
            else -> Unit
        }

        // Description validation
        when (val result = validateDescription(description)) {
            is ValidationResult.Error -> errors += result.message
            else -> Unit
        }

        // Node-level errors
        errors += validateAll(nodes)

        // Advisory warnings (do not block saving)
        val hasTransformOrAi = nodes.any {
            it.type.startsWith("Transform:") || it.type.startsWith("AI:")
        }
        if (nodes.size >= MIN_NODES && !hasTransformOrAi) {
            warnings += "No AI or Transform step detected. Consider adding one to process the trigger data before acting on it."
        }
        if (nodes.size > 20) {
            warnings += "This workflow has ${nodes.size} steps. Consider splitting it into smaller, reusable workflows for easier maintenance."
        }

        return ValidationSummary(
            isValid  = errors.isEmpty(),
            errors   = errors,
            warnings = warnings,
        )
    }
}
