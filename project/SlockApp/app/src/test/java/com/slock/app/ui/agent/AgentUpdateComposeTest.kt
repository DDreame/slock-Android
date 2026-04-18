package com.slock.app.ui.agent

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.slock.app.data.model.Agent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentUpdateComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testAgent = Agent(
        id = "agent-1",
        name = "Test Agent",
        model = "claude-sonnet-4-6",
        status = "active",
        prompt = "You are a test agent"
    )

    @Test
    fun `Edit button is visible for active agent`() {
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = testAgent)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edit").assertExists()
    }

    @Test
    fun `Edit button is visible for stopped agent`() {
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = testAgent.copy(status = "stopped"))
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edit").assertExists()
    }

    @Test
    fun `clicking Edit opens settings sheet with Agent Settings title`() {
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = testAgent)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Agent Settings").assertExists()
    }

    @Test
    fun `clicking Edit opens sheet with SAVE CONFIG button`() {
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = testAgent)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SAVE CONFIG").assertExists()
    }

    @Test
    fun `SAVE CONFIG triggers onUpdateAgent callback`() {
        var updateCalled = false
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = testAgent),
                onUpdateAgent = { _, _, _, _, _, _ -> updateCalled = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SAVE CONFIG").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue("onUpdateAgent must be called when SAVE CONFIG is clicked", updateCalled)
    }

    @Test
    fun `edit sheet does not show DELETE AGENT button`() {
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = testAgent)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DELETE AGENT").assertDoesNotExist()
    }

    @Test
    fun `update feedback message shows Toast and triggers consume`() {
        var feedbackConsumed = false
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(
                    agent = testAgent,
                    updateFeedbackMessage = "Agent updated successfully"
                ),
                onConsumeUpdateFeedback = { feedbackConsumed = true }
            )
        }
        composeTestRule.waitForIdle()
        assertTrue(
            "Toast should display update feedback message",
            ShadowToast.showedToast("Agent updated successfully")
        )
        assertTrue("onConsumeUpdateFeedback should be called after showing Toast", feedbackConsumed)
    }

    @Test
    fun `existing action buttons still visible alongside Edit`() {
        composeTestRule.setContent {
            AgentDetailScreen(
                state = AgentDetailUiState(agent = testAgent)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DM").assertExists()
        composeTestRule.onNodeWithText("Edit").assertExists()
        composeTestRule.onNodeWithText("Reset").assertExists()
    }
}
