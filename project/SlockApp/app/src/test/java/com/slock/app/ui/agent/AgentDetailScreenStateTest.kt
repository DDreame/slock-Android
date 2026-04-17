package com.slock.app.ui.agent

import com.slock.app.data.model.Agent
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentDetailScreenStateTest {

    @Test
    fun `resolveAgentDetailContentState returns loading when agent missing and no error`() {
        val state = AgentDetailUiState(agent = null, isLoading = false, error = null)

        val result = resolveAgentDetailContentState(state)

        assertEquals(AgentDetailContentState.Loading, result)
    }

    @Test
    fun `resolveAgentDetailContentState returns error when agent missing and error present`() {
        val state = AgentDetailUiState(agent = null, error = "boom")

        val result = resolveAgentDetailContentState(state)

        assertEquals(AgentDetailContentState.Error("boom"), result)
    }

    @Test
    fun `resolveAgentDetailContentState prefers content when agent exists`() {
        val agent = Agent(id = "agent-1", name = "Claude", status = "active")
        val state = AgentDetailUiState(agent = agent, isLoading = true, error = "stale")

        val result = resolveAgentDetailContentState(state)

        assertEquals(AgentDetailContentState.Content(agent), result)
    }
}
