package com.example.flowmind.data.db

import androidx.room.*
import com.example.flowmind.domain.models.ModelCandidate
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM models WHERE taskType = :type")
    fun getModelsForTask(type: String): Flow<List<ModelCandidate>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelCandidate)
}
