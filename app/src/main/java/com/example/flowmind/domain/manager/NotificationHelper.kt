package com.example.flowmind.domain.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Centralised helper for building and posting FlowMind notifications.
 *
 * All channels are created lazily on first use so this class is safe to
 * instantiate at any point in the application lifecycle.
 *
 * @param context  Application context.
 */
class NotificationHelper(private val context: Context) {

    // -------------------------------------------------------------------------
    // Channel definitions
    // -------------------------------------------------------------------------

    companion object {
        const val CHANNEL_RUNS      = "flowmind_runs"
        const val CHANNEL_SCHEDULER = "flowmind_scheduler"
        const val CHANNEL_ALERTS    = "flowmind_alerts"

        private const val NOTIF_ID_RUN_COMPLETE = 1001
        private const val NOTIF_ID_SCHEDULE     = 1002
        private const val NOTIF_ID_ALERT        = 1003
    }

    // -------------------------------------------------------------------------
    // Channel creation
    // -------------------------------------------------------------------------

    /**
     * Registers all notification channels with the OS.
     * Safe to call multiple times — the OS ignores duplicate registrations.
     */
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RUNS,
                "Workflow Runs",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Progress and completion of workflow runs" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SCHEDULER,
                "Scheduler",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Scheduled workflow trigger reminders" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Critical errors and warnings" }
        )
    }

    // -------------------------------------------------------------------------
    // Notification builders
    // -------------------------------------------------------------------------

    /**
     * Posts a notification when a workflow run completes.
     *
     * @param workflowName  Human-readable name of the workflow.
     * @param success       Whether the run succeeded.
     * @param latencyMs     Run duration in milliseconds.
     */
    fun notifyRunComplete(workflowName: String, success: Boolean, latencyMs: Long) {
        val title = if (success) "✅ $workflowName complete" else "❌ $workflowName failed"
        val body  = if (success) "Finished in ${latencyMs}ms" else "Check the Reports screen for details"

        val builder = NotificationCompat.Builder(context, CHANNEL_RUNS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        postSafely(NOTIF_ID_RUN_COMPLETE, builder)
    }

    /**
     * Posts a reminder notification for an upcoming scheduled workflow.
     *
     * @param workflowName  Name of the scheduled workflow.
     * @param triggerTime   Human-readable trigger time string (e.g. "14:30").
     */
    fun notifyScheduledTrigger(workflowName: String, triggerTime: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_SCHEDULER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Scheduled: $workflowName")
            .setContentText("Will run at $triggerTime")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        postSafely(NOTIF_ID_SCHEDULE, builder)
    }

    /**
     * Posts a high-priority alert (e.g. model download failure, DB error).
     *
     * @param title    Short alert title.
     * @param message  Detailed alert message.
     */
    fun notifyAlert(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        postSafely(NOTIF_ID_ALERT, builder)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Cancels all active FlowMind notifications.
     */
    fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun postSafely(id: Int, builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission not granted on Android 13+; silently ignore.
        }
    }
}
