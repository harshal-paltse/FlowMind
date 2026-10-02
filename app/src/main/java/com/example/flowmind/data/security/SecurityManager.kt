package com.example.flowmind.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "flowmind_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getDatabasePassphrase(): ByteArray {
        val key = "db_passphrase"
        var passphraseStr = encryptedPrefs.getString(key, null)
        if (passphraseStr == null) {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            passphraseStr = android.util.Base64.encodeToString(randomBytes, android.util.Base64.NO_WRAP)
            encryptedPrefs.edit().putString(key, passphraseStr).apply()
        }
        return android.util.Base64.decode(passphraseStr, android.util.Base64.NO_WRAP)
    }
}
