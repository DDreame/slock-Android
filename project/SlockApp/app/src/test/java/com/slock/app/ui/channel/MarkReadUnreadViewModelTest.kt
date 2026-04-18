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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MarkReadUnreadViewModelTest {

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
        whenever(channelRepository.getUnreadChannels(any())).thenReturn(
            Result.success(mapOf("ch-1" to 5))
        )
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(messageRepository.refreshMessages(any(), any(), any())).thenReturn(Result.success(emptyList()))
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))
    }

    @Test
    fun `markAsRead clears unread count and calls API`() = runTest {
        stubDefaults()
        whenever(channelRepository.markChannelRead(any(), any(), any())).thenReturn(Result.success(Unit))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        assertEquals(5, viewModel.state.value.unreadCounts["ch-1"])

        viewModel.markAsRead("ch-1")
        advanceUntilIdle()

        assertNull(viewModel.state.value.unreadCounts["ch-1"])
        verify(channelRepository).markChannelRead("server-1", "ch-1", Long.MAX_VALUE)
    }

    @Test
    fun `markAsRead rolls back unread count on API failure`() = runTest {
        stubDefaults()
        whenever(channelRepository.markChannelRead(any(), any(), any())).thenReturn(
            Result.failure(Exception("network error"))
        )

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        assertEquals(5, viewModel.state.value.unreadCounts["ch-1"])

        viewModel.markAsRead("ch-1")
        advanceUntilIdle()

        assertEquals(5, viewModel.state.value.unreadCounts["ch-1"])
        assertEquals("Mark as read failed: network error", viewModel.state.value.actionFeedbackMessage)
    }

    @Test
    fun `markAsUnread refreshes unread counts from API`() = runTest {
        stubDefaults()
        whenever(channelRepository.markChannelRead(any(), any(), any())).thenReturn(Result.success(Unit))
        whenever(channelRepository.markChannelUnread(any(), any())).thenReturn(Result.success(Unit))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        viewModel.markAsRead("ch-1")
        advanceUntilIdle()
        assertNull(viewModel.state.value.unreadCounts["ch-1"])

        whenever(channelRepository.getUnreadChannels("server-1")).thenReturn(
            Result.success(mapOf("ch-1" to 3))
        )

        viewModel.markAsUnread("ch-1")
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.unreadCounts["ch-1"])
        verify(channelRepository).markChannelUnread("server-1", "ch-1")
    }

    @Test
    fun `markAsUnread sets feedback on API failure`() = runTest {
        stubDefaults()
        whenever(channelRepository.markChannelUnread(any(), any())).thenReturn(
            Result.failure(Exception("server error"))
        )

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        viewModel.markAsUnread("ch-1")
        advanceUntilIdle()

        assertEquals("Mark as unread failed: server error", viewModel.state.value.actionFeedbackMessage)
    }
}
