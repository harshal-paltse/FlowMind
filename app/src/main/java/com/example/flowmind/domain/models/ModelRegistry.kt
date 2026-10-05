package com.example.flowmind.domain.models

/**
 * Lightweight descriptor for an ML model available to FlowMind.
 *
 * @property id                   Stable identifier used as foreign key in Room.
 * @property name                 Display name shown in the model picker UI.
 * @property sizeMb               Approximate download size in megabytes.
 * @property requiredRamMb        Minimum free RAM required to load the model.
 * @property accuracy             Quality score in the range [0.0, 1.0].
 * @property latencyMs            Typical single-inference latency on mid-range hardware.
 * @property supportedAccelerators Hardware backends the model can run on.
 * @property sha256               Checksum used to verify the downloaded asset.
 * @property downloadUrl          Remote URL for the TFLite / GGUF file.
 */
data class MLModel(
    val id: String,
    val name: String,
    val sizeMb: Long,
    val requiredRamMb: Long,
    val accuracy: Float,
    val latencyMs: Long,
    val supportedAccelerators: List<String>,
    val sha256: String,
    val downloadUrl: String
)

/**
 * Central registry of all ML models that FlowMind can use for on-device inference.
 *
 * Add new models here; the model picker UI reads [models] automatically.
 */
object ModelRegistry {

    val models: List<MLModel> = listOf(
        MLModel(
            id = "gemini_nano",
            name = "Gemini Nano (On-Device)",
            sizeMb = 1800,
            requiredRamMb = 4000,
            accuracy = 0.95f,
            latencyMs = 1500,
            supportedAccelerators = listOf("NPU", "GPU"),
            sha256 = "dummy_hash_gemini",
            downloadUrl = "https://example.com/models/gemini_nano.tflite"
        ),
        MLModel(
            id = "mobilebert",
            name = "MobileBERT QA",
            sizeMb = 100,
            requiredRamMb = 512,
            accuracy = 0.82f,
            latencyMs = 200,
            supportedAccelerators = listOf("CPU", "GPU", "NNAPI"),
            sha256 = "dummy_hash_bert",
            downloadUrl = "https://example.com/models/mobilebert.tflite"
        ),
        MLModel(
            id = "efficientdet",
            name = "EfficientDet (Vision)",
            sizeMb = 45,
            requiredRamMb = 300,
            accuracy = 0.88f,
            latencyMs = 100,
            supportedAccelerators = listOf("CPU", "GPU", "NNAPI"),
            sha256 = "dummy_hash_vision",
            downloadUrl = "https://example.com/models/efficientdet.tflite"
        ),
        MLModel(
            id = "whisper_tiny",
            name = "Whisper Tiny (Speech-to-Text)",
            sizeMb = 39,
            requiredRamMb = 256,
            accuracy = 0.78f,
            latencyMs = 350,
            supportedAccelerators = listOf("CPU", "NNAPI"),
            sha256 = "dummy_hash_whisper",
            downloadUrl = "https://example.com/models/whisper_tiny.tflite"
        )
    )

    /** Returns the model with [id], or null if not found. */
    fun findById(id: String): MLModel? = models.firstOrNull { it.id == id }

    /** Returns all models compatible with the given [accelerator]. */
    fun filterByAccelerator(accelerator: String): List<MLModel> =
        models.filter { accelerator in it.supportedAccelerators }

    /** Returns models that fit within [availableRamMb]. */
    fun filterByRam(availableRamMb: Long): List<MLModel> =
        models.filter { it.requiredRamMb <= availableRamMb }
}
