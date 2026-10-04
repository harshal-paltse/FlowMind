package com.example.flowmind.domain.network

import android.util.Log
import com.example.flowmind.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CloudflareApiClient"
private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

// ---------- request / response DTOs ----------

@Serializable
data class InferRequest(
    val capability: String,
    val text: String? = null,
    val image: String? = null,   // base-64
    val audio: String? = null    // base-64
)

@Serializable
data class InferResponse(val text: String)

@Serializable
data class NotifyRequest(
    val title: String,
    val body: String,
    val fcmToken: String,
    val email: Boolean = false,
    val pdfBase64: String? = null
)

@Serializable
data class NotifyResponse(val ok: Boolean)

// ---------- client ----------

@Singleton
class CloudflareApiClient @Inject constructor(
    private val auth: FirebaseAuth
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { msg -> Log.d(TAG, msg) }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // -----------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------

    /** Returns a fresh Firebase ID token for the current user, or throws if not signed-in. */
    private suspend fun idToken(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("User not authenticated")
        return user.getIdToken(false).await().token
            ?: throw IllegalStateException("Failed to retrieve ID token")
    }

    private suspend fun post(path: String, bodyJson: String): String {
        val token = idToken()
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE}$path")
            .addHeader("Authorization", "Bearer $token")
            .post(bodyJson.toRequestBody(JSON_MEDIA))
            .build()

        val response = http.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw RuntimeException("HTTP ${response.code}: $responseBody")
        }
        return responseBody
    }

    // -----------------------------------------------------------
    //  Public API
    // -----------------------------------------------------------

    /**
     * POST /v1/infer
     * @param capability  e.g. "text", "ocr", "vision", "audio_transcribe"
     * @param text        Optional text prompt / context
     * @param imageBase64 Optional base-64 encoded image
     * @param audioBase64 Optional base-64 encoded audio
     */
    suspend fun infer(
        capability: String,
        text: String? = null,
        imageBase64: String? = null,
        audioBase64: String? = null
    ): Result<String> = runCatching {
        val body = json.encodeToString(
            InferRequest(
                capability = capability,
                text = text,
                image = imageBase64,
                audio = audioBase64
            )
        )
        val raw = post("/v1/infer", body)
        json.decodeFromString<InferResponse>(raw).text
    }

    /**
     * POST /v1/notify
     * Sends a push notification via FCM (and optionally an email with a PDF attachment).
     */
    suspend fun notify(
        title: String,
        body: String,
        fcmToken: String,
        sendEmail: Boolean = false,
        pdfBase64: String? = null
    ): Result<Boolean> = runCatching {
        val bodyJson = json.encodeToString(
            NotifyRequest(
                title = title,
                body = body,
                fcmToken = fcmToken,
                email = sendEmail,
                pdfBase64 = pdfBase64
            )
        )
        val raw = post("/v1/notify", bodyJson)
        json.decodeFromString<NotifyResponse>(raw).ok
    }
}
