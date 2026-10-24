package com.example.model

enum class FilterMode(val displayName: String) {
    MAGIC_COLOR("Magic Color"),
    BLACK_WHITE("B&W Document"),
    GRAYSCALE("Grayscale"),
    ORIGINAL("Original"),
    LIGHTEN("Lighten")
}

enum class CloudSyncStatus(val label: String) {
    SYNCED("Synced to Cloud"),
    SYNCING("Synchronizing..."),
    PENDING("Queued for Backup"),
    OFFLINE("Offline Saved")
}

data class DocumentFolder(
    val id: String,
    val name: String,
    val iconName: String = "folder",
    val colorHex: String = "#0F766E",
    val isVault: Boolean = false,
    val pinHash: String? = null
)

data class ScannedPage(
    val id: String,
    val imagePath: String,
    val filterMode: FilterMode = FilterMode.MAGIC_COLOR,
    val rotationDegrees: Int = 0,
    val ocrText: String = "",
    val detectedType: String = "Page"
)

data class ScannedDocument(
    val id: String,
    val title: String,
    val folderId: String,
    val folderName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pages: List<ScannedPage>,
    val isEncrypted: Boolean = false,
    val encryptionPin: String? = null,
    val isUnlocked: Boolean = false,
    val cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.SYNCED,
    val tags: List<String> = emptyList(),
    val totalSizeKb: Long = 0
) {
    val fullOcrText: String
        get() = pages.joinToString("\n\n") { it.ocrText }

    val pageCount: Int
        get() = pages.size
}

data class CloudSyncConfig(
    val autoSync: Boolean = true,
    val wifiOnly: Boolean = false,
    val isOfflineMode: Boolean = false,
    val crossDeviceLinkActive: Boolean = true,
    val backupId: String = "DS-SYNC-8821X",
    val lastSyncTime: Long = System.currentTimeMillis(),
    val usedStorageMb: Float = 142.5f,
    val totalStorageMb: Float = 5120.0f
)
