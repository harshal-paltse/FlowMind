package com.example.flowmind.domain.audio

import com.example.flowmind.domain.models.TriggerType
import com.example.flowmind.domain.models.Workflow

/**
 * Converts a speech-recognition transcript into a [Workflow] object by
 * applying keyword-based intent extraction.
 *
 * This is a lightweight, on-device NLP layer. For production use, the parsing
 * logic can be replaced with a call to an AI inference endpoint while keeping
 * this class as the single entry-point for voice-driven workflow creation.
 */
class VoiceToWorkflow {

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Parses a raw voice [transcript] and produces a best-effort [Workflow].
     *
     * @param transcript The speech-to-text string from the microphone.
     * @return A [Workflow] whose trigger and name reflect the detected intent.
     */
    fun parseVoiceCommand(transcript: String): Workflow {
        val lower = transcript.lowercase()
        val (name, trigger) = detectIntent(lower)
        val description = "Extracted intent: \"$transcript\""

        return Workflow(
            name = name,
            description = description,
            triggerType = trigger,
            nodesJson = "[]", // In real app, AI would generate the nodes JSON here
            edgesJson = "[]"
        )
    }

    /**
     * Returns a confidence score in [0.0, 1.0] indicating how well the
     * [transcript] matched a known intent keyword.
     *
     * A score of **0.0** means no keyword matched (fallback intent was used).
     * A score of **1.0** means a high-priority exact phrase was found.
     */
    fun confidenceScore(transcript: String): Float {
        val lower = transcript.lowercase()
        return when {
            CAMERA_HIGH.any { lower.contains(it) }       -> 1.0f
            CAMERA_LOW.any { lower.contains(it) }        -> 0.7f
            MIC_HIGH.any { lower.contains(it) }          -> 1.0f
            MIC_LOW.any { lower.contains(it) }           -> 0.7f
            NOTIFICATION_HIGH.any { lower.contains(it) } -> 1.0f
            NOTIFICATION_LOW.any { lower.contains(it) }  -> 0.7f
            LOCATION_HIGH.any { lower.contains(it) }     -> 1.0f
            LOCATION_LOW.any { lower.contains(it) }      -> 0.7f
            else                                         -> 0.0f
        }
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    /** Maps a lower-cased transcript to a (workflowName, triggerType) pair. */
    private fun detectIntent(lower: String): Pair<String, TriggerType> = when {
        CAMERA_HIGH.any { lower.contains(it) } || CAMERA_LOW.any { lower.contains(it) } ->
            "Photo Automation" to TriggerType.CAMERA

        MIC_HIGH.any { lower.contains(it) } || MIC_LOW.any { lower.contains(it) } ->
            "Audio Automation" to TriggerType.MIC

        LOCATION_HIGH.any { lower.contains(it) } || LOCATION_LOW.any { lower.contains(it) } ->
            "Location Automation" to TriggerType.NOTIFICATION // fallback until LOCATION trigger exists

        NOTIFICATION_HIGH.any { lower.contains(it) } || NOTIFICATION_LOW.any { lower.contains(it) } ->
            "Notification Automation" to TriggerType.NOTIFICATION

        else ->
            "Voice Generated Workflow" to TriggerType.NOTIFICATION
    }

    // ---------------------------------------------------------------------------
    // Keyword dictionaries
    // ---------------------------------------------------------------------------

    private val CAMERA_HIGH = listOf("when i take a photo", "on photo capture", "after taking a picture")
    private val CAMERA_LOW  = listOf("camera", "photo", "picture", "snapshot", "scan")

    private val MIC_HIGH = listOf("when i speak", "when i record", "after recording audio")
    private val MIC_LOW  = listOf("record", "microphone", "voice", "audio", "speak", "say")

    private val NOTIFICATION_HIGH = listOf("when i get a message", "when a notification arrives", "on new notification")
    private val NOTIFICATION_LOW  = listOf("notification", "message", "alert", "ping", "notify")

    private val LOCATION_HIGH = listOf("when i arrive at", "when i leave", "when i reach", "on location change")
    private val LOCATION_LOW  = listOf("location", "gps", "place", "geofence", "arrive", "leave")
}
