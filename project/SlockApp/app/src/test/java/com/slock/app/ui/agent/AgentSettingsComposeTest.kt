package com.slock.app.ui.agent

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.slock.app.data.model.Agent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentSettingsComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testAgent = Agent(
        id = "agent-1",
        name = "TestBot",
        description = "A helpful bot",
        prompt = "Be helpful",
        status = "active",
        model = "claude-sonnet-4-20250514"
    )

    private data class UpdateCall(
        val agentId: String,
        val name: String?,
        val description: String?,
        val prompt: String?,
        val runtime: String?,
        val reasoningEffort: String?,
        val envVars: Map<String, String>?
    )

    private fun renderAndOpenSettings(
        agent: Agent = testAgent,
        onUpdateAgent: (String, String?, String?, String?, String?, String?, Map<String, String>?) -> Unit = { _, _, _, _, _, _, _ -> }
    ) {
        composeTestRule.setContent {
            AgentListScreen(
                state = AgentUiState(agents = listOf(agent)),
                onCreateAgent = { _, _, _, _, _, _, _ -> },
                onStartAgent = {},
                onStopAgent = {},
                onResetAgent = {},
                onDeleteAgent = {},
                onUpdateAgent = onUpdateAgent,
                onDmAgent = {},
                onAgentClick = {},
                onNavigateBack = {},
                showHeader = false
            )
        }
        composeTestRule.onNodeWithText("Config").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `settings sheet shows editable name field with current value`() {
        renderAndOpenSettings()
        composeTestRule.onNodeWithText("Agent Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("TestBot").assertIsDisplayed()
    }

    @Test
    fun `settings sheet shows editable description field`() {
        renderAndOpenSettings()
        composeTestRule.onNodeWithText("A helpful bot").assertIsDisplayed()
    }

    @Test
    fun `settings sheet shows editable prompt field`() {
        renderAndOpenSettings()
        composeTestRule.onNodeWithText("Be helpful").assertIsDisplayed()
    }

    @Test
    fun `settings sheet shows read-only model badge`() {
        renderAndOpenSettings()
        composeTestRule.onNodeWithText("Sonnet").assertIsDisplayed()
    }

    @Test
    fun `saving settings passes edited name to onUpdateAgent`() {
        var captured: UpdateCall? = null
        renderAndOpenSettings(
            onUpdateAgent = { id, name, desc, prompt, runtime, reasoning, envVars ->
                captured = UpdateCall(id, name, desc, prompt, runtime, reasoning, envVars)
            }
        )

        val nameFields = composeTestRule.onAllNodesWithText("TestBot")
        nameFields[0].performTextClearance()
        nameFields[0].performTextInput("NewName")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SAVE CONFIG").performClick()
        composeTestRule.waitForIdle()

        assertNotNull("onUpdateAgent must have been called", captured)
        assertEquals("agent-1", captured?.agentId)
        assertEquals("NewName", captured?.name)
    }
}
