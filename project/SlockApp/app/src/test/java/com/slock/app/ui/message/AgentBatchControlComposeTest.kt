package com.slock.app.ui.message

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slock.app.data.model.Agent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentBatchControlComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testAgents = listOf(
        Agent(id = "a1", name = "Alpha", status = "active"),
        Agent(id = "a2", name = "Beta", status = "active"),
        Agent(id = "a3", name = "Gamma", status = "stopped")
    )

    @Test
    fun `Phase 1 shows Agent Control title`() {
        composeTestRule.setContent {
            AgentBatchControlSheet(
                agents = testAgents,
                channelName = "general",
                onDismiss = {},
                onStopAll = {},
                onResumeAllWithCorrection = { _, _ -> },
                onKeepStopped = {}
            )
        }
        composeTestRule.onNodeWithText("Agent Control").assertIsDisplayed()
    }

    @Test
    fun `Phase 1 shows stop warning with channel name`() {
        composeTestRule.setContent {
            AgentBatchControlSheet(
                agents = testAgents,
                channelName = "general",
                onDismiss = {},
                onStopAll = {},
                onResumeAllWithCorrection = { _, _ -> },
                onKeepStopped = {}
            )
        }
        composeTestRule.onNodeWithText("This will immediately stop all running agents in #general")
            .assertIsDisplayed()
    }

    @Test
    fun `Phase 1 shows Stop All Agents button`() {
        composeTestRule.setContent {
            AgentBatchControlSheet(
                agents = testAgents,
                channelName = "general",
                onDismiss = {},
                onStopAll = {},
                onResumeAllWithCorrection = { _, _ -> },
                onKeepStopped = {}
            )
        }
        composeTestRule.onNodeWithText("STOP ALL AGENTS").assertIsDisplayed()
    }

    @Test
    fun `Phase 1 shows agent names`() {
        composeTestRule.setContent {
            AgentBatchControlSheet(
                agents = testAgents,
                channelName = "general",
                onDismiss = {},
                onStopAll = {},
                onResumeAllWithCorrection = { _, _ -> },
                onKeepStopped = {}
            )
        }
        composeTestRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gamma").assertIsDisplayed()
    }

    @Test
    fun `Phase 1 shows Cancel button`() {
        composeTestRule.setContent {
            AgentBatchControlSheet(
                agents = testAgents,
                channelName = "general",
                onDismiss = {},
                onStopAll = {},
                onResumeAllWithCorrection = { _, _ -> },
                onKeepStopped = {}
            )
        }
        composeTestRule.onNodeWithText("CANCEL").assertIsDisplayed()
    }

    @Test
    fun `Stop All button calls onStopAll and transitions to Phase 2`() {
        var stopped = false
        composeTestRule.setContent {
            AgentBatchControlSheet(
                agents = testAgents,
                channelName = "general",
                onDismiss = {},
                onStopAll = { onSuccess ->
                    stopped = true
                    onSuccess()
                },
                onResumeAllWithCorrection = { _, _ -> },
                onKeepStopped = {}
            )
        }
        composeTestRule.onNodeWithText("STOP ALL AGENTS").performClick()
        composeTestRule.waitForIdle()
        assertTrue(stopped)
        composeTestRule.onNodeWithText("All Agents Stopped").assertIsDisplayed()
    }

    @Test
    fun `Phase 2 shows correction text field and Resume All button`() {
        composeTestRule.setContent {
            AgentBatchControlSheet(
                agents = testAgents,
                channelName = "general",
                onDismiss = {},
                onStopAll = { it() },
                onResumeAllWithCorrection = { _, _ -> },
                onKeepStopped = {}
            )
        }
        composeTestRule.onNodeWithText("STOP ALL AGENTS").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("RESUME ALL").assertIsDisplayed()
        composeTestRule.onNodeWithText("KEEP STOPPED").assertIsDisplayed()
    }
}

class BuildSosPromptTest {

    @Test
    fun `buildSosPrompt includes SOS prefix`() {
        val result = buildSosPrompt("general", "fix the bug")
        assertTrue(result.startsWith("[SOS]"))
    }

    @Test
    fun `buildSosPrompt includes channel name`() {
        val result = buildSosPrompt("engineering", "stop modifying schema")
        assertTrue(result.contains("#engineering"))
    }

    @Test
    fun `buildSosPrompt includes user correction text`() {
        val correction = "Focus only on frontend changes"
        val result = buildSosPrompt("general", correction)
        assertTrue(result.contains(correction))
    }

    @Test
    fun `buildSosPrompt includes instructions for agents`() {
        val result = buildSosPrompt("general", "fix it")
        assertTrue(result.contains("check_messages"))
        assertTrue(result.contains("read_history"))
    }
}
