package com.example.flowmind.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.flowmind.data.RunRecordRepository
import com.example.flowmind.data.WorkflowRepository
import com.example.flowmind.domain.models.RunRecord
import com.example.flowmind.domain.network.CloudflareApiClient
import com.example.flowmind.fcm.FcmTokenRepository
import com.example.flowmind.ml.WorkflowOrchestrator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ScheduledWorkflowWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val workflowRepo: WorkflowRepository,
    private val runRepo: RunRecordRepository,
    private val orchestrator: WorkflowOrchestrator,
    private val api: CloudflareApiClient,
    private val fcmRepo: FcmTokenRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_WORKFLOW_ID = "workflow_id"
        const val KEY_WORKFLOW_NAME = "workflow_name"
    }

    override suspend fun doWork(): Result {
        val wfId   = inputData.getString(KEY_WORKFLOW_ID)   ?: return Result.failure()
        val wfName = inputData.getString(KEY_WORKFLOW_NAME) ?: "Scheduled Workflow"
        val start  = System.currentTimeMillis()
        return runCatching {
            // Execute via orchestrator (simple single-step for background runs)
            orchestrator.executeStepCloud(
                stepId     = "scheduled_$wfId",
                capability = "text",
                text       = "Scheduled run for: $wfName"
            )
            val latency = System.currentTimeMillis() - start
            runRepo.insert(RunRecord(workflowId = wfId, workflowName = wfName,
                success = true, latencyMs = latency, isLocal = false))
            // Send push notification
            runCatching {
                val token = fcmRepo.getToken()
                api.notify(title = "FlowMind: $wfName", body = "Scheduled run completed in ${latency}ms",
                    fcmToken = token)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                val latency = System.currentTimeMillis() - start
                runRepo.insert(RunRecord(workflowId = wfId, workflowName = wfName,
                    success = false, latencyMs = latency))
                Result.retry()
            }
        )
    }
}
