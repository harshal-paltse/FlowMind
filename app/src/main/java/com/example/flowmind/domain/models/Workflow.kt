package com.example.flowmind.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a user-defined automation workflow stored in Room.
 *
 * @property id Unique identifier (UUID).
 * @property name Human-readable workflow name.
 * @property description Auto-generated step summary (nodes joined with →).
 * @property triggerType How the workflow is activated.
 * @property nodesJson Serialised list of node titles.
 * @property edgesJson Serialised edge connections between nodes.
 * @property createdAt Epoch millis when first saved.
 * @property updatedAt Epoch millis of last modification; equals [createdAt] on creation.
 * @property isEnabled Whether the workflow is active and should receive triggers.
 */
@Entity(tableName = "workflows")
data class Workflow(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val triggerType: TriggerType,
    val nodesJson: String,
    val edgesJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)

enum class TriggerType { NOTIFICATION, SCHEDULE, CAMERA, MIC, SHARE }
