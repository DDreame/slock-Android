package com.slock.app.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Channel
import com.slock.app.data.model.Message
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelUiState(
    val channels: List<Channel> = emptyList(),
    val dms: List<Channel> = emptyList(),
    val channelPreviews: Map<String, Message> = emptyMap(),
    val serverId: String = "",
    val isLoading: Boolean = false,
    val isDmLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val messageRepository: MessageRepository,
    private val activeServerHolder: ActiveServerHolder,
    private val socketIOManager: SocketIOManager
) : ViewModel() {

    private val _state = MutableStateFlow(ChannelUiState())
    val state: StateFlow<ChannelUiState> = _state.asStateFlow()

    fun loadChannels(serverId: String) {
        activeServerHolder.serverId = serverId
        if (!socketIOManager.isConnected()) {
            socketIOManager.connect(serverId)
        }
        viewModelScope.launch {
            _state.update { it.copy(serverId = serverId, isLoading = it.channels.isEmpty()) }
            // Get cached data first
            channelRepository.getChannels(serverId).fold(
                onSuccess = { channels ->
                    _state.update { it.copy(channels = channels, isLoading = false) }
                    loadChannelPreviews(channels)
                },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
            // Then refresh from cloud
            channelRepository.refreshChannels(serverId).fold(
                onSuccess = { channels ->
                    _state.update { it.copy(channels = channels) }
                    loadChannelPreviews(channels)
                },
                onFailure = { /* keep cached data */ }
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

    fun loadDMs() {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDmLoading = it.dms.isEmpty()) }
            channelRepository.getDMs(serverId).fold(
                onSuccess = { dms -> _state.update { it.copy(dms = dms, isDmLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isDmLoading = false, error = err.message) } }
            )
        }
    }

    fun createDM(agentId: String? = null, userId: String? = null, onSuccess: (Channel) -> Unit, onError: (String) -> Unit = {}) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            channelRepository.createDM(serverId, agentId = agentId, userId = userId).fold(
                onSuccess = { dmChannel ->
                    _state.update { current ->
                        val exists = current.dms.any { it.id == dmChannel.id }
                        if (!exists) current.copy(dms = current.dms + dmChannel) else current
                    }
                    onSuccess(dmChannel)
                },
                onFailure = { err ->
                    _state.update { it.copy(error = err.message) }
                    onError(err.message ?: "Failed to create DM")
                }
            )
        }
    }

    private fun loadChannelPreviews(channels: List<Channel>) {
        viewModelScope.launch {
            val channelIds = channels.mapNotNull { it.id }
            if (channelIds.isEmpty()) return@launch

            // 1. Try Room cache first
            val cached = messageRepository.getLatestMessagePerChannel(channelIds)
            if (cached.isNotEmpty()) {
                _state.update { it.copy(channelPreviews = it.channelPreviews + cached) }
            }

            // 2. For channels missing from cache, fetch 1 message from API
            val missingIds = channelIds.filter { it !in cached }
            if (missingIds.isEmpty()) return@launch

            val serverId = _state.value.serverId
            val apiPreviews = mutableMapOf<String, Message>()
            for (channelId in missingIds) {
                messageRepository.refreshMessages(serverId, channelId, limit = 1).onSuccess { messages ->
                    messages.lastOrNull()?.let { apiPreviews[channelId] = it }
                }
            }
            if (apiPreviews.isNotEmpty()) {
                _state.update { it.copy(channelPreviews = it.channelPreviews + apiPreviews) }
            }
        }
    }
}
