package com.slock.app.ui.agent

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.local.SettingsPreferencesStore
import com.slock.app.data.model.ActivityLogEntry
import com.slock.app.data.model.Agent
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.socket.SocketIOManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AgentActivityLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var socketIOManager: SocketIOManager
    private lateinit var activeServerHolder: ActiveServerHolder
    private lateinit var secureTokenStorage: SecureTokenStorage
    private lateinit var settingsPreferencesStore: SettingsPreferencesStore
    private val eventFlow = MutableSharedFlow<SocketIOManager.SocketEvent>()

    private val activeAgent = Agent(
        id = "agent1", name = "TestBot", status = "active",
        activity = "Processing messages", activityDetail = "Reading #general"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        secureTokenStorage = mock()
        whenever(secureTokenStorage.serverId).thenReturn("server1")
        activeServerHolder = ActiveServerHolder(secureTokenStorage)
        socketIOManager = mock()
        whenever(socketIOManager.events).thenReturn(eventFlow)
        settingsPreferencesStore = mock()
        whenever(settingsPreferencesStore.recentAgentModelsFlow).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(repo: AgentRepository): AgentViewModel {
        return AgentViewModel(
            agentRepository = repo,
            socketIOManager = socketIOManager,
            activeServerHolder = activeServerHolder,
            settingsPreferencesStore = settingsPreferencesStore
        )
    }

    @Test
    fun `socket activity then stop clears agentActivities`() = runTest {
        val repo = FakeListAgentRepository(
            getResult = Result.success(listOf(activeAgent)),
            refreshResult = Result.success(listOf(activeAgent))
        )
        val vm = createViewModel(repo)
        vm.loadAgents("server1")
        advanceUntilIdle()

        eventFlow.emit(SocketIOManager.SocketEvent.AgentActivity(
            SocketIOManager.AgentActivityData("agent1", "Thinking", "Planning next step")
        ))
        advanceUntilIdle()

        assertNotNull(vm.state.value.agentActivities["agent1"])
        assertEquals("Thinking", vm.state.value.agentActivities["agent1"]?.activity)

        vm.stopAgent("agent1")
        advanceUntilIdle()

        assertNull(vm.state.value.agentActivities["agent1"])
        assertEquals("stopped", vm.state.value.agents.first().status)
    }

    @Test
    fun `refresh removes activity when API returns null activity`() = runTest {
        val agentWithActivity = activeAgent
        val agentWithoutActivity = activeAgent.copy(activity = null, activityDetail = null)
        val repo = FakeListAgentRepository(
            getResult = Result.success(listOf(agentWithActivity)),
            refreshResult = Result.success(listOf(agentWithoutActivity))
        )
        val vm = createViewModel(repo)
        vm.loadAgents("server1")
        advanceUntilIdle()

        assertNull(
            "After refresh, agent without activity should have no entry in agentActivities",
            vm.state.value.agentActivities["agent1"]
        )
    }

    @Test
    fun `restart clears stale activity before next socket event`() = runTest {
        val repo = FakeListAgentRepository(
            getResult = Result.success(listOf(activeAgent)),
            refreshResult = Result.success(listOf(activeAgent))
        )
        val vm = createViewModel(repo)
        vm.loadAgents("server1")
        advanceUntilIdle()

        eventFlow.emit(SocketIOManager.SocketEvent.AgentActivity(
            SocketIOManager.AgentActivityData("agent1", "Working", "Old task")
        ))
        advanceUntilIdle()
        assertEquals("Working", vm.state.value.agentActivities["agent1"]?.activity)

        vm.stopAgent("agent1")
        advanceUntilIdle()
        assertNull(vm.state.value.agentActivities["agent1"])

        vm.startAgent("agent1")
        advanceUntilIdle()
        assertNull(
            "After restart, stale activity should not reappear before next socket event",
            vm.state.value.agentActivities["agent1"]
        )
        assertEquals("active", vm.state.value.agents.first().status)
    }

    @Test
    fun `startAgent clears stale activity from prior session`() = runTest {
        val stoppedAgent = activeAgent.copy(status = "stopped")
        val repo = FakeListAgentRepository(
            getResult = Result.success(listOf(stoppedAgent)),
            refreshResult = Result.success(listOf(stoppedAgent))
        )
        val vm = createViewModel(repo)
        vm.loadAgents("server1")
        advanceUntilIdle()

        eventFlow.emit(SocketIOManager.SocketEvent.AgentActivity(
            SocketIOManager.AgentActivityData("agent1", "Stale thinking", null)
        ))
        advanceUntilIdle()

        vm.startAgent("agent1")
        advanceUntilIdle()

        assertNull(vm.state.value.agentActivities["agent1"])
    }

    @Test
    fun `loadAgents replaces activities instead of merging`() = runTest {
        val agent1 = Agent(id = "a1", name = "Bot1", status = "active", activity = "Working")
        val agent2 = Agent(id = "a2", name = "Bot2", status = "active", activity = null)
        val repo = FakeListAgentRepository(
            getResult = Result.success(listOf(agent1, agent2)),
            refreshResult = Result.success(listOf(agent1.copy(activity = null), agent2))
        )
        val vm = createViewModel(repo)
        vm.loadAgents("server1")
        advanceUntilIdle()

        assertNull(
            "After refresh, agent whose activity became null should be cleared",
            vm.state.value.agentActivities["a1"]
        )
    }

    @Test
    fun `resetAgent clears activity entry`() = runTest {
        val repo = FakeListAgentRepository(
            getResult = Result.success(listOf(activeAgent)),
            refreshResult = Result.success(listOf(activeAgent))
        )
        val vm = createViewModel(repo)
        vm.loadAgents("server1")
        advanceUntilIdle()

        assertNotNull(vm.state.value.agentActivities["agent1"])

        vm.resetAgent("agent1")
        advanceUntilIdle()

        assertNull(vm.state.value.agentActivities["agent1"])
    }
}

private class FakeListAgentRepository(
    private val getResult: Result<List<Agent>> = Result.success(emptyList()),
    private val refreshResult: Result<List<Agent>> = Result.success(emptyList())
) : AgentRepository {
    override suspend fun getAgents(serverId: String) = getResult
    override suspend fun refreshAgents(serverId: String) = refreshResult
    override suspend fun createAgent(serverId: String, name: String, description: String, prompt: String, model: String, avatar: String?) = Result.failure<Agent>(NotImplementedError())
    override suspend fun updateAgent(serverId: String, agentId: String, name: String?, description: String?, prompt: String?) = Result.failure<Agent>(NotImplementedError())
    override suspend fun deleteAgent(serverId: String, agentId: String) = Result.success(Unit)
    override suspend fun startAgent(serverId: String, agentId: String) = Result.success(Unit)
    override suspend fun stopAgent(serverId: String, agentId: String) = Result.success(Unit)
    override suspend fun resetAgent(serverId: String, agentId: String, mode: String) = Result.success(Unit)
    override suspend fun getActivityLog(serverId: String, agentId: String, limit: Int) = Result.success(emptyList<ActivityLogEntry>())
}
