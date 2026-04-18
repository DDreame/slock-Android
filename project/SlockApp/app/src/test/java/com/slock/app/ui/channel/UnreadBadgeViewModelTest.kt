package com.slock.app.ui.channel

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.Channel
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class UnreadBadgeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val channelRepository: ChannelRepository = mock()
    private val messageRepository: MessageRepository = mock()
    private val agentRepository: AgentRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val secureTokenStorage: SecureTokenStorage = mock()
    private val activeServerHolder = ActiveServerHolder(secureTokenStorage)
    private val presenceTracker = PresenceTracker()

    private fun createViewModel(): ChannelViewModel {
        return ChannelViewModel(
            channelRepository = channelRepository,
            messageRepository = messageRepository,
            agentRepository = agentRepository,
            activeServerHolder = activeServerHolder,
            socketIOManager = socketIOManager,
            presenceTracker = presenceTracker
        )
    }

    private suspend fun stubDefaults() {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(channelRepository.getChannels(any())).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "General", type = "text")))
        )
        whenever(channelRepository.refreshChannels(any())).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "General", type = "text")))
        )
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))
    }

    @Test
    fun `loadChannels fetches unread counts from API`() = runTest {
        stubDefaults()
        whenever(channelRepository.getUnreadChannels("server-1")).thenReturn(
            Result.success(mapOf("ch-1" to 5))
        )

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        assertEquals(5, viewModel.state.value.unreadCounts["ch-1"])
    }

    @Test
    fun `loadChannels keeps empty counts when API fails`() = runTest {
        stubDefaults()
        whenever(channelRepository.getUnreadChannels(any())).thenReturn(
            Result.failure(Exception("network error"))
        )

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        assertEquals(emptyMap<String, Int>(), viewModel.state.value.unreadCounts)
    }

    @Test
    fun `messageNew increments unread count for non-current channel`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(channelRepository.getChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "General", type = "text")))
        )
        whenever(channelRepository.refreshChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "General", type = "text")))
        )
        whenever(channelRepository.getUnreadChannels(any())).thenReturn(Result.success(emptyMap()))
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.MessageNew(
                SocketIOManager.MessageNewData(
                    id = "msg-1",
                    channelId = "ch-1",
                    content = "hello",
                    senderId = "u-1",
                    senderName = "User",
                    senderType = "user",
                    seq = 1,
                    createdAt = "2026-04-17T00:00:00Z"
                )
            )
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.unreadCounts["ch-1"])
    }

    @Test
    fun `messageNew does not increment for current channel`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(channelRepository.getChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "General", type = "text")))
        )
        whenever(channelRepository.refreshChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "General", type = "text")))
        )
        whenever(channelRepository.getUnreadChannels(any())).thenReturn(Result.success(emptyMap()))
        whenever(channelRepository.getChannelMembers(any(), any())).thenReturn(Result.success(emptyList()))
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        viewModel.clearUnreadCount("ch-1")
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.MessageNew(
                SocketIOManager.MessageNewData(
                    id = "msg-1",
                    channelId = "ch-1",
                    content = "hello",
                    senderId = "u-1",
                    senderName = "User",
                    senderType = "user",
                    seq = 1,
                    createdAt = "2026-04-17T00:00:00Z"
                )
            )
        )
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.unreadCounts["ch-1"])
    }

    @Test
    fun `clearUnreadCount removes count and calls markChannelRead`() = runTest {
        stubDefaults()
        whenever(channelRepository.getUnreadChannels("server-1")).thenReturn(
            Result.success(mapOf("ch-1" to 5))
        )
        whenever(channelRepository.markChannelRead(any(), any(), any())).thenReturn(Result.success(Unit))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        assertEquals(5, viewModel.state.value.unreadCounts["ch-1"])

        viewModel.clearUnreadCount("ch-1")
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.unreadCounts["ch-1"])
        verify(channelRepository).markChannelRead("server-1", "ch-1", Long.MAX_VALUE)
    }

    @Test
    fun `multiple messageNew events accumulate unread count`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(channelRepository.getChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "General", type = "text")))
        )
        whenever(channelRepository.refreshChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "General", type = "text")))
        )
        whenever(channelRepository.getUnreadChannels(any())).thenReturn(
            Result.success(mapOf("ch-1" to 2))
        )
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        repeat(3) { i ->
            events.emit(
                SocketIOManager.SocketEvent.MessageNew(
                    SocketIOManager.MessageNewData(
                        id = "msg-$i",
                        channelId = "ch-1",
                        content = "msg $i",
                        senderId = "u-1",
                        senderName = "User",
                        senderType = "user",
                        seq = (i + 1).toLong(),
                        createdAt = "2026-04-17T00:00:00Z"
                    )
                )
            )
            advanceUntilIdle()
        }

        assertEquals(5, viewModel.state.value.unreadCounts["ch-1"])
    }
}
