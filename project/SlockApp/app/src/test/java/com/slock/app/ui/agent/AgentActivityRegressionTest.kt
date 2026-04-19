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

    private val storeSource = readSource(
        "src/main/java/com/slock/app/data/store/AgentStore.kt",
        "app/src/main/java/com/slock/app/data/store/AgentStore.kt"
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
    fun `AgentStore observes socket events and stores message in AgentActivityInfo`() {
        val storeBlock = storeSource
            .substringAfter("observeSocketEvents")
        assertTrue(
            "AgentStore must store event.data.message",
            storeBlock.contains("event.data.message") || storeBlock.contains("message = event.data.message")
        )
    }
}
