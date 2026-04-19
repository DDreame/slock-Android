package com.slock.app.ui.agent

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.local.SettingsPreferencesStore
import com.slock.app.data.model.Agent
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.data.store.AgentStore
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AgentActivityDetailTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val agentRepository: AgentRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val secureTokenStorage: SecureTokenStorage = mock()
    private val activeServerHolder = ActiveServerHolder(secureTokenStorage)
    private val settingsPreferencesStore: SettingsPreferencesStore = mock()
    private val eventFlow = MutableSharedFlow<SocketIOManager.SocketEvent>()

    private fun createViewModel(): AgentViewModel {
        whenever(socketIOManager.events).thenReturn(eventFlow)
        whenever(settingsPreferencesStore.recentAgentModelsFlow).thenReturn(MutableStateFlow(emptyList()))
        val agentStore = AgentStore(socketIOManager)
        return AgentViewModel(
            agentRepository = agentRepository,
            agentStore = agentStore,
            activeServerHolder = activeServerHolder,
            settingsPreferencesStore = settingsPreferencesStore
        )
    }

    @Test
    fun `socket AgentActivity stores both activity and message`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        eventFlow.emit(
            SocketIOManager.SocketEvent.AgentActivity(
                SocketIOManager.AgentActivityData(
                    agentId = "agent-1",
                    activity = "thinking",
                    message = "Reading ChannelViewModel.kt"
                )
            )
        )
        advanceUntilIdle()

        val info = vm.state.value.agentActivities["agent-1"]
        assertNotNull(info)
        assertEquals("thinking", info!!.activity)
        assertEquals("Reading ChannelViewModel.kt", info.message)
    }

    @Test
    fun `socket AgentActivity with null message stores null`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        eventFlow.emit(
            SocketIOManager.SocketEvent.AgentActivity(
                SocketIOManager.AgentActivityData(
                    agentId = "agent-1",
                    activity = "working",
                    message = null
                )
            )
        )
        advanceUntilIdle()

        val info = vm.state.value.agentActivities["agent-1"]
        assertNotNull(info)
        assertEquals("working", info!!.activity)
        assertNull(info.message)
    }

    @Test
    fun `loadAgents populates activity info from REST including activityDetail`() = runTest {
        val agents = listOf(
            Agent(id = "agent-1", activity = "thinking", activityDetail = "Analyzing test output")
        )
        whenever(secureTokenStorage.serverId).thenReturn("server-1")
        whenever(agentRepository.getAgents("server-1")).thenReturn(Result.success(agents))
        whenever(agentRepository.refreshAgents("server-1")).thenReturn(Result.success(agents))

        val vm = createViewModel()
        vm.loadAgents("server-1")
        advanceUntilIdle()

        val info = vm.state.value.agentActivities["agent-1"]
        assertNotNull(info)
        assertEquals("thinking", info!!.activity)
        assertEquals("Analyzing test output", info.message)
    }

    @Test
    fun `loadAgents without activityDetail stores null message`() = runTest {
        val agents = listOf(
            Agent(id = "agent-1", activity = "working", activityDetail = null)
        )
        whenever(secureTokenStorage.serverId).thenReturn("server-1")
        whenever(agentRepository.getAgents("server-1")).thenReturn(Result.success(agents))
        whenever(agentRepository.refreshAgents("server-1")).thenReturn(Result.success(agents))

        val vm = createViewModel()
        vm.loadAgents("server-1")
        advanceUntilIdle()

        val info = vm.state.value.agentActivities["agent-1"]
        assertNotNull(info)
        assertEquals("working", info!!.activity)
        assertNull(info.message)
    }

    @Test
    fun `AgentActivityInfo preserves message through sequential activity updates`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        eventFlow.emit(
            SocketIOManager.SocketEvent.AgentActivity(
                SocketIOManager.AgentActivityData("agent-1", "thinking", "Step 1: reading file")
            )
        )
        advanceUntilIdle()
        assertEquals("Step 1: reading file", vm.state.value.agentActivities["agent-1"]?.message)

        eventFlow.emit(
            SocketIOManager.SocketEvent.AgentActivity(
                SocketIOManager.AgentActivityData("agent-1", "thinking", "Step 2: writing output")
            )
        )
        advanceUntilIdle()
        assertEquals("Step 2: writing output", vm.state.value.agentActivities["agent-1"]?.message)
    }
}
