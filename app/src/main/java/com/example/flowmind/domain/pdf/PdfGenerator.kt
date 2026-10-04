package com.example.flowmind.domain.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

class PdfGenerator(private val context: Context) {

    fun generateWorkflowReport(
        workflowName: String,
        resultsText: String,
        chartBitmap: Bitmap?
    ): Boolean {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size roughly
        val page = pdfDocument.startPage(pageInfo)
        
        val canvas: Canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }

        // Title
        canvas.drawText("FlowMind Report: $workflowName", 50f, 50f, paint)

        // Body Text
        paint.textSize = 16f
        var yPos = 100f
        val lines = resultsText.split("\n")
        for (line in lines) {
            canvas.drawText(line, 50f, yPos, paint)
            yPos += 25f
        }

        // Chart Bitmap
        chartBitmap?.let {
            yPos += 20f
            // Scale bitmap to fit
            val scaledBitmap = Bitmap.createScaledBitmap(it, 400, 300, true)
            canvas.drawBitmap(scaledBitmap, 50f, yPos, paint)
        }

        pdfDocument.finishPage(page)

        // Save to MediaStore
        var outputStream: OutputStream? = null
        return try {
            val fileName = "FlowMind_Report_${System.currentTimeMillis()}.pdf"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                }
            } else {
                val file = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                outputStream = java.io.FileOutputStream(file)
            }

            outputStream?.use {
                pdfDocument.writeTo(it)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }
}
