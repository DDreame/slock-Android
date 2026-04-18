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
    fun `active agents call site passes onClick with guarded onAgentClick`() {
        val activeBlock = source.substringAfter("items(activeAgents)")
            .substringBefore("items(inactiveAgents)")
        assertTrue(
            "Active agent NeoAgentCard must guard empty id before calling onAgentClick",
            activeBlock.contains("agent.id?.takeIf") && activeBlock.contains("let(onAgentClick)")
        )
    }

    @Test
    fun `inactive agents call site passes onClick with guarded onAgentClick`() {
        val inactiveBlock = source.substringAfter("items(inactiveAgents)")
            .substringBefore("}")
        assertTrue(
            "Inactive agent NeoAgentCard must guard empty id before calling onAgentClick",
            inactiveBlock.contains("agent.id?.takeIf") && inactiveBlock.contains("let(onAgentClick)")
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
