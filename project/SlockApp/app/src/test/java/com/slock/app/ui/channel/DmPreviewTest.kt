package com.slock.app.ui.channel

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.Channel
import com.slock.app.data.model.Message
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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DmPreviewTest {

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
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
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
        whenever(channelRepository.getChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.refreshChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.getUnreadChannels(any())).thenReturn(Result.success(emptyMap()))
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))
        whenever(messageRepository.getLatestMessagePerChannel(emptyList())).thenReturn(emptyMap())
        whenever(messageRepository.refreshMessages(any(), any(), any())).thenReturn(Result.success(emptyList()))
    }

    @Test
    fun `loadDMs populates channelPreviews from cached messages`() = runTest {
        stubDefaults()
        whenever(channelRepository.getDMs("srv-1")).thenReturn(
            Result.success(listOf(
                Channel(id = "dm-1", name = "Alice", type = "dm"),
                Channel(id = "dm-2", name = "Bob", type = "dm")
            ))
        )
        whenever(messageRepository.getLatestMessagePerChannel(listOf("dm-1", "dm-2"))).thenReturn(
            mapOf("dm-1" to Message(id = "msg-1", channelId = "dm-1", content = "Hey there"))
        )

        val vm = createViewModel()
        vm.loadChannels("srv-1")
        vm.loadDMs()
        advanceUntilIdle()

        val preview = vm.state.value.channelPreviews["dm-1"]
        assertNotNull(preview)
        assertEquals("Hey there", preview?.content)
    }

    @Test
    fun `loadDMs fetches API preview for DMs not in cache`() = runTest {
        stubDefaults()
        whenever(channelRepository.getDMs("srv-1")).thenReturn(
            Result.success(listOf(Channel(id = "dm-1", name = "Alice", type = "dm")))
        )
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(messageRepository.refreshMessages("srv-1", "dm-1", 1)).thenReturn(
            Result.success(listOf(Message(id = "msg-api", channelId = "dm-1", content = "From API")))
        )

        val vm = createViewModel()
        vm.loadChannels("srv-1")
        vm.loadDMs()
        advanceUntilIdle()

        val preview = vm.state.value.channelPreviews["dm-1"]
        assertNotNull(preview)
        assertEquals("From API", preview?.content)
    }

    @Test
    fun `socket messageNew updates DM preview`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.getChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.refreshChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.getUnreadChannels(any())).thenReturn(Result.success(emptyMap()))
        whenever(channelRepository.getDMs("srv-1")).thenReturn(
            Result.success(listOf(Channel(id = "dm-1", name = "Alice", type = "dm")))
        )
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(messageRepository.refreshMessages(any(), any(), any())).thenReturn(Result.success(emptyList()))

        val vm = ChannelViewModel(
            channelRepository = channelRepository,
            messageRepository = messageRepository,
            agentRepository = agentRepository,
            activeServerHolder = activeServerHolder,
            socketIOManager = socketIOManager,
            presenceTracker = presenceTracker
        )
        vm.loadChannels("srv-1")
        vm.loadDMs()
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.MessageNew(
                SocketIOManager.MessageNewData(
                    id = "msg-rt",
                    channelId = "dm-1",
                    content = "Real-time hello",
                    senderId = "user-1",
                    senderName = "Alice",
                    senderType = "user",
                    seq = 1,
                    createdAt = "2026-04-18T00:00:00Z"
                )
            )
        )
        advanceUntilIdle()

        val preview = vm.state.value.channelPreviews["dm-1"]
        assertNotNull(preview)
        assertEquals("Real-time hello", preview?.content)
        assertEquals("Alice", preview?.senderName)
    }

    @Test
    fun `DM preview not lost when channels also loaded`() = runTest {
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.getUnreadChannels(any())).thenReturn(Result.success(emptyMap()))
        whenever(channelRepository.getChannels("srv-1")).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "general", type = "text")))
        )
        whenever(channelRepository.refreshChannels("srv-1")).thenReturn(
            Result.success(listOf(Channel(id = "ch-1", name = "general", type = "text")))
        )
        whenever(channelRepository.getDMs("srv-1")).thenReturn(
            Result.success(listOf(Channel(id = "dm-1", name = "Alice", type = "dm")))
        )
        whenever(messageRepository.getLatestMessagePerChannel(listOf("ch-1"))).thenReturn(
            mapOf("ch-1" to Message(id = "msg-ch", channelId = "ch-1", content = "Channel msg"))
        )
        whenever(messageRepository.getLatestMessagePerChannel(listOf("dm-1"))).thenReturn(
            mapOf("dm-1" to Message(id = "msg-dm", channelId = "dm-1", content = "DM msg"))
        )
        whenever(messageRepository.getLatestMessagePerChannel(emptyList())).thenReturn(emptyMap())
        whenever(messageRepository.refreshMessages(any(), any(), any())).thenReturn(Result.success(emptyList()))

        val vm = createViewModel()
        vm.loadChannels("srv-1")
        vm.loadDMs()
        advanceUntilIdle()

        assertEquals("Channel msg", vm.state.value.channelPreviews["ch-1"]?.content)
        assertEquals("DM msg", vm.state.value.channelPreviews["dm-1"]?.content)
    }

    @Test
    fun `refreshDMs via DMNew event populates channelPreviews`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.getChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.refreshChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.getUnreadChannels(any())).thenReturn(Result.success(emptyMap()))
        whenever(channelRepository.getDMs("srv-1")).thenReturn(
            Result.success(listOf(Channel(id = "dm-new", name = "Charlie", type = "dm")))
        )
        whenever(messageRepository.getLatestMessagePerChannel(listOf("dm-new"))).thenReturn(
            mapOf("dm-new" to Message(id = "msg-refresh", channelId = "dm-new", content = "Refresh preview"))
        )
        whenever(messageRepository.getLatestMessagePerChannel(emptyList())).thenReturn(emptyMap())
        whenever(messageRepository.refreshMessages(any(), any(), any())).thenReturn(Result.success(emptyList()))

        val vm = ChannelViewModel(
            channelRepository = channelRepository,
            messageRepository = messageRepository,
            agentRepository = agentRepository,
            activeServerHolder = activeServerHolder,
            socketIOManager = socketIOManager,
            presenceTracker = presenceTracker
        )
        vm.loadChannels("srv-1")
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.DMNew(
                SocketIOManager.DMNewData(id = "dm-new", name = "Charlie", type = "dm")
            )
        )
        advanceUntilIdle()

        val preview = vm.state.value.channelPreviews["dm-new"]
        assertNotNull("refreshDMs path must populate channelPreviews for new DM", preview)
        assertEquals("Refresh preview", preview?.content)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DmPreviewIntegrationTest {

    @Test
    fun `DMItem call site passes lastMessage from channelPreviews`() {
        val source = java.io.File(
            "src/main/java/com/slock/app/ui/home/HomeScreen.kt"
        ).readText()

        val dmItemBlock = source.substringAfter("items(channelState.dms)")
            .substringBefore("} else {")

        assertTrue(
            "DMItem call must look up channelPreviews for the DM",
            dmItemBlock.contains("channelState.channelPreviews[dm.id")
        )
        assertTrue(
            "DMItem call must pass lastMessage parameter",
            dmItemBlock.contains("lastMessage")
        )
    }

    @Test
    fun `loadDMs calls loadChannelPreviews`() {
        val source = java.io.File(
            "src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt"
        ).readText()

        val loadDMsBlock = source.substringAfter("fun loadDMs()")
            .substringBefore("\n    fun ")

        assertTrue(
            "loadDMs must call loadChannelPreviews for DM list",
            loadDMsBlock.contains("loadChannelPreviews(dms)")
        )
    }

    @Test
    fun `refreshDMs calls loadChannelPreviews`() {
        val source = java.io.File(
            "src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt"
        ).readText()

        val refreshDMsBlock = source.substringAfter("fun refreshDMs()")
            .substringBefore("\n    fun ")

        assertTrue(
            "refreshDMs must call loadChannelPreviews for DM list",
            refreshDMsBlock.contains("loadChannelPreviews(dms)")
        )
    }
}
