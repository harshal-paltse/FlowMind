package com.example.flowmind.data

import com.example.flowmind.data.db.RunRecordDao
import com.example.flowmind.domain.models.RunRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunRecordRepository @Inject constructor(private val dao: RunRecordDao) {

    fun allRuns(): Flow<List<RunRecord>> = dao.getAllRuns()

    fun runsForWorkflow(wfId: String): Flow<List<RunRecord>> = dao.getRunsForWorkflow(wfId)

    suspend fun insert(record: RunRecord) = dao.insert(record)

    suspend fun delete(record: RunRecord) = dao.delete(record)

    suspend fun deleteSamples() = dao.deleteSamples()

    suspend fun successRate(): Float {
        val s = dao.successCount()
        val f = dao.failureCount()
        val total = s + f
        return if (total == 0) 0f else s.toFloat() / total
    }

    suspend fun avgLatencyMs(): Long = dao.avgLatencyMs()?.toLong() ?: 0L

    suspend fun localVsCloud(): Pair<Int, Int> = dao.localCount() to dao.cloudCount()

    /** Insert sample rows so the user can see charts without running a real workflow. */
    suspend fun loadSamples(workflowId: String = "sample") {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val samples = listOf(
            RunRecord(workflowId = workflowId, workflowName = "Bill OCR",      templateId = "bill",    success = true,  latencyMs = 1200, ramUsedMb = 180, isLocal = true,  isSample = true, runAt = now - 6 * dayMs),
            RunRecord(workflowId = workflowId, workflowName = "Lecture Audio",  templateId = "lecture", success = true,  latencyMs = 3400, ramUsedMb = 220, isLocal = false, isSample = true, runAt = now - 5 * dayMs),
            RunRecord(workflowId = workflowId, workflowName = "Plant Disease",  templateId = "plant",   success = true,  latencyMs = 900,  ramUsedMb = 150, isLocal = true,  isSample = true, runAt = now - 4 * dayMs),
            RunRecord(workflowId = workflowId, workflowName = "Meeting Notes",  templateId = "meeting", success = false, latencyMs = 5100, ramUsedMb = 300, isLocal = false, isSample = true, runAt = now - 3 * dayMs),
            RunRecord(workflowId = workflowId, workflowName = "Bill OCR",       templateId = "bill",    success = true,  latencyMs = 1100, ramUsedMb = 170, isLocal = true,  isSample = true, runAt = now - 2 * dayMs),
            RunRecord(workflowId = workflowId, workflowName = "Plant Disease",  templateId = "plant",   success = true,  latencyMs = 950,  ramUsedMb = 160, isLocal = true,  isSample = true, runAt = now - 1 * dayMs),
            RunRecord(workflowId = workflowId, workflowName = "Medicine Label", templateId = "medicine",success = true,  latencyMs = 800,  ramUsedMb = 130, isLocal = true,  isSample = true, runAt = now)
        )
        samples.forEach { dao.insert(it) }
    }
}
