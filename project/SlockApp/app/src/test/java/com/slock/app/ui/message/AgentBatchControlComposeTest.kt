package com.slock.app.ui.message

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
        composeTestRule.onNodeWithText("STOP ALL AGENTS").assertExists()
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
        composeTestRule.onNodeWithText("Alpha").assertExists()
        composeTestRule.onNodeWithText("Beta").assertExists()
        composeTestRule.onNodeWithText("Gamma").assertExists()
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
        composeTestRule.onNodeWithText("CANCEL").assertExists()
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
        composeTestRule.onNodeWithText("RESUME ALL").assertExists()
        composeTestRule.onNodeWithText("KEEP STOPPED").assertExists()
    }

    @Test
    fun `Resume All sends SOS prompt with correction text via callback`() {
        var receivedPrompt: String? = null
        composeTestRule.setContent {
            AgentBatchControlSheet(
                agents = testAgents,
                channelName = "general",
                onDismiss = {},
                onStopAll = { it() },
                onResumeAllWithCorrection = { prompt, onSuccess ->
                    receivedPrompt = prompt
                    onSuccess()
                },
                onKeepStopped = {}
            )
        }
        composeTestRule.onNodeWithText("STOP ALL AGENTS").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("correctionTextField").performTextReplacement("Fix the frontend only")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("RESUME ALL").performClick()
        composeTestRule.waitForIdle()
        val expected = buildSosPrompt("general", "Fix the frontend only")
        assertEquals(expected, receivedPrompt)
    }

    @Test
    fun `Keep Stopped calls onKeepStopped callback`() {
        var keptStopped = false
        composeTestRule.setContent {
            AgentBatchControlSheet(
                agents = testAgents,
                channelName = "general",
                onDismiss = {},
                onStopAll = { it() },
                onResumeAllWithCorrection = { _, _ -> },
                onKeepStopped = { keptStopped = true }
            )
        }
        composeTestRule.onNodeWithText("STOP ALL AGENTS").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("KEEP STOPPED").performClick()
        composeTestRule.waitForIdle()
        assertTrue(keptStopped)
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
