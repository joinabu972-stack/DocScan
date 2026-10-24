package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.ScannedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfExportConfig(
    val pageSizeA4: Boolean = true,
    val includePageNumbers: Boolean = true,
    val includeWatermark: Boolean = false,
    val watermarkText: String = "DOCSCAN CONFIDENTIAL",
    val title: String = ""
)

object ExportEngine {

    suspend fun generatePdf(
        context: Context,
        document: ScannedDocument,
        config: PdfExportConfig = PdfExportConfig()
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        // Standard A4: 595 x 842 pt
        val pageWidth = if (config.pageSizeA4) 595 else 612
        val pageHeight = if (config.pageSizeA4) 842 else 792

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 15, 118, 110)
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val totalPages = document.pages.size
        for ((index, page) in document.pages.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = pdfPage.canvas

            // Fill white background
            canvas.drawColor(Color.WHITE)

            // Draw header
            val headerText = document.title
            canvas.drawText(headerText, 36f, 30f, titlePaint)

            // Load and draw page bitmap
            val bitmap = ImageEngine.loadBitmapFromFile(page.imagePath)
            if (bitmap != null) {
                val processedBitmap = ImageEngine.applyFilterAndRotation(
                    bitmap,
                    page.filterMode,
                    page.rotationDegrees
                )

                val margin = 36f
                val topMargin = 42f
                val bottomMargin = 46f
                val targetWidth = pageWidth - (margin * 2)
                val targetHeight = pageHeight - topMargin - bottomMargin

                val srcRect = Rect(0, 0, processedBitmap.width, processedBitmap.height)

                // Scale while maintaining aspect ratio
                val imgAspect = processedBitmap.width.toFloat() / processedBitmap.height.toFloat()
                val targetAspect = targetWidth / targetHeight

                val drawRect: Rect
                if (imgAspect > targetAspect) {
                    val drawHeight = (targetWidth / imgAspect).toInt()
                    val offsetY = topMargin + ((targetHeight - drawHeight) / 2f)
                    drawRect = Rect(
                        margin.toInt(),
                        offsetY.toInt(),
                        (margin + targetWidth).toInt(),
                        (offsetY + drawHeight).toInt()
                    )
                } else {
                    val drawWidth = (targetHeight * imgAspect).toInt()
                    val offsetX = margin + ((targetWidth - drawWidth) / 2f)
                    drawRect = Rect(
                        offsetX.toInt(),
                        topMargin.toInt(),
                        (offsetX + drawWidth).toInt(),
                        (topMargin + targetHeight).toInt()
                    )
                }

                canvas.drawBitmap(processedBitmap, srcRect, drawRect, null)
            }

            // Watermark if requested
            if (config.includeWatermark) {
                canvas.save()
                canvas.rotate(-35f, pageWidth / 2f, pageHeight / 2f)
                canvas.drawText(config.watermarkText, pageWidth / 2f, pageHeight / 2f, watermarkPaint)
                canvas.restore()
            }

            // Draw footer page numbers
            if (config.includePageNumbers) {
                val footerText = "DocScan • Page ${index + 1} of $totalPages"
                canvas.drawText(footerText, pageWidth / 2f, pageHeight - 20f, footerPaint)
            }

            pdfDocument.finishPage(pdfPage)
        }

        val sanitizedTitle = document.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(exportDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        outFile
    }

    suspend fun generateJpegs(
        context: Context,
        document: ScannedDocument
    ): List<File> = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val generatedFiles = mutableListOf<File>()
        val sanitizedTitle = document.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")

        for ((index, page) in document.pages.withIndex()) {
            val bitmap = ImageEngine.loadBitmapFromFile(page.imagePath)
            if (bitmap != null) {
                val processedBitmap = ImageEngine.applyFilterAndRotation(
                    bitmap,
                    page.filterMode,
                    page.rotationDegrees
                )

                val outFile = File(exportDir, "${sanitizedTitle}_page_${index + 1}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { fos ->
                    processedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                }
                generatedFiles.add(outFile)
            }
        }
        generatedFiles
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "Export Document") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
