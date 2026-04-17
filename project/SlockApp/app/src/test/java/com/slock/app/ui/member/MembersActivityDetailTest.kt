package com.slock.app.ui.member

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.Agent
import com.slock.app.data.model.Member
import com.slock.app.data.model.Server
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.repository.ServerRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MembersActivityDetailTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val serverRepository: ServerRepository = mock()
    private val agentRepository: AgentRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val secureTokenStorage: SecureTokenStorage = mock()
    private val activeServerHolder = ActiveServerHolder(secureTokenStorage)
    private val presenceTracker = PresenceTracker()
    private val eventFlow = MutableSharedFlow<SocketIOManager.SocketEvent>()

    private fun createViewModel(): MembersViewModel {
        whenever(socketIOManager.events).thenReturn(eventFlow)
        return MembersViewModel(
            serverRepository = serverRepository,
            agentRepository = agentRepository,
            socketIOManager = socketIOManager,
            activeServerHolder = activeServerHolder,
            presenceTracker = presenceTracker
        )
    }

    @Test
    fun `buildAgentSubtitle includes activityDetail from REST when present`() {
        val vm = createViewModel()
        val agent = Agent(
            id = "agent-1",
            status = "active",
            activity = "thinking",
            activityDetail = "Reading ChannelViewModel.kt",
            model = "claude-sonnet-4-20250514"
        )

        val subtitle = vm.buildAgentSubtitle(agent)

        assertTrue(
            "Subtitle must include activityDetail, got: $subtitle",
            subtitle.contains("Reading ChannelViewModel.kt")
        )
        assertTrue(
            "Subtitle must include activity label, got: $subtitle",
            subtitle.contains("thinking")
        )
    }

    @Test
    fun `buildAgentSubtitle works without activityDetail`() {
        val vm = createViewModel()
        val agent = Agent(
            id = "agent-1",
            status = "active",
            activity = "thinking",
            activityDetail = null,
            model = "claude-sonnet-4-20250514"
        )

        val subtitle = vm.buildAgentSubtitle(agent)

        assertTrue("Subtitle must contain activity, got: $subtitle", subtitle.contains("thinking"))
        assertTrue("Subtitle must contain model short name, got: $subtitle", subtitle.contains("Sonnet"))
    }

    @Test
    fun `loadMembers REST path includes activityDetail in subtitle`() = runTest {
        val agents = listOf(
            Agent(
                id = "agent-1",
                name = "Claude",
                status = "active",
                activity = "thinking",
                activityDetail = "Analyzing test output",
                model = "claude-sonnet-4-20250514"
            )
        )
        whenever(secureTokenStorage.serverId).thenReturn("server-1")
        whenever(serverRepository.getServerMembers("server-1")).thenReturn(Result.success(emptyList()))
        whenever(agentRepository.getAgents("server-1")).thenReturn(Result.success(agents))
        whenever(agentRepository.refreshAgents("server-1")).thenReturn(Result.success(agents))

        val vm = createViewModel()
        vm.loadMembers("server-1")
        advanceUntilIdle()

        val agentMember = vm.state.value.members.find { it.isAgent && it.id == "agent-1" }
        assertTrue(
            "Agent member subtitle must include activityDetail from REST, got: ${agentMember?.subtitle}",
            agentMember?.subtitle?.contains("Analyzing test output") == true
        )
    }

    @Test
    fun `socket AgentActivity updates subtitle with detail message`() = runTest {
        val agents = listOf(
            Agent(id = "agent-1", name = "Claude", status = "active", model = "claude-sonnet-4-20250514")
        )
        whenever(secureTokenStorage.serverId).thenReturn("server-1")
        whenever(serverRepository.getServerMembers("server-1")).thenReturn(Result.success(emptyList()))
        whenever(agentRepository.getAgents("server-1")).thenReturn(Result.success(agents))
        whenever(agentRepository.refreshAgents("server-1")).thenReturn(Result.success(agents))

        val vm = createViewModel()
        vm.loadMembers("server-1")
        advanceUntilIdle()

        eventFlow.emit(
            SocketIOManager.SocketEvent.AgentActivity(
                SocketIOManager.AgentActivityData(
                    agentId = "agent-1",
                    activity = "thinking",
                    message = "Writing PR description"
                )
            )
        )
        advanceUntilIdle()

        val agentMember = vm.state.value.members.find { it.isAgent && it.id == "agent-1" }
        assertTrue(
            "Agent member subtitle must include socket detail, got: ${agentMember?.subtitle}",
            agentMember?.subtitle?.contains("Writing PR description") == true
        )
    }
}
