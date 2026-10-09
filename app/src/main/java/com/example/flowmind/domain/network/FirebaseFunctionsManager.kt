package com.example.flowmind.domain.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy façade kept for source-compatibility.
 *
 * All real network work is delegated to [CloudflareApiClient]. New call-sites
 * should prefer [CloudflareApiClient] directly; this class exists so that older
 * code compiled against the original API continues to work without modification.
 *
 * ## Responsibility
 * - FCM push notifications via [sendFcmNotification]
 * - Email summaries (optionally with an attached PDF) via [sendEmailSummary]
 * - Workflow export notifications via [sendWorkflowExport]
 * - Lightweight connectivity probe via [ping]
 */
@Singleton
class FirebaseFunctionsManager @Inject constructor(
    private val api: CloudflareApiClient
) {

    // ---------------------------------------------------------------------------
    // Notification helpers
    // ---------------------------------------------------------------------------

    /**
     * Sends a push notification to the device identified by [fcmToken].
     *
     * @param fcmToken The target device's FCM registration token.
     * @param title    Notification title shown in the system tray.
     * @param body     Notification body text.
     * @return `Result.success("ok")` on success, `Result.failure(...)` on error.
     */
    suspend fun sendFcmNotification(
        fcmToken: String,
        title: String,
        body: String
    ): Result<String> {
        return api.notify(title = title, body = body, fcmToken = fcmToken)
            .map { ok -> if (ok) "ok" else "failed" }
    }

    /**
     * Sends an email summary for a completed workflow run.
     *
     * @param fcmToken     FCM token used to route the request to the correct user.
     * @param workflowName Display name of the workflow (used as email subject).
     * @param summaryText  Human-readable summary of the workflow execution.
     * @param pdfBase64    Optional Base64-encoded PDF attachment.
     * @return `Result.success("ok")` on success, `Result.failure(...)` on error.
     */
    suspend fun sendEmailSummary(
        fcmToken: String,
        workflowName: String,
        summaryText: String,
        pdfBase64: String? = null
    ): Result<String> {
        return api.notify(
            title = workflowName,
            body = summaryText,
            fcmToken = fcmToken,
            sendEmail = true,
            pdfBase64 = pdfBase64
        ).map { ok -> if (ok) "ok" else "failed" }
    }

    // ---------------------------------------------------------------------------
    // Export helper
    // ---------------------------------------------------------------------------

    /**
     * Notifies the user that a workflow has been exported and is ready to share.
     *
     * Sends both a push notification and an email containing the exported
     * workflow payload so the user can import it on another device.
     *
     * @param fcmToken       FCM token of the recipient device.
     * @param workflowName   Name of the exported workflow.
     * @param exportJson     Serialised workflow JSON to include in the email body.
     * @return `Result.success("ok")` on success, `Result.failure(...)` on error.
     */
    suspend fun sendWorkflowExport(
        fcmToken: String,
        workflowName: String,
        exportJson: String
    ): Result<String> {
        val body = "Your workflow \"$workflowName\" export is ready.\n\n$exportJson"
        return api.notify(
            title = "Workflow Export: $workflowName",
            body = body,
            fcmToken = fcmToken,
            sendEmail = true
        ).map { ok -> if (ok) "ok" else "failed" }
    }

    // ---------------------------------------------------------------------------
    // Diagnostics
    // ---------------------------------------------------------------------------

    /**
     * Performs a lightweight connectivity probe by sending a no-op notification.
     *
     * Useful for the Diagnostics screen to verify that the backend is reachable
     * without triggering any visible side-effects on the device.
     *
     * @param fcmToken FCM token to use as the probe target.
     * @return `true` if the backend responded successfully, `false` otherwise.
     */
    suspend fun ping(fcmToken: String): Boolean {
        return api.notify(
            title = "ping",
            body = "",
            fcmToken = fcmToken
        ).isSuccess
    }
}
