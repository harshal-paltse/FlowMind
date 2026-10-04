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
    
    private var serviceJob = Job()
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
            
            // Reinitialize if previously cancelled
            if (serviceJob.isCancelled) {
                serviceJob = Job()
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
            delay(1000)
            
            updateNotification("Running Step: OCR...")
            orchestrator.executeStep(
                stepId = "step_ocr_1",
                taskType = "OCR",
                inputData = "Sample Data",
                isPrivateData = false,
                hasNetwork = true,
                batteryLow = false
            )
            
            updateNotification("Workflow Complete!")
            delay(2000)
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            
        } catch (e: CancellationException) {
            // Handled in onStartCommand ACTION_STOP
            throw e
        } catch (e: Exception) {
            updateNotification("Error executing workflow.")
            stopForeground(STOP_FOREGROUND_DETACH)
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
