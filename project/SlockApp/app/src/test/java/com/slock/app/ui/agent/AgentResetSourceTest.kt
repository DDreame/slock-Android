package com.slock.app.ui.agent

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AgentResetSourceTest {

    private val vmSource: String = listOf(
        File("src/main/java/com/slock/app/ui/agent/AgentDetailViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/agent/AgentDetailViewModel.kt")
    ).first { it.exists() }.readText()

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/agent/AgentDetailScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/agent/AgentDetailScreen.kt")
    ).first { it.exists() }.readText()

    private val navSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `AgentDetailUiState has isResetting field`() {
        assertTrue(
            "AgentDetailUiState must have isResetting field",
            vmSource.contains("val isResetting")
        )
    }

    @Test
    fun `AgentDetailUiState has resetFeedbackMessage field`() {
        assertTrue(
            "AgentDetailUiState must have resetFeedbackMessage field",
            vmSource.contains("val resetFeedbackMessage")
        )
    }

    @Test
    fun `AgentDetailViewModel declares resetAgent method`() {
        assertTrue(
            "AgentDetailViewModel must have resetAgent()",
            vmSource.contains("fun resetAgent()")
        )
    }

    @Test
    fun `resetAgent calls agentRepository resetAgent`() {
        val resetBlock = vmSource.substringAfter("fun resetAgent()")
            .substringBefore("fun consumeResetFeedback()")
        assertTrue(
            "resetAgent must call agentRepository.resetAgent",
            resetBlock.contains("agentRepository.resetAgent")
        )
    }

    @Test
    fun `AgentDetailScreen accepts onResetAgent callback`() {
        val signature = screenSource.substringAfter("fun AgentDetailScreen(")
            .substringBefore(") {")
        assertTrue(
            "AgentDetailScreen must accept onResetAgent callback",
            signature.contains("onResetAgent")
        )
    }

    @Test
    fun `OverviewContent renders Reset action button`() {
        val overviewBlock = screenSource.substringAfter("fun OverviewContent(")
            .substringBefore("@Composable\nprivate fun ActivityLogContent(")
        assertTrue(
            "OverviewContent must render Reset button",
            overviewBlock.contains("\"Reset\"")
        )
    }

    @Test
    fun `NavHost wires onResetAgent to viewModel`() {
        assertTrue(
            "NavHost must wire onResetAgent to viewModel::resetAgent",
            navSource.contains("onResetAgent = viewModel::resetAgent")
        )
    }

    @Test
    fun `AgentDetailScreen consumes resetFeedbackMessage in UI`() {
        assertTrue(
            "AgentDetailScreen must reference resetFeedbackMessage to display feedback",
            screenSource.contains("resetFeedbackMessage")
        )
        assertTrue(
            "AgentDetailScreen must call onConsumeResetFeedback after showing feedback",
            screenSource.contains("onConsumeResetFeedback()")
        )
    }
}
