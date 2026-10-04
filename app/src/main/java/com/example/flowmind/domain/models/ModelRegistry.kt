package com.example.flowmind.domain.models

data class MLModel(
    val id: String,
    val name: String,
    val sizeMb: Long,
    val requiredRamMb: Long,
    val accuracy: Float, // 0.0 to 1.0
    val latencyMs: Long,
    val supportedAccelerators: List<String>,
    val sha256: String,
    val downloadUrl: String
)

object ModelRegistry {
    val models = listOf(
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
        )
    )
}
