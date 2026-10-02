package com.example.flowmind.ml.planner

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowPlanner @Inject constructor() {

    private val systemPrompt = """
        You are an AI planner for an Android automation app. 
        Convert the user's natural language request into a strict JSON workflow schema.
        Available Node Types: TRIGGER (CAMERA, NOTIFICATION, SCHEDULE), AI_STEP (OCR, STT, LLM, VISION), ACTION (SAVE_DB, FILE, NOTIFY), TRANSFORM (EXTRACT, CALCULATE, CATEGORIZE).
        Output ONLY valid JSON matching the schema.
    """.trimIndent()

    suspend fun generateWorkflowFromJson(userPrompt: String): String {
        return """
        {
          "workflowName": "Expense Tracker",
          "description": "Extracts bill items and saves them.",
          "nodes": [
            { "id": "1", "type": "TRIGGER", "subtype": "CAMERA" },
            { "id": "2", "type": "AI_STEP", "subtype": "OCR" },
            { "id": "3", "type": "AI_STEP", "subtype": "LLM", "parameters": {"prompt": "Extract total and items"} },
            { "id": "4", "type": "ACTION", "subtype": "SAVE_DB" }
          ],
          "edges": [
            { "sourceId": "1", "targetId": "2" },
            { "sourceId": "2", "targetId": "3" },
            { "sourceId": "3", "targetId": "4" }
          ]
        }
        """.trimIndent()
    }
}
