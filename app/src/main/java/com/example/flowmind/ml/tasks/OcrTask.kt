package com.example.flowmind.ml.tasks

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrTask @Inject constructor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(imageUri: Uri): String {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            "Error extracting text."
        }
    }
}
