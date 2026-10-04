package com.example.flowmind.data.db

import androidx.room.*
import com.example.flowmind.domain.models.Workflow
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    fun getAllWorkflows(): Flow<List<Workflow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: Workflow)

    @Delete
    suspend fun deleteWorkflow(workflow: Workflow)

    @Query("DELETE FROM workflows")
    suspend fun deleteAll()
}

