package com.slock.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.NotificationPreference
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.local.SettingsPreferencesStore
import com.slock.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val notificationPreference: NotificationPreference = NotificationPreference.ALL_MESSAGES,
    val userName: String = "",
    val userEmail: String = "",
    val userId: String = "",
    val isRefreshingAccount: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val secureTokenStorage: SecureTokenStorage,
    private val settingsPreferencesStore: SettingsPreferencesStore
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            userName = secureTokenStorage.userName.orEmpty(),
            userId = secureTokenStorage.userId.orEmpty()
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        observeSettings()
        refreshAccount(showLoading = false)
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsPreferencesStore.notificationPreferenceFlow.collect { preference ->
                _state.update { it.copy(notificationPreference = preference) }
            }
        }
    }

    fun updateNotificationPreference(preference: NotificationPreference) {
        viewModelScope.launch {
            settingsPreferencesStore.setNotificationPreference(preference)
        }
    }

    fun refreshAccount(showLoading: Boolean = true) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingAccount = showLoading, error = null) }
            authRepository.getMe().fold(
                onSuccess = { user ->
                    _state.update {
                        it.copy(
                            userName = user.name.orEmpty(),
                            userEmail = user.email.orEmpty(),
                            userId = user.id.orEmpty(),
                            isRefreshingAccount = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRefreshingAccount = false,
                            error = error.message ?: "Failed to load account"
                        )
                    }
                }
            )
        }
    }
}
