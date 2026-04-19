package com.slock.app.data.store

import com.slock.app.data.socket.SocketIOManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelStore @Inject constructor(
    private val socketIOManager: SocketIOManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

    private val _currentChannelId = MutableStateFlow<String?>(null)
    val currentChannelId: StateFlow<String?> = _currentChannelId.asStateFlow()

    init {
        observeSocketEvents()
    }

    fun setUnreadCounts(counts: Map<String, Int>) {
        _unreadCounts.value = counts
    }

    fun incrementUnread(channelId: String) {
        if (channelId == currentChannelId.value) return
        _unreadCounts.value = _unreadCounts.value +
            (channelId to ((_unreadCounts.value[channelId] ?: 0) + 1))
    }

    fun clearUnread(channelId: String) {
        _unreadCounts.value = _unreadCounts.value - channelId
    }

    fun restoreUnread(channelId: String, count: Int) {
        _unreadCounts.value = _unreadCounts.value + (channelId to count)
    }

    fun setCurrentChannel(channelId: String?) {
        _currentChannelId.value = channelId
    }

    private fun observeSocketEvents() {
        scope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.MessageNew -> {
                        incrementUnread(event.data.channelId)
                    }
                    else -> { }
                }
            }
        }
    }
}
