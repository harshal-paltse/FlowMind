package com.example.flowmind.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM push messages and token-refresh events.
 *
 * On token refresh the new token is persisted via [FcmTokenRepository].
 * Extend [onMessageReceived] as needed to surface in-app notifications.
 */
class FlowMindMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // Token is fetched on demand in FcmTokenRepository; nothing extra needed here.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received: ${message.notification?.title} / ${message.notification?.body}")
        // TODO: surface as a local notification if the app is in the foreground.
    }
}
