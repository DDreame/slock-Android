package com.slock.app.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Message
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessageUiState(
    val messages: List<Message> = emptyList(),
    val channelId: String = "",
    val channelName: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(MessageUiState())
    val state: StateFlow<MessageUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        socketEventsJob = viewModelScope.launch {
            try {
                socketIOManager.events.collect { event ->
                    when (event) {
                        is SocketIOManager.SocketEvent.MessageNew -> {
                            val data = event.data
                            if (data.channelId == _state.value.channelId) {
                                _state.update { current ->
                                    val newMessage = Message(
                                        id = data.id,
                                        channelId = data.channelId,
                                        content = data.content,
                                        senderId = data.senderId,
                                        senderName = data.senderName,
                                        senderType = data.senderType,
                                        createdAt = data.createdAt
                                    )
                                    val exists = current.messages.any { it.id == newMessage.id }
                                    if (!exists) {
                                        current.copy(messages = listOf(newMessage) + current.messages)
                                    } else current
                                }
                            }
                        }
                        is SocketIOManager.SocketEvent.ThreadUpdated -> {
                            val parentChannelId = event.data.parentChannelId
                            if (parentChannelId == _state.value.channelId) {
                                loadMessages(_state.value.channelId)
                            }
                        }
                        else -> { /* ignore */ }
                    }
                }
            } catch (e: Exception) {
                // Prevent socket errors from crashing the app
            }
        }
    }

    fun loadMessages(channelId: String) {
        val serverId = activeServerHolder.serverId
        if (serverId.isNullOrBlank()) {
            _state.update { it.copy(channelId = channelId, isLoading = false, error = "Server not selected") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(channelId = channelId, isLoading = it.messages.isEmpty(), error = null) }
            // Get cached data first
            messageRepository.getMessages(serverId, channelId).fold(
                onSuccess = { messages ->
                    _state.update { it.copy(messages = messages.reversed(), isLoading = false) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
            // Then refresh from cloud (cloud always wins)
            messageRepository.refreshMessages(serverId, channelId).fold(
                onSuccess = { messages ->
                    _state.update { it.copy(messages = messages.reversed()) }
                },
                onFailure = { /* keep cached data */ }
            )
            socketIOManager.joinChannel(channelId)
        }
    }

    fun sendMessage(content: String) {
        if (_state.value.isSending) return
        val serverId = activeServerHolder.serverId
        if (serverId.isNullOrBlank()) {
            _state.update { it.copy(error = "Server not selected") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            messageRepository.sendMessage(serverId, _state.value.channelId, content).fold(
                onSuccess = { message ->
                    _state.update { it.copy(messages = listOf(message) + it.messages, isSending = false) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isSending = false, error = error.message) }
                }
            )
        }
    }

    fun loadMoreMessages() {
        // Pagination logic - could be implemented with before cursor
    }

    override fun onCleared() {
        super.onCleared()
        socketEventsJob?.cancel()
        val channelId = _state.value.channelId
        if (channelId.isNotBlank()) {
            socketIOManager.leaveChannel(channelId)
        }
    }
}
