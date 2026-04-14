package com.slock.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using EncryptedSharedPreferences.
 * Tokens are encrypted at rest using AES-256-GCM.
 */
@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "slock_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_SERVER_ID = "server_id"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var accessToken: String?
        get() = securePrefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = securePrefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = securePrefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = securePrefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var userId: String?
        get() = securePrefs.getString(KEY_USER_ID, null)
        set(value) = securePrefs.edit().putString(KEY_USER_ID, value).apply()

    var userName: String?
        get() = securePrefs.getString(KEY_USER_NAME, null)
        set(value) = securePrefs.edit().putString(KEY_USER_NAME, value).apply()

    var serverId: String?
        get() = securePrefs.getString(KEY_SERVER_ID, null)
        set(value) = securePrefs.edit().putString(KEY_SERVER_ID, value).apply()

    fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    fun saveUser(id: String, name: String) {
        this.userId = id
        this.userName = name
    }

    fun clearTokens() {
        accessToken = null
        refreshToken = null
        userId = null
        userName = null
        serverId = null
    }

    fun hasTokens(): Boolean = !accessToken.isNullOrEmpty()
}
