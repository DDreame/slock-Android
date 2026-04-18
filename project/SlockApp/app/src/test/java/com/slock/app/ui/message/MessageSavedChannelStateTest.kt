package com.slock.app.ui.message

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MessageSavedChannelStateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository: MessageRepository = mock()
    private val channelRepository: ChannelRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val activeServerHolder: ActiveServerHolder = mock()
    private val presenceTracker = PresenceTracker()

    @Before
    fun stubCacheFreshness() = runBlocking {
        whenever(messageRepository.isCachedMessagesFresh(any(), any())).thenReturn(false)
    }

    @Test
    fun loadMessages_exposesSavedChannelStatusFromRepository() = runTest {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(activeServerHolder.serverId).thenReturn("server-1")
        whenever(channelRepository.isChannelSaved("server-1", "channel-1")).thenReturn(Result.success(true))
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("server-1", "channel-1", 50))
            .thenReturn(Result.success(emptyList()))

        val viewModel = MessageViewModel(messageRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)

        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isCurrentChannelSaved)
        assertFalse(viewModel.state.value.isSavedStatusLoading)
        verify(channelRepository).isChannelSaved("server-1", "channel-1")
    }

    @Test
    fun toggleSavedChannel_savesWhenChannelIsNotSaved() = runTest {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(activeServerHolder.serverId).thenReturn("server-1")
        whenever(channelRepository.isChannelSaved("server-1", "channel-1")).thenReturn(Result.success(false))
        whenever(channelRepository.saveChannel("server-1", "channel-1")).thenReturn(Result.success(Unit))
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("server-1", "channel-1", 50))
            .thenReturn(Result.success(emptyList()))

        val viewModel = MessageViewModel(messageRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        viewModel.toggleSavedChannel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isCurrentChannelSaved)
        verify(channelRepository).saveChannel("server-1", "channel-1")
    }

    @Test
    fun toggleSavedChannel_removesWhenChannelIsAlreadySaved() = runTest {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(activeServerHolder.serverId).thenReturn("server-1")
        whenever(channelRepository.isChannelSaved("server-1", "channel-1")).thenReturn(Result.success(true))
        whenever(channelRepository.removeSavedChannel("server-1", "channel-1")).thenReturn(Result.success(Unit))
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("server-1", "channel-1", 50))
            .thenReturn(Result.success(emptyList()))

        val viewModel = MessageViewModel(messageRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        viewModel.toggleSavedChannel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isCurrentChannelSaved)
        verify(channelRepository).removeSavedChannel("server-1", "channel-1")
    }

    @Test
    fun toggleSavedChannel_exposesFeedbackWhenSaveFails() = runTest {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(activeServerHolder.serverId).thenReturn("server-1")
        whenever(channelRepository.isChannelSaved("server-1", "channel-1")).thenReturn(Result.success(false))
        whenever(channelRepository.saveChannel("server-1", "channel-1"))
            .thenReturn(Result.failure(IllegalStateException("save failed")))
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("server-1", "channel-1", 50))
            .thenReturn(Result.success(emptyList()))

        val viewModel = MessageViewModel(messageRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        viewModel.toggleSavedChannel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isCurrentChannelSaved)
        assertFalse(viewModel.state.value.isSavedStatusLoading)
        assertEquals("save failed", viewModel.state.value.savedChannelFeedbackMessage)

        viewModel.consumeSavedChannelFeedback()

        assertEquals(null, viewModel.state.value.savedChannelFeedbackMessage)
        verify(channelRepository).saveChannel("server-1", "channel-1")
    }

    @Test
    fun toggleSavedChannel_exposesFeedbackWhenRemoveFails() = runTest {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(activeServerHolder.serverId).thenReturn("server-1")
        whenever(channelRepository.isChannelSaved("server-1", "channel-1")).thenReturn(Result.success(true))
        whenever(channelRepository.removeSavedChannel("server-1", "channel-1"))
            .thenReturn(Result.failure(IllegalStateException("remove failed")))
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("server-1", "channel-1", 50))
            .thenReturn(Result.success(emptyList()))

        val viewModel = MessageViewModel(messageRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        viewModel.toggleSavedChannel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isCurrentChannelSaved)
        assertFalse(viewModel.state.value.isSavedStatusLoading)
        assertEquals("remove failed", viewModel.state.value.savedChannelFeedbackMessage)

        viewModel.consumeSavedChannelFeedback()

        assertEquals(null, viewModel.state.value.savedChannelFeedbackMessage)
        verify(channelRepository).removeSavedChannel("server-1", "channel-1")
    }
}
