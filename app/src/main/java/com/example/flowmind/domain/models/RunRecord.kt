package com.example.flowmind.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One execution record written at the end of every workflow run.
 *
 * @property id            Unique run identifier (UUID).
 * @property workflowId    FK to [Workflow.id].
 * @property workflowName  Snapshot of the workflow name at run time.
 * @property templateId    Template key used, e.g. "bill", "lecture", "plant".
 * @property success       Whether the run completed without errors.
 * @property latencyMs     Wall-clock duration of the run in milliseconds.
 * @property ramUsedMb     Peak RAM consumed during the run.
 * @property isLocal       true = on-device inference, false = cloud inference.
 * @property resultJson    Serialised result payload from the final node.
 * @property pdfPath       Absolute path to the exported PDF, if any.
 * @property isSample      Rows tagged SAMPLE can be bulk-deleted from the Reports screen.
 * @property errorMessage  Failure reason when [success] is false; blank on success.
 * @property userNote      Optional free-text annotation added by the user post-run.
 * @property runAt         Epoch millis when the run started.
 */
@Entity(tableName = "run_records")
data class RunRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workflowId: String,
    val workflowName: String,
    val templateId: String = "",
    val success: Boolean,
    val latencyMs: Long,
    val ramUsedMb: Long = 0L,
    val isLocal: Boolean = true,
    val resultJson: String = "",
    val pdfPath: String = "",
    val isSample: Boolean = false,
    val errorMessage: String = "",
    val userNote: String = "",
    val runAt: Long = System.currentTimeMillis()
)
