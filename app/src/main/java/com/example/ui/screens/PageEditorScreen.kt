package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.example.model.DocumentFolder
import com.example.model.FilterMode
import com.example.ui.DocScanViewModel
import com.example.ui.Screen
import com.example.ui.theme.AmberSecurity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEditorScreen(viewModel: DocScanViewModel) {
    val session by viewModel.scannerSession.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateBack()
    }

    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var documentTitle by remember(session.documentTitle) { mutableStateOf(session.documentTitle) }
    var selectedFolderId by remember(session.targetFolderId) { mutableStateOf(session.targetFolderId) }
    var isEncrypted by remember { mutableStateOf(false) }
    var encryptionPin by remember { mutableStateOf("1234") }
    var showOcrText by remember { mutableStateOf(false) }

    val safeIndex = selectedPageIndex.coerceIn(0, (session.stagedPages.size - 1).coerceAtLeast(0))
    val currentPage = session.stagedPages.getOrNull(safeIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Review & Enhance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Page ${safeIndex + 1} of ${session.stagedPages.size}",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.updateStagedTitle(documentTitle)
                            viewModel.updateStagedFolder(selectedFolderId)
                            viewModel.saveStagedDocument(isEncrypted = isEncrypted, pin = encryptionPin)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("save_document_top_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Doc", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Main Document Image Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
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
                        contentDescription = "Scanned Page Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .rotate(currentPage.rotationDegrees.toFloat())
                    )
                } else {
                    Text("No page captured", color = Color.White)
                }

                // Page Tag in Bottom Corner
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Filter: ${currentPage?.filterMode?.displayName ?: "Magic Color"}",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Page Navigation Strip (if multiple pages)
            if (session.stagedPages.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    items(session.stagedPages.indices.toList()) { idx ->
                        val isSel = idx == safeIndex
                        Box(
                            modifier = Modifier
                                .size(46.dp, 58.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) TealPrimary else Slate400,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedPageIndex = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P.${idx + 1}",
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) TealPrimary else Slate700
                            )
                        }
                    }
                }
            }

            // Quick Tool Actions: Rotate, Delete, Add Page
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.rotateStagedPage(safeIndex) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("rotate_page_button")
                ) {
                    Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rotate", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.CameraScanner) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_page_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Page", fontSize = 12.sp)
                }

                if (session.stagedPages.size > 1) {
                    OutlinedButton(
                        onClick = { viewModel.deleteStagedPage(safeIndex) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("delete_page_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CamScanner Filter Engine Chips
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Document Filters (CamScanner Mode)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(FilterMode.values()) { filter ->
                            val isFilterSel = currentPage?.filterMode == filter
                            FilterChip(
                                selected = isFilterSel,
                                onClick = { viewModel.updateStagedPageFilter(safeIndex, filter) },
                                label = { Text(filter.displayName, fontSize = 12.sp) },
                                leadingIcon = {
                                    if (isFilterSel) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_chip_${filter.name}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // OCR Support & Searchable Text Inspector
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OCR Extracted Text",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("OCR Text", currentPage?.ocrText ?: "")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "OCR text copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", color = TealPrimary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentPage?.ocrText?.ifBlank { "No text recognized yet." } ?: "No text recognized.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Slate700,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Document Organization & Categorized Folders
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Document Properties",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = documentTitle,
                        onValueChange = { documentTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Organize into Folder:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate500
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(folders) { folder ->
                            val isSelected = selectedFolderId == folder.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFolderId = folder.id },
                                label = { Text(folder.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (folder.isVault) Icons.Default.Lock else Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) Color.White else TealPrimary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("editor_folder_${folder.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // End-to-End Encryption Toggle (E2EE Vault)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberSecurity.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberSecurity.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = AmberSecurity,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "End-to-End Encryption",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "AES-256 Client-Side Vault Security",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Switch(
                                    checked = isEncrypted,
                                    onCheckedChange = { isEncrypted = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AmberSecurity,
                                        checkedTrackColor = AmberSecurity.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("e2ee_switch")
                                )
                            }

                            if (isEncrypted) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = encryptionPin,
                                    onValueChange = { if (it.length <= 6) encryptionPin = it },
                                    label = { Text("4-6 Digit Security PIN") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Document Big Button
            Button(
                onClick = {
                    viewModel.updateStagedTitle(documentTitle)
                    viewModel.updateStagedFolder(selectedFolderId)
                    viewModel.saveStagedDocument(isEncrypted = isEncrypted, pin = encryptionPin)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 16.dp)
                    .testTag("save_document_bottom_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete & Save to Folder", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
