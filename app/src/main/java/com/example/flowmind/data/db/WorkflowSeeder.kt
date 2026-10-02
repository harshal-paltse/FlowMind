package com.example.flowmind.data.db

import com.example.flowmind.domain.models.TriggerType
import com.example.flowmind.domain.models.Workflow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowSeeder @Inject constructor(
    private val workflowDao: WorkflowDao
) {
    suspend fun seedDemoWorkflows() {
        val workflows = listOf(
            Workflow(
                name = "Bill & Expense Tracker",
                description = "Take a photo of a handwritten bill -> extract items/prices -> calculate total -> categorize -> save to tracker.",
                triggerType = TriggerType.CAMERA,
                nodesJson = """[
                    {"id": "1", "type": "TRIGGER", "subtype": "CAMERA"},
                    {"id": "2", "type": "AI_STEP", "subtype": "OCR"},
                    {"id": "3", "type": "TRANSFORM", "subtype": "EXTRACT_CALCULATE"},
                    {"id": "4", "type": "AI_STEP", "subtype": "LLM", "parameters": {"task": "categorize expense"}},
                    {"id": "5", "type": "ACTION", "subtype": "SAVE_DB"}
                ]""",
                edgesJson = """[{"sourceId":"1","targetId":"2"}, {"sourceId":"2","targetId":"3"}, {"sourceId":"3","targetId":"4"}, {"sourceId":"4","targetId":"5"}]"""
            ),
            Workflow(
                name = "Lecture to Notes & Quiz",
                description = "Record a lecture -> transcribe -> identify concepts -> generate notes -> create 5 quiz questions.",
                triggerType = TriggerType.MIC,
                nodesJson = """[
                    {"id": "1", "type": "TRIGGER", "subtype": "MIC"},
                    {"id": "2", "type": "AI_STEP", "subtype": "STT"},
                    {"id": "3", "type": "AI_STEP", "subtype": "LLM", "parameters": {"task": "summarize notes"}},
                    {"id": "4", "type": "AI_STEP", "subtype": "LLM", "parameters": {"task": "generate 5 questions"}},
                    {"id": "5", "type": "ACTION", "subtype": "SAVE_FILE"}
                ]""",
                edgesJson = """[{"sourceId":"1","targetId":"2"}, {"sourceId":"2","targetId":"3"}, {"sourceId":"3","targetId":"4"}, {"sourceId":"4","targetId":"5"}]"""
            ),
            Workflow(
                name = "Plant Disease Care Plan",
                description = "Take a photo of a plant -> identify disease -> explain symptoms -> retrieve treatment -> create care plan.",
                triggerType = TriggerType.CAMERA,
                nodesJson = """[
                    {"id": "1", "type": "TRIGGER", "subtype": "CAMERA"},
                    {"id": "2", "type": "AI_STEP", "subtype": "VISION_CLASSIFY", "parameters": {"model": "plant_disease_net"}},
                    {"id": "3", "type": "AI_STEP", "subtype": "LLM", "parameters": {"task": "explain symptoms & treatment"}},
                    {"id": "4", "type": "ACTION", "subtype": "NOTIFY"}
                ]""",
                edgesJson = """[{"sourceId":"1","targetId":"2"}, {"sourceId":"2","targetId":"3"}, {"sourceId":"3","targetId":"4"}]"""
            )
        )
        workflows.forEach { workflow ->
            workflowDao.insertWorkflow(workflow)
        }
    }
}
