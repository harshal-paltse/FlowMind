package com.example.flowmind.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey

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
)
