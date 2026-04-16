package com.slock.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val resetEmailSent: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val secureTokenStorage: SecureTokenStorage
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val authExpired: SharedFlow<Unit> = secureTokenStorage.authExpired

    init {
        checkExistingSession()
    }

    fun checkExistingSession() {
        viewModelScope.launch {
            authRepository.isLoggedIn().collect { loggedIn ->
                _state.update { it.copy(isLoggedIn = loggedIn, isCheckingSession = false) }
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name, error = null) }
    fun onEmailChange(email: String) = _state.update { it.copy(email = email, error = null) }
    fun onPasswordChange(password: String) = _state.update { it.copy(password = password, error = null) }
    
    fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.login(_state.value.email, _state.value.password).fold(
                onSuccess = { _state.update { it.copy(isLoading = false, isLoggedIn = true) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message ?: "Login failed") } }
            )
        }
    }
    
    fun register() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.register(_state.value.email, _state.value.password, _state.value.name).fold(
                onSuccess = { _state.update { it.copy(isLoading = false, isLoggedIn = true) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message ?: "Register failed") } }
            )
        }
    }
    
    fun forgotPassword() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.forgotPassword(_state.value.email).fold(
                onSuccess = { _state.update { it.copy(isLoading = false, resetEmailSent = true) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            _state.update { it.copy(isLoggedIn = false) }
            onComplete()
        }
    }
}
