package com.slock.app.ui.channel

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.Channel
import com.slock.app.data.model.ChannelMember
import com.slock.app.data.model.Message
import com.slock.app.data.model.UploadResponse
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.socket.SocketIOManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelViewModelCreateDMTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var socketIOManager: SocketIOManager
    private lateinit var activeServerHolder: ActiveServerHolder
    private lateinit var secureTokenStorage: SecureTokenStorage
    private lateinit var presenceTracker: PresenceTracker
    private val eventFlow = MutableSharedFlow<SocketIOManager.SocketEvent>()
    private val connectionFlow = MutableSharedFlow<SocketIOManager.ConnectionState>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        secureTokenStorage = mock()
        whenever(secureTokenStorage.serverId).thenReturn("server1")
        activeServerHolder = ActiveServerHolder(secureTokenStorage)
        presenceTracker = PresenceTracker()
        socketIOManager = mock()
        whenever(socketIOManager.events).thenReturn(eventFlow)
        whenever(socketIOManager.connectionState).thenReturn(connectionFlow)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        channelRepository: ChannelRepository = FakeChannelRepository(),
        agentRepository: AgentRepository = mock()
    ): ChannelViewModel {
        runBlocking {
            whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))
        }
        return ChannelViewModel(
            channelRepository = channelRepository,
            messageRepository = FakeMessageRepository(),
            agentRepository = agentRepository,
            activeServerHolder = activeServerHolder,
            socketIOManager = socketIOManager,
            presenceTracker = presenceTracker
        )
    }

    @Test
    fun `createDM success invokes onSuccess with channel and adds to dms`() = runTest {
        val dmChannel = Channel(id = "dm-1", name = "DM with Agent", type = "dm")
        val repo = FakeChannelRepository(createDMResult = Result.success(dmChannel))
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        var successChannel: Channel? = null
        vm.createDM(agentId = "agent1", onSuccess = { successChannel = it })
        advanceUntilIdle()

        assertNotNull(successChannel)
        assertEquals("dm-1", successChannel!!.id)
        assertTrue(vm.state.value.dms.any { it.id == "dm-1" })
    }

    @Test
    fun `createDM failure invokes onError and sets error state`() = runTest {
        val repo = FakeChannelRepository(
            createDMResult = Result.failure(RuntimeException("DM creation failed"))
        )
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        var errorMsg: String? = null
        vm.createDM(agentId = "agent1", onSuccess = {}, onError = { errorMsg = it })
        advanceUntilIdle()

        assertEquals("DM creation failed", errorMsg)
        assertEquals("DM creation failed", vm.state.value.error)
    }

    @Test
    fun `createDM does not duplicate existing DM in state`() = runTest {
        val dmChannel = Channel(id = "dm-1", name = "DM with Agent", type = "dm")
        val repo = FakeChannelRepository(createDMResult = Result.success(dmChannel))
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        vm.createDM(agentId = "agent1", onSuccess = {})
        advanceUntilIdle()
        assertEquals(1, vm.state.value.dms.count { it.id == "dm-1" })

        vm.createDM(agentId = "agent1", onSuccess = {})
        advanceUntilIdle()
        assertEquals(1, vm.state.value.dms.count { it.id == "dm-1" })
    }

    @Test
    fun `createDM reuses existing DM when matching agent member found`() = runTest {
        val existingDm = Channel(
            id = "dm-existing",
            name = "DM with Agent",
            type = "dm",
            members = listOf(ChannelMember(agentId = "agent1"))
        )
        val repo = FakeChannelRepository(
            getDMsResult = Result.success(listOf(existingDm)),
            createDMResult = Result.failure(RuntimeException("Should not be called"))
        )
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        vm.loadDMs()
        advanceUntilIdle()

        var successChannel: Channel? = null
        vm.createDM(agentId = "agent1", onSuccess = { successChannel = it })
        advanceUntilIdle()

        assertEquals("dm-existing", successChannel?.id)
    }

    @Test
    fun `createDM reuses existing DM when matching user member found`() = runTest {
        val existingDm = Channel(
            id = "dm-user",
            name = "DM with User",
            type = "dm",
            members = listOf(ChannelMember(userId = "user1"))
        )
        val repo = FakeChannelRepository(
            getDMsResult = Result.success(listOf(existingDm)),
            createDMResult = Result.failure(RuntimeException("Should not be called"))
        )
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        vm.loadDMs()
        advanceUntilIdle()

        var successChannel: Channel? = null
        vm.createDM(userId = "user1", onSuccess = { successChannel = it })
        advanceUntilIdle()

        assertEquals("dm-user", successChannel?.id)
    }

    @Test
    fun `createDM calls API when no matching DM exists in state`() = runTest {
        val newDm = Channel(id = "dm-new", name = "New DM", type = "dm")
        val repo = FakeChannelRepository(
            getDMsResult = Result.success(listOf(
                Channel(id = "dm-other", name = "Other DM", type = "dm", members = listOf(ChannelMember(agentId = "other-agent")))
            )),
            createDMResult = Result.success(newDm)
        )
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        vm.loadDMs()
        advanceUntilIdle()

        var successChannel: Channel? = null
        vm.createDM(agentId = "agent1", onSuccess = { successChannel = it })
        advanceUntilIdle()

        assertEquals("dm-new", successChannel?.id)
        assertTrue(vm.state.value.dms.any { it.id == "dm-new" })
    }

    @Test
    fun `findExistingDM returns null when dms list is empty`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        assertNull(vm.findExistingDM("agent1", null))
    }

    @Test
    fun `findExistingDM matches by agentId in members`() = runTest {
        val dmWithAgent = Channel(
            id = "dm-1", name = "DM", type = "dm",
            members = listOf(ChannelMember(agentId = "agent1"))
        )
        val repo = FakeChannelRepository(getDMsResult = Result.success(listOf(dmWithAgent)))
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        vm.loadDMs()
        advanceUntilIdle()

        val found = vm.findExistingDM("agent1", null)
        assertEquals("dm-1", found?.id)
    }

    @Test
    fun `findExistingDM returns null when no member matches`() = runTest {
        val dmWithOther = Channel(
            id = "dm-1", name = "DM", type = "dm",
            members = listOf(ChannelMember(agentId = "other-agent"))
        )
        val repo = FakeChannelRepository(getDMsResult = Result.success(listOf(dmWithOther)))
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        vm.loadDMs()
        advanceUntilIdle()

        assertNull(vm.findExistingDM("agent1", null))
    }

    @Test
    fun `createDM with no serverId does nothing`() = runTest {
        whenever(secureTokenStorage.serverId).thenReturn(null)
        activeServerHolder = ActiveServerHolder(secureTokenStorage)
        val vm = createViewModel()
        advanceUntilIdle()

        var called = false
        vm.createDM(agentId = "agent1", onSuccess = { called = true })
        advanceUntilIdle()

        assertFalse(called)
    }

    @Test
    fun `createDM waits for DMs to load before checking — race condition prevented`() = runTest {
        val existingDm = Channel(
            id = "dm-existing", name = "DM", type = "dm",
            members = listOf(ChannelMember(agentId = "agent1"))
        )
        val dmGate = CompletableDeferred<Result<List<Channel>>>()
        val repo = DelayedDMRepository(
            getDMsGate = dmGate,
            createDMResult = Result.failure(RuntimeException("Should not call createDM API"))
        )
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        var successChannel: Channel? = null
        vm.createDM(agentId = "agent1", onSuccess = { successChannel = it })
        advanceUntilIdle()

        assertNull("createDM should not have resolved yet — DMs still loading", successChannel)

        dmGate.complete(Result.success(listOf(existingDm)))
        advanceUntilIdle()

        assertEquals("dm-existing", successChannel?.id)
    }

    @Test
    fun `createDM errors when getDMs fails — does not blindly call create API`() = runTest {
        val repo = FakeChannelRepository(
            getDMsResult = Result.failure(RuntimeException("Network error")),
            createDMResult = Result.success(Channel(id = "should-not-appear"))
        )
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        var errorMsg: String? = null
        var successCalled = false
        vm.createDM(agentId = "agent1", onSuccess = { successCalled = true }, onError = { errorMsg = it })
        advanceUntilIdle()

        assertFalse("createDM should not succeed when DMs failed to load", successCalled)
        assertNotNull("createDM should report error when DMs failed to load", errorMsg)
        assertFalse(
            "state should not contain phantom DM from create API",
            vm.state.value.dms.any { it.id == "should-not-appear" }
        )
    }

    @Test
    fun `DMNew then reopen reuses refreshed DM — no duplicate create API call`() = runTest {
        val newDm = Channel(
            id = "dm-new", name = "DM with Agent", type = "dm",
            members = listOf(ChannelMember(agentId = "agent1"))
        )
        val repo = SequentialDMRepository(
            getDMsResults = listOf(
                Result.success(emptyList()),
                Result.success(listOf(newDm))
            ),
            createDMResult = Result.failure(RuntimeException("Should not call create API"))
        )
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        vm.loadDMs()
        advanceUntilIdle()

        eventFlow.emit(SocketIOManager.SocketEvent.DMNew(
            SocketIOManager.DMNewData(id = "dm-new", name = "DM with Agent", type = "dm")
        ))
        advanceUntilIdle()

        var successChannel: Channel? = null
        vm.createDM(agentId = "agent1", onSuccess = { successChannel = it })
        advanceUntilIdle()

        assertEquals("dm-new", successChannel?.id)
    }

    @Test
    fun `createDM after API success stores channel with members for future reuse`() = runTest {
        val newDm = Channel(
            id = "dm-new", name = "DM", type = "dm",
            members = listOf(ChannelMember(agentId = "agent1"))
        )
        val repo = FakeChannelRepository(createDMResult = Result.success(newDm))
        val vm = createViewModel(channelRepository = repo)
        advanceUntilIdle()

        vm.createDM(agentId = "agent1", onSuccess = {})
        advanceUntilIdle()

        val found = vm.findExistingDM("agent1", null)
        assertNotNull("After createDM, findExistingDM should match the new DM", found)
        assertEquals("dm-new", found?.id)
    }
}

private class FakeChannelRepository(
    private val createDMResult: Result<Channel> = Result.success(Channel(id = "dm-default")),
    private val getDMsResult: Result<List<Channel>> = Result.success(emptyList())
) : ChannelRepository {
    override suspend fun getChannels(serverId: String) = Result.success(emptyList<Channel>())
    override suspend fun refreshChannels(serverId: String) = Result.success(emptyList<Channel>())
    override suspend fun createChannel(serverId: String, name: String, type: String) = Result.failure<Channel>(NotImplementedError())
    override suspend fun updateChannel(serverId: String, channelId: String, name: String) = Result.failure<Channel>(NotImplementedError())
    override suspend fun deleteChannel(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun joinChannel(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun leaveChannel(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun markChannelRead(serverId: String, channelId: String, seq: Long) = Result.failure<Unit>(NotImplementedError())
    override suspend fun getDMs(serverId: String) = getDMsResult
    override suspend fun createDM(serverId: String, agentId: String?, userId: String?) = createDMResult
    override suspend fun getChannelMembers(serverId: String, channelId: String) = Result.success(emptyList<ChannelMember>())
    override suspend fun getUnreadChannels(serverId: String) = Result.success(emptyList<Channel>())
    override suspend fun stopAllChannelAgents(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun resumeAllChannelAgents(serverId: String, channelId: String, prompt: String) = Result.failure<Unit>(NotImplementedError())
}

private class DelayedDMRepository(
    private val getDMsGate: CompletableDeferred<Result<List<Channel>>>,
    private val createDMResult: Result<Channel> = Result.success(Channel(id = "dm-default"))
) : ChannelRepository {
    override suspend fun getChannels(serverId: String) = Result.success(emptyList<Channel>())
    override suspend fun refreshChannels(serverId: String) = Result.success(emptyList<Channel>())
    override suspend fun createChannel(serverId: String, name: String, type: String) = Result.failure<Channel>(NotImplementedError())
    override suspend fun updateChannel(serverId: String, channelId: String, name: String) = Result.failure<Channel>(NotImplementedError())
    override suspend fun deleteChannel(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun joinChannel(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun leaveChannel(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun markChannelRead(serverId: String, channelId: String, seq: Long) = Result.failure<Unit>(NotImplementedError())
    override suspend fun getDMs(serverId: String) = getDMsGate.await()
    override suspend fun createDM(serverId: String, agentId: String?, userId: String?) = createDMResult
    override suspend fun getChannelMembers(serverId: String, channelId: String) = Result.success(emptyList<ChannelMember>())
    override suspend fun getUnreadChannels(serverId: String) = Result.success(emptyList<Channel>())
    override suspend fun stopAllChannelAgents(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun resumeAllChannelAgents(serverId: String, channelId: String, prompt: String) = Result.failure<Unit>(NotImplementedError())
}

private class SequentialDMRepository(
    private val getDMsResults: List<Result<List<Channel>>>,
    private val createDMResult: Result<Channel> = Result.success(Channel(id = "dm-default"))
) : ChannelRepository {
    private var getDMsCallCount = 0
    override suspend fun getChannels(serverId: String) = Result.success(emptyList<Channel>())
    override suspend fun refreshChannels(serverId: String) = Result.success(emptyList<Channel>())
    override suspend fun createChannel(serverId: String, name: String, type: String) = Result.failure<Channel>(NotImplementedError())
    override suspend fun updateChannel(serverId: String, channelId: String, name: String) = Result.failure<Channel>(NotImplementedError())
    override suspend fun deleteChannel(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun joinChannel(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun leaveChannel(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun markChannelRead(serverId: String, channelId: String, seq: Long) = Result.failure<Unit>(NotImplementedError())
    override suspend fun getDMs(serverId: String): Result<List<Channel>> {
        val idx = getDMsCallCount.coerceAtMost(getDMsResults.lastIndex)
        getDMsCallCount++
        return getDMsResults[idx]
    }
    override suspend fun createDM(serverId: String, agentId: String?, userId: String?) = createDMResult
    override suspend fun getChannelMembers(serverId: String, channelId: String) = Result.success(emptyList<ChannelMember>())
    override suspend fun getUnreadChannels(serverId: String) = Result.success(emptyList<Channel>())
    override suspend fun stopAllChannelAgents(serverId: String, channelId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun resumeAllChannelAgents(serverId: String, channelId: String, prompt: String) = Result.failure<Unit>(NotImplementedError())
}

private class FakeMessageRepository : MessageRepository {
    override suspend fun sendMessage(serverId: String, channelId: String, content: String, attachmentIds: List<String>?, asTask: Boolean, parentMessageId: String?) = Result.failure<Message>(NotImplementedError())
    override suspend fun getMessages(serverId: String, channelId: String, limit: Int, before: String?, after: String?) = Result.success(emptyList<Message>())
    override suspend fun refreshMessages(serverId: String, channelId: String, limit: Int) = Result.success(emptyList<Message>())
    override suspend fun searchMessages(serverId: String, query: String, searchServerId: String?, channelId: String?) = Result.success(emptyList<Message>())
    override suspend fun getLatestMessagePerChannel(channelIds: List<String>) = emptyMap<String, Message>()
    override suspend fun uploadFile(serverId: String, fileName: String, mimeType: String, bytes: ByteArray) = Result.failure<UploadResponse>(NotImplementedError())
}
