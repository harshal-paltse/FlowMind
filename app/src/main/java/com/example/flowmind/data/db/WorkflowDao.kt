package com.example.flowmind.data.db

import androidx.room.*
import com.example.flowmind.domain.models.Workflow
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {

    /** Emits the full ordered list every time any workflow changes. */
    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    fun getAllWorkflows(): Flow<List<Workflow>>

    /** Returns a single workflow by its [id], or `null` if not found. */
    @Query("SELECT * FROM workflows WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): Workflow?

    /** Returns all workflows whose [Workflow.name] matches [name] (case-sensitive). */
    @Query("SELECT * FROM workflows WHERE name = :name")
    suspend fun findByName(name: String): List<Workflow>

    /** Toggles [Workflow.isEnabled] and bumps [Workflow.updatedAt] in a single targeted update. */
    @Query("UPDATE workflows SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

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

