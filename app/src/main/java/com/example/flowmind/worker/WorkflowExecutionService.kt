package com.example.flowmind.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val CHANNEL_ID = "flowmind_execution_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_WORKFLOW"
        const val EXTRA_WORKFLOW_ID = "EXTRA_WORKFLOW_ID"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val workflowId = intent?.getStringExtra(EXTRA_WORKFLOW_ID) ?: return START_NOT_STICKY

        if (intent?.action == ACTION_START) {
            val notification = createNotification("Executing workflow...")
            startForeground(NOTIFICATION_ID, notification)

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
            
            updateNotification("Running OCR Step...")
            orchestrator.executeStep("OCR", "Sample Data")
            
            updateNotification("Workflow Complete!")
            delay(2000)
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            
        } catch (e: Exception) {
            updateNotification("Error executing workflow.")
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun createNotification(contentText: String): Notification {
        // Create an Intent to stop the workflow from the notification
        val stopIntent = Intent(this, WorkflowExecutionService::class.java).apply {
            action = "ACTION_STOP_WORKFLOW"
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this, 0, stopIntent, android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("FlowMind Automation")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // ADDED: Single notification controllable action!
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
