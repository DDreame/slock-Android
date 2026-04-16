package com.slock.app.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Message
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.ThreadRepository
import com.slock.app.data.socket.SocketIOManager
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThreadReplyUiState(
    val parentMessage: Message? = null,
    val replies: List<Message> = emptyList(),
    val threadChannelId: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ThreadReplyViewModel @Inject constructor(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val socketManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(ThreadReplyUiState())
    val state: StateFlow<ThreadReplyUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        socketEventsJob = viewModelScope.launch {
            try {
                socketManager.events.collect { event ->
                    when (event) {
                        is SocketIOManager.SocketEvent.MessageNew -> {
                            val data = event.data
                            if (data.channelId == _state.value.threadChannelId) {
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
                                    val exists = current.replies.any { it.id == newMessage.id }
                                    if (!exists) {
                                        current.copy(replies = current.replies + newMessage)
                                    } else current
                                }
                            }
                        }
                        else -> { /* ignore */ }
                    }
                }
            } catch (e: Exception) {
                // Prevent socket errors from crashing
            }
        }
    }

    fun loadThread(parentMessage: Message, threadChannelId: String) {
        _state.update { it.copy(parentMessage = parentMessage, threadChannelId = threadChannelId) }
        val serverId = activeServerHolder.serverId ?: ""
        Log.d("ThreadReplyVM", "loadThread: threadChannelId=$threadChannelId, serverId=$serverId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.replies.isEmpty()) }

            // Primary: use standard messages endpoint (matches JS frontend: GET /messages/channel/${threadChannelId})
            messageRepository.refreshMessages(serverId, threadChannelId).fold(
                onSuccess = { replies ->
                    Log.d("ThreadReplyVM", "refreshMessages success: ${replies.size} replies")
                    _state.update { it.copy(replies = replies, isLoading = false) }
                },
                onFailure = { err ->
                    Log.e("ThreadReplyVM", "refreshMessages failed, trying thread endpoint", err)
                    // Fallback: use thread-specific endpoint (GET /threads/{threadId}/messages)
                    threadRepository.getThreadReplies(serverId, threadChannelId).fold(
                        onSuccess = { replies ->
                            Log.d("ThreadReplyVM", "getThreadReplies success: ${replies.size} replies")
                            _state.update { it.copy(replies = replies, isLoading = false) }
                        },
                        onFailure = { err2 ->
                            Log.e("ThreadReplyVM", "getThreadReplies also failed", err2)
                            _state.update { it.copy(isLoading = false, error = err2.message) }
                        }
                    )
                }
            )
            socketManager.joinChannel(threadChannelId)
        }
    }

    fun sendReply(content: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            messageRepository.sendMessage(activeServerHolder.serverId ?: "", _state.value.threadChannelId, content).fold(
                onSuccess = { message -> _state.update { it.copy(replies = it.replies + message, isSending = false) } },
                onFailure = { err -> _state.update { it.copy(isSending = false, error = err.message) } }
            )
        }
    }

    fun loadMoreReplies() {
        // Pagination
    }

    override fun onCleared() {
        super.onCleared()
        socketEventsJob?.cancel()
        val threadChannelId = _state.value.threadChannelId
        if (threadChannelId.isNotBlank()) {
            socketManager.leaveChannel(threadChannelId)
        }
    }
}
