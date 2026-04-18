package com.slock.app.ui.message

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DmRetryRecoveryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository: MessageRepository = mock()
    private val channelRepository: ChannelRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val activeServerHolder: ActiveServerHolder = mock()
    private val presenceTracker = PresenceTracker()

    private fun createViewModel(): MessageViewModel {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        return MessageViewModel(
            messageRepository, channelRepository,
            socketIOManager, activeServerHolder, presenceTracker
        )
    }

    @Test
    fun `loadMessages with null serverId sets Server not selected error`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn(null)

        val vm = createViewModel()
        vm.loadMessages("dm-channel-1")
        advanceUntilIdle()

        assertEquals("Server not selected", vm.state.value.error)
        assertEquals("dm-channel-1", vm.state.value.channelId)
    }

    @Test
    fun `retryLoadMessages resolves serverId from channel cache when null`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn(null)
        whenever(channelRepository.getServerIdForChannel("dm-channel-1")).thenReturn("srv-correct")

        val vm = createViewModel()
        vm.loadMessages("dm-channel-1")
        advanceUntilIdle()

        assertEquals("Server not selected", vm.state.value.error)

        whenever(activeServerHolder.serverId).thenReturn("srv-correct")
        whenever(channelRepository.isChannelSaved("srv-correct", "dm-channel-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-correct", "dm-channel-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("srv-correct", "dm-channel-1", 50))
            .thenReturn(Result.success(emptyList()))

        vm.retryLoadMessages()
        advanceUntilIdle()

        verify(channelRepository).getServerIdForChannel("dm-channel-1")
        assertNull("Error should be cleared after successful retry", vm.state.value.error)
    }

    @Test
    fun `retryLoadMessages resolves correct server in multi-server scenario`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn(null)
        whenever(channelRepository.getServerIdForChannel("dm-on-srv2")).thenReturn("srv-2")

        val vm = createViewModel()
        vm.loadMessages("dm-on-srv2")
        advanceUntilIdle()

        assertEquals("Server not selected", vm.state.value.error)

        whenever(activeServerHolder.serverId).thenReturn("srv-2")
        whenever(channelRepository.isChannelSaved("srv-2", "dm-on-srv2")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-2", "dm-on-srv2", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("srv-2", "dm-on-srv2", 50))
            .thenReturn(Result.success(emptyList()))

        vm.retryLoadMessages()
        advanceUntilIdle()

        verify(channelRepository).getServerIdForChannel("dm-on-srv2")
        assertNull("Error should be cleared with correct server from cache", vm.state.value.error)
    }

    @Test
    fun `retryLoadMessages with serverId already set skips channel lookup`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.failure(Exception("network error")))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50))
            .thenReturn(Result.success(emptyList()))

        val vm = createViewModel()
        vm.loadMessages("ch-1")
        advanceUntilIdle()

        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))

        vm.retryLoadMessages()
        advanceUntilIdle()

        assertNull("Error should be cleared after retry with existing serverId", vm.state.value.error)
    }

    @Test
    fun `retryLoadMessages with channel not in cache still shows controlled error`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn(null)
        whenever(channelRepository.getServerIdForChannel("unknown-ch")).thenReturn(null)

        val vm = createViewModel()
        vm.loadMessages("unknown-ch")
        advanceUntilIdle()

        vm.retryLoadMessages()
        advanceUntilIdle()

        verify(channelRepository).getServerIdForChannel("unknown-ch")
        assertEquals(
            "Should still show error when channel not in cache",
            "Server not selected",
            vm.state.value.error
        )
    }
}
