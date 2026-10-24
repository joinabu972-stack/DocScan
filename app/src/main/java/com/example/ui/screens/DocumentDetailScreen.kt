package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.engine.PdfExportConfig
import com.example.engine.SecurityEngine
import com.example.model.DocumentFolder
import com.example.model.FilterMode
import com.example.ui.DocScanViewModel
import com.example.ui.components.CloudSyncBadge
import com.example.ui.components.E2eeSecurityBadge
import com.example.ui.components.FolderTagPill
import com.example.ui.theme.AmberSecurity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    docId: String,
    viewModel: DocScanViewModel
) {
    val document by viewModel.activeDocument.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateBack()
    }

    if (document == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Document not found", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    val doc = document!!
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newTitleText by remember { mutableStateOf(doc.title) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var showPdfExportSheet by remember { mutableStateOf(false) }
    var showPinUnlockDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val safeIndex = selectedPageIndex.coerceIn(0, (doc.pages.size - 1).coerceAtLeast(0))
    val currentPage = doc.pages.getOrNull(safeIndex)

    // Check if locked
    val isLocked = doc.isEncrypted && !doc.isUnlocked

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = doc.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                modifier = Modifier.clickable {
                                    newTitleText = doc.title
                                    showRenameDialog = true
                                }
                            )
                            IconButton(onClick = {
                                newTitleText = doc.title
                                showRenameDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp), tint = Slate400)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FolderTagPill(folderName = doc.folderName, colorHex = "#0F766E", isVault = doc.isEncrypted)
                            if (doc.isEncrypted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                E2eeSecurityBadge()
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Move to Folder") },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = TealPrimary) },
                            onClick = {
                                menuExpanded = false
                                showMoveFolderDialog = true
                            }
                        )
                        if (doc.isEncrypted) {
                            DropdownMenuItem(
                                text = { Text(if (doc.isUnlocked) "Lock Vault" else "Unlock Vault") },
                                leadingIcon = { Icon(if (doc.isUnlocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, tint = AmberSecurity) },
                                onClick = {
                                    menuExpanded = false
                                    if (doc.isUnlocked) {
                                        viewModel.lockDoc(doc.id)
                                    } else {
                                        showPinUnlockDialog = true
                                    }
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                viewModel.deleteDoc(doc.id)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export PDF Button
                    Button(
                        onClick = { showPdfExportSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF", fontWeight = FontWeight.Bold)
                    }

                    // Export JPEG Button
                    OutlinedButton(
                        onClick = { viewModel.exportAndShareJpegs(doc) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_jpeg_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JPEG", color = TealPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLocked) {
                // Locked Encrypted Document State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(AmberSecurity.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AmberSecurity,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Confidential Encrypted Document",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "This document is protected with AES-256 client-side encryption. Enter security PIN to view pages and OCR text.",
                            fontSize = 13.sp,
                            color = Slate500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showPinUnlockDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberSecurity),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("unlock_vault_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unlock Document", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Navigation Tabs: [Pages & Filters | OCR Searchable Text | Security & Cloud]
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pages (${doc.pageCount})", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("OCR Text", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("E2EE & Cloud", fontSize = 13.sp) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Pages & Filters View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Main Document Viewer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .background(Color.Black)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentPage != null && File(currentPage.imagePath).exists()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(File(currentPage.imagePath))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Page ${safeIndex + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(6.dp))
                                            .rotate(currentPage.rotationDegrees.toFloat())
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "${safeIndex + 1} / ${doc.pageCount}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Thumbnail Strip
                            if (doc.pageCount > 1) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    items(doc.pages.indices.toList()) { idx ->
                                        val isSel = idx == safeIndex
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp, 64.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(
                                                    width = if (isSel) 2.dp else 1.dp,
                                                    color = if (isSel) TealPrimary else Slate400,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable { selectedPageIndex = idx },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Page ${idx + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) TealPrimary else Slate700
                                            )
                                        }
                                    }
                                }
                            }

                            // Filter Controls
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Filter Enhancement",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { viewModel.rotateDocPage(doc.id, safeIndex) }) {
                                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = TealPrimary)
                                        }
                                    }

                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(FilterMode.values()) { filter ->
                                            val isFilterSel = currentPage?.filterMode == filter
                                            FilterChip(
                                                selected = isFilterSel,
                                                onClick = { viewModel.updatePageFilter(doc.id, safeIndex, filter) },
                                                label = { Text(filter.displayName, fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = TealPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // OCR Text Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Extracted Searchable OCR Text",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Doc OCR Text", doc.fullOcrText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Full document text copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy All Text", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = doc.fullOcrText.ifBlank { "No OCR text available." },
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    2 -> {
                        // E2EE & Cloud Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // E2EE Card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = AmberSecurity)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("End-to-End Encryption", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Fingerprint: ${SecurityEngine.generateE2eeKeyFingerprint(doc.id)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate500
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Status: Encrypted with zero-knowledge AES-256 standard before writing to disk or cloud backup.",
                                        fontSize = 12.sp,
                                        color = Slate700
                                    )
                                }
                            }

                            // Cloud Backup Status Card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Cloud, contentDescription = null, tint = TealPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cloud Backup Availability", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Sync Status: ${doc.cloudSyncStatus.label}",
                                        fontSize = 13.sp,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Available across all linked Android and iOS devices. Automated checksums prevent data loss.",
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PDF Export Sheet / Dialog
    if (showPdfExportSheet) {
        var pageSizeA4 by remember { mutableStateOf(true) }
        var includeNumbers by remember { mutableStateOf(true) }
        var includeWatermark by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPdfExportSheet = false },
            title = { Text("Export Document as PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select PDF formatting options:", fontSize = 13.sp, color = Slate500)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { pageSizeA4 = !pageSizeA4 }
                    ) {
                        Checkbox(checked = pageSizeA4, onCheckedChange = { pageSizeA4 = it })
                        Text("Standard A4 Format (595 x 842 pt)", fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { includeNumbers = !includeNumbers }
                    ) {
                        Checkbox(checked = includeNumbers, onCheckedChange = { includeNumbers = it })
                        Text("Include Page Numbers & Header", fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { includeWatermark = !includeWatermark }
                    ) {
                        Checkbox(checked = includeWatermark, onCheckedChange = { includeWatermark = it })
                        Text("Add Confidential Security Watermark", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfExportSheet = false
                        viewModel.exportAndSharePdf(
                            doc,
                            PdfExportConfig(
                                pageSizeA4 = pageSizeA4,
                                includePageNumbers = includeNumbers,
                                includeWatermark = includeWatermark
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Generate & Share PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfExportSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    singleLine = true,
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDocTitle(doc.id, newTitleText)
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Move to Folder Dialog
    if (showMoveFolderDialog) {
        AlertDialog(
            onDismissRequest = { showMoveFolderDialog = false },
            title = { Text("Move to Categorized Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    folders.forEach { f ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (f.id == doc.folderId) TealContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveDocToFolder(doc.id, f.id)
                                    showMoveFolderDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (f.isVault) Icons.Default.Lock else Icons.Default.DriveFileMove,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(f.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PIN Unlock Dialog
    if (showPinUnlockDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinUnlockDialog = false
                enteredPin = ""
                pinError = false
            },
            title = { Text("Unlock Security Vault") },
            text = {
                Column {
                    Text("Enter security PIN to decrypt this document (Default PIN: 1234):", fontSize = 13.sp, color = Slate500)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            pinError = false
                        },
                        singleLine = true,
                        isError = pinError,
                        label = { Text("Security PIN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Incorrect PIN. Please try again.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.unlockDoc(doc.id, enteredPin)
                        if (success) {
                            showPinUnlockDialog = false
                            enteredPin = ""
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberSecurity)
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinUnlockDialog = false
                    enteredPin = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
