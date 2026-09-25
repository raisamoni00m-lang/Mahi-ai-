package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure storage for Mahi AI's Gemini API key on the Android device.
 * Uses Android Keystore with AES-GCM encryption when available,
 * falling back gracefully to secure private preferences.
 */
class SecureKeyStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "mahi_ai_secure_prefs"
        private const val KEY_ENCRYPTED_API_KEY = "encrypted_gemini_api_key"
        private const val KEY_IV = "gemini_api_key_iv"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MahiAiKeyStoreAlias"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Fallback will use obfuscated storage if hardware keystore is unavailable
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (_: Exception) {
            null
        }
    }

    fun saveApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return

        val secretKey = getSecretKey()
        if (secretKey != null) {
            try {
                val cipher = Cipher.getInstance(AES_MODE)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(trimmed.toByteArray(StandardCharsets.UTF_8))

                prefs.edit()
                    .putString(KEY_ENCRYPTED_API_KEY, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                    .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                    .apply()
                return
            } catch (_: Exception) {
                // fallback to obfuscated storage
            }
        }

        // Fallback obfuscation
        val encoded = Base64.encodeToString(trimmed.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_ENCRYPTED_API_KEY, encoded)
            .remove(KEY_IV)
            .apply()
    }

    fun getApiKey(): String? {
        val encryptedStr = prefs.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
        val ivStr = prefs.getString(KEY_IV, null)

        val secretKey = getSecretKey()
        if (secretKey != null && ivStr != null) {
            try {
                val iv = Base64.decode(ivStr, Base64.NO_WRAP)
                val encryptedBytes = Base64.decode(encryptedStr, Base64.NO_WRAP)
                val cipher = Cipher.getInstance(AES_MODE)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val decrypted = cipher.doFinal(encryptedBytes)
                return String(decrypted, StandardCharsets.UTF_8)
            } catch (_: Exception) {
                // fall through
            }
        }

        return try {
            val decoded = Base64.decode(encryptedStr, Base64.NO_WRAP)
            String(decoded, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun hasSavedKey(): Boolean {
        val key = getApiKey()
        return !key.isNullOrBlank()
    }

    fun clearKey() {
        prefs.edit().remove(KEY_ENCRYPTED_API_KEY).remove(KEY_IV).apply()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
}
