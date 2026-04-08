package com.slock.app.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.model.Message
import com.slock.app.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessageUiState(
    val messages: List<Message> = emptyList(),
    val channelId: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(MessageUiState())
    val state: StateFlow<MessageUiState> = _state.asStateFlow()
    
    fun loadMessages(channelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(channelId = channelId, isLoading = true) }
            messageRepository.getMessages(channelId).fold(
                onSuccess = { messages -> 
                    _state.update { it.copy(messages = messages, isLoading = false) }
                },
                onFailure = { error -> 
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }
    
    fun sendMessage(content: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            messageRepository.sendMessage(_state.value.channelId, content).fold(
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
        // Pagination logic
    }
}
