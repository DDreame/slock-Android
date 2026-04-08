package com.slock.app.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Message
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.ThreadRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun loadThread(parentMessage: Message, threadChannelId: String) {
        _state.update { it.copy(parentMessage = parentMessage, threadChannelId = threadChannelId) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            threadRepository.getThreadReplies(activeServerHolder.serverId ?: "", threadChannelId).fold(
                onSuccess = { replies -> _state.update { it.copy(replies = replies, isLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
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
}
