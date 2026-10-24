package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ImageEngine
import com.example.ui.DocScanViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealPrimary

@Composable
fun CameraScannerScreen(viewModel: DocScanViewModel) {
    val session by viewModel.scannerSession.collectAsState()
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateBack()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri -> viewModel.onGalleryImagePicked(uri) }
            viewModel.proceedToEditor()
        }
    }

    fun triggerVibrate() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
    ) {
        // Document Scanner Viewfinder Canvas (Border Guides & Corner Markers)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padX = size.width * 0.08f
            val topY = size.height * 0.16f
            val docWidth = size.width * 0.84f
            val docHeight = size.height * 0.60f

            // Document Framing Rectangle
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(padX, topY),
                size = Size(docWidth, docHeight),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 14f), 0f)
                )
            )

            // High-Contrast Corner L-markers
            val cornerLen = 32.dp.toPx()
            val strokeW = 4.dp.toPx()
            val markerColor = CyanAccent

            // Top-Left
            drawLine(markerColor, Offset(padX, topY), Offset(padX + cornerLen, topY), strokeW)
            drawLine(markerColor, Offset(padX, topY), Offset(padX, topY + cornerLen), strokeW)

            // Top-Right
            val rightX = padX + docWidth
            drawLine(markerColor, Offset(rightX, topY), Offset(rightX - cornerLen, topY), strokeW)
            drawLine(markerColor, Offset(rightX, topY), Offset(rightX, topY + cornerLen), strokeW)

            // Bottom-Left
            val botY = topY + docHeight
            drawLine(markerColor, Offset(padX, botY), Offset(padX + cornerLen, botY), strokeW)
            drawLine(markerColor, Offset(padX, botY), Offset(padX, botY - cornerLen), strokeW)

            // Bottom-Right
            drawLine(markerColor, Offset(rightX, botY), Offset(rightX - cornerLen, botY), strokeW)
            drawLine(markerColor, Offset(rightX, botY), Offset(rightX, botY - cornerLen), strokeW)

            // Optional 3x3 Grid
            if (session.isGridOn) {
                val gridColor = Color.White.copy(alpha = 0.15f)
                val oneThirdW = docWidth / 3f
                val oneThirdH = docHeight / 3f
                drawLine(gridColor, Offset(padX + oneThirdW, topY), Offset(padX + oneThirdW, botY), 1f)
                drawLine(gridColor, Offset(padX + (oneThirdW * 2), topY), Offset(padX + (oneThirdW * 2), botY), 1f)
                drawLine(gridColor, Offset(padX, topY + oneThirdH), Offset(rightX, topY + oneThirdH), 1f)
                drawLine(gridColor, Offset(padX, topY + (oneThirdH * 2)), Offset(rightX, topY + (oneThirdH * 2)), 1f)
            }
        }

        // Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("scanner_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close scanner", tint = Color.White)
            }

            // Mode Selector Pill (Batch vs Single)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { viewModel.toggleBatchMode() }
                    .testTag("batch_mode_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (session.isBatchMode) "Batch Mode (Multi-page)" else "Single Page Mode",
                        color = if (session.isBatchMode) CyanAccent else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.toggleGrid() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (session.isGridOn) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Toggle Grid",
                        tint = if (session.isGridOn) CyanAccent else Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleFlash() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (session.isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (session.isFlashOn) Color.Yellow else Color.White
                    )
                }
            }
        }

        // Live Document Alignment Tip
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 260.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Align document within the frame",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Capture Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Import Button
                IconButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Slate800)
                        .testTag("gallery_import_shutter")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery Import",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Shutter Capture Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(TealPrimary)
                        .clickable(enabled = !session.isProcessing) {
                            triggerVibrate()
                            // Generate high-resolution scanned page bitmap
                            val pageBitmap = ImageEngine.createSampleScannedPageBitmap(
                                title = "Scanned Page ${session.stagedPages.size + 1}",
                                bodyLines = listOf(
                                    "DOCUMENT REF: DSC-${System.currentTimeMillis().toString().takeLast(6)}",
                                    "PAGE NUMBER: ${session.stagedPages.size + 1}",
                                    "OPTICAL DENSITY: 300 DPI AUTO-ENHANCED",
                                    "SECURITY FINGERPRINT: 256-BIT ENCRYPTION READY",
                                    "STATUS: AUTO-CONVERTED WITH CAMSCANNER FILTERS"
                                ),
                                docType = "DIGITAL SCAN"
                            )
                            viewModel.onBitmapCaptured(pageBitmap)
                        }
                        .testTag("shutter_capture_button")
                ) {
                    if (session.isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Take Photo",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Staged Pages Review Button
                if (session.stagedPages.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.proceedToEditor() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("proceed_editor_button")
                    ) {
                        Text(
                            text = "${session.stagedPages.size} Pgs",
                            color = Slate900,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Slate900,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(54.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle status
            Text(
                text = if (session.stagedPages.isEmpty())
                    "Tap shutter to capture page • Instant CamScanner processing"
                else
                    "${session.stagedPages.size} pages staged. Tap shutter for next page or tap review.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}
