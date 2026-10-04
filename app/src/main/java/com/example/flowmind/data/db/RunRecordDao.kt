package com.example.flowmind.data.db

import androidx.room.*
import com.example.flowmind.domain.models.RunRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RunRecordDao {

    @Query("SELECT * FROM run_records ORDER BY runAt DESC")
    fun getAllRuns(): Flow<List<RunRecord>>

    @Query("SELECT * FROM run_records ORDER BY runAt DESC LIMIT 100")
    suspend fun getAll(): List<RunRecord>


    @Query("SELECT * FROM run_records WHERE workflowId = :wfId ORDER BY runAt DESC")
    fun getRunsForWorkflow(wfId: String): Flow<List<RunRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RunRecord)

    @Delete
    suspend fun delete(record: RunRecord)

    @Query("DELETE FROM run_records WHERE isSample = 1")
    suspend fun deleteSamples()

    @Query("SELECT COUNT(*) FROM run_records WHERE success = 1")
    suspend fun successCount(): Int

    @Query("SELECT COUNT(*) FROM run_records WHERE success = 0")
    suspend fun failureCount(): Int

    @Query("SELECT AVG(latencyMs) FROM run_records WHERE success = 1")
    suspend fun avgLatencyMs(): Double?

    @Query("SELECT COUNT(*) FROM run_records WHERE isLocal = 1")
    suspend fun localCount(): Int

    @Query("SELECT COUNT(*) FROM run_records WHERE isLocal = 0")
    suspend fun cloudCount(): Int
}
