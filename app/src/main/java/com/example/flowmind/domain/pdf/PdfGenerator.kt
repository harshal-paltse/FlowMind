package com.example.flowmind.domain.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

/**
 * Generates PDF reports for FlowMind workflow run results.
 *
 * Supports both Android Q+ (scoped storage via MediaStore) and legacy storage paths.
 * Each report contains a styled title, body text and an optional chart bitmap.
 * Long reports are automatically split across multiple A4 pages.
 *
 * @param context Android context used for MediaStore access and file resolution.
 */
class PdfGenerator(private val context: Context) {

    companion object {
        /** A4 page width in points (72 dpi). */
        private const val PAGE_WIDTH = 595

        /** A4 page height in points (72 dpi). */
        private const val PAGE_HEIGHT = 842

        /** Left margin for all content on the page. */
        private const val MARGIN_LEFT = 50f

        /** Top margin where content begins. */
        private const val MARGIN_TOP = 60f

        /** Line height used when rendering body text. */
        private const val LINE_HEIGHT = 24f

        /** Bottom boundary — content below this triggers a new page. */
        private const val MARGIN_BOTTOM = PAGE_HEIGHT - 60f

        /** File MIME type for PDF documents. */
        private const val MIME_PDF = "application/pdf"
    }

    /**
     * Generates a PDF workflow report and saves it to the device's Downloads folder.
     *
     * @param workflowName Human-readable name of the workflow being reported.
     * @param resultsText  Newline-delimited result text to render as body content.
     * @param chartBitmap  Optional chart bitmap inserted after the text. Scaled to fit.
     * @return `true` if the PDF was written successfully, `false` on any error.
     */
    fun generateWorkflowReport(
        workflowName: String,
        resultsText: String,
        chartBitmap: Bitmap?
    ): Boolean {
        val pdfDocument = PdfDocument()
        var pageNumber = 1

        fun startNewPage(): Pair<PdfDocument.Page, Canvas> {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
            val page = pdfDocument.startPage(pageInfo)
            return page to page.canvas
        }

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 11f
            isAntiAlias = true
        }

        var (currentPage, canvas) = startNewPage()

        // ── Title ──────────────────────────────────────────────────────────
        canvas.drawText("FlowMind Report: $workflowName", MARGIN_LEFT, MARGIN_TOP, titlePaint)

        // Decorative underline beneath the title
        val linePaint = Paint().apply { color = Color.parseColor("#3949AB"); strokeWidth = 2f }
        canvas.drawLine(MARGIN_LEFT, MARGIN_TOP + 8f, PAGE_WIDTH - MARGIN_LEFT, MARGIN_TOP + 8f, linePaint)

        // ── Body Text (with pagination) ────────────────────────────────────
        var yPos = MARGIN_TOP + 40f
        val lines = resultsText.split("\n")

        for (line in lines) {
            if (yPos + LINE_HEIGHT > MARGIN_BOTTOM) {
                drawFooter(canvas, footerPaint, pageNumber - 1)
                pdfDocument.finishPage(currentPage)
                val next = startNewPage()
                currentPage = next.first
                canvas = next.second
                yPos = MARGIN_TOP
            }
            canvas.drawText(line, MARGIN_LEFT, yPos, bodyPaint)
            yPos += LINE_HEIGHT
        }

        // ── Chart Bitmap ───────────────────────────────────────────────────
        chartBitmap?.let { bmp ->
            yPos += 16f
            val chartH = 250f
            if (yPos + chartH > MARGIN_BOTTOM) {
                drawFooter(canvas, footerPaint, pageNumber - 1)
                pdfDocument.finishPage(currentPage)
                val next = startNewPage()
                currentPage = next.first
                canvas = next.second
                yPos = MARGIN_TOP
            }
            val scaledBitmap = Bitmap.createScaledBitmap(bmp, 460, chartH.toInt(), true)
            canvas.drawBitmap(scaledBitmap, MARGIN_LEFT, yPos, null)
        }

        drawFooter(canvas, footerPaint, pageNumber - 1)
        pdfDocument.finishPage(currentPage)

        // ── Write to storage ───────────────────────────────────────────────
        var outputStream: OutputStream? = null
        return try {
            val fileName = "FlowMind_Report_${System.currentTimeMillis()}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, MIME_PDF)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                }
            } else {
                val file = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )
                outputStream = java.io.FileOutputStream(file)
            }

            outputStream?.use { pdfDocument.writeTo(it) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Draws a footer line at the bottom of the given [canvas] showing the page number
     * and the FlowMind app brand string.
     *
     * @param canvas     Canvas to draw onto.
     * @param paint      Paint configured for footer text.
     * @param pageNum    Current page number displayed in the footer.
     */
    private fun drawFooter(canvas: Canvas, paint: Paint, pageNum: Int) {
        val footerY = PAGE_HEIGHT - 30f
        canvas.drawText("FlowMind • Page $pageNum", MARGIN_LEFT, footerY, paint)
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        canvas.drawLine(MARGIN_LEFT, footerY - 10f, PAGE_WIDTH - MARGIN_LEFT, footerY - 10f, linePaint)
    }

    // ── Run summary overload ──────────────────────────────────────────────────

    /**
     * Generates a structured run-history PDF from a list of [RunRecord]s.
     *
     * Each record is rendered as a table row with columns: #, Workflow, Status,
     * Latency (ms), and Inference (Local/Cloud). A summary footer shows total
     * run count and average latency.
     *
     * Pagination is handled automatically — rows overflow to new A4 pages.
     *
     * @param runs  List of [com.example.flowmind.domain.models.RunRecord]s to tabulate.
     * @return `true` if the PDF was written successfully, `false` on any error.
     */
    fun generateRunSummaryReport(
        runs: List<com.example.flowmind.domain.models.RunRecord>
    ): Boolean {
        val pdfDocument = PdfDocument()
        var pageNumber = 1

        fun startNewPage(): Pair<PdfDocument.Page, Canvas> {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
            val page = pdfDocument.startPage(info)
            return page to page.canvas
        }

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A237E"); textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.WHITE; textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
        }
        val cellPaint = Paint().apply { color = Color.DKGRAY; textSize = 11f; isAntiAlias = true }
        val footerPaint = Paint().apply { color = Color.GRAY; textSize = 11f; isAntiAlias = true }
        val rowBg     = Paint().apply { color = Color.parseColor("#F3F4F6") }
        val headerBg  = Paint().apply { color = Color.parseColor("#3949AB") }

        var (currentPage, canvas) = startNewPage()

        // Title
        canvas.drawText("FlowMind — Run History Report", MARGIN_LEFT, MARGIN_TOP, titlePaint)
        val ul = Paint().apply { color = Color.parseColor("#3949AB"); strokeWidth = 2f }
        canvas.drawLine(MARGIN_LEFT, MARGIN_TOP + 8f, PAGE_WIDTH - MARGIN_LEFT, MARGIN_TOP + 8f, ul)

        // Column widths
        val cols = floatArrayOf(30f, 170f, 65f, 90f, 90f)
        val colX = FloatArray(cols.size).also { arr ->
            arr[0] = MARGIN_LEFT
            for (i in 1 until cols.size) arr[i] = arr[i - 1] + cols[i - 1]
        }
        val rowH = 20f
        var yPos = MARGIN_TOP + 30f

        // Header row
        canvas.drawRect(MARGIN_LEFT, yPos, PAGE_WIDTH - MARGIN_LEFT, yPos + rowH, headerBg)
        val headers = listOf("#", "Workflow", "Status", "Latency", "Inference")
        headers.forEachIndexed { i, h ->
            canvas.drawText(h, colX[i] + 4f, yPos + 14f, headerPaint)
        }
        yPos += rowH

        // Data rows
        runs.forEachIndexed { idx, run ->
            if (yPos + rowH > MARGIN_BOTTOM) {
                drawFooter(canvas, footerPaint, pageNumber - 1)
                pdfDocument.finishPage(currentPage)
                val next = startNewPage(); currentPage = next.first; canvas = next.second
                yPos = MARGIN_TOP
            }
            if (idx % 2 == 0) {
                canvas.drawRect(MARGIN_LEFT, yPos, PAGE_WIDTH - MARGIN_LEFT, yPos + rowH, rowBg)
            }
            val cells = listOf(
                "${idx + 1}",
                run.workflowName.take(22),
                if (run.success) "✓ OK" else "✗ Fail",
                "${run.latencyMs} ms",
                if (run.isLocal) "Local" else "Cloud"
            )
            cells.forEachIndexed { i, text ->
                cellPaint.color = when {
                    i == 2 && run.success  -> Color.parseColor("#2E7D32")
                    i == 2 && !run.success -> Color.parseColor("#C62828")
                    else -> Color.DKGRAY
                }
                canvas.drawText(text, colX[i] + 4f, yPos + 14f, cellPaint)
            }
            yPos += rowH
        }

        // Summary footer text
        yPos += 12f
        val avgMs = if (runs.isEmpty()) 0L else runs.sumOf { it.latencyMs } / runs.size
        val successCount = runs.count { it.success }
        val summaryPaint = Paint().apply {
            color = Color.parseColor("#1A237E"); textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
        }
        canvas.drawText(
            "Total: ${runs.size} runs • Success: $successCount • Avg latency: ${avgMs} ms",
            MARGIN_LEFT, yPos, summaryPaint
        )

        drawFooter(canvas, footerPaint, pageNumber - 1)
        pdfDocument.finishPage(currentPage)

        var outputStream: java.io.OutputStream? = null
        return try {
            val fileName = "FlowMind_RunHistory_${System.currentTimeMillis()}.pdf"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, MIME_PDF)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val file = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName
                )
                outputStream = java.io.FileOutputStream(file)
            }
            outputStream?.use { pdfDocument.writeTo(it) }
            true
        } catch (e: Exception) {
            e.printStackTrace(); false
        } finally {
            pdfDocument.close()
        }
    }
}
