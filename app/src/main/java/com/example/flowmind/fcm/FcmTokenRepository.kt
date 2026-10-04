package com.example.flowmind.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves and caches the FCM registration token for this device.
 *
 * The token is fetched once per app session (lazy, on first call to [getToken])
 * and refreshed automatically by the OS; pass this token to the backend whenever
 * you want to receive a push notification on this device.
 */
@Singleton
class FcmTokenRepository @Inject constructor() {

    private var cachedToken: String? = null

    /**
     * Returns the current FCM token.  Suspends until the token is available.
     * Throws if FCM is unavailable (e.g. device without Play Services).
     */
    suspend fun getToken(): String {
        cachedToken?.let { return it }

        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d("FcmTokenRepository", "FCM token fetched: $token")
            cachedToken = token
            token
        } catch (e: Exception) {
            Log.e("FcmTokenRepository", "Failed to get FCM token", e)
            throw e
        }
    }

    /** Clears the in-memory cache (forces a fresh fetch on next call). */
    fun invalidate() {
        cachedToken = null
    }
}
