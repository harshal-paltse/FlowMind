package com.example.flowmind.domain.validation

import com.example.flowmind.ui.screens.NodeUI

class WorkflowValidator {

    companion object {
        const val MAX_NODES = 30

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

    fun validate(nodes: List<NodeUI>): ValidationResult {
        if (nodes.isEmpty()) return ValidationResult.Error("Add at least one step.")
        if (nodes.size > MAX_NODES) return ValidationResult.Error("Max $MAX_NODES steps.")
        for (node in nodes) {
            if (node.type !in ALLOWED_NODE_TYPES)
                return ValidationResult.Error("Unknown step type: ${node.type}")
            if (node.title.length > 100)
                return ValidationResult.Error("Step title too long: ${node.title}")
        }
        val hasTrigger = nodes.any { it.type.startsWith("Trigger:") }
        if (!hasTrigger) return ValidationResult.Error("Workflow needs at least one Trigger step.")
        return ValidationResult.Success
    }
}
