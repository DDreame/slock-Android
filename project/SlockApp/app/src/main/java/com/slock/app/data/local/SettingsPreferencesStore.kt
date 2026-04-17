package com.slock.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class NotificationPreference(
    val storageValue: String,
    val title: String,
    val description: String
) {
    MENTIONS_ONLY(
        storageValue = "mentions_only",
        title = "@MENTION ONLY",
        description = "Direct messages and messages that mention you."
    ),
    ALL_MESSAGES(
        storageValue = "all_messages",
        title = "ALL MESSAGES",
        description = "Every direct message and channel message."
    ),
    MUTE(
        storageValue = "mute",
        title = "MUTE",
        description = "Turn off message notifications."
    );

    companion object {
        fun fromStorageValue(value: String?): NotificationPreference {
            return entries.firstOrNull { it.storageValue == value } ?: ALL_MESSAGES
        }
    }
}

@Singleton
class SettingsPreferencesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val NOTIFICATION_PREFERENCE = stringPreferencesKey("notification_preference")
        private val RECENT_AGENT_MODELS = stringPreferencesKey("recent_agent_models")
        private const val AGENT_MODELS_SEPARATOR = "\n"
        private const val MAX_RECENT_AGENT_MODELS = 8
    }

    val notificationPreferenceFlow: Flow<NotificationPreference> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            NotificationPreference.fromStorageValue(preferences[NOTIFICATION_PREFERENCE])
        }

    val recentAgentModelsFlow: Flow<List<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[RECENT_AGENT_MODELS]
                .orEmpty()
                .split(AGENT_MODELS_SEPARATOR)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

    suspend fun setNotificationPreference(preference: NotificationPreference) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_PREFERENCE] = preference.storageValue
        }
    }

    suspend fun getNotificationPreference(): NotificationPreference {
        return notificationPreferenceFlow.first()
    }

    suspend fun addRecentAgentModel(modelId: String) {
        val trimmedModelId = modelId.trim()
        if (trimmedModelId.isEmpty()) return

        dataStore.edit { preferences ->
            val existingModels = preferences[RECENT_AGENT_MODELS]
                .orEmpty()
                .split(AGENT_MODELS_SEPARATOR)
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val updatedModels = listOf(trimmedModelId) + existingModels.filterNot { it == trimmedModelId }
            preferences[RECENT_AGENT_MODELS] = updatedModels.take(MAX_RECENT_AGENT_MODELS).joinToString(AGENT_MODELS_SEPARATOR)
        }
    }
}
