package com.slock.app.ui.agent

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentOnboardingComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `empty state shows first agent helper line`() {
        composeTestRule.setContent {
            AgentListScreen(
                state = AgentUiState(agents = emptyList()),
                onCreateAgent = { _, _, _, _, _, _, _ -> },
                onStartAgent = {},
                onStopAgent = {},
                onResetAgent = {},
                onDeleteAgent = {},
                onUpdateAgent = { _, _, _, _, _, _, _ -> },
                onDmAgent = {},
                onAgentClick = {},
                onNavigateBack = {},
                showHeader = false
            )
        }

        composeTestRule.onNodeWithText("No agents yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("You can edit prompts, runtime, and model after creation.")
            .assertIsDisplayed()
    }

    @Test
    fun `create feedback shows Toast and triggers consume callback`() {
        var feedbackConsumed = false

        composeTestRule.setContent {
            AgentListScreen(
                state = AgentUiState(createFeedbackMessage = "Agent created. Open a DM or configure it from the list."),
                onCreateAgent = { _, _, _, _, _, _, _ -> },
                onStartAgent = {},
                onStopAgent = {},
                onResetAgent = {},
                onDeleteAgent = {},
                onUpdateAgent = { _, _, _, _, _, _, _ -> },
                onDmAgent = {},
                onAgentClick = {},
                onConsumeCreateFeedback = { feedbackConsumed = true },
                onNavigateBack = {},
                showHeader = false
            )
        }

        composeTestRule.waitForIdle()

        assertTrue(
            "Toast should display create feedback message",
            ShadowToast.showedToast("Agent created. Open a DM or configure it from the list.")
        )
        assertTrue("create feedback should be consumed after showing Toast", feedbackConsumed)
    }
}
