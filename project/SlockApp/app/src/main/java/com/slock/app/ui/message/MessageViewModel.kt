package com.slock.app.ui.message

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Attachment
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

data class PendingAttachment(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean = other is PendingAttachment && uri == other.uri
    override fun hashCode(): Int = uri.hashCode()
}

data class MessageUiState(
    val messages: List<Message> = emptyList(),
    val channelId: String = "",
    val channelName: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val isSending: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val replyingTo: Message? = null,
    val pendingAttachments: List<PendingAttachment> = emptyList(),
    val previewImageUrl: String? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchMatchIndices: List<Int> = emptyList(),
    val currentSearchMatchPosition: Int = -1
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
        val replyTo = _state.value.replyingTo
        val attachments = _state.value.pendingAttachments.toList()

        // Optimistic UI: add pending message immediately
        val pendingId = "pending-${System.currentTimeMillis()}"
        val pendingMessage = Message(
            id = pendingId,
            channelId = _state.value.channelId,
            content = content,
            senderName = "You",
            senderType = "user",
            createdAt = java.time.Instant.now().toString(),
            parentMessageId = replyTo?.id
        )
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, isUploading = attachments.isNotEmpty(), error = null, replyingTo = null, pendingAttachments = emptyList(), messages = listOf(pendingMessage) + it.messages) }

            // Upload attachments first
            val attachmentIds = mutableListOf<String>()
            var uploadFailCount = 0
            for (attachment in attachments) {
                messageRepository.uploadFile(serverId, attachment.name, attachment.mimeType, attachment.bytes).fold(
                    onSuccess = { uploadResponse ->
                        uploadResponse.id?.let { attachmentIds.add(it) }
                    },
                    onFailure = { uploadFailCount++ }
                )
            }
            _state.update { it.copy(isUploading = false) }

            if (uploadFailCount > 0 && attachmentIds.isEmpty() && content.isBlank()) {
                // All uploads failed and no text content — abort send
                _state.update { current ->
                    current.copy(
                        messages = current.messages.filter { it.id != pendingId },
                        isSending = false,
                        error = "图片上传失败，消息未发送"
                    )
                }
                return@launch
            }

            val finalAttachmentIds = attachmentIds.ifEmpty { null }
            messageRepository.sendMessage(serverId, _state.value.channelId, content, attachmentIds = finalAttachmentIds, parentMessageId = replyTo?.id).fold(
                onSuccess = { message ->
                    _state.update { current ->
                        val updated = current.messages.map { if (it.id == pendingId) message else it }
                        current.copy(messages = updated, isSending = false)
                    }
                },
                onFailure = { error ->
                    _state.update { current ->
                        current.copy(
                            messages = current.messages.filter { it.id != pendingId },
                            isSending = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun addAttachment(attachment: PendingAttachment) {
        _state.update { it.copy(pendingAttachments = it.pendingAttachments + attachment) }
    }

    fun removeAttachment(uri: Uri) {
        _state.update { it.copy(pendingAttachments = it.pendingAttachments.filter { a -> a.uri != uri }) }
    }

    fun showImagePreview(url: String) {
        _state.update { it.copy(previewImageUrl = url) }
    }

    fun dismissImagePreview() {
        _state.update { it.copy(previewImageUrl = null) }
    }

    fun setReplyTo(message: Message) {
        _state.update { it.copy(replyingTo = message) }
    }

    fun clearReplyTo() {
        _state.update { it.copy(replyingTo = null) }
    }

    fun loadMoreMessages() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMoreMessages) return
        val serverId = activeServerHolder.serverId
        if (serverId.isNullOrBlank()) return

        val oldestMessage = current.messages.lastOrNull() ?: return
        val beforeCursor = oldestMessage.seq.toString()

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            messageRepository.getMessages(serverId, current.channelId, limit = 50, before = beforeCursor).fold(
                onSuccess = { olderMessages ->
                    _state.update { state ->
                        val existingIds = state.messages.map { it.id }.toSet()
                        val newMessages = olderMessages.reversed().filter { it.id !in existingIds }
                        state.copy(
                            messages = state.messages + newMessages,
                            isLoadingMore = false,
                            hasMoreMessages = olderMessages.size >= 50
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    fun toggleSearch() {
        _state.update {
            if (it.isSearchActive) {
                it.copy(isSearchActive = false, searchQuery = "", searchMatchIndices = emptyList(), currentSearchMatchPosition = -1)
            } else {
                it.copy(isSearchActive = true)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { current ->
            val matches = if (query.isBlank()) {
                emptyList()
            } else {
                current.messages.indices.filter { i ->
                    current.messages[i].content.orEmpty().contains(query, ignoreCase = true)
                }
            }
            val position = if (matches.isNotEmpty()) 0 else -1
            current.copy(searchQuery = query, searchMatchIndices = matches, currentSearchMatchPosition = position)
        }
    }

    fun nextSearchResult() {
        _state.update { current ->
            if (current.searchMatchIndices.isEmpty()) return@update current
            val next = (current.currentSearchMatchPosition + 1) % current.searchMatchIndices.size
            current.copy(currentSearchMatchPosition = next)
        }
    }

    fun previousSearchResult() {
        _state.update { current ->
            if (current.searchMatchIndices.isEmpty()) return@update current
            val prev = if (current.currentSearchMatchPosition <= 0) current.searchMatchIndices.size - 1
                       else current.currentSearchMatchPosition - 1
            current.copy(currentSearchMatchPosition = prev)
        }
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
