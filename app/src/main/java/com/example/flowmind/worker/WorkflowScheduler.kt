package com.example.flowmind.worker

import android.content.Context
import androidx.work.*
import com.example.flowmind.domain.models.Workflow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowScheduler @Inject constructor(
    private val context: Context
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Schedule a workflow to run once after [delayMinutes] minutes. */
    fun scheduleOnce(workflow: Workflow, delayMinutes: Long) {
        val request = OneTimeWorkRequestBuilder<ScheduledWorkflowWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(
                ScheduledWorkflowWorker.KEY_WORKFLOW_ID   to workflow.id,
                ScheduledWorkflowWorker.KEY_WORKFLOW_NAME to workflow.name
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(workflow.id)
            .build()
        workManager.enqueueUniqueWork(workflow.id, ExistingWorkPolicy.REPLACE, request)
    }

    /** Schedule a workflow to run every [intervalHours] hours. */
    fun scheduleRepeating(workflow: Workflow, intervalHours: Long) {
        val request = PeriodicWorkRequestBuilder<ScheduledWorkflowWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setInputData(workDataOf(
                ScheduledWorkflowWorker.KEY_WORKFLOW_ID   to workflow.id,
                ScheduledWorkflowWorker.KEY_WORKFLOW_NAME to workflow.name
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(workflow.id)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "repeat_${workflow.id}", ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    /** Cancel all scheduled runs for a workflow. */
    fun cancel(workflowId: String) {
        workManager.cancelAllWorkByTag(workflowId)
    }

    /** Get live work info for a workflow (for showing "Next run" status). */
    fun getWorkInfo(workflowId: String) =
        workManager.getWorkInfosByTagLiveData(workflowId)
}
