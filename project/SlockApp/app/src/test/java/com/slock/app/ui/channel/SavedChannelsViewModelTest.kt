package com.slock.app.ui.channel

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Channel
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SavedChannelsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val channelRepository: ChannelRepository = mock()
    private val activeServerHolder: ActiveServerHolder = mock()

    @Test
    fun loadSavedChannels_exposesRepositoryChannels() = runTest {
        val channels = listOf(
            Channel(id = "ch-1", name = "general", type = "text"),
            Channel(id = "ch-2", name = "announcements", type = "text")
        )
        whenever(activeServerHolder.serverId).thenReturn("server-1")
        whenever(channelRepository.getSavedChannels("server-1")).thenReturn(Result.success(channels))

        val viewModel = SavedChannelsViewModel(channelRepository, activeServerHolder)
        viewModel.loadSavedChannels()
        advanceUntilIdle()

        assertEquals(channels, viewModel.state.value.channels)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun removeSavedChannel_updatesVisibleList() = runTest {
        val channels = listOf(
            Channel(id = "ch-1", name = "general", type = "text"),
            Channel(id = "ch-2", name = "announcements", type = "text")
        )
        whenever(activeServerHolder.serverId).thenReturn("server-1")
        whenever(channelRepository.getSavedChannels("server-1")).thenReturn(Result.success(channels))
        whenever(channelRepository.removeSavedChannel("server-1", "ch-1")).thenReturn(Result.success(Unit))

        val viewModel = SavedChannelsViewModel(channelRepository, activeServerHolder)
        viewModel.loadSavedChannels()
        advanceUntilIdle()

        viewModel.removeSavedChannel("ch-1")
        advanceUntilIdle()

        assertEquals(listOf(channels[1]), viewModel.state.value.channels)
        assertTrue(viewModel.state.value.removingIds.isEmpty())
        verify(channelRepository).removeSavedChannel("server-1", "ch-1")
    }
}
