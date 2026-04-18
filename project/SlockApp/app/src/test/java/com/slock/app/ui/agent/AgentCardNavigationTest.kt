package com.slock.app.ui.agent

import org.junit.Assert.*
import org.junit.Test

class AgentCardNavigationTest {

    private val source by lazy {
        java.io.File("src/main/java/com/slock/app/ui/agent/AgentListScreen.kt").readText()
    }

    @Test
    fun `NeoAgentCard has onClick parameter`() {
        val signature = source.substringAfter("private fun NeoAgentCard(")
            .substringBefore(") {")
        assertTrue(
            "NeoAgentCard must accept an onClick parameter",
            signature.contains("onClick")
        )
    }

    @Test
    fun `NeoAgentCard root container is clickable`() {
        val cardBody = source.substringAfter("private fun NeoAgentCard(")
            .substringBefore("// Card header")
        assertTrue(
            "NeoAgentCard root must have .clickable modifier",
            cardBody.contains(".clickable")
        )
    }

    @Test
    fun `active agents call site passes onClick with onAgentClick`() {
        val activeBlock = source.substringAfter("items(activeAgents)")
            .substringBefore("items(inactiveAgents)")
        assertTrue(
            "Active agent NeoAgentCard must pass onClick = { onAgentClick(...) }",
            activeBlock.contains("onClick = { onAgentClick(agent.id")
        )
    }

    @Test
    fun `inactive agents call site passes onClick with onAgentClick`() {
        val inactiveBlock = source.substringAfter("items(inactiveAgents)")
            .substringBefore("}")
        assertTrue(
            "Inactive agent NeoAgentCard must pass onClick = { onAgentClick(...) }",
            inactiveBlock.contains("onClick = { onAgentClick(agent.id")
        )
    }

    @Test
    fun `NavHost wires onAgentClick to agent detail navigation`() {
        val navSource = java.io.File("src/main/java/com/slock/app/ui/navigation/NavHost.kt").readText()
        val agentListBlock = navSource.substringAfter("AgentListScreen(")
            .substringBefore("onNavigateBack")
        assertTrue(
            "NavHost must wire onAgentClick to navigate to agent detail",
            agentListBlock.contains("onAgentClick")
        )
    }
}
