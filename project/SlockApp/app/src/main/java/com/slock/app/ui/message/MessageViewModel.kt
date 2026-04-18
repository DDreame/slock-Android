package com.slock.app.ui.message

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.model.Attachment
import com.slock.app.data.model.Message
import com.slock.app.data.repository.ChannelRepository
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
    val sendError: String? = null,
    val replyingTo: Message? = null,
    val pendingAttachments: List<PendingAttachment> = emptyList(),
    val previewImageUrl: String? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchMatchIndices: List<Int> = emptyList(),
    val currentSearchMatchPosition: Int = -1,
    val currentSearchMatchMessageId: String? = null,
    val reactionOverridesByMessageId: Map<String, List<MessageReactionUiModel>> = emptyMap(),
    val onlineIds: Set<String> = emptySet(),
    val isCurrentChannelSaved: Boolean = false,
    val isSavedStatusLoading: Boolean = false,
    val savedChannelFeedbackMessage: String? = null
)

private fun saveChannelFailureMessage(isRemoving: Boolean, error: Throwable): String {
    val fallback = if (isRemoving) {
        "Failed to remove saved channel"
    } else {
        "Failed to save channel"
    }
    return error.message?.takeIf { it.isNotBlank() } ?: fallback
}

fun computeSearchMatches(state: MessageUiState): MessageUiState {
    if (!state.isSearchActive || state.searchQuery.isBlank()) return state
    val matches = state.messages.indices.filter { i ->
        state.messages[i].content.orEmpty().contains(state.searchQuery, ignoreCase = true)
    }
    val position = when {
        matches.isEmpty() -> -1
        state.currentSearchMatchMessageId != null -> {
            val newPos = matches.indexOfFirst { state.messages[it].id == state.currentSearchMatchMessageId }
            if (newPos >= 0) newPos else 0
        }
        state.currentSearchMatchPosition >= matches.size -> 0
        state.currentSearchMatchPosition < 0 -> 0
        else -> state.currentSearchMatchPosition
    }
    val messageId = if (position >= 0 && position < matches.size) {
        state.messages.getOrNull(matches[position])?.id
    } else null
    return state.copy(searchMatchIndices = matches, currentSearchMatchPosition = position, currentSearchMatchMessageId = messageId)
}

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val channelRepository: ChannelRepository,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder,
    private val presenceTracker: PresenceTracker
) : ViewModel() {

    private val _state = MutableStateFlow(MessageUiState())
    val state: StateFlow<MessageUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null
    private var presenceJob: Job? = null

    private fun recomputeSearchMatches(state: MessageUiState): MessageUiState =
        computeSearchMatches(state)

    init {
        observePresence()
        observeSocketEvents()
    }

    private fun observePresence() {
        presenceJob = viewModelScope.launch {
            presenceTracker.onlineIds.collect { onlineIds ->
                _state.update { it.copy(onlineIds = onlineIds) }
            }
        }
    }

    private fun observeSocketEvents() {
        socketEventsJob = viewModelScope.launch {
            try {
                socketIOManager.events.collect { event ->
                    when (event) {
                        is SocketIOManager.SocketEvent.UserPresence -> {
                            if (event.status == "online") {
                                presenceTracker.setOnline(event.userId)
                            } else {
                                presenceTracker.setOffline(event.userId)
                            }
                        }
                        is SocketIOManager.SocketEvent.AgentActivity -> {
                            presenceTracker.setOnline(event.data.agentId)
                        }
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
                                        recomputeSearchMatches(current.copy(messages = listOf(newMessage) + current.messages))
                                    } else current
                                }
                            }
                        }
                        is SocketIOManager.SocketEvent.MessageUpdated -> {
                            val data = event.data
                            if (data.channelId == _state.value.channelId) {
                                _state.update { current ->
                                    val updatedMessages = current.messages.map { message ->
                                        if (message.id == data.id) {
                                            message.copy(content = data.content.ifBlank { message.content })
                                        } else {
                                            message
                                        }
                                    }
                                    recomputeSearchMatches(current.copy(messages = updatedMessages))
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
            _state.update {
                it.copy(
                    channelId = channelId,
                    isLoading = false,
                    error = "Server not selected",
                    savedChannelFeedbackMessage = null,
                    isSavedStatusLoading = false,
                    isCurrentChannelSaved = false
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    channelId = channelId,
                    isLoading = it.messages.isEmpty(),
                    error = null,
                    savedChannelFeedbackMessage = null,
                    isSavedStatusLoading = true,
                    isCurrentChannelSaved = false
                )
            }

            refreshSavedChannelState(serverId, channelId)

            // Get cached data first
            messageRepository.getMessages(serverId, channelId).fold(
                onSuccess = { messages ->
                    _state.update {
                        recomputeSearchMatches(
                            it.copy(messages = messages.reversed(), isLoading = false, error = null)
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
            // Then refresh from cloud (cloud always wins)
            messageRepository.refreshMessages(serverId, channelId).fold(
                onSuccess = { messages ->
                    _state.update {
                        recomputeSearchMatches(
                            it.copy(messages = messages.reversed(), error = null)
                        )
                    }
                },
                onFailure = { /* keep cached data */ }
            )
            socketIOManager.joinChannel(channelId)
        }
    }

    fun toggleSavedChannel() {
        val serverId = activeServerHolder.serverId
        val channelId = _state.value.channelId
        if (serverId.isNullOrBlank() || channelId.isBlank() || _state.value.isSavedStatusLoading) return

        viewModelScope.launch {
            val currentlySaved = _state.value.isCurrentChannelSaved
            _state.update { it.copy(isSavedStatusLoading = true, savedChannelFeedbackMessage = null) }

            val result = if (currentlySaved) {
                channelRepository.removeSavedChannel(serverId, channelId)
            } else {
                channelRepository.saveChannel(serverId, channelId)
            }

            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isCurrentChannelSaved = !currentlySaved,
                            isSavedStatusLoading = false,
                            savedChannelFeedbackMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSavedStatusLoading = false,
                            savedChannelFeedbackMessage = saveChannelFailureMessage(currentlySaved, error)
                        )
                    }
                }
            )
        }
    }

    fun consumeSavedChannelFeedback() {
        _state.update { it.copy(savedChannelFeedbackMessage = null) }
    }

    private suspend fun refreshSavedChannelState(serverId: String, channelId: String) {
        channelRepository.isChannelSaved(serverId, channelId).fold(
            onSuccess = { isSaved ->
                _state.update {
                    it.copy(
                        isCurrentChannelSaved = isSaved,
                        isSavedStatusLoading = false
                    )
                }
            },
            onFailure = {
                _state.update { it.copy(isSavedStatusLoading = false) }
            }
        )
    }

    fun sendMessage(content: String) {
        if (_state.value.isSending) return
        val serverId = activeServerHolder.serverId
        if (serverId.isNullOrBlank()) {
            _state.update { it.copy(sendError = "Server not selected") }
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
            _state.update { recomputeSearchMatches(it.copy(isSending = true, isUploading = attachments.isNotEmpty(), sendError = null, replyingTo = null, pendingAttachments = emptyList(), messages = listOf(pendingMessage) + it.messages)) }

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
                    recomputeSearchMatches(current.copy(
                        messages = current.messages.filter { it.id != pendingId },
                        isSending = false,
                        sendError = "附件上传失败，消息未发送"
                    ))
                }
                return@launch
            }

            val finalAttachmentIds = attachmentIds.ifEmpty { null }
            messageRepository.sendMessage(serverId, _state.value.channelId, content, attachmentIds = finalAttachmentIds, parentMessageId = replyTo?.id).fold(
                onSuccess = { message ->
                    _state.update { current ->
                        val updated = current.messages.map { if (it.id == pendingId) message else it }
                        recomputeSearchMatches(current.copy(messages = updated, isSending = false))
                    }
                },
                onFailure = { error ->
                    _state.update { current ->
                        recomputeSearchMatches(current.copy(
                            messages = current.messages.filter { it.id != pendingId },
                            isSending = false,
                            sendError = error.message
                        ))
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

    fun dismissSendError() {
        _state.update { it.copy(sendError = null) }
    }

    fun retryLoadMessages() {
        val channelId = _state.value.channelId
        if (channelId.isNotBlank()) {
            loadMessages(channelId)
        }
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
                        recomputeSearchMatches(state.copy(
                            messages = state.messages + newMessages,
                            isLoadingMore = false,
                            hasMoreMessages = olderMessages.size >= 50
                        ))
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
                it.copy(isSearchActive = false, searchQuery = "", searchMatchIndices = emptyList(), currentSearchMatchPosition = -1, currentSearchMatchMessageId = null)
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
            val messageId = if (position >= 0) current.messages.getOrNull(matches[position])?.id else null
            current.copy(searchQuery = query, searchMatchIndices = matches, currentSearchMatchPosition = position, currentSearchMatchMessageId = messageId)
        }
    }

    fun nextSearchResult() {
        _state.update { current ->
            if (current.searchMatchIndices.isEmpty()) return@update current
            val next = (current.currentSearchMatchPosition + 1) % current.searchMatchIndices.size
            val messageId = current.messages.getOrNull(current.searchMatchIndices[next])?.id
            current.copy(currentSearchMatchPosition = next, currentSearchMatchMessageId = messageId)
        }
    }

    fun previousSearchResult() {
        _state.update { current ->
            if (current.searchMatchIndices.isEmpty()) return@update current
            val prev = if (current.currentSearchMatchPosition <= 0) current.searchMatchIndices.size - 1
                       else current.currentSearchMatchPosition - 1
            val messageId = current.messages.getOrNull(current.searchMatchIndices[prev])?.id
            current.copy(currentSearchMatchPosition = prev, currentSearchMatchMessageId = messageId)
        }
    }

    fun toggleReaction(message: Message, emoji: String) {
        if (message.id.isNullOrBlank()) return
        _state.update { current ->
            current.copy(
                reactionOverridesByMessageId = updateReactionOverrides(
                    currentOverrides = current.reactionOverridesByMessageId,
                    message = message,
                    emoji = emoji
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketEventsJob?.cancel()
        presenceJob?.cancel()
        val channelId = _state.value.channelId
        if (channelId.isNotBlank()) {
            socketIOManager.leaveChannel(channelId)
        }
    }
}
