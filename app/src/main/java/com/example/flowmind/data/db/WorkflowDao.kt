package com.example.flowmind.data.db

import androidx.room.*
import com.example.flowmind.domain.models.Workflow
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {

    /** Emits the full ordered list every time any workflow changes. */
    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    fun getAllWorkflows(): Flow<List<Workflow>>

    /**
     * Emits only non-archived workflows ordered by creation date (newest first).
     *
     * Prefer this over [getAllWorkflows] for any screen that should not show
     * archived rows (e.g. the Dashboard).
     */
    @Query("SELECT * FROM workflows WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActiveWorkflows(): Flow<List<Workflow>>

    /** Returns a single workflow by its [id], or `null` if not found. */
    @Query("SELECT * FROM workflows WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): Workflow?

    /** Returns all workflows whose [Workflow.name] matches [name] (case-sensitive). */
    @Query("SELECT * FROM workflows WHERE name = :name")
    suspend fun findByName(name: String): List<Workflow>

    /** Toggles [Workflow.isEnabled] and bumps [Workflow.updatedAt] in a single targeted update. */
    @Query("UPDATE workflows SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * Soft-archives or restores the workflow with [id] and bumps [Workflow.updatedAt].
     *
     * Pass `archived = true` to archive; `archived = false` to restore.
     */
    @Query("UPDATE workflows SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateArchived(id: String, archived: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * Returns all archived workflows ordered by most-recently updated.
     *
     * Use this to populate an "Archive" or "Trash" screen.
     */
    @Query("SELECT * FROM workflows WHERE isArchived = 1 ORDER BY updatedAt DESC")
    suspend fun findArchived(): List<Workflow>

    /** Returns the total number of stored workflows without loading any row data. */
    @Query("SELECT COUNT(*) FROM workflows")
    suspend fun countWorkflows(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: Workflow)

    @Delete
    suspend fun deleteWorkflow(workflow: Workflow)

    @Query("DELETE FROM workflows")
    suspend fun deleteAll()
}

