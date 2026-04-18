package com.slock.app.ui.agent

import com.slock.app.data.model.DEFAULT_AGENT_REASONING_EFFORT_ID
import com.slock.app.data.model.DEFAULT_AGENT_RUNTIME_ID
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentRuntimeConfigStateTest {

    @Test
    fun `normalizeAgentEnvVars trims keys and drops blank entries`() {
        val result = normalizeAgentEnvVars(
            listOf(
                AgentEnvVarDraft(key = " OPENAI_API_KEY ", value = "secret"),
                AgentEnvVarDraft(key = "", value = "ignored"),
                AgentEnvVarDraft(key = " SLACK_TOKEN", value = "token-1")
            )
        )

        assertEquals(
            linkedMapOf(
                "OPENAI_API_KEY" to "secret",
                "SLACK_TOKEN" to "token-1"
            ),
            result
        )
    }

    @Test
    fun `seedAgentEnvVarDrafts sorts existing env vars by key`() {
        val result = seedAgentEnvVarDrafts(
            mapOf(
                "Z_KEY" to "z",
                "A_KEY" to "a"
            )
        )

        assertEquals(
            listOf(
                AgentEnvVarDraft(key = "A_KEY", value = "a"),
                AgentEnvVarDraft(key = "Z_KEY", value = "z")
            ),
            result
        )
    }

    @Test
    fun `resolveSelectedAgentRuntime falls back to default for unknown runtime`() {
        assertEquals(DEFAULT_AGENT_RUNTIME_ID, resolveSelectedAgentRuntime("unsupported-runtime"))
    }

    @Test
    fun `resolveSelectedReasoningEffort keeps supported runtime selection`() {
        assertEquals("xhigh", resolveSelectedReasoningEffort("codex", "xhigh"))
    }

    @Test
    fun `resolveSelectedReasoningEffort resets unsupported runtime to default`() {
        assertEquals(
            DEFAULT_AGENT_REASONING_EFFORT_ID,
            resolveSelectedReasoningEffort("claude", "xhigh")
        )
    }
}
