package com.example.flowmind.domain.audio

import com.example.flowmind.domain.models.TriggerType
import com.example.flowmind.domain.models.Workflow

class VoiceToWorkflow {

    fun parseVoiceCommand(transcript: String): Workflow {
        val lowerTranscript = transcript.lowercase()
        
        // Very basic NLP extraction simulation
        var name = "Voice Generated Workflow"
        var trigger = TriggerType.NOTIFICATION
        
        if (lowerTranscript.contains("when i take a photo") || lowerTranscript.contains("camera")) {
            trigger = TriggerType.CAMERA
            name = "Photo Automation"
        } else if (lowerTranscript.contains("when i speak") || lowerTranscript.contains("record")) {
            trigger = TriggerType.MIC
            name = "Audio Automation"
        } else if (lowerTranscript.contains("when i get a message") || lowerTranscript.contains("notification")) {
            trigger = TriggerType.NOTIFICATION
            name = "Notification Automation"
        }

        val description = "Extracted intent: \"$transcript\""

        return Workflow(
            name = name,
            description = description,
            triggerType = trigger,
            nodesJson = "[]", // In real app, AI would generate the nodes JSON here
            edgesJson = "[]"
        )
    }
}
