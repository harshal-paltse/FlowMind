package com.example.flowmind.domain.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * A downloadable ML model that can be selected for on-device inference.
 *
 * Stored in Room so the user's download status persists across sessions.
 *
 * @property id              Stable identifier matching [ModelRegistry.MLModel.id].
 * @property name            Human-readable display name.
 * @property taskType        The ML task this model performs, e.g. "nlp", "vision", "speech".
 * @property sizeMb          Download size in megabytes.
 * @property requiredRamMb   Minimum free RAM required to load and run the model.
 * @property latencyScore    Lower is faster; used for ranking in the UI (0–100).
 * @property accuracyScore   Higher is better; used for ranking in the UI (0–100).
 * @property accelerator     Preferred hardware backend, e.g. "GPU", "NNAPI", "CPU".
 * @property downloadUrl     Remote URL of the TFLite / GGUF asset.
 * @property sha256          SHA-256 checksum for download integrity verification.
 * @property isDownloaded    Whether the model file is present on-device and ready to use.
 */
@Entity(tableName = "models")
data class ModelCandidate(
    @PrimaryKey val id: String,
    val name: String,
    val taskType: String,
    val sizeMb: Int,
    val requiredRamMb: Int,
    val latencyScore: Int,
    val accuracyScore: Int,
    val accelerator: String,
    val downloadUrl: String,
    val sha256: String,
    val isDownloaded: Boolean = false
) {
    /**
     * A composite score used for default ranking in the model picker.
     * Weights accuracy (60 %) more than speed (40 %).
     */
    @get:Ignore
    val compositeScore: Float
        get() = accuracyScore * 0.6f + (100 - latencyScore) * 0.4f

    /** Returns true when the model can run entirely on the CPU without extra hardware. */
    @get:Ignore
    val isCpuCompatible: Boolean
        get() = accelerator.equals("CPU", ignoreCase = true)

    /** Human-readable summary shown in tooltips and accessibility labels. */
    @get:Ignore
    val summary: String
        get() = "$name · ${sizeMb}MB · $accelerator · accuracy $accuracyScore%"
}
