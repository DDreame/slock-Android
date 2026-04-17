package com.slock.app.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Channel
import com.slock.app.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedChannelsUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val removingIds: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class SavedChannelsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(SavedChannelsUiState())
    val state: StateFlow<SavedChannelsUiState> = _state.asStateFlow()

    fun loadSavedChannels() {
        val serverId = activeServerHolder.serverId
        if (serverId.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = "Server not selected") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            channelRepository.getSavedChannels(serverId).fold(
                onSuccess = { channels ->
                    _state.update {
                        it.copy(
                            channels = channels,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load saved channels"
                        )
                    }
                }
            )
        }
    }

    fun removeSavedChannel(channelId: String) {
        val serverId = activeServerHolder.serverId
        if (serverId.isNullOrBlank() || channelId.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(removingIds = it.removingIds + channelId, error = null) }
            channelRepository.removeSavedChannel(serverId, channelId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            channels = it.channels.filterNot { channel -> channel.id == channelId },
                            removingIds = it.removingIds - channelId
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            removingIds = it.removingIds - channelId,
                            error = error.message ?: "Failed to remove saved channel"
                        )
                    }
                }
            )
        }
    }
}
