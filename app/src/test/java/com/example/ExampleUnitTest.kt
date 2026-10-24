package com.example

import com.example.engine.OcrEngine
import com.example.engine.SecurityEngine
import com.example.model.CloudSyncStatus
import com.example.model.FilterMode
import com.example.model.ScannedDocument
import com.example.model.ScannedPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun securityEngine_pinVerification_worksAccurately() {
        val pin = "8842"
        val hash = SecurityEngine.hashPin(pin)
        assertTrue(SecurityEngine.verifyPin("8842", hash))
        assertFalse(SecurityEngine.verifyPin("1234", hash))
    }

    @Test
    fun securityEngine_e2eeFingerprint_isGenerated() {
        val fingerprint = SecurityEngine.generateE2eeKeyFingerprint("doc_123")
        assertTrue(fingerprint.startsWith("E2EE-AES256-"))
        assertEquals(24, fingerprint.length)
    }

    @Test
    fun ocrEngine_keywordExtraction_extractsSearchTokens() {
        val sampleText = "Commercial Transaction Receipt for Professional Services Total 52.70"
        val keywords = OcrEngine.extractKeywords(sampleText)
        assertTrue(keywords.contains("commercial"))
        assertTrue(keywords.contains("receipt"))
        assertTrue(keywords.contains("professional"))
        assertTrue(OcrEngine.searchMatches(sampleText, "receipt"))
        assertTrue(OcrEngine.searchMatches(sampleText, "52.70"))
        assertFalse(OcrEngine.searchMatches(sampleText, "medical prescription"))
    }

    @Test
    fun scannedDocument_modelProperties_calculatedCorrectly() {
        val pages = listOf(
            ScannedPage("p1", "/path/1.jpg", FilterMode.MAGIC_COLOR, 0, "Page 1 OCR text"),
            ScannedPage("p2", "/path/2.jpg", FilterMode.BLACK_WHITE, 90, "Page 2 OCR text")
        )
        val doc = ScannedDocument(
            id = "doc_test",
            title = "Contract Test",
            folderId = "folder_work",
            folderName = "Work",
            createdAt = 1000L,
            updatedAt = 1000L,
            pages = pages,
            isEncrypted = true,
            cloudSyncStatus = CloudSyncStatus.SYNCED
        )

        assertEquals(2, doc.pageCount)
        assertTrue(doc.fullOcrText.contains("Page 1 OCR text"))
        assertTrue(doc.fullOcrText.contains("Page 2 OCR text"))
        assertEquals(CloudSyncStatus.SYNCED, doc.cloudSyncStatus)
    }
}
