package com.slock.app.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.Member
import com.slock.app.data.model.User
import com.slock.app.data.repository.AuthRepository
import com.slock.app.data.repository.ServerRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val member: Member? = null,
    val isOwnProfile: Boolean = true,
    val isOnline: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
    val editName: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null
)

fun resolveProfileDisplayData(state: ProfileUiState): ProfileDisplayData {
    val name = state.member?.displayName
        ?: state.member?.name
        ?: state.user?.name
        ?: ""
    val email = state.user?.email ?: ""
    val role = state.member?.role ?: ""
    val avatar = state.user?.avatar
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    return ProfileDisplayData(
        name = name,
        email = email,
        role = role,
        avatar = avatar,
        initial = initial,
        isOnline = state.isOnline,
        isOwnProfile = state.isOwnProfile
    )
}

data class ProfileDisplayData(
    val name: String,
    val email: String,
    val role: String,
    val avatar: String?,
    val initial: String,
    val isOnline: Boolean,
    val isOwnProfile: Boolean
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val serverRepository: ServerRepository,
    private val activeServerHolder: ActiveServerHolder,
    private val presenceTracker: PresenceTracker,
    private val secureTokenStorage: SecureTokenStorage,
    private val socketIOManager: SocketIOManager
) : ViewModel() {

    private val targetUserId: String? = savedStateHandle["userId"]
    private val currentUserId: String? get() = secureTokenStorage.userId

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var socketJob: Job? = null

    init {
        loadProfile()
        observePresence()
    }

    private fun loadProfile() {
        val isOwnProfile = targetUserId == null || targetUserId == currentUserId
        _state.update { it.copy(isLoading = true, isOwnProfile = isOwnProfile) }

        viewModelScope.launch {
            if (isOwnProfile) {
                loadOwnProfile()
            } else {
                loadOtherProfile(targetUserId!!)
            }
        }
    }

    private suspend fun loadOwnProfile() {
        authRepository.getMe().fold(
            onSuccess = { user ->
                _state.update {
                    it.copy(
                        user = user,
                        isOnline = true,
                        isLoading = false
                    )
                }
                loadMemberRole(user.id)
            },
            onFailure = { err ->
                _state.update { it.copy(isLoading = false, error = err.message) }
            }
        )
    }

    private suspend fun loadOtherProfile(userId: String) {
        val serverId = activeServerHolder.serverId
        if (serverId.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = "Server not selected") }
            return
        }
        serverRepository.getServerMembers(serverId).fold(
            onSuccess = { members ->
                val member = members.find { it.userId == userId }
                val isOnline = presenceTracker.isOnline(userId)
                _state.update {
                    it.copy(
                        user = member?.user,
                        member = member,
                        isOnline = isOnline,
                        isLoading = false,
                        error = if (member == null) "User not found" else null
                    )
                }
            },
            onFailure = { err ->
                _state.update { it.copy(isLoading = false, error = err.message) }
            }
        )
    }

    private suspend fun loadMemberRole(userId: String?) {
        if (userId == null) return
        val serverId = activeServerHolder.serverId ?: return
        serverRepository.getServerMembers(serverId).fold(
            onSuccess = { members ->
                val member = members.find { it.userId == userId }
                _state.update { it.copy(member = member) }
            },
            onFailure = { }
        )
    }

    private fun observePresence() {
        socketJob = viewModelScope.launch {
            val trackId = targetUserId ?: currentUserId ?: return@launch
            presenceTracker.onlineIds.collect { onlineIds ->
                _state.update { it.copy(isOnline = trackId in onlineIds || it.isOwnProfile) }
            }
        }
    }

    fun startEditing() {
        val currentName = _state.value.user?.name ?: ""
        _state.update { it.copy(isEditing = true, editName = currentName, saveError = null) }
    }

    fun cancelEditing() {
        _state.update { it.copy(isEditing = false, editName = "", saveError = null) }
    }

    fun updateEditName(name: String) {
        _state.update { it.copy(editName = name) }
    }

    fun saveName() {
        val newName = _state.value.editName.trim()
        if (newName.isBlank()) {
            _state.update { it.copy(saveError = "Name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            authRepository.updateMe(newName).fold(
                onSuccess = { updatedUser ->
                    _state.update {
                        it.copy(
                            user = updatedUser,
                            isEditing = false,
                            editName = "",
                            isSaving = false
                        )
                    }
                },
                onFailure = { err ->
                    _state.update { it.copy(isSaving = false, saveError = err.message) }
                }
            )
        }
    }

    fun retry() = loadProfile()

    override fun onCleared() {
        super.onCleared()
        socketJob?.cancel()
    }
}
