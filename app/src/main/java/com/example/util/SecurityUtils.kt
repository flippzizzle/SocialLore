package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashSecret(secret: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verifySecret(secret: String, expectedHashBase64: String, saltBase64: String): Boolean {
        return try {
            val candidate = hashSecret(secret, saltBase64)
            MessageDigest.isEqual(
                candidate.toByteArray(Charsets.UTF_8),
                expectedHashBase64.toByteArray(Charsets.UTF_8)
            )
        } catch (_: Exception) {
            false
        }
    }

    fun generateFriendCode(uid: String): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray(Charsets.UTF_8))
        val suffix = (0 until 6).joinToString("") { idx ->
            val b = digest[idx].toInt() and 0xFF
            chars[b % chars.length].toString()
        }
        return "LORE-$suffix"
    }
}
