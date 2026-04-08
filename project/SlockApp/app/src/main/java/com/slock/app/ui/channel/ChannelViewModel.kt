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

data class ChannelUiState(
    val channels: List<Channel> = emptyList(),
    val serverId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(ChannelUiState())
    val state: StateFlow<ChannelUiState> = _state.asStateFlow()

    fun loadChannels(serverId: String) {
        activeServerHolder.serverId = serverId
        viewModelScope.launch {
            _state.update { it.copy(serverId = serverId, isLoading = true) }
            channelRepository.getChannels(serverId).fold(
                onSuccess = { channels -> _state.update { it.copy(channels = channels, isLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun createChannel(name: String, type: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            channelRepository.createChannel(serverId, name, type).fold(
                onSuccess = { channel -> _state.update { it.copy(channels = it.channels + channel) } },
                onFailure = { err -> _state.update { it.copy(error = err.message) } }
            )
        }
    }
}
