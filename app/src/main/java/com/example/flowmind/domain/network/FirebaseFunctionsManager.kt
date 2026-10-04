package com.example.flowmind.domain.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy façade kept for source-compatibility.
 * All real work is now done by [CloudflareApiClient].
 */
@Singleton
class FirebaseFunctionsManager @Inject constructor(
    private val api: CloudflareApiClient
) {
    suspend fun sendFcmNotification(
        fcmToken: String,
        title: String,
        body: String
    ): Result<String> {
        return api.notify(title = title, body = body, fcmToken = fcmToken)
            .map { ok -> if (ok) "ok" else "failed" }
    }

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
}
