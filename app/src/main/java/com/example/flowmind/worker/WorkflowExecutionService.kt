package com.example.flowmind.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.flowmind.ml.WorkflowOrchestrator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class WorkflowExecutionService : Service() {

    @Inject lateinit var orchestrator: WorkflowOrchestrator
    
    /**
     * Supervisor job so that individual coroutine failures don't cancel the
     * entire scope; replaced atomically each time the service (re-)starts.
     */
    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val CHANNEL_ID = "flowmind_execution_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_WORKFLOW"
        const val ACTION_STOP = "ACTION_STOP_WORKFLOW"
        const val EXTRA_WORKFLOW_ID = "EXTRA_WORKFLOW_ID"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        if (action == ACTION_STOP) {
            serviceJob.cancel()
            updateNotification("Workflow Cancelled")
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START) {
            val workflowId = intent.getStringExtra(EXTRA_WORKFLOW_ID) ?: return START_NOT_STICKY
            
            // Reinitialize atomically if the previous job was cancelled
            if (serviceJob.isCancelled) {
                serviceJob = SupervisorJob()
                serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
            }
            
            val notification = createNotification("Executing workflow...")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            serviceScope.launch {
                executeWorkflow(workflowId)
            }
        }

        return START_STICKY
    }

    private suspend fun executeWorkflow(workflowId: String) {
        try {
            updateNotification("Loading AI Models...")
            delay(500)

            updateNotification("Running workflow steps...")
            orchestrator.executeStepCloud(
                stepId     = "svc_${workflowId}",
                capability = "text",
                text       = "Execute background workflow: $workflowId"
            )

            updateNotification("Workflow Complete!")
            delay(1500)

            // Remove notification entirely on clean completion
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

        } catch (e: CancellationException) {
            // Propagate so WorkManager / the scope can clean up correctly
            throw e
        } catch (e: Exception) {
            updateNotification("Error: ${e.message?.take(60) ?: "Unknown error"}")
            // Also remove notification on error — avoids an orphaned status-bar entry
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotification(contentText: String): Notification {
        val stopIntent = Intent(this, WorkflowExecutionService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this, 0, stopIntent, android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("FlowMind Automation")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_delete, "Stop Workflow", stopPendingIntent)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workflow Execution",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
