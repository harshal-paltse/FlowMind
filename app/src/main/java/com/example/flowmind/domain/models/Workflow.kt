package com.example.flowmind.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workflows")
data class Workflow(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val triggerType: TriggerType,
    val nodesJson: String,
    val edgesJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TriggerType { NOTIFICATION, SCHEDULE, CAMERA, MIC, SHARE }
