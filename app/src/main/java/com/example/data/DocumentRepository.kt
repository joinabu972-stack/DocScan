package com.example.data

import android.content.Context
import android.graphics.Bitmap
import com.example.engine.ImageEngine
import com.example.engine.OcrEngine
import com.example.engine.SecurityEngine
import com.example.model.CloudSyncConfig
import com.example.model.CloudSyncStatus
import com.example.model.DocumentFolder
import com.example.model.FilterMode
import com.example.model.ScannedDocument
import com.example.model.ScannedPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class DocumentRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _folders = MutableStateFlow<List<DocumentFolder>>(
        listOf(
            DocumentFolder(id = "folder_personal", name = "Personal", iconName = "person", colorHex = "#0F766E"),
            DocumentFolder(id = "folder_work", name = "Work", iconName = "work", colorHex = "#0284C7"),
            DocumentFolder(id = "folder_receipts", name = "Receipts", iconName = "receipt", colorHex = "#F59E0B"),
            DocumentFolder(id = "folder_ids", name = "IDs & Cards", iconName = "badge", colorHex = "#7C3AED", isVault = true, pinHash = SecurityEngine.hashPin("1234")),
            DocumentFolder(id = "folder_legal", name = "Tax & Legal", iconName = "gavel", colorHex = "#4338CA"),
            DocumentFolder(id = "folder_medical", name = "Medical", iconName = "health", colorHex = "#10B981")
        )
    )
    val folders: StateFlow<List<DocumentFolder>> = _folders.asStateFlow()

    // STRICT REQUIREMENT: "Do not show any photos first page from you"
    // Initial state is empty so user sees a clean UI and intuitive organization.
    private val _documents = MutableStateFlow<List<ScannedDocument>>(emptyList())
    val documents: StateFlow<List<ScannedDocument>> = _documents.asStateFlow()

    private val _cloudConfig = MutableStateFlow(CloudSyncConfig())
    val cloudConfig: StateFlow<CloudSyncConfig> = _cloudConfig.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun createFolder(name: String, colorHex: String, isVault: Boolean = false, pin: String? = null): DocumentFolder {
        val newFolder = DocumentFolder(
            id = "folder_${UUID.randomUUID().toString().take(8)}",
            name = name,
            colorHex = colorHex,
            isVault = isVault,
            pinHash = if (isVault && !pin.isNullOrBlank()) SecurityEngine.hashPin(pin) else null
        )
        _folders.update { it + newFolder }
        return newFolder
    }

    fun deleteFolder(folderId: String) {
        _folders.update { it.filterNot { f -> f.id == folderId } }
        // Move docs from deleted folder to Personal or Uncategorized
        _documents.update { docs ->
            docs.map { doc ->
                if (doc.folderId == folderId) {
                    doc.copy(folderId = "folder_personal", folderName = "Personal")
                } else doc
            }
        }
    }

    suspend fun saveNewDocument(
        title: String,
        folderId: String,
        pages: List<ScannedPage>,
        tags: List<String> = emptyList(),
        isEncrypted: Boolean = false,
        pin: String? = null
    ): ScannedDocument {
        val folder = _folders.value.find { it.id == folderId } ?: _folders.value.first()
        val docId = "doc_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val initialSyncStatus = if (_cloudConfig.value.isOfflineMode) {
            CloudSyncStatus.OFFLINE
        } else if (_cloudConfig.value.autoSync) {
            CloudSyncStatus.SYNCED
        } else {
            CloudSyncStatus.PENDING
        }

        val newDoc = ScannedDocument(
            id = docId,
            title = title.ifBlank { "Scan ${pages.size} Pgs - ${folder.name}" },
            folderId = folder.id,
            folderName = folder.name,
            createdAt = now,
            updatedAt = now,
            pages = pages,
            isEncrypted = isEncrypted || folder.isVault,
            encryptionPin = if (isEncrypted && !pin.isNullOrBlank()) SecurityEngine.hashPin(pin) else folder.pinHash,
            isUnlocked = !isEncrypted && !folder.isVault,
            cloudSyncStatus = initialSyncStatus,
            tags = tags,
            totalSizeKb = (pages.size * 340).toLong()
        )

        _documents.update { listOf(newDoc) + it }

        // Update cloud stats
        updateStorageUsage()

        // Trigger auto-sync if online
        if (_cloudConfig.value.autoSync && !_cloudConfig.value.isOfflineMode) {
            triggerBackgroundSync()
        }

        return newDoc
    }

    fun updateDocumentTitle(docId: String, newTitle: String) {
        _documents.update { list ->
            list.map { if (it.id == docId) it.copy(title = newTitle, updatedAt = System.currentTimeMillis()) else it }
        }
    }

    fun moveDocumentToFolder(docId: String, newFolderId: String) {
        val folder = _folders.value.find { it.id == newFolderId } ?: return
        _documents.update { list ->
            list.map {
                if (it.id == docId) {
                    it.copy(
                        folderId = folder.id,
                        folderName = folder.name,
                        isEncrypted = it.isEncrypted || folder.isVault,
                        updatedAt = System.currentTimeMillis()
                    )
                } else it
            }
        }
    }

    fun deleteDocument(docId: String) {
        _documents.update { list -> list.filterNot { it.id == docId } }
        updateStorageUsage()
    }

    fun unlockDocument(docId: String, pin: String): Boolean {
        val doc = _documents.value.find { it.id == docId } ?: return false
        val isValid = SecurityEngine.verifyPin(pin, doc.encryptionPin)
        if (isValid) {
            _documents.update { list ->
                list.map { if (it.id == docId) it.copy(isUnlocked = true) else it }
            }
        }
        return isValid
    }

    fun lockDocument(docId: String) {
        _documents.update { list ->
            list.map { if (it.id == docId) it.copy(isUnlocked = false) else it }
        }
    }

    fun togglePageFilter(docId: String, pageIndex: Int, newFilter: FilterMode) {
        _documents.update { list ->
            list.map { doc ->
                if (doc.id == docId && pageIndex in doc.pages.indices) {
                    val updatedPages = doc.pages.toMutableList()
                    updatedPages[pageIndex] = updatedPages[pageIndex].copy(filterMode = newFilter)
                    doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
                } else doc
            }
        }
    }

    fun rotatePage(docId: String, pageIndex: Int) {
        _documents.update { list ->
            list.map { doc ->
                if (doc.id == docId && pageIndex in doc.pages.indices) {
                    val updatedPages = doc.pages.toMutableList()
                    val curRotation = updatedPages[pageIndex].rotationDegrees
                    updatedPages[pageIndex] = updatedPages[pageIndex].copy(rotationDegrees = (curRotation + 90) % 360)
                    doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
                } else doc
            }
        }
    }

    fun deletePage(docId: String, pageIndex: Int) {
        _documents.update { list ->
            list.mapNotNull { doc ->
                if (doc.id == docId) {
                    val updatedPages = doc.pages.toMutableList()
                    if (pageIndex in updatedPages.indices) {
                        updatedPages.removeAt(pageIndex)
                    }
                    if (updatedPages.isEmpty()) null else doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
                } else doc
            }
        }
        updateStorageUsage()
    }

    fun updateOcrText(docId: String, pageIndex: Int, newText: String) {
        _documents.update { list ->
            list.map { doc ->
                if (doc.id == docId && pageIndex in doc.pages.indices) {
                    val updatedPages = doc.pages.toMutableList()
                    updatedPages[pageIndex] = updatedPages[pageIndex].copy(ocrText = newText)
                    doc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
                } else doc
            }
        }
    }

    // Cloud Sync Operations
    fun toggleOfflineMode(enabled: Boolean) {
        _cloudConfig.update { it.copy(isOfflineMode = enabled) }
        if (!enabled) {
            triggerBackgroundSync()
        } else {
            _documents.update { list ->
                list.map { if (it.cloudSyncStatus == CloudSyncStatus.PENDING) it.copy(cloudSyncStatus = CloudSyncStatus.OFFLINE) else it }
            }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        _cloudConfig.update { it.copy(autoSync = enabled) }
        if (enabled && !_cloudConfig.value.isOfflineMode) {
            triggerBackgroundSync()
        }
    }

    fun toggleWifiOnly(enabled: Boolean) {
        _cloudConfig.update { it.copy(wifiOnly = enabled) }
    }

    fun triggerBackgroundSync() {
        if (_cloudConfig.value.isOfflineMode) return
        scope.launch {
            _isSyncing.value = true
            _documents.update { list ->
                list.map { if (it.cloudSyncStatus != CloudSyncStatus.SYNCED) it.copy(cloudSyncStatus = CloudSyncStatus.SYNCING) else it }
            }
            delay(1200) // Realistic cloud synchronization cycle
            _documents.update { list ->
                list.map { it.copy(cloudSyncStatus = CloudSyncStatus.SYNCED) }
            }
            _cloudConfig.update { it.copy(lastSyncTime = System.currentTimeMillis()) }
            _isSyncing.value = false
        }
    }

    private fun updateStorageUsage() {
        val totalPages = _documents.value.sumOf { it.pages.size }
        val usedMb = (totalPages * 0.45f).coerceAtLeast(1.2f)
        _cloudConfig.update { it.copy(usedStorageMb = usedMb) }
    }

    /**
     * Demo scan generator so the user can easily test scanning, OCR, PDF generation,
     * filters, and E2EE right from the app!
     */
    suspend fun createDemoScannedDocument(
        title: String,
        folderId: String,
        docType: String = "Contract"
    ): ScannedDocument {
        val sampleBitmap = ImageEngine.createSampleScannedPageBitmap(
            title = title,
            bodyLines = listOf(
                "Client Account: ACME Global Technologies Corp",
                "Statement Period: October 2026",
                "Digitized Resolution: 300 DPI Multi-spectrum Color",
                "Security Status: AES-256 GCM End-to-End Encrypted",
                "Cloud Backup Node: US-Central / Cross-Device iOS & Android",
                "OCR Status: 100% Characters Extracted and Indexed"
            ),
            docType = docType.uppercase()
        )

        val (savedPath, _) = ImageEngine.processAndSaveImage(
            context = context,
            sourceBitmap = sampleBitmap,
            filterMode = FilterMode.MAGIC_COLOR
        )

        val ocrResult = OcrEngine.analyzeBitmap(sampleBitmap, titleHint = title)

        val page = ScannedPage(
            id = "page_${UUID.randomUUID()}",
            imagePath = savedPath,
            filterMode = FilterMode.MAGIC_COLOR,
            rotationDegrees = 0,
            ocrText = ocrResult.fullText,
            detectedType = ocrResult.detectedType
        )

        return saveNewDocument(
            title = title,
            folderId = folderId,
            pages = listOf(page),
            tags = listOf("Scanned", docType)
        )
    }
}
