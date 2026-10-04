package com.example.flowmind.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** One execution record written at the end of every workflow run. */
@Entity(tableName = "run_records")
data class RunRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workflowId: String,
    val workflowName: String,
    val templateId: String = "",          // e.g. "bill", "lecture", "plant"
    val success: Boolean,
    val latencyMs: Long,
    val ramUsedMb: Long = 0L,
    val isLocal: Boolean = true,          // true = on-device, false = cloud
    val resultJson: String = "",          // serialised result payload
    val pdfPath: String = "",
    val isSample: Boolean = false,        // rows tagged SAMPLE can be bulk-deleted
    val runAt: Long = System.currentTimeMillis()
)
