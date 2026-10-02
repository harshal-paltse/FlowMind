package com.example.flowmind.data.db

import androidx.room.*
import com.example.flowmind.domain.models.Workflow
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows")
    fun getAllWorkflows(): Flow<List<Workflow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: Workflow)
}
