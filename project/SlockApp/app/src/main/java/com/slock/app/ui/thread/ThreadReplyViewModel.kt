package com.slock.app.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Message
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.ThreadRepository
import com.slock.app.data.api.ApiService
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
    val isLoadingMore: Boolean = false,
    val hasMoreReplies: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ThreadReplyViewModel @Inject constructor(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val apiService: ApiService,
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

            // Try wrapped format first: GET /messages/channel/{id} → { messages: [...] }
            var replies: List<Message>? = null
            try {
                val response = apiService.getMessages(threadChannelId)
                if (response.isSuccessful && response.body() != null) {
                    val msgs = response.body()!!.messages
                    if (msgs.isNotEmpty()) {
                        replies = msgs
                        Log.d("ThreadReplyVM", "getMessages wrapped: ${msgs.size} replies")
                    }
                }
            } catch (e: Exception) {
                Log.w("ThreadReplyVM", "getMessages wrapped failed", e)
            }

            // Fallback: response may be plain array (JS does: it.messages ?? it)
            if (replies == null) {
                try {
                    val response = apiService.getMessagesRaw(threadChannelId)
                    if (response.isSuccessful && response.body() != null) {
                        replies = response.body()!!
                        Log.d("ThreadReplyVM", "getMessagesRaw array: ${replies.size} replies")
                    }
                } catch (e: Exception) {
                    Log.w("ThreadReplyVM", "getMessagesRaw failed", e)
                }
            }

            // Final fallback: thread-specific endpoint
            if (replies == null) {
                threadRepository.getThreadReplies(serverId, threadChannelId).fold(
                    onSuccess = { r ->
                        replies = r
                        Log.d("ThreadReplyVM", "getThreadReplies: ${r.size} replies")
                    },
                    onFailure = { err ->
                        Log.e("ThreadReplyVM", "All endpoints failed", err)
                    }
                )
            }

            _state.update { it.copy(replies = replies ?: emptyList(), isLoading = false, hasMoreReplies = (replies?.size ?: 0) >= 50, error = if (replies == null) "Failed to load replies" else null) }
            socketManager.joinChannel(threadChannelId)
        }
    }

    fun sendReply(content: String) {
        if (_state.value.isSending) return
        val pendingId = "pending-${System.currentTimeMillis()}"
        val pendingMessage = Message(
            id = pendingId,
            channelId = _state.value.threadChannelId,
            content = content,
            senderName = "You",
            senderType = "user",
            createdAt = java.time.Instant.now().toString()
        )
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, replies = it.replies + pendingMessage) }
            messageRepository.sendMessage(activeServerHolder.serverId ?: "", _state.value.threadChannelId, content).fold(
                onSuccess = { message ->
                    _state.update { current ->
                        val updated = current.replies.map { if (it.id == pendingId) message else it }
                        current.copy(replies = updated, isSending = false)
                    }
                },
                onFailure = { err ->
                    _state.update { current ->
                        current.copy(
                            replies = current.replies.filter { it.id != pendingId },
                            isSending = false,
                            error = err.message
                        )
                    }
                }
            )
        }
    }

    fun loadMoreReplies() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMoreReplies) return
        val serverId = activeServerHolder.serverId ?: return
        val oldestReply = current.replies.firstOrNull() ?: return
        val beforeCursor = oldestReply.seq.toString()

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            threadRepository.getThreadReplies(serverId, current.threadChannelId, limit = 50, before = beforeCursor).fold(
                onSuccess = { olderReplies ->
                    _state.update { state ->
                        val existingIds = state.replies.map { it.id }.toSet()
                        val newReplies = olderReplies.filter { it.id !in existingIds }
                        state.copy(
                            replies = newReplies + state.replies,
                            isLoadingMore = false,
                            hasMoreReplies = olderReplies.size >= 50
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    fun followThread() {
        val serverId = activeServerHolder.serverId ?: return
        val parentMessageId = _state.value.parentMessage?.id ?: return
        viewModelScope.launch {
            threadRepository.followThread(serverId, parentMessageId)
        }
    }

    fun unfollowThread() {
        val serverId = activeServerHolder.serverId ?: return
        val threadChannelId = _state.value.threadChannelId
        if (threadChannelId.isBlank()) return
        viewModelScope.launch {
            threadRepository.unfollowThread(serverId, threadChannelId)
        }
    }

    fun markThreadDone() {
        val serverId = activeServerHolder.serverId ?: return
        val threadChannelId = _state.value.threadChannelId
        if (threadChannelId.isBlank()) return
        viewModelScope.launch {
            threadRepository.markThreadDone(serverId, threadChannelId)
        }
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
