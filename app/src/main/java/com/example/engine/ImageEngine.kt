package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import com.example.model.FilterMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageEngine {

    suspend fun processAndSaveImage(
        context: Context,
        sourceBitmap: Bitmap,
        filterMode: FilterMode = FilterMode.MAGIC_COLOR,
        rotationDegrees: Int = 0
    ): Pair<String, Bitmap> = withContext(Dispatchers.IO) {
        val filteredBitmap = applyFilterAndRotation(sourceBitmap, filterMode, rotationDegrees)
        val scansDir = File(context.filesDir, "scans")
        if (!scansDir.exists()) scansDir.mkdirs()

        val filename = "scan_${UUID.randomUUID()}.jpg"
        val destFile = File(scansDir, filename)

        FileOutputStream(destFile).use { out ->
            filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        Pair(destFile.absolutePath, filteredBitmap)
    }

    suspend fun loadBitmapFromFile(path: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun applyFilterAndRotation(
        source: Bitmap,
        filterMode: FilterMode,
        rotationDegrees: Int
    ): Bitmap {
        var baseBitmap = source
        if (rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            baseBitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }

        if (filterMode == FilterMode.ORIGINAL) {
            return baseBitmap
        }

        val output = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (filterMode) {
            FilterMode.MAGIC_COLOR -> {
                // High contrast, vivid text sharpening, bright background
                val cm = ColorMatrix(floatArrayOf(
                    1.35f, 0f, 0f, 0f, -25f,
                    0f, 1.35f, 0f, 0f, -25f,
                    0f, 0f, 1.35f, 0f, -25f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            FilterMode.BLACK_WHITE -> {
                // High contrast threshold binarization for clean paper
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val contrast = 2.2f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                val thresholdMatrix = ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(thresholdMatrix)
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            FilterMode.GRAYSCALE -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            FilterMode.LIGHTEN -> {
                val cm = ColorMatrix(floatArrayOf(
                    1.1f, 0f, 0f, 0f, 40f,
                    0f, 1.1f, 0f, 0f, 40f,
                    0f, 0f, 1.1f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            FilterMode.ORIGINAL -> {}
        }

        canvas.drawBitmap(baseBitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Creates a realistic document page bitmap for simulated scanning or demonstration
     */
    fun createSampleScannedPageBitmap(
        title: String,
        bodyLines: List<String>,
        docType: String = "DOCUMENT"
    ): Bitmap {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean white paper background
        val bgPaint = Paint().apply {
            color = android.graphics.Color.rgb(252, 252, 252)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle document border
        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(220, 226, 235)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(40f, 40f, width - 40f, height - 40f, borderPaint)

        // Header band
        val headerPaint = Paint().apply {
            color = android.graphics.Color.rgb(15, 118, 110)
        }
        canvas.drawRect(80f, 80f, width - 80f, 140f, headerPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            isFakeBoldText = true
        }
        canvas.drawText("DOCSCAN OFFICIAL DIGITAL RECORD  •  $docType", 110f, 122f, headerTextPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            textSize = 52f
            isFakeBoldText = true
        }
        canvas.drawText(title, 80f, 230f, titlePaint)

        // Subtitle line
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(100, 116, 139)
            textSize = 28f
        }
        canvas.drawText("Digitized with OCR & AES-256 E2EE • Confidential Record", 80f, 280f, metaPaint)

        // Divider
        val divPaint = Paint().apply {
            color = android.graphics.Color.rgb(203, 213, 225)
            strokeWidth = 2f
        }
        canvas.drawLine(80f, 310f, width - 80f, 310f, divPaint)

        // Body lines
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(51, 65, 85)
            textSize = 32f
        }
        var yPos = 380f
        for (line in bodyLines) {
            canvas.drawText(line, 80f, yPos, bodyPaint)
            yPos += 52f
        }

        // Stamp / verification footer
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(16, 185, 129)
            textSize = 26f
            isFakeBoldText = true
        }
        canvas.drawText("✓ OCR VERIFIED & INDEXED • CLOUD SYNC READY", 80f, height - 100f, stampPaint)

        return bitmap
    }
}
