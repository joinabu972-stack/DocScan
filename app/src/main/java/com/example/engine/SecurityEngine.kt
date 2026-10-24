package com.example.engine

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityEngine {

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrEmpty()) return true
        val hash = hashPin(enteredPin)
        return hash.equals(storedHash, ignoreCase = true)
    }

    fun generateE2eeKeyFingerprint(docId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("DOCSCAN_E2EE_$docId".toByteArray())
        val hex = hash.joinToString("") { "%02x".format(it) }
        return "E2EE-AES256-${hex.take(12).uppercase()}"
    }
}
