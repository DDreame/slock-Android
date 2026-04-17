package com.slock.app.ui.agent

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.local.SettingsPreferencesStore
import com.slock.app.data.model.Agent
import com.slock.app.data.model.DEFAULT_AGENT_MODEL_OPTIONS
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AgentViewModelModelListTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val agentRepository: AgentRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val secureTokenStorage: SecureTokenStorage = mock()
    private val activeServerHolder = ActiveServerHolder(secureTokenStorage)
    private val settingsPreferencesStore: SettingsPreferencesStore = mock()

    @Test
    fun `deriveAvailableAgentModels merges recent discovered and defaults without duplicates`() {
        val models = deriveAvailableAgentModels(
            recentModels = listOf("custom-model", "claude-sonnet-4-20250514", " "),
            discoveredModels = listOf("server-model", "claude-haiku-4-5-20251001", "custom-model")
        )

        assertEquals(
            listOf(
                "custom-model",
                "claude-sonnet-4-20250514",
                "server-model",
                "claude-haiku-4-5-20251001",
                "claude-opus-4-20250514"
            ),
            models
        )
    }

    @Test
    fun `loadAgents populates availableModels from seen agent models and recent models`() = runTest {
        val recentModelsFlow = MutableStateFlow(listOf("custom-model", "claude-sonnet-4-20250514"))
        val agents = listOf(
            Agent(id = "agent-1", model = "server-model"),
            Agent(id = "agent-2", model = "claude-haiku-4-5-20251001")
        )

        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(settingsPreferencesStore.recentAgentModelsFlow).thenReturn(recentModelsFlow)
        whenever(agentRepository.getAgents("server-1")).thenReturn(Result.success(agents))
        whenever(agentRepository.refreshAgents("server-1")).thenReturn(Result.success(agents))

        val viewModel = createViewModel()

        viewModel.loadAgents("server-1")
        advanceUntilIdle()

        assertEquals(
            listOf(
                "custom-model",
                "claude-sonnet-4-20250514",
                "server-model",
                "claude-haiku-4-5-20251001",
                "claude-opus-4-20250514"
            ),
            viewModel.state.value.availableModels
        )
    }

    @Test
    fun `createAgent with custom model uses exact model and persists it`() = runTest {
        val customModelId = "gpt-5.4-mini-custom"
        val recentModelsFlow = MutableStateFlow(emptyList<String>())
        val createdAgent = Agent(
            id = "agent-1",
            name = "Custom Agent",
            description = "Handles edge cases",
            prompt = "Be precise",
            model = customModelId,
            status = "active"
        )

        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(settingsPreferencesStore.recentAgentModelsFlow).thenReturn(recentModelsFlow)
        whenever(
            agentRepository.createAgent(
                serverId = "server-1",
                name = "Custom Agent",
                description = "Handles edge cases",
                prompt = "Be precise",
                model = customModelId,
                avatar = null
            )
        ).thenReturn(Result.success(createdAgent))
        doAnswer {
            recentModelsFlow.value = listOf(customModelId)
            Unit
        }.whenever(settingsPreferencesStore).addRecentAgentModel(customModelId)

        val viewModel = createViewModel()
        activeServerHolder.serverId = "server-1"

        viewModel.createAgent(
            name = "Custom Agent",
            description = "Handles edge cases",
            prompt = "Be precise",
            model = customModelId
        )
        advanceUntilIdle()

        verify(agentRepository).createAgent(
            serverId = "server-1",
            name = "Custom Agent",
            description = "Handles edge cases",
            prompt = "Be precise",
            model = customModelId,
            avatar = null
        )
        verify(settingsPreferencesStore).addRecentAgentModel(customModelId)
        assertEquals(customModelId, viewModel.state.value.availableModels.first())
        assertTrue(viewModel.state.value.availableModels.containsAll(DEFAULT_AGENT_MODEL_OPTIONS))
        assertEquals(customModelId, viewModel.state.value.agents.single().model)
    }

    private fun createViewModel(): AgentViewModel {
        return AgentViewModel(
            agentRepository = agentRepository,
            socketIOManager = socketIOManager,
            activeServerHolder = activeServerHolder,
            settingsPreferencesStore = settingsPreferencesStore
        )
    }
}
