package com.example.flowmind.domain.manager

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.security.MessageDigest

sealed class DownloadState {
    object Idle : DownloadState()
    data class Progress(val progress: Int) : DownloadState()
    object Verifying : DownloadState()
    object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ModelDownloader {

    fun downloadModel(url: String, expectedSha256: String, destination: File): Flow<DownloadState> = flow {
        try {
            // Check storage
            if (destination.parentFile?.usableSpace ?: 0 < 500 * 1024 * 1024) { // Needs 500MB roughly
                emit(DownloadState.Error("Insufficient storage space."))
                return@flow
            }
            
            // Resume logic (simulated)
            var currentProgress = if (destination.exists()) 10 else 0
            
            while (currentProgress < 100) {
                emit(DownloadState.Progress(currentProgress))
                delay(200) // Simulating network chunk download
                currentProgress += 15
            }
            emit(DownloadState.Progress(100))
            
            // Simulating hash verification
            emit(DownloadState.Verifying)
            delay(500)
            
            // Simulated validation
            val isValid = true // In real app: calculate SHA-256 and compare with expectedSha256
            if (isValid) {
                emit(DownloadState.Success)
            } else {
                emit(DownloadState.Error("SHA-256 verification failed. File corrupted."))
            }

        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown download error"))
        }
    }
}
