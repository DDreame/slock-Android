package com.slock.app.ui.agent

import androidx.lifecycle.SavedStateHandle
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.ActivityLogEntry
import com.slock.app.data.model.Agent
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.data.store.AgentStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
class AgentDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var socketIOManager: SocketIOManager
    private lateinit var activeServerHolder: ActiveServerHolder
    private lateinit var secureTokenStorage: SecureTokenStorage
    private val eventFlow = MutableSharedFlow<SocketIOManager.SocketEvent>()

    private val testAgent = Agent(
        id = "agent1",
        name = "TestBot",
        model = "claude-sonnet-4-20250514",
        status = "active",
        activity = "Processing messages",
        activityDetail = "Reading #general"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        secureTokenStorage = mock()
        whenever(secureTokenStorage.serverId).thenReturn("server1")
        activeServerHolder = ActiveServerHolder(secureTokenStorage)
        socketIOManager = mock()
        whenever(socketIOManager.events).thenReturn(eventFlow)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        agentId: String = "agent1",
        routeServerId: String? = null,
        agentRepository: AgentRepository = FakeAgentRepository()
    ): AgentDetailViewModel {
        val agentStore = AgentStore(socketIOManager)
        return AgentDetailViewModel(
            savedStateHandle = SavedStateHandle(
                buildMap {
                    put("agentId", agentId)
                    routeServerId?.let { put("serverId", it) }
                }
            ),
            agentRepository = agentRepository,
            agentStore = agentStore,
            socketIOManager = socketIOManager,
            activeServerHolder = activeServerHolder
        )
    }

    @Test
    fun `loads agent and activity log on init`() = runTest {
        val logEntries = listOf(
            ActivityLogEntry(timestamp = "2026-04-17T10:00:00Z", activity = "Started", detail = "Booting up")
        )
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.success(logEntries)
        )

        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("TestBot", state.agent?.name)
        assertEquals("active", state.agent?.status)
        assertEquals("Processing messages", state.latestActivity)
        assertFalse(state.isLoading)
        assertEquals(1, state.activityLog.size)
        assertEquals("Started", state.activityLog[0].activity)
        assertFalse(state.isLoadingLog)
    }

    @Test
    fun `socket activity event prepends to log and updates latest`() = runTest {
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.success(emptyList())
        )

        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        eventFlow.emit(
            SocketIOManager.SocketEvent.AgentActivity(
                SocketIOManager.AgentActivityData("agent1", "Tool call", "Running grep")
            )
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Tool call", state.latestActivity)
        assertEquals("Running grep", state.latestActivityDetail)
        assertEquals(1, state.activityLog.size)
        assertEquals("Tool call", state.activityLog[0].activity)
        assertEquals("Running grep", state.activityLog[0].detail)
    }

    @Test
    fun `socket events for other agents are ignored`() = runTest {
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.success(emptyList())
        )

        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        eventFlow.emit(
            SocketIOManager.SocketEvent.AgentActivity(
                SocketIOManager.AgentActivityData("other-agent", "Doing stuff", null)
            )
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Processing messages", state.latestActivity)
        assertTrue(state.activityLog.isEmpty())
    }

    @Test
    fun `agent not found in list sets null agent`() = runTest {
        val repo = FakeAgentRepository(agentsResult = Result.success(emptyList()))

        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        val state = vm.state.value
        assertNull(state.agent)
        assertFalse(state.isLoading)
    }

    @Test
    fun `agent load failure sets error`() = runTest {
        val repo = FakeAgentRepository(
            agentsResult = Result.failure(RuntimeException("Network error"))
        )

        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Network error", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `selectTab updates selectedTab`() = runTest {
        val repo = FakeAgentRepository(agentsResult = Result.success(listOf(testAgent)))

        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        assertEquals(0, vm.state.value.selectedTab)
        vm.selectTab(1)
        assertEquals(1, vm.state.value.selectedTab)
        vm.selectTab(0)
        assertEquals(0, vm.state.value.selectedTab)
    }

    @Test
    fun `activity log failure does not affect agent loading`() = runTest {
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.failure(RuntimeException("Log fetch failed"))
        )

        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("TestBot", state.agent?.name)
        assertTrue(state.activityLog.isEmpty())
        assertFalse(state.isLoadingLog)
        assertNull(state.error)
    }

    @Test
    fun `blank agentId sets error`() = runTest {
        val vm = createViewModel(agentId = "")
        advanceUntilIdle()

        assertEquals("Agent not found", vm.state.value.error)
    }

    @Test
    fun `route serverId fallback loads agent when active server context is missing`() = runTest {
        activeServerHolder.serverId = null
        val repo = FakeAgentRepository(agentsResult = Result.success(listOf(testAgent)))

        val vm = createViewModel(routeServerId = "server-from-route", agentRepository = repo)
        advanceUntilIdle()

        assertEquals("server-from-route", vm.serverId)
        assertEquals("TestBot", vm.state.value.agent?.name)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `missing active and route server context sets explicit error`() = runTest {
        activeServerHolder.serverId = null

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("Server context unavailable", vm.state.value.error)
    }

    @Test
    fun `socket event during history load is preserved after merge`() = runTest {
        val logGate = CompletableDeferred<Unit>()
        val historicalEntries = listOf(
            ActivityLogEntry(timestamp = "2026-04-17T09:00:00Z", activity = "Historical", detail = "Old entry")
        )
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.success(historicalEntries),
            activityLogGate = logGate
        )

        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        assertTrue(vm.state.value.isLoadingLog)

        eventFlow.emit(
            SocketIOManager.SocketEvent.AgentActivity(
                SocketIOManager.AgentActivityData("agent1", "Live event", "Real-time entry")
            )
        )
        advanceUntilIdle()

        assertEquals(1, vm.state.value.activityLog.size)
        assertEquals("Live event", vm.state.value.activityLog[0].activity)

        logGate.complete(Unit)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoadingLog)
        assertEquals(2, state.activityLog.size)
        assertEquals("Live event", state.activityLog[0].activity)
        assertEquals("Historical", state.activityLog[1].activity)
    }

    @Test
    fun `stopAgent clears latestActivity and latestActivityDetail`() = runTest {
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.success(emptyList())
        )
        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        assertEquals("Processing messages", vm.state.value.latestActivity)
        assertEquals("Reading #general", vm.state.value.latestActivityDetail)

        vm.stopAgent()
        advanceUntilIdle()

        assertNull(vm.state.value.latestActivity)
        assertNull(vm.state.value.latestActivityDetail)
        assertEquals("stopped", vm.state.value.agent?.status)
    }

    @Test
    fun `startAgent clears stale latestActivity`() = runTest {
        val stoppedAgent = testAgent.copy(status = "stopped")
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(stoppedAgent)),
            activityLogResult = Result.success(emptyList())
        )
        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        assertEquals("Processing messages", vm.state.value.latestActivity)

        vm.startAgent()
        advanceUntilIdle()

        assertNull(vm.state.value.latestActivity)
        assertNull(vm.state.value.latestActivityDetail)
        assertEquals("active", vm.state.value.agent?.status)
    }

    @Test
    fun `socket activity then stop then restart clears activity between transitions`() = runTest {
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.success(emptyList())
        )
        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        eventFlow.emit(SocketIOManager.SocketEvent.AgentActivity(
            SocketIOManager.AgentActivityData("agent1", "Thinking", "Deep analysis")
        ))
        advanceUntilIdle()
        assertEquals("Thinking", vm.state.value.latestActivity)

        vm.stopAgent()
        advanceUntilIdle()
        assertNull(vm.state.value.latestActivity)

        vm.startAgent()
        advanceUntilIdle()
        assertNull("After restart, stale activity should not reappear", vm.state.value.latestActivity)
        assertEquals("active", vm.state.value.agent?.status)
    }

    @Test
    fun `resetAgent clears activity and shows success feedback`() = runTest {
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.success(emptyList()),
            resetResult = Result.success(Unit)
        )
        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        assertEquals("Processing messages", vm.state.value.latestActivity)

        vm.resetAgent()
        advanceUntilIdle()

        assertNull(vm.state.value.latestActivity)
        assertNull(vm.state.value.latestActivityDetail)
        assertTrue(vm.state.value.activityLog.isEmpty())
        assertFalse(vm.state.value.isResetting)
        assertEquals("Agent reset successful", vm.state.value.resetFeedbackMessage)
    }

    @Test
    fun `resetAgent failure sets error feedback`() = runTest {
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.success(emptyList()),
            resetResult = Result.failure(RuntimeException("reset failed"))
        )
        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        vm.resetAgent()
        advanceUntilIdle()

        assertFalse(vm.state.value.isResetting)
        assertEquals("reset failed", vm.state.value.resetFeedbackMessage)
    }

    @Test
    fun `consumeResetFeedback clears feedback message`() = runTest {
        val repo = FakeAgentRepository(
            agentsResult = Result.success(listOf(testAgent)),
            activityLogResult = Result.success(emptyList()),
            resetResult = Result.success(Unit)
        )
        val vm = createViewModel(agentRepository = repo)
        advanceUntilIdle()

        vm.resetAgent()
        advanceUntilIdle()
        assertEquals("Agent reset successful", vm.state.value.resetFeedbackMessage)

        vm.consumeResetFeedback()
        assertNull(vm.state.value.resetFeedbackMessage)
    }
}

private class FakeAgentRepository(
    private val agentsResult: Result<List<Agent>> = Result.success(emptyList()),
    private val activityLogResult: Result<List<ActivityLogEntry>> = Result.success(emptyList()),
    private val activityLogGate: CompletableDeferred<Unit>? = null,
    private val resetResult: Result<Unit> = Result.success(Unit)
) : AgentRepository {
    override suspend fun getAgents(serverId: String) = agentsResult
    override suspend fun refreshAgents(serverId: String) = agentsResult
    override suspend fun createAgent(
        serverId: String,
        name: String,
        description: String,
        prompt: String,
        model: String,
        runtime: String?,
        reasoningEffort: String?,
        envVars: Map<String, String>?,
        avatar: String?
    ) = Result.failure<Agent>(NotImplementedError())
    override suspend fun updateAgent(
        serverId: String,
        agentId: String,
        name: String?,
        description: String?,
        prompt: String?,
        runtime: String?,
        reasoningEffort: String?,
        envVars: Map<String, String>?
    ) = Result.failure<Agent>(NotImplementedError())
    override suspend fun deleteAgent(serverId: String, agentId: String) = Result.failure<Unit>(NotImplementedError())
    override suspend fun startAgent(serverId: String, agentId: String) = Result.success(Unit)
    override suspend fun stopAgent(serverId: String, agentId: String) = Result.success(Unit)
    override suspend fun resetAgent(serverId: String, agentId: String, mode: String) = resetResult
    override suspend fun getActivityLog(serverId: String, agentId: String, limit: Int): Result<List<ActivityLogEntry>> {
        activityLogGate?.await()
        return activityLogResult
    }
}
