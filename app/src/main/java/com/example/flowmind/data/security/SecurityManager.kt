package com.example.flowmind.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralises cryptographic key management and secure credential storage for FlowMind.
 *
 * All sensitive values (e.g. the SQLCipher database passphrase and API tokens) are
 * stored inside an [EncryptedSharedPreferences] instance backed by an AES-256-GCM
 * [MasterKey] held in the Android Keystore.
 *
 * ### Responsibilities
 * - Generate and persist a random 256-bit database passphrase on first launch.
 * - Rotate the passphrase on demand (e.g. after a security event).
 * - Securely clear all stored secrets (e.g. on user sign-out or account deletion).
 * - Provide helpers for storing and retrieving arbitrary encrypted string tokens.
 *
 * ### Thread Safety
 * This class is annotated [@Singleton][Singleton] so Hilt will provide a single
 * shared instance. All calls are synchronous; wrap them in a background dispatcher
 * ([kotlinx.coroutines.Dispatchers.IO]) for UI usage.
 *
 * @param context Android [Context] used to open the Keystore and preference file.
 */
@Singleton
class SecurityManager @Inject constructor(private val context: Context) {

    companion object {
        private const val TAG = "SecurityManager"

        /** Name of the encrypted SharedPreferences file. */
        private const val PREFS_FILE = "flowmind_secure_prefs"

        /** Key under which the database passphrase is stored. */
        private const val KEY_DB_PASSPHRASE = "db_passphrase"

        /** Number of random bytes used for the database passphrase (256 bits). */
        private const val PASSPHRASE_BYTE_LENGTH = 32
    }

    // ── Keystore master key ───────────────────────────────────────────────────

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    // ── Encrypted preferences ─────────────────────────────────────────────────

    /**
     * The [EncryptedSharedPreferences] instance used for all secret storage.
     *
     * Keys are encrypted with AES-256-SIV and values with AES-256-GCM.
     */
    val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Database passphrase ───────────────────────────────────────────────────

    /**
     * Returns the current database passphrase as a raw [ByteArray].
     *
     * On the very first call a cryptographically-random 256-bit passphrase is
     * generated via [SecureRandom], Base64-encoded and persisted. Subsequent
     * calls decode and return the stored value.
     *
     * @return A 32-byte array suitable for use as a SQLCipher passphrase.
     */
    fun getDatabasePassphrase(): ByteArray {
        var passphraseStr = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
        if (passphraseStr == null) {
            passphraseStr = generateAndStorePassphrase()
            Log.i(TAG, "New database passphrase generated and stored.")
        }
        return android.util.Base64.decode(passphraseStr, android.util.Base64.NO_WRAP)
    }

    /**
     * Rotates the database passphrase by generating a fresh random key and
     * overwriting the stored value.
     *
     * **Important:** callers are responsible for re-encrypting the database
     * with the new passphrase (e.g. via SQLCipher's `PRAGMA rekey`) before the
     * old passphrase is discarded.
     *
     * @return The new 32-byte passphrase.
     */
    fun rotateDatabasePassphrase(): ByteArray {
        val newPassphraseStr = generateAndStorePassphrase()
        Log.i(TAG, "Database passphrase rotated.")
        return android.util.Base64.decode(newPassphraseStr, android.util.Base64.NO_WRAP)
    }

    // ── Generic token storage ─────────────────────────────────────────────────

    /**
     * Encrypts and stores a string [token] under the given [key].
     *
     * @param key   Preference key (e.g. `"api_token"`, `"refresh_token"`).
     * @param token Plain-text value to encrypt and persist.
     */
    fun storeToken(key: String, token: String) {
        encryptedPrefs.edit().putString(key, token).apply()
        Log.d(TAG, "Token stored for key: $key")
    }

    /**
     * Retrieves and decrypts a previously stored string token.
     *
     * @param key Preference key used when the token was stored.
     * @return The plain-text token, or `null` if no value is stored for [key].
     */
    fun retrieveToken(key: String): String? =
        encryptedPrefs.getString(key, null)

    /**
     * Removes a single stored token identified by [key].
     *
     * @param key Preference key of the value to remove.
     */
    fun clearToken(key: String) {
        encryptedPrefs.edit().remove(key).apply()
        Log.d(TAG, "Token cleared for key: $key")
    }

    // ── Secure wipe ───────────────────────────────────────────────────────────

    /**
     * Permanently deletes **all** secrets stored in the encrypted preferences.
     *
     * This should be called during sign-out or account deletion flows to ensure
     * no residual credentials remain on the device.
     */
    fun clearAllSecrets() {
        encryptedPrefs.edit().clear().apply()
        Log.w(TAG, "All secure preferences cleared.")
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Generates a new [PASSPHRASE_BYTE_LENGTH]-byte random passphrase, stores it
     * in [encryptedPrefs] and returns its Base64-encoded representation.
     */
    private fun generateAndStorePassphrase(): String {
        val randomBytes = ByteArray(PASSPHRASE_BYTE_LENGTH)
        SecureRandom().nextBytes(randomBytes)
        val encoded = android.util.Base64.encodeToString(randomBytes, android.util.Base64.NO_WRAP)
        encryptedPrefs.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
        return encoded
    }
}
