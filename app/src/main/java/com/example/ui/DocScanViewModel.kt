package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DocumentRepository
import com.example.engine.ExportEngine
import com.example.engine.ImageEngine
import com.example.engine.OcrEngine
import com.example.engine.PdfExportConfig
import com.example.model.CloudSyncConfig
import com.example.model.DocumentFolder
import com.example.model.FilterMode
import com.example.model.ScannedDocument
import com.example.model.ScannedPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed class Screen {
    object Dashboard : Screen()
    object CameraScanner : Screen()
    data class PageEditor(val isNewDocument: Boolean = false) : Screen()
    data class DocumentDetail(val docId: String) : Screen()
    object CloudSyncVault : Screen()
    object FolderManager : Screen()
}

data class ScannerSessionState(
    val stagedPages: List<ScannedPage> = emptyList(),
    val documentTitle: String = "Scanned Doc",
    val targetFolderId: String = "folder_personal",
    val isBatchMode: Boolean = true,
    val isFlashOn: Boolean = false,
    val isGridOn: Boolean = true,
    val isProcessing: Boolean = false
)

class DocScanViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DocumentRepository(application.applicationContext, viewModelScope)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>("all")
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _selectedDocId = MutableStateFlow<String?>(null)
    val selectedDocId: StateFlow<String?> = _selectedDocId.asStateFlow()

    private val _scannerSession = MutableStateFlow(ScannerSessionState())
    val scannerSession: StateFlow<ScannerSessionState> = _scannerSession.asStateFlow()

    val folders: StateFlow<List<DocumentFolder>> = repository.folders
    val cloudConfig: StateFlow<CloudSyncConfig> = repository.cloudConfig
    val isSyncing: StateFlow<Boolean> = repository.isSyncing

    // Filtered documents by search query (matching title OR OCR full text) and folder
    val filteredDocuments: StateFlow<List<ScannedDocument>> = combine(
        repository.documents,
        _searchQuery,
        _selectedFolderId
    ) { docs, query, folderId ->
        docs.filter { doc ->
            val matchesFolder = folderId == null || folderId == "all" || doc.folderId == folderId
            val matchesQuery = query.isBlank() ||
                    doc.title.contains(query, ignoreCase = true) ||
                    doc.tags.any { it.contains(query, ignoreCase = true) } ||
                    doc.fullOcrText.contains(query, ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active document being viewed in DocumentDetail screen
    val activeDocument: StateFlow<ScannedDocument?> = combine(
        repository.documents,
        _selectedDocId
    ) { docs, docId ->
        if (docId == null) null else docs.find { it.id == docId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Screen navigation
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun navigateBack() {
        when (_currentScreen.value) {
            is Screen.Dashboard -> { /* Root screen */ }
            is Screen.CameraScanner -> _currentScreen.value = Screen.Dashboard
            is Screen.PageEditor -> _currentScreen.value = Screen.Dashboard
            is Screen.DocumentDetail -> {
                _selectedDocId.value = null
                _currentScreen.value = Screen.Dashboard
            }
            is Screen.CloudSyncVault -> _currentScreen.value = Screen.Dashboard
            is Screen.FolderManager -> _currentScreen.value = Screen.Dashboard
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFolder(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun selectDocument(docId: String) {
        _selectedDocId.value = docId
        _currentScreen.value = Screen.DocumentDetail(docId)
    }

    // Scanner Capture Operations
    fun startNewScanSession() {
        val defaultFolder = folders.value.firstOrNull()?.id ?: "folder_personal"
        _scannerSession.value = ScannerSessionState(
            stagedPages = emptyList(),
            targetFolderId = defaultFolder,
            documentTitle = "Scan ${System.currentTimeMillis().toString().takeLast(4)}"
        )
        _currentScreen.value = Screen.CameraScanner
    }

    fun toggleFlash() {
        _scannerSession.update { it.copy(isFlashOn = !it.isFlashOn) }
    }

    fun toggleGrid() {
        _scannerSession.update { it.copy(isGridOn = !it.isGridOn) }
    }

    fun toggleBatchMode() {
        _scannerSession.update { it.copy(isBatchMode = !it.isBatchMode) }
    }

    fun onBitmapCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            _scannerSession.update { it.copy(isProcessing = true) }

            val (savedPath, _) = ImageEngine.processAndSaveImage(
                context = getApplication(),
                sourceBitmap = bitmap,
                filterMode = FilterMode.MAGIC_COLOR
            )

            val ocrResult = OcrEngine.analyzeBitmap(
                bitmap = bitmap,
                titleHint = _scannerSession.value.documentTitle
            )

            val newPage = ScannedPage(
                id = "page_${UUID.randomUUID()}",
                imagePath = savedPath,
                filterMode = FilterMode.MAGIC_COLOR,
                rotationDegrees = 0,
                ocrText = ocrResult.fullText,
                detectedType = ocrResult.detectedType
            )

            _scannerSession.update { state ->
                state.copy(
                    stagedPages = state.stagedPages + newPage,
                    isProcessing = false
                )
            }

            // If not batch mode, proceed directly to page editor
            if (!_scannerSession.value.isBatchMode) {
                _currentScreen.value = Screen.PageEditor(isNewDocument = true)
            }
        }
    }

    fun onGalleryImagePicked(uri: Uri) {
        viewModelScope.launch {
            _scannerSession.update { it.copy(isProcessing = true) }
            val bitmap = ImageEngine.loadBitmapFromUri(getApplication(), uri)
            if (bitmap != null) {
                onBitmapCaptured(bitmap)
            } else {
                _scannerSession.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun proceedToEditor() {
        if (_scannerSession.value.stagedPages.isNotEmpty()) {
            _currentScreen.value = Screen.PageEditor(isNewDocument = true)
        }
    }

    // Page Editor State
    fun updateStagedPageFilter(pageIndex: Int, newFilter: FilterMode) {
        _scannerSession.update { state ->
            if (pageIndex in state.stagedPages.indices) {
                val updated = state.stagedPages.toMutableList()
                updated[pageIndex] = updated[pageIndex].copy(filterMode = newFilter)
                state.copy(stagedPages = updated)
            } else state
        }
    }

    fun rotateStagedPage(pageIndex: Int) {
        _scannerSession.update { state ->
            if (pageIndex in state.stagedPages.indices) {
                val updated = state.stagedPages.toMutableList()
                val rot = updated[pageIndex].rotationDegrees
                updated[pageIndex] = updated[pageIndex].copy(rotationDegrees = (rot + 90) % 360)
                state.copy(stagedPages = updated)
            } else state
        }
    }

    fun deleteStagedPage(pageIndex: Int) {
        _scannerSession.update { state ->
            val updated = state.stagedPages.toMutableList()
            if (pageIndex in updated.indices) {
                updated.removeAt(pageIndex)
            }
            state.copy(stagedPages = updated)
        }
    }

    fun updateStagedTitle(title: String) {
        _scannerSession.update { it.copy(documentTitle = title) }
    }

    fun updateStagedFolder(folderId: String) {
        _scannerSession.update { it.copy(targetFolderId = folderId) }
    }

    fun saveStagedDocument(isEncrypted: Boolean = false, pin: String? = null) {
        val state = _scannerSession.value
        if (state.stagedPages.isEmpty()) return

        viewModelScope.launch {
            val doc = repository.saveNewDocument(
                title = state.documentTitle,
                folderId = state.targetFolderId,
                pages = state.stagedPages,
                isEncrypted = isEncrypted,
                pin = pin
            )
            _selectedDocId.value = doc.id
            _currentScreen.value = Screen.DocumentDetail(doc.id)
        }
    }

    // Document Detail Actions
    fun updateDocTitle(docId: String, newTitle: String) {
        repository.updateDocumentTitle(docId, newTitle)
    }

    fun moveDocToFolder(docId: String, newFolderId: String) {
        repository.moveDocumentToFolder(docId, newFolderId)
    }

    fun deleteDoc(docId: String) {
        repository.deleteDocument(docId)
        _selectedDocId.value = null
        _currentScreen.value = Screen.Dashboard
    }

    fun unlockDoc(docId: String, pin: String): Boolean {
        return repository.unlockDocument(docId, pin)
    }

    fun lockDoc(docId: String) {
        repository.lockDocument(docId)
    }

    fun updatePageFilter(docId: String, pageIndex: Int, newFilter: FilterMode) {
        repository.togglePageFilter(docId, pageIndex, newFilter)
    }

    fun rotateDocPage(docId: String, pageIndex: Int) {
        repository.rotatePage(docId, pageIndex)
    }

    fun deleteDocPage(docId: String, pageIndex: Int) {
        repository.deletePage(docId, pageIndex)
    }

    // PDF & JPEG Export
    fun exportAndSharePdf(document: ScannedDocument, config: PdfExportConfig = PdfExportConfig()) {
        viewModelScope.launch {
            val pdfFile = ExportEngine.generatePdf(getApplication(), document, config)
            ExportEngine.shareFile(getApplication(), pdfFile, "application/pdf", "Share PDF Document")
        }
    }

    fun exportAndShareJpegs(document: ScannedDocument) {
        viewModelScope.launch {
            val jpegs = ExportEngine.generateJpegs(getApplication(), document)
            if (jpegs.isNotEmpty()) {
                ExportEngine.shareFile(getApplication(), jpegs.first(), "image/jpeg", "Share Document Page")
            }
        }
    }

    // Cloud Sync & Vault
    fun syncNow() {
        repository.triggerBackgroundSync()
    }

    fun toggleOfflineMode(enabled: Boolean) {
        repository.toggleOfflineMode(enabled)
    }

    fun toggleAutoSync(enabled: Boolean) {
        repository.toggleAutoSync(enabled)
    }

    fun toggleWifiOnly(enabled: Boolean) {
        repository.toggleWifiOnly(enabled)
    }

    // Folder Management
    fun createFolder(name: String, colorHex: String, isVault: Boolean, pin: String?) {
        repository.createFolder(name, colorHex, isVault, pin)
    }

    fun deleteFolder(folderId: String) {
        repository.deleteFolder(folderId)
    }

    // Demo scan action for immediate testing
    fun createDemoScan(docType: String = "Contract") {
        viewModelScope.launch {
            val defaultFolder = folders.value.firstOrNull()?.id ?: "folder_personal"
            val title = when (docType.lowercase()) {
                "receipt" -> "Office Supplies Receipt"
                "id" -> "State Driver ID Record"
                else -> "Commercial Service Agreement"
            }
            val doc = repository.createDemoScannedDocument(
                title = title,
                folderId = defaultFolder,
                docType = docType
            )
            _selectedDocId.value = doc.id
            _currentScreen.value = Screen.DocumentDetail(doc.id)
        }
    }
}
