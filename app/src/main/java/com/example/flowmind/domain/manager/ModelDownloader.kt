package com.example.flowmind.domain.manager

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

// ─────────────────────────────────────────────────────────────────────────────
// DownloadState
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sealed hierarchy representing the lifecycle states of a model download.
 *
 * Collectors should handle every subtype:
 * - [Idle]      – no download active.
 * - [Progress]  – bytes are being received; [progress] is 0..100.
 * - [Verifying] – download complete; SHA-256 integrity check in progress.
 * - [Success]   – file received and verified successfully.
 * - [Error]     – a terminal failure with a human-readable [message].
 */
sealed class DownloadState {
    /** No active download. */
    object Idle : DownloadState()

    /**
     * Download is in progress.
     *
     * @param progress Completion percentage in the range [0, 100].
     */
    data class Progress(val progress: Int) : DownloadState()

    /** Download finished; integrity verification is running. */
    object Verifying : DownloadState()

    /** Download and verification both succeeded. */
    object Success : DownloadState()

    /**
     * A terminal error occurred.
     *
     * @param message Human-readable description of the failure.
     */
    data class Error(val message: String) : DownloadState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ModelDownloader
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Downloads on-device AI model files and verifies their integrity via SHA-256.
 *
 * Downloads are simulated with incremental progress and a configurable number
 * of automatic retries on transient failures. In a production build the
 * simulation loop should be replaced with a real HTTP streaming call.
 *
 * ### Usage
 * ```kotlin
 * modelDownloader
 *     .downloadModel(url, expectedSha256, destination)
 *     .collect { state ->
 *         when (state) {
 *             is DownloadState.Progress  -> updateProgressBar(state.progress)
 *             is DownloadState.Verifying -> showSpinner()
 *             is DownloadState.Success   -> onModelReady()
 *             is DownloadState.Error     -> showError(state.message)
 *             else -> Unit
 *         }
 *     }
 * ```
 *
 * @param maxRetries Maximum number of retry attempts before emitting [DownloadState.Error].
 */
class ModelDownloader(private val maxRetries: Int = 3) {

    companion object {
        /** Minimum free-storage bytes required before a download is attempted (500 MB). */
        private const val MIN_FREE_BYTES = 500L * 1024 * 1024

        /** Simulated network chunk size increment per tick (in percent). */
        private const val CHUNK_INCREMENT = 15

        /** Delay between simulated network ticks in milliseconds. */
        private const val TICK_DELAY_MS = 200L
    }

    /**
     * Starts a model download and emits [DownloadState] updates as a cold [Flow].
     *
     * The flow handles:
     * 1. Pre-flight storage check.
     * 2. Resume detection (if [destination] already exists, progress starts at 10%).
     * 3. Simulated chunked download with progress updates.
     * 4. SHA-256 integrity verification of the downloaded file.
     * 5. Automatic retry up to [maxRetries] times on [Exception].
     *
     * @param url           Remote URL of the model file (used in production; unused in sim).
     * @param expectedSha256 Lower-case hex SHA-256 digest used for integrity verification.
     * @param destination    Local [File] path where the model will be written.
     * @return A cold [Flow] of [DownloadState] events.
     */
    fun downloadModel(
        url: String,
        expectedSha256: String,
        destination: File
    ): Flow<DownloadState> = flow {
        // ── 1. Storage pre-flight check ────────────────────────────────────
        val freeSpace = destination.parentFile?.usableSpace ?: 0L
        if (freeSpace < MIN_FREE_BYTES) {
            emit(DownloadState.Error("Insufficient storage space (need ≥ 500 MB free)."))
            return@flow
        }

        var attempt = 0
        var succeeded = false

        while (attempt < maxRetries && !succeeded) {
            attempt++
            try {
                // ── 2. Resume detection ────────────────────────────────────
                var currentProgress = if (destination.exists()) 10 else 0

                // ── 3. Simulated chunked download ──────────────────────────
                while (currentProgress < 100) {
                    emit(DownloadState.Progress(currentProgress))
                    delay(TICK_DELAY_MS)
                    currentProgress = (currentProgress + CHUNK_INCREMENT).coerceAtMost(100)
                }
                emit(DownloadState.Progress(100))

                // ── 4. SHA-256 integrity verification ──────────────────────
                emit(DownloadState.Verifying)
                delay(500)

                val isValid = verifySha256(destination, expectedSha256)
                if (isValid) {
                    emit(DownloadState.Success)
                    succeeded = true
                } else {
                    destination.delete() // Remove corrupted file
                    if (attempt >= maxRetries) {
                        emit(DownloadState.Error("SHA-256 mismatch after $maxRetries attempt(s). File deleted."))
                    } else {
                        // Brief back-off before retry
                        delay(1000L * attempt)
                    }
                }

            } catch (e: Exception) {
                if (attempt >= maxRetries) {
                    emit(DownloadState.Error("Download failed after $maxRetries attempt(s): ${e.message ?: "Unknown error"}"))
                } else {
                    delay(1000L * attempt) // Exponential back-off (simulated)
                }
            }
        }
    }

    /**
     * Computes the SHA-256 digest of [file] and compares it with [expected].
     *
     * Returns `true` when the file does not exist (digest assumed valid when
     * running in simulation mode with no real file written).
     *
     * @param file     File to hash.
     * @param expected Lower-case hex SHA-256 string to compare against.
     * @return `true` if the digests match or the file is absent (sim mode).
     */
    private fun verifySha256(file: File, expected: String): Boolean {
        if (!file.exists()) return true // Simulation mode – no real file written

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hex = digest.digest().joinToString("") { "%02x".format(it) }
            hex.equals(expected, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
