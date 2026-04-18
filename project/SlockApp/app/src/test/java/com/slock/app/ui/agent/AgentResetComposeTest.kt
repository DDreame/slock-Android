package com.slock.app.ui.agent

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slock.app.data.model.Agent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentResetComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val activeAgent = Agent(
        id = "agent-1",
        name = "TestBot",
        model = "claude-sonnet-4-20250514",
        status = "active"
    )

    private val stoppedAgent = activeAgent.copy(status = "stopped")

    @Test
    fun `reset button is visible on agent detail for active agent`() {
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = activeAgent)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
    }

    @Test
    fun `reset button is visible on agent detail for stopped agent`() {
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = stoppedAgent)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
    }

    @Test
    fun `clicking reset button shows confirmation dialog`() {
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = activeAgent)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reset").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reset Agent").assertIsDisplayed()
        composeTestRule.onNodeWithText("RESET").assertIsDisplayed()
    }

    @Test
    fun `confirming reset triggers onResetAgent callback`() {
        var resetCalled = false
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = activeAgent),
                onResetAgent = { resetCalled = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reset").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("RESET").performClick()
        composeTestRule.waitForIdle()
        assertTrue("onResetAgent should be called after confirming", resetCalled)
    }
}
