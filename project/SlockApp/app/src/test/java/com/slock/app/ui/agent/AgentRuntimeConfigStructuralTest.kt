package com.slock.app.ui.agent

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRuntimeConfigStructuralTest {

    private val agentListScreenSource: String = Files.readString(
        Path.of(
            "app/src/main/java/com/slock/app/ui/agent/AgentListScreen.kt"
        )
    )

    private val navHostSource: String = Files.readString(
        Path.of(
            "app/src/main/java/com/slock/app/ui/navigation/NavHost.kt"
        )
    )

    @Test
    fun `agent settings and create sheets include runtime reasoning and env var sections`() {
        assertTrue(agentListScreenSource.contains("NeoLabel(\"RUNTIME\")"))
        assertTrue(agentListScreenSource.contains("NeoLabel(\"REASONING EFFORT\")"))
        assertTrue(agentListScreenSource.contains("NeoLabel(\"ENV VARS\")"))
        assertTrue(agentListScreenSource.contains("supportsAgentReasoningEffort(selectedRuntime)"))
    }

    @Test
    fun `env var editor exposes add and remove controls`() {
        assertTrue(agentListScreenSource.contains("text = \"ADD ENV VAR\""))
        assertTrue(agentListScreenSource.contains("NeoPressableBox(onClick = onRemove)"))
    }

    @Test
    fun `nav host forwards runtime reasoning and env vars to view model`() {
        assertTrue(navHostSource.contains("viewModel.createAgent(name, desc, prompt, model, runtime, reasoningEffort, envVars)"))
        assertTrue(navHostSource.contains("viewModel.updateAgent(agentId, name, desc, prompt, runtime, reasoningEffort, envVars)"))
    }
}
