package com.slock.app.ui.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AgentDisplayStateIntegrationTest {

    private fun readSource(vararg candidates: String): String {
        return candidates.map { File(it) }.first { it.exists() }.readText()
    }

    private val displayStateSource = readSource(
        "src/main/java/com/slock/app/ui/agent/AgentDisplayState.kt",
        "app/src/main/java/com/slock/app/ui/agent/AgentDisplayState.kt"
    )

    private val listScreenSource = readSource(
        "src/main/java/com/slock/app/ui/agent/AgentListScreen.kt",
        "app/src/main/java/com/slock/app/ui/agent/AgentListScreen.kt"
    )

    private val detailScreenSource = readSource(
        "src/main/java/com/slock/app/ui/agent/AgentDetailScreen.kt",
        "app/src/main/java/com/slock/app/ui/agent/AgentDetailScreen.kt"
    )

    private val viewModelSource = readSource(
        "src/main/java/com/slock/app/ui/agent/AgentViewModel.kt",
        "app/src/main/java/com/slock/app/ui/agent/AgentViewModel.kt"
    )

    private val detailViewModelSource = readSource(
        "src/main/java/com/slock/app/ui/agent/AgentDetailViewModel.kt",
        "app/src/main/java/com/slock/app/ui/agent/AgentDetailViewModel.kt"
    )

    @Test
    fun `AgentDisplayState enum exists with all canonical states`() {
        assertTrue(displayStateSource.contains("ONLINE"))
        assertTrue(displayStateSource.contains("THINKING"))
        assertTrue(displayStateSource.contains("WORKING"))
        assertTrue(displayStateSource.contains("ERROR"))
        assertTrue(displayStateSource.contains("OFFLINE"))
    }

    @Test
    fun `resolveDisplayState function exists`() {
        assertTrue(displayStateSource.contains("fun resolveDisplayState("))
    }

    @Test
    fun `NeoAgentCard uses resolveDisplayState`() {
        val cardBlock = listScreenSource
            .substringAfter("fun NeoAgentCard(")
            .substringBefore("@Composable")
        assertTrue(
            "NeoAgentCard must call resolveDisplayState",
            cardBlock.contains("resolveDisplayState")
        )
    }

    @Test
    fun `NeoAgentCard uses displayState dotColor`() {
        val cardBlock = listScreenSource
            .substringAfter("fun NeoAgentCard(")
            .substringBefore("@Composable")
        assertTrue(
            "NeoAgentCard must use displayState.dotColor",
            cardBlock.contains("displayState.dotColor")
        )
    }

    @Test
    fun `NeoAgentCard uses displayState statusText`() {
        val cardBlock = listScreenSource
            .substringAfter("fun NeoAgentCard(")
            .substringBefore("@Composable")
        assertTrue(
            "NeoAgentCard must use displayState.statusText",
            cardBlock.contains("displayState.statusText")
        )
    }

    @Test
    fun `Detail screen uses resolveDisplayState`() {
        assertTrue(
            "AgentDetailScreen must call resolveDisplayState",
            detailScreenSource.contains("resolveDisplayState")
        )
    }

    @Test
    fun `Detail screen uses displayState dotColor for hero indicator`() {
        assertTrue(
            "Detail screen must use displayState.dotColor",
            detailScreenSource.contains("displayState.dotColor")
        )
    }

    @Test
    fun `Detail screen uses displayState statusText for hero indicator`() {
        assertTrue(
            "Detail screen must use displayState.statusText",
            detailScreenSource.contains("displayState.statusText")
        )
    }

    @Test
    fun `Detail screen guards Current Activity with isActive`() {
        val overviewBlock = detailScreenSource
            .substringAfter("fun OverviewContent(")
            .substringBefore("fun ActivityLogContent(")
        assertTrue(
            "Current Activity section must be guarded by isActive",
            overviewBlock.contains("isActive && activity != null")
        )
    }

    @Test
    fun `AgentViewModel stopAgent clears agentActivities`() {
        val stopBlock = viewModelSource
            .substringAfter("fun stopAgent(")
            .substringBefore("fun resetAgent(")
        assertTrue(
            "stopAgent must remove agent from agentActivities",
            stopBlock.contains("agentActivities") && stopBlock.contains("- agentId")
        )
    }

    @Test
    fun `AgentDetailViewModel stopAgent clears latestActivity`() {
        val stopBlock = detailViewModelSource
            .substringAfter("fun stopAgent(")
            .substringBefore("fun retry(")
        assertTrue(
            "stopAgent must set latestActivity to null",
            stopBlock.contains("latestActivity = null")
        )
        assertTrue(
            "stopAgent must set latestActivityDetail to null",
            stopBlock.contains("latestActivityDetail = null")
        )
    }

    @Test
    fun `List screen no longer has hardcoded thinking check`() {
        val cardBlock = listScreenSource
            .substringAfter("fun NeoAgentCard(")
            .substringBefore("@Composable")
        assertFalse(
            "NeoAgentCard should not hardcode Thinking string check",
            cardBlock.contains("\"Thinking\"") || cardBlock.contains("== \"thinking\"")
        )
    }
}
