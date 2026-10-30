package com.example.flowmind.data

import com.example.flowmind.data.db.WorkflowDao
import com.example.flowmind.domain.models.Workflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for [Workflow] persistence.
 *
 * All callers (ViewModels, workers) must go through this repository rather than
 * talking to [WorkflowDao] directly, which keeps the data layer swappable.
 *
 * @property dao Room data-access object injected by Hilt.
 */
@Singleton
class WorkflowRepository @Inject constructor(private val dao: WorkflowDao) {

    /** Emits the full workflow list, ordered by creation date (newest first). */
    fun allWorkflows(): Flow<List<Workflow>> = dao.getAllWorkflows()

    /** Persists a new workflow or overwrites an existing one with the same [Workflow.id]. */
    suspend fun save(workflow: Workflow) = dao.insertWorkflow(workflow)

    /** Removes a workflow from the database. */
    suspend fun delete(workflow: Workflow) = dao.deleteWorkflow(workflow)

    /**
     * Returns a single [Workflow] by its unique [id], or `null` if it does not exist.
     * Useful for deep-link navigation and result-screen data loading.
     */
    suspend fun findById(id: String): Workflow? = dao.findById(id)

    /**
     * Returns all workflows whose [Workflow.name] equals [name] (case-sensitive).
     * Used by [com.example.flowmind.ui.screens.BuilderViewModel] to detect duplicate names.
     */
    suspend fun findByName(name: String): List<Workflow> = dao.findByName(name)

    /**
     * Toggles the [Workflow.isEnabled] flag for the workflow with [id] without
     * replacing the entire row, and automatically bumps [Workflow.updatedAt].
     */
    suspend fun setEnabled(id: String, enabled: Boolean) = dao.updateEnabled(id, enabled)

    /** Returns the total number of saved workflows without loading row data. */
    suspend fun count(): Int = dao.countWorkflows()

    // -------------------------------------------------------------------------
    // Archive helpers
    // -------------------------------------------------------------------------

    /**
     * Emits only non-archived workflows, ordered by creation date (newest first).
     *
     * This is the primary stream consumed by the Dashboard and should be preferred
     * over [allWorkflows] in any screen that does not explicitly need archived rows.
     */
    fun allActiveWorkflows(): Flow<List<Workflow>> =
        allWorkflows().map { list -> list.filter { !it.isArchived } }

    /**
     * Soft-archives the workflow with [id].
     *
     * Archived workflows are excluded from [allActiveWorkflows] but can be
     * restored with [unarchiveWorkflow] or permanently removed with [delete].
     * Also bumps [Workflow.updatedAt] to reflect the state change.
     */
    suspend fun archiveWorkflow(id: String) = dao.updateArchived(id, archived = true)

    /**
     * Restores a previously archived workflow, making it visible in [allActiveWorkflows] again.
     */
    suspend fun unarchiveWorkflow(id: String) = dao.updateArchived(id, archived = false)
    /**
     * Returns a one-shot list of all archived workflows, ordered by most-recently updated.
     * Useful for rendering an "Archive" or "Trash" screen.
     */
     
    suspend fun findArchived(): List<Workflow> = dao.findArchived()
}
}
.
