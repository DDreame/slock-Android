package com.slock.app.ui.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AgentActivityRegressionTest {

    private fun readSource(vararg candidates: String): String {
        return candidates.map { File(it) }.first { it.exists() }.readText()
    }

    private val agentListSource = readSource(
        "src/main/java/com/slock/app/ui/agent/AgentListScreen.kt",
        "app/src/main/java/com/slock/app/ui/agent/AgentListScreen.kt"
    )

    private val viewModelSource = readSource(
        "src/main/java/com/slock/app/ui/agent/AgentViewModel.kt",
        "app/src/main/java/com/slock/app/ui/agent/AgentViewModel.kt"
    )

    @Test
    fun `AgentUiState agentActivities uses AgentActivityInfo not plain String`() {
        assertTrue(
            "agentActivities must use AgentActivityInfo type",
            viewModelSource.contains("Map<String, AgentActivityInfo>")
        )
    }

    @Test
    fun `AgentActivityInfo data class captures both activity and message`() {
        assertTrue(
            "AgentActivityInfo must have activity field",
            viewModelSource.contains("val activity: String")
        )
        assertTrue(
            "AgentActivityInfo must have message field",
            viewModelSource.contains("val message: String?")
        )
    }

    @Test
    fun `NeoAgentCard receives AgentActivityInfo not plain String`() {
        val cardBlock = agentListSource
            .substringAfter("fun NeoAgentCard(")
            .substringBefore("{")
        assertTrue(
            "NeoAgentCard must accept AgentActivityInfo parameter",
            cardBlock.contains("AgentActivityInfo")
        )
    }

    @Test
    fun `NeoAgentCard displays activity message detail`() {
        val cardBlock = agentListSource
            .substringAfter("fun NeoAgentCard(")
            .substringBefore("@Composable")
        assertTrue(
            "NeoAgentCard must reference activityInfo.message for detail display",
            cardBlock.contains("activityInfo") && cardBlock.contains("message")
        )
    }

    @Test
    fun `socket event stores message in AgentActivityInfo`() {
        val observeBlock = viewModelSource
            .substringAfter("observeAgentActivities")
            .substringBefore("override fun onCleared")
        assertTrue(
            "observeAgentActivities must store event.data.message",
            observeBlock.contains("event.data.message")
        )
    }
}
